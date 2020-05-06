package Client;

import Client.UserInterface.MainWindowController;
import Message.Message;
import javafx.application.Platform;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import static Message.MessageTypes.*;

public class ClientMessageReader implements Runnable {

    private final Socket server;
    private final Logger logger;
    private final Client client;
    private final MainWindowController controller;
    private final ClientMessageParser messageParser;

    private DataInputStream fromServer;

    private boolean readerIsOpen = true;

    /**
     * Constructor .
     *
     * @param server
     * @param controller
     * @param client
     */
    ClientMessageReader(Socket server, MainWindowController controller, Client client) {
        this.server = server;
        this.client = client;
        this.controller = controller;
        this.messageParser = new ClientMessageParser("");
        this.logger = LogManager.getLogger(ClientMessageReader.class.getName());

    }

    /**
     * It waits for server message and after receiving message from it,
     * tries to pars the message (by calling ClientMessageParser) and send it to
     * MainViewController.
     */
    @Override
    public void run() {
        String message;

        //Try to check if it can establish a connection or not, if
        //it was successful, reading process will start.
        try {
            fromServer = new DataInputStream(server.getInputStream());
        } catch (IOException e) {
            logger.fatal("Couldn't get input stream from server socket");
            readerIsOpen = false;
            client.logOut();
        }

        if (readerIsOpen)
            logger.debug("Now you can get your message!");

        while (readerIsOpen) {
            try {
                message = fromServer.readUTF();
                try {
                    Message parsed = messageParser.parse(message);
                    handleMessage(parsed);
                } catch (Exception e) {
                    logger.fatal("Server don't work properly!");
                    logger.fatal(e.getMessage());
                }
            } catch (IOException e) {
                logger.fatal("Reading from Server was not successful!");
                logger.fatal(e.getMessage());
                readerIsOpen = false;
                logger.error("Server is not Accessible");
                logger.error("Client shunted down by force!");
                stop();
            }
        }
        logger.debug("Client reader is shutting down!");
    }

    /**
     * Based on the type of the parsed message, it calls
     * the proper function of MainWindowController.
     *
     * @param parsedMessage
     */
    private void handleMessage(Message parsedMessage) {
        if (parsedMessage.getType() == PRIVATE) {
            Platform.runLater(() -> controller.showPrivateMessage(parsedMessage));
        } else if (parsedMessage.getType() == PUBLIC) {
            Platform.runLater(() -> controller.showPublicMessage(parsedMessage));
        } else if (parsedMessage.getType() == RESPONSE) {
            Platform.runLater(() -> controller.showResponse(parsedMessage));
        } else if (parsedMessage.getType() == PRIVATE_DATA) {
            Platform.runLater(() -> controller.showPrivateData(parsedMessage));
        } else if (parsedMessage.getType() == PUBLIC_DATA) {
            Platform.runLater(() -> controller.showPublicData(parsedMessage));
        }
    }

    /**
     * It stops the reading process.
     */
    public void stop() {
        readerIsOpen = false;
        client.setOffline();
    }
}
