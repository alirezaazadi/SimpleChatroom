package Client.UserInterface;

import Client.Client;
import Client.ClientMessageBuilder;
import Client.Exceptions.SendingMessageToServerFailed;
import Client.FileSharing.ClientFileDownloader;
import Client.Utils;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Building custom cell for list views
 * it contains a button and label
 */
public class CustomCell extends ListCell<String> {

    private final Pane pane = new Pane();
    private final HBox hbox = new HBox();
    private final Label message = new Label();
    private final JFXButton downloadButton = new JFXButton();

    private final Logger logger = Logger.getLogger(CustomCell.class.getName());

    String lastItem;

    /**
     * Constructor
     *
     * @param mainWindowController
     * @param client
     */
    public CustomCell(MainWindowController mainWindowController, Client client) {
        super();
        hbox.getChildren().addAll(message, pane, downloadButton);
        HBox.setHgrow(pane, Priority.SOMETIMES);

        ImageView downloadIcon = new ImageView();
        downloadIcon.setImage(new Image(new File(
                Utils.rebuildPath("src\\Resources\\Icons\\download.png")
        ).toURI().toString()));

        //Adding image (Icon) for download button
        downloadButton.setGraphic(downloadIcon);

        //Adding style to download button
        downloadButton.setStyle("-fx-background-radius: 5em;");

        /**
         * If clicked on the download button,
         * it will run a thread to download it from server,
         * before it, send download request message to server
         * to it starts it's file uploader service.
         */
        downloadButton.setOnAction(event -> {
            String text = message.getText();
            String fileName = text.substring(text.indexOf(":") + 1, text.lastIndexOf("["));
            String source = text.substring(text.indexOf("[") + 1, text.indexOf("]"));
            long fileLength = Long.parseLong(text.substring(text.lastIndexOf("[") + 1,
                    text.lastIndexOf("]")));
            try {
                client.sendMessageToServer(ClientMessageBuilder.downloadRequest(fileName, source));

                DirectoryChooser chooseDirectory = new DirectoryChooser();
                chooseDirectory.setTitle("Choose a save location");
                File saveLocation = chooseDirectory.showDialog(new Stage());

                new Thread(new ClientFileDownloader(mainWindowController, client, fileName,
                        fileLength, saveLocation)).start();

            } catch (SendingMessageToServerFailed e) {
                logger.error(e.getMessage());
                Platform.runLater(() -> mainWindowController.logOut(event));
            }

        });
    }


    /**
     * Explain and specify the manner of this custom cell
     * based on the situation.
     *
     * @param item
     * @param empty
     */
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        setText(null);  // No text in label of super class
        if (empty) {
            lastItem = null;
            setGraphic(null);
        } else {
            lastItem = item;
            message.setText(item != null ? item : "<null>");
            setGraphic(hbox);
        }
        if (message.getText().startsWith("*")) {
            downloadButton.setDisable(false);
            downloadButton.setVisible(true);
        } else {
            downloadButton.setDisable(true);
            downloadButton.setVisible(false);
        }
    }
}
