package Server;

import Message.Message;
import Message.Parser.Exceptions.*;
import Server.Exceptions.ReceivingFromClientWasFailed;
import Server.Exceptions.SendingToClientWasFailed;
import Server.FileSharing.ServerFileDownloader;
import Server.FileSharing.ServerFileUploader;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Message.MessageTypes.*;
import static Message.StaticMessages.*;
import static Message.Status.Status.*;

public class ServerWorker implements Runnable {

    private final Client client;
    private final Logger logger;
    private final ExecutorService fileDownloadingExecutor;
    private final ExecutorService fileUploadingExecutor;
    private final int FILE_SHARING_CAPACITY;

    private ServerMessageParser parser;

    private int downloadingInProgress = 0;
    private int uploadingInProgress = 0;
    private boolean isOnline = true;
    private boolean establishingConnectionState = true;

    public ServerWorker(Client client) {
        this.client = client;
        this.logger = LogManager.getLogger(ServerWorker.class.getName());

        this.FILE_SHARING_CAPACITY = client.getServer().getCAPACITY();
        this.fileDownloadingExecutor = Executors.newFixedThreadPool(FILE_SHARING_CAPACITY);
        this.fileUploadingExecutor = Executors.newFixedThreadPool(FILE_SHARING_CAPACITY);
    }

    /**
     * This method call when a new thread created for the client, it will run till
     * client request for log out or client connection be inaccessible.
     */
    @Override
    public void run() {
        String message;

        while (isOnline) {
            try {
                //Wait for client to send a message .
                logger.debug("Waiting for client message");
                message = client.receiveMessageFromClient();

                logger.debug("User Message : " + message);

                // If is the first message from client, will parse as the handshake request
                if (establishingConnectionState) {
                    /* If this handshake was valid (sign and user name) it will send
                       the welcome message to the client and broad cast this join to the
                       other online users.
                     */
                    if (establishingConnection(message)) {
                        welcomeMessages();
                    } else
                        rejectConnection();//If it was not valid, it will reject this connection.
                    establishingConnectionState = false;
                } else {
                    /* For the rest of time, parse any message from client to
                       to recognize the proper action for the request.
                     */
                    Message parsedMessage = parser.parse(message);
                    if (parsedMessage.getType() == PUBLIC_DATA || parsedMessage.getType() == PRIVATE_DATA)
                        handlingUploadMessages(parsedMessage);
                    else if (parsedMessage.getType() == PRIVATE)
                        handlingPrivateMessage(parsedMessage);
                    else if (parsedMessage.getType() == PUBLIC)
                        handlingPublicMessage(parsedMessage);
                    else if (parsedMessage.getType() == COMMAND)
                        handlingCommands(parsedMessage);
                    else if (parsedMessage.getType() == DL)
                        handlingDownloadRequest(parsedMessage);

                }
            } catch (MessageIsNotValid | MessageTypeIsNotValid | MessageLengthDoesNotExists | MessageIsTooLong |
                    ReceiverDoesNotExists e) {
                logger.error(e.getMessage());
            } catch (SendingToClientWasFailed | ReceivingFromClientWasFailed e) {
                //Any problem in connecting to client, will refuse the connection.
                forceLogout(e);
            }
        }
    }


    /**
     * If client requested to upload a file,
     * it will run it's file downloader service on a separate thread
     * not that a client can not have unlimited upload request.
     *
     * @param message
     * @throws SendingToClientWasFailed
     */
    private void handlingUploadMessages(Message message) throws SendingToClientWasFailed {
        if (downloadingInProgress < FILE_SHARING_CAPACITY) {
            fileDownloadingExecutor.execute(new ServerFileDownloader(this, client, message));
            downloadingInProgress++;
        } else
            client.sendMessageToClient(ServerMessageBuilder.responseBuilder(FILE_UPLOADING_REJECTED,
                    "You can't upload more file till previous files be done!"));
    }

    /**
     * If client requested to download a file,
     * it will run it's file uploader service on a separate thread
     * not that a client can not have unlimited download request.
     *
     * @param message
     * @throws SendingToClientWasFailed
     */
    private void handlingDownloadRequest(Message message) throws SendingToClientWasFailed {
        if (uploadingInProgress < FILE_SHARING_CAPACITY) {
            fileUploadingExecutor.execute(new ServerFileUploader(this, client, message));
            uploadingInProgress++;
        } else
            client.sendMessageToClient(ServerMessageBuilder.responseBuilder(FILE_DOWNLOADING_REJECTED,
                    "You can't download more file till previous files be done!"));
    }

    /**
     * If received message from source client type was COMMAND,
     * based on the message body, it will call the proper method.
     * if it wsa GET_USERS_LIST request, it will send the online users
     * to the client.
     *
     * @param message
     */
    private void handlingCommands(Message message) {
        try {
            if (message.getBody().equals(SIGN_OUT))
                signOut(false);
            else if (message.getBody().equals(GET_USERS_LIST))
                client.sendMessageToClient(ServerMessageBuilder.responseBuilder(LIST_PASSED,
                        ServerMessageBuilder.sendUsersList(client.getServer().getOnlineUsers())
                ));
        } catch (SendingToClientWasFailed e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * If received message from source client type was PUBLIC,
     * it will call broadCastMessage from server class to Broad cast the message
     * to all clients (except it self).
     *
     * @param message
     */
    private void handlingPublicMessage(Message message) {
        client.getServer().broadCastMessage(ServerMessageBuilder.publicMessage(message.getSafeBody(),
                message.getLength(), client.getUserName()));
    }

    /**
     * It will call if the message type was PRIVATE, and it sends the message
     * to receivers list which contains is message object.
     * if all of the receiver, received it's own message, it will send
     * SENDING_PRIVATE_MESSAGE_WAS_SUCCESSFUL message to source client otherwise
     * it will send a message which contains the list of not received clients.
     *
     * @param message
     */
    private void handlingPrivateMessage(Message message) {
        String[] notReceivedClients = client.getServer().
                sendPrivateMessage(ServerMessageBuilder.privateMessage(message.getSafeBody(),
                        message.getLength(), client.getUserName(),
                        message.getReceivers()), message.getReceivers());
        try {
            if (notReceivedClients.length == 0)
                client.sendMessageToClient(ServerMessageBuilder.
                        responseBuilder(SENDING_PRIVATE_MESSAGE_WAS_SUCCESSFUL, ""));
            else
                client.sendMessageToClient(ServerMessageBuilder.responseBuilder(
                        SENDING_PRIVATE_MESSAGE_WAS_NOT_SUCCESSFUL,
                        ServerMessageBuilder.convertListToString(notReceivedClients)
                ));
        } catch (SendingToClientWasFailed e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * It takes handshake message and if it was valid (validation check by
     * validateHandshake function), try to add the extracted username (by
     * extractingUserName function) to the set of clients by calling
     * addClient function from server class.
     * if it was not duplicate or valid (see user name validation of Client Manager class)
     * it will return true.
     *
     * @param handshake
     * @return
     */
    private boolean establishingConnection(String handshake) {
        if (Optional.ofNullable(handshake).isPresent()) {
            if (validateHandshake(handshake)) {
                if (client.getServer().addClient(client, extractingUserName(handshake))) {
                    parser = new ServerMessageParser(client.getUserName());
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Send a welcome message to client and all the other
     * clients on public chatroom .
     * and send handshake accept response to client to start it's job :)
     *
     * @throws SendingToClientWasFailed
     */
    private void welcomeMessages() throws SendingToClientWasFailed {
        client.sendMessageToClient(ServerMessageBuilder.responseBuilder(HANDSHAKE_ACCEPTED, ""));
        client.sendMessageToClient(ServerMessageBuilder.responseBuilder(HANDSHAKE_ACCEPTED,
                ServerMessageBuilder.handshakeAcceptResponse(client.getUserName())
        ));
        String joinMessage = ServerMessageBuilder.userJoiningInChatRoom
                (client.getUserName());
        String publicMessage = ServerMessageBuilder.publicMessage(
                joinMessage,
                joinMessage.length(),
                "Server"
        );
        client.getServer().broadCastMessage(publicMessage);
    }


    /**
     * Takes the handshake and validates it
     * this message format is : Hello<\Username\>
     * if it was valid , it will return true.
     *
     * @param handshake
     * @return
     */
    private boolean validateHandshake(String handshake) {
        try {
            String handshakeSign = handshake.substring(0, handshake.indexOf("<"));
            return handshakeSign.equals(HANDSHAKE_SIGN);
        } catch (IndexOutOfBoundsException e) {
            logger.debug(String.format("Passed handshake doesn't contain handshake sign, handshake: %s", handshake));
            return false;
        }
    }

    /**
     * If handshake was not valid or acceptable, this method send
     * the rejection code and close the connection.
     */
    private void rejectConnection() {
        isOnline = false;
        try {
            client.sendMessageToClient(ServerMessageBuilder.responseBuilder(HANDSHAKE_REJECTED, ""));
            client.getServer().updateCapacity(-1);
        } catch (SendingToClientWasFailed e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * If the client send sign out request, it will break message
     * sending and receiving process and send LOG_OUT message response to the
     * client and broad cast this log out to all of the actives clients.
     *
     * @throws SendingToClientWasFailed
     */
    private void signOut(boolean forcedSignOut) throws SendingToClientWasFailed {
        if (!forcedSignOut)
            client.sendMessageToClient(ServerMessageBuilder.responseBuilder(LOG_OUT, ""));
        isOnline = false;
        client.getServer().broadCastMessage(ServerMessageBuilder.responseBuilder(USER_LOGOUT,
                ServerMessageBuilder.logoutMessage(client.getUserName())
        ));
        try {
            client.disconnectClient();
            logger.debug("Client logged out!");
        } catch (IOException e) {
            logger.error("Logging of client (closing socket) was not successful!");
        }
    }

    /**
     * If there was any connection problem between the server
     * and the client, it will logout and remove the client
     * and close the client thread.
     *
     * @param e
     */
    private void forceLogout(Exception e) {
        logger.fatal(e.getMessage());
        logger.debug("This Connection forced close!");
        try {
            signOut(true);
        } catch (SendingToClientWasFailed ex) {
        }
    }

    /**
     * It take a handshake (with valid sign) and
     * will try to extract the username from it.
     * if it was available, it will return true.
     *
     * @param handshake
     * @return
     */
    private String extractingUserName(String handshake) {
        try {
            return handshake.substring(handshake.indexOf("<") + 1,
                    handshake.lastIndexOf(">"));
        } catch (IndexOutOfBoundsException e) {
            logger.debug(String.format("Passed handshake doesn't contain username, handshake: %s", handshake));
            return "";
        }

    }


    /**
     * change the value of server downloader thread
     */
    public synchronized void decreaseDownloadInProgress() {
        downloadingInProgress--;
    }

    /**
     * change the value of server uploader thread
     */
    public synchronized void decreaseUploadInProgress() {
        uploadingInProgress--;
    }

}
