package tigerworkshop.webapphardwarebridge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Configurator extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/setting.fxml"));

            Stage loginStage = new Stage();
            loginStage.setTitle("WebApp Hardware Bridge Configurator");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setResizable(false);
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
