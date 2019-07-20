package tigerworkshop.webapphardwarebridge;

import com.sun.management.OperatingSystemMXBean;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.responses.Setting;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.TLSUtil;
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

        logger.debug("OS Name: " + System.getProperty("os.name"));
        logger.debug("OS Version: " + System.getProperty("os.version"));
        logger.debug("OS Architecture: " + System.getProperty("os.arch"));

        logger.debug("Java Version: " + System.getProperty("java.version"));
        logger.debug("Java Vendor: " + System.getProperty("java.vendor"));

        logger.debug("Available processors (cores): " + Runtime.getRuntime().availableProcessors());
        logger.debug("JVM Maximum memory (bytes): " + Runtime.getRuntime().maxMemory());
        logger.debug("System memory (bytes): " + ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize());

        SettingService settingService = SettingService.getInstance();
        Setting setting = settingService.getSetting();

        try {
            // Create WebSocket Server
            BridgeWebSocketServer webSocketServer = new BridgeWebSocketServer(setting.getBind(), setting.getPort());

            // Add Serial Services
            HashMap<String, String> serials = setting.getSerials();
            for (Map.Entry<String, String> elem : serials.entrySet()) {
                SerialWebSocketService serialWebSocketService = new SerialWebSocketService(elem.getValue(), elem.getKey());
                serialWebSocketService.setServer(webSocketServer);
            }

            // Add Printer Service
            PrinterWebSocketService printerWebSocketService = new PrinterWebSocketService();
            printerWebSocketService.setServer(webSocketServer);

            // WSS/TLS Options
            if (setting.getTLSEnabled()) {
                if (setting.getTLSSelfSigned()) {
                    logger.info("TLS Enabled with self-signed certificate");
                    TLSUtil.generateSelfSignedCertificate(setting.getAddress(), setting.getTLSCert(), setting.getTLSKey());
                    logger.info("For first time setup, open in browser and trust the certificate: " + setting.getUri().replace("wss", "https"));
                }

                webSocketServer.setWebSocketFactory(TLSUtil.getSecureFactory(setting.getTLSCert(), setting.getTLSKey()));
            }

            // Start WebSocket Server
            webSocketServer.start();

            logger.info("WebSocket started on port: " + webSocketServer.getPort() + ", " + setting.getUri());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
