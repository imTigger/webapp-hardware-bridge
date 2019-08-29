package tigerworkshop.webapphardwarebridge.websocketservices;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.SettingService;

import java.net.URI;

public class CloudProxyClientWebSocketService implements WebSocketServiceInterface {
    WebSocketClient client;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketServerInterface server = null;
    private SettingService settingService = SettingService.getInstance();
    private Thread thread;

    public CloudProxyClientWebSocketService() {
        logger.info("Starting ProxyClientWebSocketService");
    }

    @Override
    public void start() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        logger.trace("ProxyClientWebSocketService initializing");

                        client = new WebSocketClient(new URI(settingService.getSetting().getCloudProxyUrl())) {
                            @Override
                            public void onOpen(ServerHandshake handshakedata) {
                                logger.info("ProxyClientWebSocketService connected to " + this.getURI() + ", timeout = " + settingService.getSetting().getCloudProxyTimeout());
                            }

                            @Override
                            public void onMessage(String message) {
                                if (message == null) return;
                                logger.info("ProxyClientWebSocketService onMessage:" + message);
                                server.onDataReceived("proxy", message);
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                logger.info("ProxyClientWebSocketService connection closed");
                            }

                            @Override
                            public void onError(Exception ex) {
                                logger.info("ProxyClientWebSocketService connection error: " + ex.getMessage());
                            }
                        };
                        client.setConnectionLostTimeout(settingService.getSetting().getCloudProxyTimeout().intValue());
                        client.connectBlocking();

                        logger.trace("ProxyClientWebSocketService initialized");

                        while (true) {
                            if (client.isClosed()) {
                                logger.info("ProxyClientWebSocketService Reconnecting");
                                break;
                            }
                            Thread.sleep(5000);
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public void stop() {
        logger.info("Stopping CloudProxyClientWebSocketService");
        thread.interrupt();
    }

    @Override
    public void onDataReceived(String message) {
        logger.info("ProxyClientWebSocketService onDataReceived: " + message);
        client.send(message);
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
    }
}
