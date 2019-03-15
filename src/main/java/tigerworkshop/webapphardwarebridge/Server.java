package tigerworkshop.webapphardwarebridge;

import com.sun.management.OperatingSystemMXBean;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.websocketservices.PrinterWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.SerialWebSocketService;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

public class Server {

    private static Logger logger = LoggerFactory.getLogger("Server");

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
        logger.info("OS Version: " + System.getProperty("os.version"));
        logger.info("OS Architecture: " + System.getProperty("os.arch"));

        logger.info("Java Version: " + System.getProperty("java.version"));
        logger.info("Java Vendor: " + System.getProperty("java.vendor"));

        logger.info("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        logger.info("JVM Maximum memory (bytes): " + Runtime.getRuntime().maxMemory());
        logger.info("System memory (bytes): " + ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize());

        SettingService settingService = SettingService.getInstance();

        try {
            // Create WebSocket Server
            int port = settingService.getPort();
            BridgeWebSocketServer webSocketServer = new BridgeWebSocketServer(port);

            // Add Serial Services
            HashMap<String, String> serials = settingService.getSerials();
            for (Map.Entry<String, String> elem : serials.entrySet()) {
                SerialWebSocketService serialWebSocketService = new SerialWebSocketService(elem.getValue(), elem.getKey());
                webSocketServer.addService(serialWebSocketService);
            }

            // Add Printer Service
            PrinterWebSocketService printerWebSocketService = new PrinterWebSocketService();
            webSocketServer.addService(printerWebSocketService);

            // Start WebSocket Server
            webSocketServer.start();
            logger.info("WebSocket started on port: " + webSocketServer.getPort());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
