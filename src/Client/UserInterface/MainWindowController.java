package Client.UserInterface;

import Client.Client;
import Client.ClientMessageBuilder;
import Client.Exceptions.SendingMessageToServerFailed;
import Client.Utils;
import Message.Message;
import com.jfoenix.controls.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

import static Message.MessageTypes.BROADCAST;
import static Message.MessageTypes.RESPONSE;
import static Message.StaticMessages.GET_USERS_LIST;
import static Message.StaticMessages.SIGN_OUT;
import static Message.Status.Status.*;

public class MainWindowController implements Initializable {

    private final String SERVICE_IS_OUT_OF_ACCESS = "Server is offline";
    @FXML
    public JFXProgressBar uploadBar;
    @FXML
    private JFXListView<String> privateListView;
    @FXML
    private JFXButton logoutButton;
    @FXML
    private JFXListView<String> publicListView;
    @FXML
    private TextArea message;
    @FXML
    private JFXRadioButton publicRadio;
    @FXML
    private JFXRadioButton privateRadio;
    @FXML
    private JFXButton sendButton;
    @FXML
    private Label dragFile;
    @FXML
    private JFXButton getOnlineUsersButton;
    @FXML
    private JFXButton clearButton;
    @FXML
    private JFXListView<JFXCheckBox> onlineUsersListView;
    @FXML
    private JFXCheckBox selectAllCheckBox;
    @FXML
    private JFXScrollPane scrollPane;
    @FXML
    private JFXScrollPane privateMessageScroll;
    @FXML
    private JFXScrollPane publicMessageScrollPane;
    @FXML
    private Label userNameLabel;
    @FXML

    private ImageView userImage;
    private Logger logger;
    private Client client;
    private ServerSocket downloaderSocket;

    /**
     * Initializing the mian window and fxml file.
     *
     * @param location
     * @param resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        BasicConfigurator.configure();
        logger = LogManager.getLogger(MainWindowController.class.getName());

        ToggleGroup messagesGroup = new ToggleGroup();
        privateRadio.setToggleGroup(messagesGroup);
        publicRadio.setToggleGroup(messagesGroup);

        userImage.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\user.png")
        ).toURI().toString()));

        publicRadio.setSelected(true);
        ImageView sendIcon = new ImageView();
        sendIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\send.png")
        ).toURI().toString()));
        sendButton.setGraphic(sendIcon);

        ImageView clearIcon = new ImageView();
        clearIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\clear.png")
        ).toURI().toString()));
        clearButton.setGraphic(clearIcon);

        ImageView logoutIcon = new ImageView();
        logoutIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\logout.png")
        ).toURI().toString()));
        logoutButton.setGraphic(logoutIcon);

        ImageView refreshIcon = new ImageView();
        refreshIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\refresh.png")
        ).toURI().toString()));
        getOnlineUsersButton.setGraphic(refreshIcon);

        scrollPane.getStylesheets().add(this.getClass().getResource(
                Utils.rebuildPath("CSS\\scrollBarsStyle.css")).toExternalForm());

        privateMessageScroll.getStylesheets().add(this.getClass().getResource(
                Utils.rebuildPath("CSS\\scrollBarsStyle.css")).toExternalForm());

        publicMessageScrollPane.getStylesheets().add(this.getClass().getResource(
                Utils.rebuildPath("CSS\\scrollBarsStyle.css")).toExternalForm());

        publicListView.setCellFactory(param -> new CustomCell(this, client));
        privateListView.setCellFactory(param -> new CustomCell(this, client));
    }

    public ServerSocket getDownloaderSocket() {
        return downloaderSocket;
    }

    /**
     * After initializing, it's set client and call runReader
     * and tries to get online users from server.
     *
     * @param client
     */
    protected void setupClient(Client client) {
        this.client = client;
        this.client.runReader(this);
        Platform.runLater(() -> getOnlineUsers(new ActionEvent()));
        userNameLabel.setText(client.getUserName());
        try {
            this.downloaderSocket = new ServerSocket(client.getServerSocket().getPort() + 2);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * After clicking on getOnlineUsersButton, it sends
     * GET_USERS_LIST request to server.
     *
     * @param event
     */
    @FXML
    void getOnlineUsers(ActionEvent event) {
        try {
            client.sendMessageToServer(GET_USERS_LIST);
        } catch (SendingMessageToServerFailed e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * After clicking on clearButton, it cleans all of the private messages.
     *
     * @param event
     */
    @FXML
    void clearAllMessage(ActionEvent event) {
        privateListView.getItems().clear();
    }

    /**
     * Log out the clint and disable send and getOnlineUsersButton.
     * this process begin by sending SIGN_OUT to server and calling
     * logout function from client.
     *
     * @param event
     */
    @FXML
    public void logOut(ActionEvent event) {
        try {
            client.sendMessageToServer(SIGN_OUT);
            client.logOut();
        } catch (SendingMessageToServerFailed e) {
            logger.error(e.getMessage());
        } finally {
            disableButtons();
        }
    }

    /**
     * After clicking to sendButton, it takes the message
     * from message text area and based on the radio buttons, call the proper
     * message builder and call sendMessageToServer to send it's message.
     * if the server was not available, it log out the client.
     *
     * @param event
     */
    @FXML
    void sendMessage(ActionEvent event) {
        if (client.isOnline()) {
            String messageText = message.getText();
            if (!messageText.equals("")) {
                try {
                    if (publicRadio.isSelected()) {

                        client.sendMessageToServer(ClientMessageBuilder.publicMessage(
                                messageText, messageText.length()));

                    } else if (privateRadio.isSelected()) {
                        getOnlineUsers(new ActionEvent());
                        String[] receivers = getReceiversList();
                        if (receivers.length > 0)
                            client.sendMessageToServer(ClientMessageBuilder.privateMessageBuilder(messageText,
                                    messageText.length(), receivers));
                    }
                } catch (SendingMessageToServerFailed e) {
                    logger.error(e.getMessage());
                }
            } else
                logger.debug("Empty Message");
        } else {
            showPrivateMessage(new Message("Client", new String[]{client.getUserName()},
                    SERVICE_IS_OUT_OF_ACCESS, SERVICE_IS_OUT_OF_ACCESS.length(), RESPONSE));
            logOut(new ActionEvent());
        }
    }

    /**
     * If the selectAllCheckBox be chosen, based on the pre state,
     * choose all of the online users or deselect all of them.
     *
     * @param event
     */
    @FXML
    void selectAll(ActionEvent event) {
        if (selectAllCheckBox.isSelected()) {
            for (JFXCheckBox checkBox : onlineUsersListView.getItems())
                if (!checkBox.isDisabled())
                    checkBox.setSelected(true);
        } else {
            for (JFXCheckBox checkBox : onlineUsersListView.getItems())
                checkBox.setSelected(false);
        }
    }

    /**
     * Return an array of chosen receivers from onlineUsersListView.
     *
     * @return
     */
    private String[] getReceiversList() {
        ArrayList<String> list = new ArrayList<>(onlineUsersListView.getItems().size() * 2);
        onlineUsersListView.getItems().stream().filter(CheckBox::isSelected).filter(
                userName -> !userName.isDisabled()
        ).forEach(e -> list.add(e.getText()));
        return list.stream().toArray(String[]::new);
    }

    /**
     * It adds private message to privateListView.
     *
     * @param parsedMessage
     */
    public void showPrivateMessage(Message parsedMessage) {
        privateListView.getItems().add("[" + parsedMessage.getSender() + "]: " +
                beautifyMessage(parsedMessage.getSafeBody(), 40));
    }

    /**
     * It adds public message to publicListView.
     *
     * @param parsedMessage
     */
    public void showPublicMessage(Message parsedMessage) {
        if (parsedMessage.getSender().equals("Server"))
            getOnlineUsers(new ActionEvent());
        publicListView.getItems().add("[" + parsedMessage.getSender() + "]: " +
                beautifyMessage(parsedMessage.getSafeBody(), 80));
    }

    /**
     * It's handle the server response's and base on the status code
     * call showPublicMessage or showPrivateMessage.
     *
     * @param parsedMessage
     */
    public void showResponse(Message parsedMessage) {
        String[] parts = parsedMessage.getSafeBody().split(",", 2);
        if (parts.length == 2) {
            String status = parts[0];
            String newBody;
            if (parsedMessage.getSafeBody().startsWith(LIST_PASSED)) {
                addingOnlineUsers(parsedMessage);
            } else if (status.equals(LOG_OUT)) {
                newBody = "You logged out successfully";
                showPrivateMessage(
                        new Message(parsedMessage.getSender(), new String[]{},
                                newBody, newBody.length(), RESPONSE)
                );
            } else if (status.equals(USER_LOGOUT)) {
                getOnlineUsers(new ActionEvent());
                showPublicMessage(
                        new Message(parsedMessage.getSender(), new String[]{},
                                parts[1], parts[1].length(), BROADCAST)
                );
            } else if (status.equals(SENDING_PRIVATE_MESSAGE_WAS_SUCCESSFUL)) {
                newBody = "Private message(s) sent successfully";
                showPrivateMessage(
                        new Message(parsedMessage.getSender(), new String[]{},
                                newBody, newBody.length(), RESPONSE)
                );
            } else if (status.equals(SENDING_PRIVATE_MESSAGE_WAS_NOT_SUCCESSFUL)) {
                newBody = "These users didn't receive the private message" + parts[1];
                showPrivateMessage(
                        new Message(parsedMessage.getSender(), parsedMessage.getReceivers(),
                                newBody, newBody.length(), RESPONSE)
                );
            } else if (status.equals(HANDSHAKE_ACCEPTED)) {
                showPrivateMessage(
                        new Message(parsedMessage.getSender(), parsedMessage.getReceivers(),
                                parts[1], parts[1].length(), RESPONSE)
                );
            }
        } else
            logger.error("Server doesnt' work properly!");
    }

    /**
     * Adding Online Users to online users list :)
     *
     * @param parsedMessage
     */
    private void addingOnlineUsers(Message parsedMessage) {
        onlineUsersListView.getItems().clear();
        for (String userName : parsedMessage.getReceivers()) {
            JFXCheckBox checkBox = new JFXCheckBox(userName);
            if (userName.equals(client.getUserName()))
                checkBox.setDisable(true);
            onlineUsersListView.getItems().add(checkBox);

        }
        selectAllCheckBox.setSelected(false);
    }


    /**
     * It takes a message and break the message in to more than 1 line
     * bigger than passed limit.
     *
     * @param message
     * @param breakLength
     * @return
     */
    private String beautifyMessage(String message, int breakLength) {

        if (message.length() > breakLength) {
            StringBuilder resultMessage = new StringBuilder();

            for (char character : message.toCharArray()) {
                resultMessage.append(character);
                if (resultMessage.length() % breakLength == 0)
                    resultMessage.append("\n");
            }
            return resultMessage.toString();
        } else
            return message;
    }

    /**
     * If the user logs out,  disables buttons.
     */
    private void disableButtons() {
        getOnlineUsersButton.setDisable(true);
        sendButton.setDisable(true);
        logoutButton.setDisable(true);
    }

    /**
     * Close the program :)
     *
     * @param windowEvent
     */
    public void logoutAndExit(WindowEvent windowEvent) {
        System.exit(0);
    }

    /**
     * Do nothing :)
     *
     * @param event
     */
    @FXML
    void dragOver(DragEvent event) {
        if (event.isConsumed())
            return;
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            if (!files.isEmpty())
                event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }


    /**
     * If a file dragged and dropped, it will open conformation
     * window and will pass the file information to it.
     *
     * @param event
     */
    @FXML
    void onDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean state = false;
        if (db.hasFiles()) {
            state = true;
            List<File> files = db.getFiles();
            File input = files.get(0);
            if (input.isFile())
                loadConfirmPage(input, publicRadio.isSelected(), getReceiversList());
        }
        event.setDropCompleted(state);
        event.consume();
    }

    /**
     * Opening Conformation window and passing data to it.
     *
     * @param file
     * @param isPublic
     * @param receivers
     */
    private void loadConfirmPage(File file, boolean isPublic, String... receivers) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    Utils.rebuildPath("\\FXML\\confirmPage.fxml")
            ));
            Parent root = loader.load();
            ConfirmPageController confirmPageController = loader.getController();
            confirmPageController.setUp(this, client, file, isPublic, receivers);
            Stage ConfirmStage = new Stage();
            ConfirmStage.setTitle("Chatroom");
            ConfirmStage.setScene(new Scene(root));
            ConfirmStage.setResizable(false);
            ConfirmStage.getIcons().add(new Image(
                    new File(Utils.rebuildPath("src\\Resources\\Icons\\icon.png")).toURI().toString()));
            ConfirmStage.setTitle("Confirm?");
            ConfirmStage.setOnCloseRequest(confirmPageController::exit);
            ConfirmStage.show();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * It will show sent file message in private message list.
     * these file starts with *. e.g:
     * *[Sender]:FileName[FileSize]
     *
     * @param parsedMessage
     */
    public void showPrivateData(Message parsedMessage) {
        privateListView.getItems().add(
                String.format("*[%s]:%s[%d]", parsedMessage.getSender(),
                        parsedMessage.getBody(), parsedMessage.getLength())
        );
    }

    /**
     * It will show sent file message in public message list.
     * these file starts with *. e.g:
     * *[Sender]:FileName[FileSize]
     *
     * @param parsedMessage
     */
    public void showPublicData(Message parsedMessage) {
        publicListView.getItems().add(
                String.format("*[%s]:%s[%d]", parsedMessage.getSender(),
                        parsedMessage.getBody(), parsedMessage.getLength())
        );
    }
}
