package Client.UserInterface;

import Client.Utils;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class OpeningWindowController implements Initializable {

    public JFXButton loginButton = new JFXButton();

    @FXML
    public ImageView imageView;

    @FXML
    private JFXTextField usernameTextField;

    @FXML
    private JFXTextField serverAddress;

    @FXML
    private JFXTextField serverPort;

    @FXML
    private AnchorPane basePane;

    /**
     * Username validation in client side. username only can contains letters.
     *
     * @param userName
     * @return
     */
    protected static boolean validateUserName(String userName) {
        return (!userName.equals("") && (!userName.contains(",") && !userName.contains(":")
                && !userName.contains("<") && !userName.contains(">") && !userName.contains("=")
                && !userName.contains("\n") && !userName.contains("\r") && !userName.contains("\t")
                && !userName.contains(" "))) && userName.chars().allMatch(Character::isLetter);
    }

    /**
     * Initialize the fxml.
     *
     * @param location
     * @param resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        imageView.setImage(new Image(new File(Utils.rebuildPath("src\\Resources\\Icons\\main.png")).toURI().toString()));

        ImageView loginIcon = new ImageView();
        loginIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\login.png")
        ).toURI().toString()));
        loginButton.setGraphic(loginIcon);
    }

    /**
     * Clear serverAddress TextField after clicking on it.
     *
     * @param event
     */
    @FXML
    void clearServer(MouseEvent event) {
        serverAddress.clear();
    }

    /**
     * Clear usernameTextField after clicking on it.
     *
     * @param event
     */
    @FXML
    void clearUserName(MouseEvent event) {
        usernameTextField.clear();
    }

    /**
     * Clear serverPort TextField after clicking on it.
     *
     * @param mouseEvent
     */
    public void clearPort(MouseEvent mouseEvent) {
        serverPort.clear();
    }

    /**
     * After clicking to login button, it runs ClientMessageReader
     * and change the name of button.
     *
     * @param event
     */
    @FXML
    void login(ActionEvent event) {
        setIsConnecting();
        Thread connectorThread = new Thread(new ClientConnector(this, event));
        connectorThread.setDaemon(true);
        connectorThread.start();
    }

    /**
     * Change the server address text field value and styles.
     *
     * @param text
     * @param styles
     */
    protected void setServerAddress(String text, String... styles) {
        serverAddress.setText(text);
        if (Optional.ofNullable(styles).isPresent()) {
            for (String style : styles)
                serverAddress.setStyle(style);
        }
    }

    /**
     * Change the port text field value and styles.
     *
     * @param text
     * @param styles
     */
    protected void setServerPort(String text, String... styles) {
        serverPort.setText(text);
        if (Optional.ofNullable(styles).isPresent()) {
            for (String style : styles)
                serverPort.setStyle(style);
        }
    }

    /**
     * Change the username text field value and styles.
     *
     * @param text
     * @param styles
     */
    protected void setUsernameTextField(String text, String... styles) {
        usernameTextField.setText(text);
        if (Optional.ofNullable(styles).isPresent()) {
            for (String style : styles)
                usernameTextField.setStyle(style);
        }
    }

    /**
     * Returns serverAddress Text Field value.
     *
     * @return
     */
    protected String getServerAddress() {
        return serverAddress.getText();
    }

    /**
     * Returns serverPort Text Field value.
     *
     * @return
     */
    protected String getServerPort() {
        return serverPort.getText();
    }

    /**
     * Returns usernameTextField value.
     *
     * @return
     */
    protected String getUserName() {
        return usernameTextField.getText();
    }

    /**
     * Change login button text to "Connected!"
     */
    public void setConnected() {
        ImageView loginIcon = new ImageView();
        loginIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\login.png")
        ).toURI().toString()));
        loginButton.setGraphic(loginIcon);
        loginButton.setText("Connected");
    }

    /**
     * Change login button text to "Connecting ...!"
     */
    private void setIsConnecting() {
        loginButton.setText("Connecting ...");
    }

    /**
     * Change login button text to it's default value.
     */
    protected void resetLoginButton() {
        ImageView loginIcon = new ImageView();
        loginIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\login.png")
        ).toURI().toString()));
        loginButton.setGraphic(loginIcon);
        loginButton.setText("");
    }

    /**
     * Change login button text to "Connecting ...!"
     * and change the color of it to red.
     */
    protected void setIsOffline() {
        ImageView offline = new ImageView();
        offline.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\try.png")
        ).toURI().toString()));
        loginButton.setGraphic(offline);
        loginButton.setText("");
    }

    /**
     * If the user press the enter key and release it,
     * it will call the login function.
     *
     * @param keyEvent
     */
    public void callLogin(KeyEvent keyEvent) {
        if (keyEvent.getCode().equals(KeyCode.ENTER))
            login(new ActionEvent(basePane, keyEvent.getTarget()));
    }
}
