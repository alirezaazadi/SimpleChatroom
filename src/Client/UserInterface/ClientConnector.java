package Client.UserInterface;

import Client.Client;
import Client.Exceptions.ClientInitializedBefore;
import Client.Exceptions.ServerIsNotAccessible;
import Client.Utils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class ClientConnector implements Runnable {

    private final OpeningWindowController controller;
    private final Logger logger;
    private final ActionEvent event;

    private Client client;
    private String userName;

    /**
     * Constructor.
     *
     * @param controller
     * @param event
     */
    ClientConnector(OpeningWindowController controller, ActionEvent event) {
        this.controller = controller;
        BasicConfigurator.configure();
        this.logger = LogManager.getLogger(ClientConnector.class.getName());
        this.event = event;
    }

    /**
     * It's validate username and port. the username validation first happens at the
     * client side and after that by calling isUserNameIsValid form Client.
     * if connection and other fields were okay, it's build and show the main panel.
     */
    @Override
    public void run() {
        String hostAddress = controller.getServerAddress();
        String portNumber = controller.getServerPort();

        int portValue;

        try {
            portValue = Integer.parseInt(portNumber);
        } catch (NumberFormatException e) {
            logger.error("PortNumber is not valid!");
            Platform.runLater(() -> controller.setServerPort("PortNumber is not valid!",
                    "-fx-text-fill: red;"));
            Platform.runLater(controller::resetLoginButton);
            return;
        }

        client = new Client(hostAddress, portValue);
        try {
            userName = controller.getUserName();
            if (OpeningWindowController.validateUserName(userName)) {
                try {
                    client.runClient(userName);
                    if (client.isUserNameIsValid())
                        Platform.runLater(this::openMainPage);
                    else {
                        logger.error("This username is not available!");
                        Platform.runLater(() -> controller.setUsernameTextField("This username is not available!",
                                "-fx-text-fill: red;"));
                        Platform.runLater(controller::resetLoginButton);
                    }
                } catch (ClientInitializedBefore e) {
                    logger.fatal(e.getMessage());
                }
            } else {
                logger.error("This username is not valid!");
                Platform.runLater(() -> controller.setUsernameTextField("This username is not valid!",
                        "-fx-text-fill: red;"));
                Platform.runLater(controller::resetLoginButton);
            }
        } catch (ServerIsNotAccessible e) {
            Platform.runLater(controller::setIsOffline);
            logger.error("Invalid data passed!");
        }
    }

    /**
     * Build and show the main panel and close login panel.
     */
    private void openMainPage() {
        try {
            Platform.runLater(controller::setConnected);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    Utils.rebuildPath("\\FXML\\main.fxml")
            ));
            Parent root = loader.load();
            MainWindowController mainWindowController = loader.getController();
            Stage mainWindowStage = new Stage();
            mainWindowStage.setTitle("Chatroom");
            mainWindowStage.setScene(new Scene(root));
            mainWindowStage.setResizable(false);
            mainWindowStage.getIcons().add(new Image(
                    new File(Utils.rebuildPath("src\\Resources\\Icons\\icon.png")).toURI().toString()));
            mainWindowStage.setTitle("Logged in as " + userName);
            mainWindowStage.setOnCloseRequest(mainWindowController::logoutAndExit);
            mainWindowStage.show();
            mainWindowController.setupClient(client);
            final Node source = (Node) event.getSource();
            final Stage currentStage = (Stage) source.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            logger.fatal("Can't load main panel!");
        }
    }
}
