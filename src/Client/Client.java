package Client;

import Client.Exceptions.ClientInitializedBefore;
import Client.Exceptions.ConnectionToServerNotEstablished;
import Client.Exceptions.SendingMessageToServerFailed;
import Client.Exceptions.ServerIsNotAccessible;
import Client.UserInterface.MainWindowController;
import Message.Message;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Message.Status.Status.HANDSHAKE_ACCEPTED;

public class Client {

    private final String serverAddress;
    private final int port;
    private final Logger logger;
    private final ExecutorService clientThreads;

    private DataOutputStream toServer;
    private Socket serverSocket;
    private ClientMessageReader reader;
    private String userName;
    private boolean userNameIsValid = false;

    private boolean isOnline = true;

    /**
     * Client class constructor
     *
     * @param serverAddress
     * @param port
     */
    public Client(String serverAddress, int port) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.clientThreads = Executors.newFixedThreadPool(1);

        BasicConfigurator.configure();
        this.logger = LogManager.getLogger(Client.class.getName());

    }

    /**
     * Completely disconnect client and stop reader thread.
     */
    public void logOut() {
        reader.stop();
        setOffline();
        clientThreads.shutdown();
    }

    /**
     * Return true if chosen username is valid.
     *
     * @return
     */
    public boolean isUserNameIsValid() {
        return userNameIsValid;
    }

    /**
     * It takes user name and start the client.
     * at the beginning validate the username by server by calling validateUserName method.
     *
     * @param userName
     * @throws ServerIsNotAccessible
     * @throws ClientInitializedBefore
     */
    public void runClient(String userName) throws ServerIsNotAccessible, ClientInitializedBefore {
        this.userName = userName;

        try {
            serverSocket = new Socket(serverAddress, port);
            toServer = new DataOutputStream(serverSocket.getOutputStream());

            if (validateUserName())
                userNameIsValid = true;
            else
                logger.debug("Username is not valid");
        } catch (IOException | SendingMessageToServerFailed | ConnectionToServerNotEstablished e) {
            logger.fatal(e.getMessage());
            throw new ServerIsNotAccessible("Server is not accessible!");
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

    }

    /**
     * If the user name was valid, runs the message reader thread
     * to get message from server.
     *
     * @param controller
     */
    public void runReader(MainWindowController controller) {
        if (isUserNameIsValid()) {
            reader = new ClientMessageReader(serverSocket, controller, this);
            clientThreads.execute(reader);
        }
    }

    /**
     * It takes a message from client (MainWindow controller class is the producer)
     * and send it to the server. note that this massage made by ClientMessageBuilder.
     *
     * @param message
     * @return
     * @throws SendingMessageToServerFailed
     */
    public boolean sendMessageToServer(String message) throws SendingMessageToServerFailed {
        if (Optional.ofNullable(toServer).isPresent()) {
            if (!message.equals("")) {
                try {
                    toServer.writeUTF(message);
                    toServer.flush();
                    return true;
                } catch (IOException e) {
                    logger.error("Couldn't send the message to server");
                    logger.error(e.getMessage());
                    throw new SendingMessageToServerFailed("Server Stream didn't created!");
                }
            } else return false;
        } else
            throw new SendingMessageToServerFailed("Server Stream didn't created!");
    }

    /**
     * First of all run method call this function.
     * it makes a connection and send the handshake to server and wait for the response,
     * and if there was not any connection problem, returns the handshake validation result
     * and the result change the userNameIsValid field.
     *
     * @return
     * @throws Exception
     */
    private boolean validateUserName() throws Exception {

        if (sendMessageToServer(ClientMessageBuilder.handshakeMessage(userName))) {
            if (Optional.ofNullable(serverSocket).isPresent()) {
                String serverResponse = new DataInputStream(serverSocket.getInputStream()).readUTF();
                ClientMessageParser parser = new ClientMessageParser(userName);
                Message parsed = parser.parse(serverResponse);
                return (parsed.getSafeBody().startsWith(HANDSHAKE_ACCEPTED));
            } else
                throw new ConnectionToServerNotEstablished("Server socket didn't initialize");
        } else
            return false;
    }

    /**
     * Only set false to isOnline variable.
     */
    public void setOffline() {
        isOnline = false;
    }

    /**
     * return isOnline variable to check if client is online or not.
     *
     * @return
     */
    public boolean isOnline() {
        return isOnline;
    }

    /**
     * Return the client username.
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    public Socket getServerSocket() {
        return serverSocket;
    }
}

