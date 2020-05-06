package Server.FileSharing;

import Message.Message;
import Server.Client;
import Server.ServerMessageBuilder;
import Server.ServerWorker;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;

import static Message.MessageTypes.PUBLIC_DATA;

/**
 * This service is going to download the specific file
 * form the client. for this, it makes a Server socket (ONT SERVER PORT + 1 [E.G 15001] and wait
 * till the client file uploader connect to it, then receive it
 * from client and save it to the server system's.
 */
public class ServerFileDownloader implements Runnable {

    private final int BUFFER_SIZE = 4096;

    private final ServerWorker serverWorker;
    private final Client client;
    private final Message message;
    private final Logger logger;

    /**
     * Constructor.
     *
     * @param serverWorker
     * @param client
     * @param message
     */
    public ServerFileDownloader(ServerWorker serverWorker, Client client, Message message) {
        this.serverWorker = serverWorker;
        this.client = client;
        this.message = message;

        this.logger = Logger.getLogger(ServerFileDownloader.class.getName());
    }

    @Override
    public void run() {
        try {
            File file = buildDestFile();

            //If file exits, we remove it.
            if (file.exists())
                file.delete();

            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rws");

            byte[] buffer = new byte[BUFFER_SIZE];

            long fileLength = message.getLength();

            int loopLimit = (int) (fileLength / BUFFER_SIZE);

            logger.debug("Waiting for client");
            Socket senderSocket = client.getFileDownloaderSocket().accept();
            logger.debug("Client Connected :" + Server.Server.getClientInfo(senderSocket));
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
            logger.debug(String.format("File Saved at : %s", file.getAbsolutePath()));

            if (message.getType() == PUBLIC_DATA)
                client.getServer().broadCastMessage(ServerMessageBuilder.publicFileSendMessage(
                        message.getSender(),
                        message.getBody(),
                        fileLength
                ));

            else
                client.getServer().sendPrivateMessage(ServerMessageBuilder.privateFileSendMessage(
                        message.getSender(),
                        message.getBody(),
                        fileLength,
                        message.getReceivers()
                ), message.getReceivers());


        } catch (SecurityException | IOException e) {
            logger.error(e.getMessage());
        } finally {
            serverWorker.decreaseDownloadInProgress();
        }
    }

    /**
     * Build the destination folder and the return the result file.
     * "DownloadFiles/UserName/file"
     *
     * @return
     * @throws SecurityException
     */
    private File buildDestFile() throws SecurityException {
        File destinationFolder = new File(Utils.rebuildPath(String.format("%s\\DownloadedFiles\\%s",
                System.getProperty("user.dir"), message.getSender())));

        destinationFolder.mkdirs();

        return new File(Utils.rebuildPath(String.format("%s\\%s", destinationFolder.getAbsolutePath(),
                message.getBody())));
    }

}
