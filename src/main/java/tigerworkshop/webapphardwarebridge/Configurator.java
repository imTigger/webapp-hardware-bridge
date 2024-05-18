package tigerworkshop.webapphardwarebridge;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Configurator extends Application {
    private static final Logger logger = LoggerFactory.getLogger(Configurator.class);

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader loader = new FXMLLoader(Thread.currentThread().getContextClassLoader().getResource("setting.fxml"));

            Stage loginStage = new Stage();
            loginStage.setTitle("WebApp Hardware Bridge Configurator");
            loginStage.setScene(new Scene(loader.load()));
            loginStage.setResizable(false);
            loginStage.show();
        } catch (IOException e) {
            logger.error("Failed to open setting window", e);
        }
    }
}
