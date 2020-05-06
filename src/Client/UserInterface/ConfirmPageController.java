package Client.UserInterface;

import Client.Client;
import Client.FileSharing.ClientFileUploader;
import Client.Utils;
import Message.Message;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import static Message.MessageTypes.PRIVATE;

/**
 * Show upload conformation window.
 */
public class ConfirmPageController implements Initializable {

    private static final long GIGA = 1_073_741_824L;
    private static final long MEGA = 1_048_576L;
    private static final long KILO = 1024L;

    @FXML
    private JFXButton discardButton;

    @FXML
    private JFXButton confirmButton;

    @FXML
    private JFXTextField fileName;

    @FXML
    private JFXTextField fileSize;

    @FXML
    private Label unit;

    @FXML
    private JFXTextField sendAs;

    @FXML
    private JFXTextField receivers;

    private Client client;
    private MainWindowController mainWindowController;

    private String[] list;
    private File file;


    private boolean isPublic;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    /**
     * If user clicked on confirm button, it will run a thread
     * to run file uploader service.
     *
     * @param event
     */
    @FXML
    void confirm(ActionEvent event) {
        new Thread(new ClientFileUploader(mainWindowController, client, file, isPublic, list)).start();
        final Node source = (Node) event.getSource();
        final Stage currentStage = (Stage) source.getScene().getWindow();

        currentStage.close();
    }

    /**
     * It will close the conformation window :)
     *
     * @param event
     */
    @FXML
    void discard(ActionEvent event) {
        final Node source = (Node) event.getSource();
        final Stage currentStage = (Stage) source.getScene().getWindow();

        String discard = String.format("Sending %s canceled!", file.getName());

        //And show a message from client which contains cancellation message
        mainWindowController.showPrivateMessage(new Message(
                "Client",
                new String[]{client.getUserName()},
                discard,
                discard.length(),
                PRIVATE

        ));

        currentStage.close();
    }

    /**
     * Close the window after to clock on window close button
     *
     * @param windowEvent
     */
    public void exit(WindowEvent windowEvent) {
        discard(new ActionEvent(discardButton, windowEvent.getTarget()));
    }

    /**
     * Takes below variables and set it to fields in window :)
     *
     * @param mainWindowController
     * @param client
     * @param file
     * @param isPublic
     * @param receivers
     */
    public void setUp(MainWindowController mainWindowController, Client client, File file,
                      boolean isPublic, String... receivers) {

        this.mainWindowController = mainWindowController;
        this.client = client;
        this.file = file;
        this.fileName.setText(file.getName());
        this.fileSize.setText(handlingFileSize(file.length()));
        this.isPublic = isPublic;
        this.sendAs.setText((isPublic) ? "Public" : "Private");
        if (!isPublic) {
            this.receivers.setText(Utils.convertListToString(receivers));
        } else
            this.receivers.setText("Public");
        this.list = receivers;

        if (!isPublic) {
            if (receivers.length < 1)
                confirmButton.setDisable(true);
        }
    }

    /**
     * Based on the file size, it will convert byte to
     * the readable volumes :)
     *
     * @param fileSize
     * @return
     */
    private String handlingFileSize(long fileSize) {
        if (fileSize < KILO) {
            unit.setText("B");
            return String.valueOf(fileSize);
        } else if (fileSize < MEGA) {
            unit.setText("KB");
            return String.format("%.2f", (float) fileSize / KILO);
        } else if (fileSize < GIGA) {
            unit.setText("MB");
            return String.format("%.2f", (float) fileSize / MEGA);
        } else {
            unit.setText("GB");
            return String.format("%.2f", (float) fileSize / GIGA);
        }
    }
}
