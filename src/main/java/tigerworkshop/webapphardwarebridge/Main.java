package tigerworkshop.webapphardwarebridge;

import com.sun.management.OperatingSystemMXBean;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.services.SerialService;
import tigerworkshop.webapphardwarebridge.services.SettingService;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Main {

    private static Logger logger = LoggerFactory.getLogger("Main");

    public static void main(String[] args) {
        boolean alreadyRunning;
        try {
            JUnique.acquireLock(Config.APP_ID);
            alreadyRunning = false;
        } catch (AlreadyLockedException e) {
            alreadyRunning = true;
            e.printStackTrace();
        }
        if (alreadyRunning) {
            return;
        }

        logger.info("Application Started");
        logger.info("Program Version: " + Config.VERSION);

        logger.info("OS Name: " + System.getProperty("os.name"));
        logger.info("OS Version: " +  System.getProperty("os.version"));
        logger.info("OS Architecture: " + System.getProperty("os.arch"));

        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Java Vendor: " + System.getProperty("java.vendor"));

        logger.info("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        logger.info("JVM Maximum memory (bytes): " + Runtime.getRuntime().maxMemory());
        logger.info("System memory (bytes): " + ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize());

        SettingService settingService = SettingService.getInstance();

        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }

        Image image = null;
        try {
            final URL url = Main.class.getClassLoader().getResource("blub.gif");
            image = ImageIO.read(url);
        } catch (Exception e) {

        }

        final PopupMenu popup = new PopupMenu();
        final TrayIcon trayIcon = new TrayIcon(image, Config.APP_NAME);
        final SystemTray tray = SystemTray.getSystemTray();

        // Create a pop-up menu components
        MenuItem settingItem = new MenuItem("Setting");
        settingItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(new File("setting.json"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        MenuItem logItem = new MenuItem("Log");
        logItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().open(new File("log"));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        //Add components to pop-up menu
        popup.add(settingItem);
        popup.add(logItem);
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }

        try {
            int port = settingService.getPort();
            HashMap<String, String> serials = settingService.getSerials();

            BridgeWebSocketServer webSocketServer = null;
            try {
                webSocketServer = new BridgeWebSocketServer(port);

                for (Map.Entry<String, String> elem : serials.entrySet()) {
                    SerialService serialService = new SerialService(webSocketServer, elem.getValue(), elem.getKey());
                }

                webSocketServer.start();
                logger.info("WebSocket started on port: " + webSocketServer.getPort());
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        trayIcon.displayMessage(Config.APP_NAME, "Service started", TrayIcon.MessageType.INFO);
    }
}
