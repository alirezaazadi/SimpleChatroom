package Client.FileSharing;

import Client.Client;
import Client.UserInterface.MainWindowController;
import Client.Utils;
import Message.Message;
import javafx.application.Platform;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import static Message.MessageTypes.PRIVATE;

/**
 * This service is going to download the specific file
 * form the server. for this, it makes a Server socket (ONT SERVER PORT + 2 [E.G 15002] and wait
 * till the server file uploader connect to it, then receive it
 * from server and save it to the client system's.
 */
public class ClientFileDownloader implements Runnable {

    private final MainWindowController mainWindowController;
    private final Client client;
    private final String fileName;

    private final Logger logger;
    private final File saveLocation;
    private final int BUFFER_SIZE = 4096;
    private final long fileLength;

    /**
     * Constructor
     *
     * @param mainWindowController
     * @param client
     * @param fileName
     * @param fileLength
     * @param saveLocation
     */
    public ClientFileDownloader(MainWindowController mainWindowController, Client client, String fileName,
                                long fileLength, File saveLocation) {
        this.mainWindowController = mainWindowController;
        this.client = client;
        this.fileName = fileName;
        this.fileLength = fileLength;
        this.saveLocation = saveLocation;
        logger = Logger.getLogger(ClientFileDownloader.class.getName());
    }

    /**
     * Get a socket and returns a string with this format:
     * [HOST IP ADDRESS:PORT]
     * example: [192.168.1.4:15000]
     *
     * @param client
     * @return
     */
    private static String getClientInfo(Socket client) {
        return String.format("%s:%s", client.getInetAddress().getHostAddress(), client.getPort());
    }

    @Override
    public void run() {
        try {
            File file = buildDestFile();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");

            byte[] buffer = new byte[BUFFER_SIZE];

            int loopLimit = (int) (fileLength / BUFFER_SIZE);

            logger.debug("Waiting for Sever");
            Socket senderSocket = mainWindowController.getDownloaderSocket().accept();
            logger.debug("Server Connected :" + getClientInfo(senderSocket));

            sendingState(String.format("%s Downloading started!", fileName));

            DataInputStream fromSender = new DataInputStream(senderSocket.getInputStream());

            for (int i = 0; i < loopLimit; i++) {
                fromSender.read(buffer);
                randomAccessFile.write(buffer);
                randomAccessFile.skipBytes(BUFFER_SIZE);
            }

            buffer = new byte[(int) (fileLength - loopLimit * BUFFER_SIZE)];

            if (fromSender.read(buffer) != -1)
                randomAccessFile.write(buffer);

            randomAccessFile.close();
            logger.debug("File downloaded completely!");

            sendingState(String.format("%s download completed!", fileName));

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Return destination file object.
     *
     * @return
     * @throws SecurityException
     */
    private File buildDestFile() throws SecurityException {
        return new File(Utils.rebuildPath(String.format("%s\\%s", saveLocation.getAbsolutePath(),
                fileName)));
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
