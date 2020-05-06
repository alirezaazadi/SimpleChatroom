package Client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.File;

public class ClientRunner extends Application {


    public static void main(String[] args) {
        launch(args);
    }

    /**
     * It shows the login window :)
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource(
                Utils.rebuildPath("\\UserInterface\\FXML\\login.fxml")));
        primaryStage.setTitle("Chatroom Login");
        primaryStage.setScene(new Scene(root));
        primaryStage.setResizable(false);
        primaryStage.getIcons().add(new Image(
                new File(Utils.rebuildPath("src\\Resources\\Icons\\icon.png")).toURI().toString()));
        primaryStage.setOnCloseRequest(e -> Platform.exit());
        primaryStage.show();
    }


}
