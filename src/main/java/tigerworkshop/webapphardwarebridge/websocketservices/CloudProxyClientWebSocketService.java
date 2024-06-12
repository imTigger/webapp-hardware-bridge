package tigerworkshop.webapphardwarebridge.websocketservices;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import tigerworkshop.webapphardwarebridge.interfaces.GUIListenerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.utils.ThreadUtil;

import java.awt.*;
import java.net.URI;

@Log4j2
public class CloudProxyClientWebSocketService implements WebSocketServiceInterface {
    private WebSocketClient client;
    private WebSocketServerInterface server = null;
    private final ConfigService configService = ConfigService.getInstance();
    private Thread thread;

    @Setter
    private GUIListenerInterface notificationListener;

    public CloudProxyClientWebSocketService() {
        log.info("Starting ProxyClientWebSocketService");
    }

    @Override
    public void start() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    try {
                        log.trace("ProxyClientWebSocketService initializing");

                        client = new WebSocketClient(new URI(configService.getConfig().getCloudProxy().getUrl())) {
                            @Override
                            public void onOpen(ServerHandshake handshakeData) {
                                log.info("ProxyClientWebSocketService connected to {}, timeout = {}", this.getURI(), configService.getConfig().getCloudProxy().getTimeout());
                            }

                            @Override
                            public void onMessage(String message) {
                                if (message == null) return;
                                log.info("ProxyClientWebSocketService onMessage:" + message);
                                server.onDataReceived("proxy", message);
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                log.info("ProxyClientWebSocketService connection closed");
                            }

                            @Override
                            public void onError(Exception ex) {
                                log.info("ProxyClientWebSocketService connection error: {}", ex.getMessage());
                            }
                        };
                        client.setConnectionLostTimeout(configService.getConfig().getCloudProxy().getTimeout());
                        client.connectBlocking();

                        log.trace("ProxyClientWebSocketService initialized");

                        while (true) {
                            if (client.isClosed()) {
                                notificationListener.notify("Cloud Proxy", "Connection lost, reconnection...", TrayIcon.MessageType.WARNING);
                                log.info("ProxyClientWebSocketService Reconnecting");
                                break;
                            }
                            ThreadUtil.silentSleep(5000);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });

        thread.start();
    }

    @Override
    public void stop() {
        log.info("Stopping CloudProxyClientWebSocketService");

        thread.interrupt();

        log.info("Stopped CloudProxyClientWebSocketService");
    }

    @Override
    public void onDataReceived(String message) {
        log.info("ProxyClientWebSocketService onDataReceived: {}", message);
        client.send(message);
    }

    @Override
    public void onDataReceived(byte[] message) {
        log.error("ProxyClientWebSocketService onDataReceived: binary data not supported");
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
    }
}
