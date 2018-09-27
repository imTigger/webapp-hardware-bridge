package tigerworkshop.webapphardwarebridge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.sun.management.OperatingSystemMXBean;
import it.sauronsoftware.junique.AlreadyLockedException;
import it.sauronsoftware.junique.JUnique;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.services.SerialService;

import java.io.FileReader;
import java.lang.management.ManagementFactory;
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

        try {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("setting.json"));
            JsonElement element = gson.fromJson(reader, JsonElement.class);

            int port = element.getAsJsonObject().get("setting").getAsJsonObject().get("port").getAsInt();
            JsonObject serials = element.getAsJsonObject().get("serials").getAsJsonObject();


            BridgeWebSocketServer webSocketServer = null;
            try {
                webSocketServer = new BridgeWebSocketServer(port);

                for (Map.Entry<String, JsonElement> elem : serials.entrySet()) {
                    webSocketServer.addSerialMapping(elem.getKey(), elem.getValue().getAsString());
                }

                webSocketServer.start();
                logger.info("WebSocket started on port: " + webSocketServer.getPort());
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }

            for (Map.Entry<String, JsonElement> elem : serials.entrySet()) {
                SerialService serialService = new SerialService(webSocketServer, elem.getValue().getAsString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
