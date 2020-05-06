package Server;

import Server.Exceptions.SendingToClientWasFailed;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Message.StaticMessages.SERVER_IS_OFFLINE;
import static Message.Status.Status.SERVER_ERROR;

public class Server {

    private final int MAIN_PORT;
    private final int FILE_SHARING_PORT;
    private final int CAPACITY;

    private final ExecutorService CLIENT_EXECUTOR;
    private final ClientsManager clientsManager;
    private final Logger logger;

    private int connectedClients = 0;

    private ServerSocket serverSocket;
    private boolean isOnline;
    private ServerSocket fileSharingSocket;


    public Server(int port, int capacity) {
        BasicConfigurator.configure();

        this.MAIN_PORT = port;
        this.FILE_SHARING_PORT = MAIN_PORT + 1;
        this.CAPACITY = capacity;
        this.CLIENT_EXECUTOR = Executors.newFixedThreadPool(CAPACITY);

        this.isOnline = true;
        this.logger = LogManager.getLogger(Server.class.getName());
        this.clientsManager = new ClientsManager(CAPACITY);
    }

    /**
     * Get a socket and returns a string with this format:
     * [HOST IP ADDRESS:PORT]
     * example: [192.168.1.4:15000]
     *
     * @param client
     * @return
     */
    public static String getClientInfo(Socket client) {
        return String.format("%s:%s", client.getInetAddress().getHostAddress(), client.getPort());
    }

    /**
     * Run Server based on the configuration.
     * if port number was not valid or was not available, it will not
     * run the server and set isOnline variable to false, so it can not
     * move into the while loop for accepting clients.
     * if port was valid, and server had enough capacity for new client
     * it will wait for connection request from a client after connecting a client
     * run a new thread (ServerWorker class) to a thread handle all of the client requests.
     *
     * @return
     */
    public void runServer() {
        initializeSocket();
        showCurrentState();

        while (isOnline) {

            try {

                Socket connectedClient = serverSocket.accept();

                if (connectedClients < CAPACITY) {

                    logger.debug(String.format("Client [%s] accepted", getClientInfo(connectedClient)));

                    CLIENT_EXECUTOR.execute(new ServerWorker(
                            new Client(this, connectedClient, fileSharingSocket)));

                    connectedClients++;

                    showCurrentState();
                }

            } catch (IOException e) {
                logger.error(String.format("There was a problem with connecting to the Client"));
                logger.error(e.toString());
            }
        }

        logger.debug("Server is ShutDown!");
    }

    /**
     * Initializing server socket.
     */
    private void initializeSocket() {
        try {
            serverSocket = new ServerSocket(MAIN_PORT);
            fileSharingSocket = new ServerSocket(FILE_SHARING_PORT);
        } catch (IOException e) {
            logger.fatal(String.format("Port %d is not usable!", MAIN_PORT));
            isOnline = false;
        }
    }

    private void showCurrentState() {
        int cap = CAPACITY - connectedClients;
        if (cap > 0)
            logger.debug(String.format("Server {%s} with {%d} client capacity is Listening on port {%d}.",
                    serverSocket.getInetAddress().getHostAddress(),
                    cap, MAIN_PORT));
        else
            logger.debug("Server is Full!");
    }

    /**
     * Shutdown server (By breaking while loop), setting connected clients
     * count more than CAPACITY and closing all resources and disconnecting all
     * clients by killing all of their threads using disconnectAllClients method.
     */
    protected void shutDownServer() {
        isOnline = false;
        connectedClients = CAPACITY + 1;
        try {
            disconnectAllClients();
            serverSocket.close();
        } catch (IOException e) {
            logger.fatal("Closing Server Socket was not successful");
        }
    }

    /**
     * Get a message and send it for all client
     * clients list will be given from clientManager
     *
     * @return
     */
    private void disconnectAllClients() {
        for (String userName : getOnlineUsers()) {
            sendMessageToClient(ServerMessageBuilder.responseBuilder(SERVER_ERROR, SERVER_IS_OFFLINE), userName);
            disconnectClient(userName);
        }
    }

    /**
     * it gets a Client object and userName string and call clientsManager.addClient to add this client
     * if user name was valid and usable, it returns true otherwise return false
     * note that if adding was successful, it will increase connectedClients by one.
     *
     * @param client
     * @param userName
     * @return
     */
    protected boolean addClient(Client client, String userName) {
        return clientsManager.addClient(client, userName);
    }

    /**
     * Get a message and send it for all client
     * clients list will be given from clientManager.
     *
     * @param message
     */
    public void broadCastMessage(String message) {
        for (String userName : clientsManager.getUserNameList())
            sendMessageToClient(message, userName);
    }

    /**
     * Get a message form a client and will try to send it to
     * the list of clients. and returns a list from clients which
     * couldn't receive message (or didn't save)
     *
     * @param message
     * @param receivers
     * @return
     */
    public String[] sendPrivateMessage(String message, String[] receivers) {
        LinkedList<String> rejectedList = new LinkedList<>();

        for (String receiver : receivers) {
            if (!sendMessageToClient(message, receiver))
                rejectedList.add(receiver);
        }
        String[] rejectedArray = new String[rejectedList.size()];
        rejectedList.toArray(rejectedArray);
        return rejectedArray;
    }

    /**
     * A Helper function used in "sendPrivateMessage" and "broadCastMessage"
     * for sending their message. it takes a message and a user name
     * and send this message to that user userName.
     * If couldn't send this message, it will return false otherwise it returns true.
     *
     * @param message
     * @param userName
     * @return
     */
    private boolean sendMessageToClient(String message, String userName) {
        try {
            Optional.ofNullable(clientsManager.getUser(userName)).get().sendMessageToClient(message);
        } catch (SendingToClientWasFailed | NoSuchElementException e) {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Get a username and removes it from client manager hashmap
     * by calling removeClient from ClientManager class.
     *
     * @param userName
     * @return
     */
    protected boolean disconnectClient(String userName) {
        if (clientsManager.removeClient(userName)) {
            updateCapacity(-1);
            return true;
        }
        return false;
    }

    /**
     * Returns a set of online user names by calling getUserNameList from
     * Client Manager class.
     *
     * @return
     */
    protected Set<String> getOnlineUsers() {
        return clientsManager.getUserNameList();
    }

    /**
     * Update capacity based on the input. it used in reject connection
     * in Client worker.
     *
     * @param number
     */
    protected synchronized void updateCapacity(int number) {
        connectedClients = +number;
        showCurrentState();
    }

    /**
     * Return the server capacity
     *
     * @return
     */
    public int getCAPACITY() {
        return CAPACITY;
    }

    /**
     * Return the server port
     *
     * @return
     */
    public int getMAIN_PORT() {
        return MAIN_PORT;
    }
}
