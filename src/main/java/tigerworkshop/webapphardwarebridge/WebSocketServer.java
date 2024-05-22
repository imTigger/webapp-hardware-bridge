package tigerworkshop.webapphardwarebridge;

import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.NotificationListenerInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.utils.CertificateGenerator;
import tigerworkshop.webapphardwarebridge.utils.TLSUtil;
import tigerworkshop.webapphardwarebridge.websocketservices.CloudProxyClientWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.PrinterWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.SerialWebSocketService;

@Log4j2
public class WebSocketServer {
    private static final WebSocketServer server = new WebSocketServer();
    private static final ConfigService configService = ConfigService.getInstance();
    private static final Config config = configService.getConfig();

    private BridgeWebSocketServer bridgeWebSocketServer;

    private NotificationListenerInterface notificationListener;

    public WebSocketServer() {

    }

    public WebSocketServer(NotificationListenerInterface notificationListener) {
        this.notificationListener = notificationListener;
    }

    public static void main(String[] args) {
        try {
            server.start();
        } catch (
                Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        var webSocketConfig = config.getWebSocketServer();

        // Create WebSocket Server
        bridgeWebSocketServer = new BridgeWebSocketServer(webSocketConfig.getBind(), webSocketConfig.getPort());
        bridgeWebSocketServer.setReuseAddr(true);
        bridgeWebSocketServer.setConnectionLostTimeout(3);

        // Add Serial Services;
        if (config.getSerial().isEnabled()) {
            for (var mapping : config.getSerial().getMappings()) {
                SerialWebSocketService serialWebSocketService = new SerialWebSocketService(mapping.getName(), mapping.getType());
                serialWebSocketService.setServer(bridgeWebSocketServer);
                serialWebSocketService.start();
            }
        }

        // Add Printer Service
        if (config.getPrinter().isEnabled() && !config.getPrinter().getMappings().isEmpty()) {
            PrinterWebSocketService printerWebSocketService = new PrinterWebSocketService();
            printerWebSocketService.setServer(bridgeWebSocketServer);
            printerWebSocketService.setNotificationListener(notificationListener);
            printerWebSocketService.start();
        }

        // Add Cloud Proxy Client Service
        if (config.getCloudProxy().isEnabled()) {
            CloudProxyClientWebSocketService cloudProxyClientWebSocketService = new CloudProxyClientWebSocketService();
            cloudProxyClientWebSocketService.setServer(bridgeWebSocketServer);
            cloudProxyClientWebSocketService.start();
        }

        // WSS/TLS Options
        if (webSocketConfig.getTls().isEnabled()) {
            if (webSocketConfig.getTls().isSelfSigned()) {
                log.info("TLS Enabled with self-signed certificate");
                CertificateGenerator.generateSelfSignedCertificate(webSocketConfig.getAddress(), webSocketConfig.getTls().getCert(), webSocketConfig.getTls().getKey());
                log.info("For first time setup, open in browser and trust the certificate: {}", webSocketConfig.getUri().replace("wss", "https"));
            }

            bridgeWebSocketServer.setWebSocketFactory(TLSUtil.getSecureFactory(webSocketConfig.getTls().getCert(), webSocketConfig.getTls().getKey(), webSocketConfig.getTls().getCaBundle()));
        }

        // Start WebSocket Server
        bridgeWebSocketServer.start();

        log.info("WebSocket started on {}", webSocketConfig.getUri());
    }
}
