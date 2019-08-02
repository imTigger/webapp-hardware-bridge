package tigerworkshop.webapphardwarebridge.websocketservices;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.SettingService;

import java.net.URI;
import java.net.URISyntaxException;

public class CloudProxyClientWebSocketService implements WebSocketServiceInterface {
    CloudProxyWebSockerClient client;
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketServerInterface server = null;
    private SettingService settingService = SettingService.getInstance();

    public CloudProxyClientWebSocketService() {
        logger.info("Starting ProxyClientWebSocketService");

        try {
            client = new CloudProxyWebSockerClient(new URI(settingService.getSetting().getCloudProxyUrl()));
            client.connect();
        } catch (URISyntaxException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onDataReceived(String message) {
        logger.info("ProxyClientWebSocketService onDataReceived: " + message);
        client.send(message);
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
        client.setServer(server);
    }

    public static class CloudProxyWebSockerClient extends WebSocketClient {
        private WebSocketServerInterface server;
        private Logger logger = LoggerFactory.getLogger(getClass());

        CloudProxyWebSockerClient(URI serverUri) {
            super(serverUri);
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            logger.info("ProxyClientWebSocketService connection opened");
        }

        @Override
        public void onMessage(String message) {
            logger.info("ProxyClientWebSocketService onMessage:" + message);
            server.onDataReceived("proxy", message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            logger.info("ProxyClientWebSocketService connection closed, reason: " + reason);
        }

        @Override
        public void onError(Exception ex) {
            logger.info("ProxyClientWebSocketService connection error", ex);
        }

        public void setServer(WebSocketServerInterface server) {
            this.server = server;
        }
    }

}
