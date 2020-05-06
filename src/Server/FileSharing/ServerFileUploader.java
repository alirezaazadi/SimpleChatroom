package Server.FileSharing;

import Message.Message;
import Server.Client;
import Server.Exceptions.SendingToClientWasFailed;
import Server.ServerMessageBuilder;
import Server.ServerWorker;
import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import static Message.Status.Status.FILE_REMOVED;

/**
 * This class going to upload the requested file to the client
 * it connects to client file download socket, and send the file to it.
 */
public class ServerFileUploader implements Runnable {

    private final ServerWorker serverWorker;
    private final Client client;
    private final Message message;

    private final Logger logger;

    private final int BUFFER_SIZE = 4096;

    public ServerFileUploader(ServerWorker serverWorker, Client client, Message message) {
        this.serverWorker = serverWorker;
        this.client = client;
        this.message = message;

        logger = Logger.getLogger(ServerFileUploader.class.getName());
    }

    @Override
    public void run() {
        try {

            //Wait for client to be ready
            Thread.sleep(1000);

            Socket uploadTo = new Socket(client.getIP(), client.getServer().getMAIN_PORT() + 2);

            File file = new File(Utils.rebuildPath(
                    String.format("%s\\DownloadedFiles\\%s\\%s",
                            System.getProperty("user.dir"),
                            message.getSender(),
                            message.getBody())
            ));

            DataOutputStream toClient = new DataOutputStream(uploadTo.getOutputStream());

            if (file.exists()) {

                logger.debug(String.format("Start uploading %s", file.getName()));

                if (!file.canRead())
                    logger.error("File is not readable :)");

                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");

                byte[] buffer = new byte[BUFFER_SIZE];

                while (randomAccessFile.read(buffer) != -1)
                    toClient.write(buffer);

                randomAccessFile.close();

                logger.debug(String.format("%s uploaded successfully!", file.getName()));

            } else {
                logger.debug(String.format("%s doesn't exists :)", file.getName()));
                logger.debug(file.getAbsolutePath());
                client.sendMessageToClient(ServerMessageBuilder
                        .responseBuilder(FILE_REMOVED, String.format("%s doesn't exists :)", file.getName())));
            }

        } catch (IOException | SendingToClientWasFailed | InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            serverWorker.decreaseUploadInProgress();
        }
    }
}
