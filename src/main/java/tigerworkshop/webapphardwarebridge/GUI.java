package tigerworkshop.webapphardwarebridge;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.interfaces.NotificationListenerInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Objects;

@Log4j2
public class GUI extends Application implements NotificationListenerInterface {
    private static final ConfigService configService = ConfigService.getInstance();

    TrayIcon trayIcon;
    SystemTray tray;

    @Override
    public void start(Stage primaryStage) {

    }

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.launch();
    }

    public void launch() {
        WebSocketServer webSocketServer = new WebSocketServer(this);
        WebAPIServer webAPIServer = new WebAPIServer();

        // Create tray icon
        try {
            if (!SystemTray.isSupported()) {
                log.warn("SystemTray is not supported");
                return;
            }

            final Image image = ImageIO.read(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("icon.png")));

            tray = SystemTray.getSystemTray();
            trayIcon = new TrayIcon(image, Constants.APP_NAME);

            var desktop = Desktop.getDesktop();
            var config = configService.getConfig();

            MenuItem settingItem = new MenuItem("Web UI");
            settingItem.addActionListener(e -> {
                try {
                    if (desktop == null || !desktop.isSupported(Desktop.Action.BROWSE)) {
                        throw new Exception("Desktop browse is not supported");
                    }

                    desktop.browse(new URI(config.getWebApiServer().getUri()));
                } catch (
                        Exception ex) {
                    log.error("Failed to open Web UI", ex);
                }
            });

            MenuItem logItem = new MenuItem("Log");
            logItem.addActionListener(e -> {
                try {
                    if (desktop == null || !desktop.isSupported(Desktop.Action.OPEN)) {
                        throw new Exception("Desktop open is not supported");
                    }

                    desktop.open(new File("log"));
                } catch (
                        Exception ex) {
                    log.error("Failed to open log folder", ex);
                }
            });

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));

            // Add components to pop-up menu
            final PopupMenu popup = new PopupMenu();
            popup.add(settingItem);
            popup.add(logItem);
            popup.addSeparator();
            popup.add(exitItem);

            trayIcon.setPopupMenu(popup);

            tray.add(trayIcon);

            notify(Constants.APP_NAME, " is running in background!", TrayIcon.MessageType.INFO);
        } catch (Exception e) {
            log.error("TrayIcon could not be added", e);
        }

        try {
            webSocketServer.start();
            webAPIServer.start();
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void notify(String title, String message, TrayIcon.MessageType messageType) {
        try {
            trayIcon.displayMessage(title, message, messageType);
        } catch (Exception e) {
            log.error("Failed to display notification", e);
        }
    }
}
