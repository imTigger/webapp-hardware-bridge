package tigerworkshop.webapphardwarebridge;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.NotificationListenerInterface;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class GUI extends Application implements NotificationListenerInterface {
    private static final Logger logger = LoggerFactory.getLogger(GUI.class);

    TrayIcon trayIcon;
    SystemTray tray;

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.launch();
    }

    public void launch() {
        Server server = new Server(this);

        // Create tray icon
        try {
            if (!SystemTray.isSupported()) {
                logger.warn("SystemTray is not supported");
                return;
            }

            final Image image = ImageIO.read(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("icon.png")));

            tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(image, Config.APP_NAME);

            // Create a pop-up menu components
            MenuItem settingItem = new MenuItem("Configurator");
            settingItem.addActionListener(e -> Platform.runLater(() -> {
                try {
                    Platform.setImplicitExit(false);
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/setting.fxml"));

                    Stage stage = new Stage();
                    stage.setTitle("WebApp Hardware Bridge Configurator");
                    stage.setScene(new Scene(loader.load()));
                    stage.setResizable(false);
                    stage.show();
                    stage.setOnHiding(event -> server.restart());
                } catch (
                        Exception ex) {
                    logger.error("Failed to open setting window", ex);
                }
            }));

            MenuItem logItem = new MenuItem("Log");
            logItem.addActionListener(e -> {
                try {
                    Desktop.getDesktop().open(new File("log"));
                } catch (
                        IOException ex) {
                    logger.error("Failed to open log folder", ex);
                }
            });

            MenuItem restartItem = new MenuItem("Restart");
            restartItem.addActionListener(e -> server.restart());

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));

            // Add components to pop-up menu
            final PopupMenu popup = new PopupMenu();
            popup.add(settingItem);
            popup.add(logItem);
            popup.addSeparator();
            popup.add(restartItem);
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            tray.add(trayIcon);

            notify(Config.APP_NAME, " is running in background!", TrayIcon.MessageType.INFO);
        } catch (Exception e) {
            logger.error("TrayIcon could not be added", e);
        }

        server.start();
    }

    public void notify(String title, String message, TrayIcon.MessageType messageType) {
        try {
            trayIcon.displayMessage(title, message, messageType);
        } catch (Exception e) {
            logger.error("Failed to display notification", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {

    }
}
