package Client.FileSharing;

import Client.Client;
import Client.ClientMessageBuilder;
import Client.Exceptions.SendingMessageToServerFailed;
import Client.UserInterface.MainWindowController;
import Message.Message;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import static Message.MessageTypes.PRIVATE;

/**
 * This service is going to connect to server downloader socket
 * and upload the specific file to it. and show the state to the user.
 */
public class ClientFileUploader implements Runnable {

    private final MainWindowController mainWindowController;

    private final File file;
    private final boolean isPublic;
    private final int BUFFER_SIZE = 4096;
    private final String[] receivers;
    private final Client client;

    private final Logger logger;

    /**
     * Constructor.
     *
     * @param mainWindowController
     * @param client
     * @param file
     * @param isPublic
     * @param receivers
     */
    public ClientFileUploader(MainWindowController mainWindowController, Client client, File file, boolean isPublic,
                              String... receivers) {

        this.mainWindowController = mainWindowController;
        this.file = file;
        this.isPublic = isPublic;
        this.receivers = receivers;
        this.client = client;

        this.logger = Logger.getLogger(Client.class.getName());
    }

    @Override
    public void run() {
        /*
         *Based on the message state, it will build the sending message.
         */
        String message = (isPublic) ? ClientMessageBuilder.publicFileSendMessage(file.getName(), file.length()) :
                ClientMessageBuilder.privateFileSendMessage(file.getName(), file.length(), receivers);

        try {
            //Send the upload request to server to make it ready for downloading
            client.sendMessageToServer(message);
            Thread.sleep(1000);
            sendingFile();
        } catch (SendingMessageToServerFailed | InterruptedException e) {
            logger.error(e.getMessage());
        }


    }

    /**
     * Reading from file and send it iteratively to
     * the server downloader socket.
     */
    private void sendingFile() {
        try {

            Socket fileGetterSocket = new Socket(client.getServerSocket().getInetAddress(),
                    client.getServerSocket().getPort() + 1);

            DataOutputStream toServer = new DataOutputStream(fileGetterSocket.getOutputStream());

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

            byte[] buffer = new byte[BUFFER_SIZE];

            Thread.sleep(1000);

            sendingState(String.format("Sending %s started!", file.getName()));

            while (randomAccessFile.read(buffer) != -1) {
                toServer.write(buffer);
            }

            randomAccessFile.close();

            sendingState(String.format("%s Uploaded Successfully!", file.getName()));

        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage());
            sendingState(String.format("Sending %s to server was failed!", file.getName()));
        }
    }

    /**
     * Send the proper message to client private messages
     * from client.
     *
     * @param message
     */
    private void sendingState(String message) {
        Platform.runLater(() -> mainWindowController.showPrivateMessage(new Message(
                "Client",
                new String[]{client.getUserName()},
                message,
                message.length(),
                PRIVATE

        )));
    }
}
