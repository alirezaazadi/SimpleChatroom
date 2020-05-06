package Server;

import Server.Exceptions.ReceivingFromClientWasFailed;
import Server.Exceptions.SendingToClientWasFailed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Client {

    private final Server server;
    private final ServerSocket fileDownloaderSocket;
    private final Socket clientSocket;

    private final DataOutputStream toClient;
    private final DataInputStream fromClient;

    private String userName;

    /**
     * Constructor. It holds all fields about a client.
     *
     * @param server
     * @param clientSocket
     * @param fileDownloaderSocket
     * @throws IOException
     */
    public Client(Server server, Socket clientSocket, ServerSocket fileDownloaderSocket) throws IOException {
        this.server = server;
        this.clientSocket = clientSocket;
        this.fileDownloaderSocket = fileDownloaderSocket;
        this.toClient = new DataOutputStream(clientSocket.getOutputStream());
        this.fromClient = new DataInputStream(clientSocket.getInputStream());

    }

    /**
     * Takes a message (Built by ServerMessageBuilder) and tries to send it to the client socket.
     *
     * @param message
     * @throws SendingToClientWasFailed
     */
    public void sendMessageToClient(String message) throws SendingToClientWasFailed {
        try {
            toClient.writeUTF(message);
            toClient.flush();

        } catch (IOException e) {
            throw new SendingToClientWasFailed(String.format("Sending message to client [%s] was not successful!",
                    Server.getClientInfo(clientSocket)));
        }
    }

    /**
     * It tries to receive a message from client.
     *
     * @return
     * @throws ReceivingFromClientWasFailed
     */
    public String receiveMessageFromClient() throws ReceivingFromClientWasFailed {
        try {
            return fromClient.readUTF();
        } catch (IOException e) {
            throw new ReceivingFromClientWasFailed(String.format("Receiving message from client [%s] was" +
                            " not successful!",
                    Server.getClientInfo(clientSocket)));
        }
    }


    /**
     * It close the client socket and call disconnectClient from server class.
     * not that it calls when a client requests for logout (or when the client closed
     * it's own program)
     *
     * @throws IOException
     */
    public void disconnectClient() throws IOException {
        clientSocket.close();
        server.disconnectClient(userName);
    }

    /**
     * Return server object.
     *
     * @return
     */
    public Server getServer() {
        return server;
    }

    /**
     * Return file downloader service socket
     *
     * @return
     */
    public ServerSocket getFileDownloaderSocket() {
        return fileDownloaderSocket;
    }

    /**
     * Return client ip address as string.
     *
     * @return
     */
    public String getIP() {
        return clientSocket.getInetAddress().getHostAddress();
    }

    /**
     * Return the client user name
     *
     * @return
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set a new username for client.
     *
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }
}
