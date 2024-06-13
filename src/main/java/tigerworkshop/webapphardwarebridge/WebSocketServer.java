package tigerworkshop.webapphardwarebridge;

import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.GUIInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.websocketservices.CloudProxyClientWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.PrinterWebSocketService;
import tigerworkshop.webapphardwarebridge.websocketservices.SerialWebSocketService;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

@Log4j2
public class WebSocketServer implements WebSocketServerInterface {
    private static final WebSocketServer server = new WebSocketServer(null);
    private static final ConfigService configService = ConfigService.getInstance();

    private Javalin javalinServer;

    private final HashMap<String, ArrayList<WsContext>> socketChannelSubscriptions = new HashMap<>();
    private final HashMap<String, ArrayList<WebSocketServiceInterface>> serviceChannelSubscriptions = new HashMap<>();
    private final ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

    private final GUIInterface guiInterface;

    public WebSocketServer(GUIInterface guiInterface) {
        this.guiInterface = guiInterface;
    }

    public static void main(String[] args) {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        Config config = configService.getConfig();
        Config.WebSocketServer webSocketConfig = config.getWebSocketServer();

        javalinServer = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
        });

        // Add Printer Service
        if (config.getPrinter().isEnabled() && !config.getPrinter().getMappings().isEmpty()) {
            PrinterWebSocketService printerWebSocketService = new PrinterWebSocketService(guiInterface);
            printerWebSocketService.start();

            javalinServer.ws(printerWebSocketService.getChannel(), ws -> {
                ws.onConnect(ctx -> {
                    log.info("{} connected to {}", ctx.host(), printerWebSocketService.getChannel());

                    addSocketToChannel(printerWebSocketService.getChannel(), ctx);
                });

                ws.onClose(ctx -> {
                    log.info("{} disconnected from {}", ctx.host(), printerWebSocketService.getChannel());

                    removeSocketFromChannel(printerWebSocketService.getChannel(), ctx);
                });

                ws.onMessage(ctx -> {
                    log.info("{} sent message to {}: {}", ctx.host(), printerWebSocketService.getChannel(), ctx.message());

                    processMessage("/printer", ctx.message());
                });
            });

            registerService(printerWebSocketService);
        }

        // Add Serial Service
        if (config.getSerial().isEnabled()) {
            config.getSerial().getMappings().forEach(mapping -> {
                try {
                    log.info("Starting SerialWebSocketService: {}", mapping.toString());
                    SerialWebSocketService serialWebSocketService = new SerialWebSocketService(guiInterface, mapping);
                    serialWebSocketService.start();

                    registerService(serialWebSocketService);

                    javalinServer.ws(serialWebSocketService.getChannel(), ws -> {
                        ws.onConnect(ctx -> {
                            log.info("{} connected to {}", ctx.host(), serialWebSocketService.getChannel());

                            addSocketToChannel(serialWebSocketService.getChannel(), ctx);
                        });

                        ws.onClose(ctx -> {
                            log.info("{} disconnected from {}", ctx.host(), serialWebSocketService.getChannel());

                            removeSocketFromChannel(serialWebSocketService.getChannel(), ctx);
                        });

                        ws.onMessage(ctx -> {
                            log.info("{} sent message to {}: {}", ctx.host(), serialWebSocketService.getChannel(), ctx.message());

                            processMessage(serialWebSocketService.getChannel(), ctx.message());
                        });
                    });
                } catch (Exception e) {
                    String message = "Failed to start SerialWebSocketService for " + mapping.getType() + ": " + e.getMessage();
                    log.error(message);

                    if (guiInterface != null) {
                        guiInterface.notify("Error", message, TrayIcon.MessageType.ERROR);
                    }
                }
            });
        }

        // Add Cloud Proxy Client Service
        if (config.getCloudProxy().isEnabled()) {
            CloudProxyClientWebSocketService cloudProxyClientWebSocketService = new CloudProxyClientWebSocketService(guiInterface);
            cloudProxyClientWebSocketService.start();

            registerService(cloudProxyClientWebSocketService);
        }

        javalinServer.start(webSocketConfig.getBind(), webSocketConfig.getPort());

        log.info("WebSocket started on {}", webSocketConfig.getUri());
    }

    public void stop() throws Exception {
        synchronized (this) {
            for (int i = 0; i < services.size(); i++) {
                services.get(i).stop();
            }
        }

        javalinServer.stop();
    }

    /*
     * Service to Server listener
     */
    @Override
    public void onDataReceived(String channel, String message) {
        log.trace("Received data from channel: {}, Data: {}", channel, message);

        if (channel.equals("proxy")) {
            processMessage("/printer", message);
        }

        ArrayList<WsContext> connectionList = socketChannelSubscriptions.get(channel);

        if (connectionList == null) {
            log.trace("connectionList is null, ignoring the message");
            return;
        }

        for (Iterator<WsContext> it = connectionList.iterator(); it.hasNext(); ) {
            WsContext conn = it.next();
            try {
                conn.send(message);
            } catch (Exception e) {
                log.warn("Exception: Removing connection from list");
                it.remove();
            }
        }
    }

    @Override
    public void registerService(WebSocketServiceInterface service) {
        service.onRegister(this);
        addServiceToChannel(service.getChannel(), service);
    }

    @Override
    public void unregisterService(WebSocketServiceInterface service) {
        service.onUnregister();
        removeServiceFromChannel(service.getChannel(), service);
    }

    /*
     * Message handler
     */
    private void processMessage(String channel, String message) {
        ArrayList<WebSocketServiceInterface> services = getServicesForChannel(channel);
        for (WebSocketServiceInterface service : services) {
            log.trace("Attempt to send: {} to channel: {}", message, channel);
            service.onDataReceived(message);
        }
    }

    private void processMessage(String channel, ByteBuffer blob) {
        ArrayList<WebSocketServiceInterface> services = getServicesForChannel(channel);
        for (WebSocketServiceInterface service : services) {
            log.trace("Attempt to send: {} to channel: {}", blob, channel);
            service.onDataReceived(blob.array());
        }
    }

    /*
     * Socket to Channel operations
     */
    private ArrayList<WsContext> getSocketsForChannel(String channel) {
        return socketChannelSubscriptions.getOrDefault(channel, new ArrayList<>());
    }

    void addSocketToChannel(String channel, WsContext socket) {
        ArrayList<WsContext> connectionList = getSocketsForChannel(channel);
        connectionList.add(socket);
        socketChannelSubscriptions.put(channel, connectionList);
    }

    private void removeSocketFromChannel(String channel, WsContext socket) {
        ArrayList<WsContext> connectionList = getSocketsForChannel(channel);
        connectionList.remove(socket);
        socketChannelSubscriptions.put(channel, connectionList);
    }

    /*
     * Service to Channel operations
     */
    private ArrayList<WebSocketServiceInterface> getServicesForChannel(String channel) {
        ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

        services.addAll(serviceChannelSubscriptions.getOrDefault(channel, new ArrayList<>()));
        services.addAll(serviceChannelSubscriptions.getOrDefault("*", new ArrayList<>()));

        return services;
    }

    private void addServiceToChannel(String channel, WebSocketServiceInterface service) {
        ArrayList<WebSocketServiceInterface> serviceList = serviceChannelSubscriptions.getOrDefault(channel, new ArrayList<>());

        serviceList.add(service);
        serviceChannelSubscriptions.put(channel, serviceList);

        if (!services.contains(service)) {
            services.add(service);
        }
    }

    private void removeServiceFromChannel(String channel, WebSocketServiceInterface service) {
        ArrayList<WebSocketServiceInterface> serviceList = getServicesForChannel(channel);
        serviceList.remove(service);
        serviceChannelSubscriptions.put(channel, serviceList);

        services.remove(service);
    }
}
