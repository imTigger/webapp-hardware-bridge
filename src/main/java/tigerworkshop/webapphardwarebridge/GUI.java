package tigerworkshop.webapphardwarebridge;

import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.GUIListenerInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Objects;

@Log4j2
public class GUI implements GUIListenerInterface {
    private static final ConfigService configService = ConfigService.getInstance();

    private final WebSocketServer webSocketServer = new WebSocketServer(this);
    private final WebAPIServer webAPIServer = new WebAPIServer(this);
    private Config config = configService.getConfig();

    Desktop desktop = Desktop.getDesktop();
    TrayIcon trayIcon;
    SystemTray tray;

    public static void main(String[] args) {
        GUI gui = new GUI();
        gui.launch();
    }

    public void launch() {
        // Create tray icon
        try {
            if (!SystemTray.isSupported()) {
                log.warn("SystemTray is not supported");
                return;
            }

            final Image image = ImageIO.read(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("icon.png")));

            MenuItem settingItem = new MenuItem("Web UI");
            settingItem.addActionListener(e -> {
                try {
                    if (desktop == null || !desktop.isSupported(Desktop.Action.BROWSE)) {
                        throw new Exception("Desktop browse is not supported");
                    }

                    desktop.browse(new URI(config.getWebApiServer().getUri()));
                } catch (Exception ex) {
                    log.error("Failed to open Web UI", ex);
                }
            });

            MenuItem appDirectoryItem = new MenuItem("App Directory");
            appDirectoryItem.addActionListener(e -> {
                try {
                    if (desktop == null || !desktop.isSupported(Desktop.Action.OPEN)) {
                        throw new Exception("Desktop open is not supported");
                    }

                    desktop.open(new File("."));
                } catch (Exception ex) {
                    log.error("Failed to open log folder", ex);
                }
            });

            MenuItem logDirectoryItem = new MenuItem("Log Directory");
            logDirectoryItem.addActionListener(e -> {
                try {
                    if (desktop == null || !desktop.isSupported(Desktop.Action.OPEN)) {
                        throw new Exception("Desktop open is not supported");
                    }

                    desktop.open(new File("log"));
                } catch (Exception ex) {
                    log.error("Failed to open log folder", ex);
                }
            });

            MenuItem restartItem = new MenuItem("Restart");
            restartItem.addActionListener(e -> restart());

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));

            // Add components to pop-up menu
            final PopupMenu popupMenu = new PopupMenu();
            popupMenu.add(settingItem);
            popupMenu.addSeparator();
            popupMenu.add(appDirectoryItem);
            popupMenu.add(logDirectoryItem);
            popupMenu.addSeparator();
            popupMenu.add(restartItem);
            popupMenu.add(exitItem);

            trayIcon = new TrayIcon(image, Constants.APP_NAME);
            trayIcon.setPopupMenu(popupMenu);

            tray = SystemTray.getSystemTray();
            tray.add(trayIcon);

            notify(Constants.APP_NAME, " is running in background!", TrayIcon.MessageType.INFO);
        } catch (Exception e) {
            log.error("TrayIcon could not be added", e);
        }

        try {
            webSocketServer.start();
            webAPIServer.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void notify(String title, String message, TrayIcon.MessageType messageType) {
        try {
            trayIcon.displayMessage(title, message, messageType);
        } catch (Exception e) {
            log.error("Failed to display notification", e);
        }
    }

    @Override
    public void restart() {
        try {
            config = configService.getConfig();

            webSocketServer.stop();
            webSocketServer.start();

            webAPIServer.stop();
            webAPIServer.start();
        } catch (Exception e) {
            log.error("Failed to restart server", e);
        }
    }
}
