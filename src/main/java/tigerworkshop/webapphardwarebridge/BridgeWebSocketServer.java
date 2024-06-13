package tigerworkshop.webapphardwarebridge;

import lombok.extern.log4j.Log4j2;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.utils.ConnectionAttachment;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Log4j2
public class BridgeWebSocketServer extends WebSocketServer implements WebSocketServerInterface {
    private final HashMap<String, ArrayList<WebSocket>> socketChannelSubscriptions = new HashMap<>();
    private final HashMap<String, ArrayList<WebSocketServiceInterface>> serviceChannelSubscriptions = new HashMap<>();
    private final ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

    private final ConfigService configService = ConfigService.getInstance();

    public BridgeWebSocketServer(String address, int port) {
        super(new InetSocketAddress(address, port));
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        try {
            String descriptor = handshake.getResourceDescriptor();

            URI uri = new URI(descriptor);
            String channel = uri.getPath();
            List<NameValuePair> params = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
            String token = params.stream().filter(pair -> pair.getName().equals("access_token")).findFirst().map(NameValuePair::getValue).orElse(null);

            // Refuse connection if authentication is enabled and token is not match
            Config.Authentication authConfig = configService.getConfig().getWebSocketServer().getAuthentication();
            if (authConfig.isEnabled() && (token == null || !token.equals(authConfig.getToken()))) {
                connection.close(CloseFrame.REFUSE, "Token Mismatch");
                return;
            }

            connection.setAttachment(new ConnectionAttachment(channel, params));
            addSocketToChannel(channel, connection);

            log.info("{} connected to {}", connection.getRemoteSocketAddress(), channel);
        } catch (URISyntaxException e) {
            log.error("{} error", connection.getRemoteSocketAddress(), e);
            connection.close();
        }
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        if (connection.getAttachment() != null) {
            removeSocketFromChannel(((ConnectionAttachment) connection.getAttachment()).getChannel(), connection);
        }
    }

    /*
     * Server to Service communication
     */

    @Override
    public void onMessage(WebSocket connection, String message) {
        log.trace("onMessage@String: {}: {}", connection.getRemoteSocketAddress(), message);

        String channel = ((ConnectionAttachment) connection.getAttachment()).getChannel();

        processMessage(channel, message);
    }

    @Override
    public void onMessage(WebSocket connection, ByteBuffer blob) {
        log.trace("onMessage@ByteBuffer: {}: {}", connection.getRemoteSocketAddress(), blob);

        String channel = ((ConnectionAttachment) connection.getAttachment()).getChannel();

        processMessage(channel, blob);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error(ex.getMessage(), ex);
    }

    @Override
    public void onStart() {
        log.info("BridgeWebSocketServer started");
        setConnectionLostTimeout(1);
    }

    public void stop() throws InterruptedException {
        log.info("BridgeWebSocketServer stopping");

        for (WebSocket socket : getConnections()) {
            socket.close();
        }

        synchronized (this) {
            for (int i = 0; i < services.size(); i++) {
                services.get(i).stop();
            }
        }

        super.stop();

        log.info("BridgeWebSocketServer stopped");
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

        ArrayList<WebSocket> connectionList = socketChannelSubscriptions.get(channel);

        if (connectionList == null) {
            log.trace("connectionList is null, ignoring the message");
            return;
        }

        for (Iterator<WebSocket> it = connectionList.iterator(); it.hasNext(); ) {
            WebSocket conn = it.next();
            try {
                conn.send(message);
            } catch (WebsocketNotConnectedException e) {
                log.warn("WebsocketNotConnectedException: Removing connection from list");
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
    private ArrayList<WebSocket> getSocketsForChannel(String channel) {
        return socketChannelSubscriptions.getOrDefault(channel, new ArrayList<>());
    }

    private void addSocketToChannel(String channel, WebSocket socket) {
        ArrayList<WebSocket> connectionList = getSocketsForChannel(channel);
        connectionList.add(socket);
        socketChannelSubscriptions.put(channel, connectionList);
    }

    private void removeSocketFromChannel(String channel, WebSocket socket) {
        ArrayList<WebSocket> connectionList = getSocketsForChannel(channel);
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
