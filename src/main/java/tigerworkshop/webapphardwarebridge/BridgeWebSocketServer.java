package tigerworkshop.webapphardwarebridge;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.ConnectionAttachment;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BridgeWebSocketServer extends WebSocketServer implements WebSocketServerInterface {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private HashMap<String, ArrayList<WebSocket>> channelConnectionList = new HashMap<>();

    private ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

    private SettingService settingService = SettingService.getInstance();

    public BridgeWebSocketServer(String address, int port) {
        super(new InetSocketAddress(address, port));
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        try {
            String descriptor = handshake.getResourceDescriptor();

            URI uri = new URI(descriptor);
            String channel = uri.getPath();
            List<NameValuePair> params = URLEncodedUtils.parse(uri, Charset.forName("UTF-8"));
            String token = getToken(params);

            if (settingService.getTokenAuthenticationEnabled() && (token == null || !token.equals(settingService.getToken()))) {
                connection.close(CloseFrame.REFUSE, "Token Mismatch");
                return;
            }

            connection.setAttachment(new ConnectionAttachment(channel, params, token));
            addConnectionToChannel(channel, connection);

            logger.info(connection.getRemoteSocketAddress().toString() + " connected to " + channel);
        } catch (URISyntaxException e) {
            logger.error(connection.getRemoteSocketAddress().toString() + " error", e);
            connection.close();
        }
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        if (connection.getAttachment() != null) {
            removeConnectionFromChannel(((ConnectionAttachment) connection.getAttachment()).getChannel(), connection);
        }
        logger.debug(connection.getRemoteSocketAddress().toString() + " disconnected, reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        logger.info("onMessage: " + connection + ": " + message);

        for (WebSocketServiceInterface service : services) {
            if (connection.getResourceDescriptor().startsWith(service.getPrefix())) {
                logger.info("Attempt to send: " + message + " to prefix: " + service.getPrefix());
                service.onDataReceived(message);
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.error(ex.getMessage(), ex);
    }

    @Override
    public void onStart() {
        logger.info("BridgeWebSocketServer started");
        setConnectionLostTimeout(30);
    }

    public void addService(WebSocketServiceInterface service) {
        services.add(service);
        service.setServer(this);
    }

    @Override
    public void onDataReceived(WebSocketServiceInterface service, String message) {
        if (logger.isTraceEnabled()) {
            logger.trace("Received data from prefix: " + service.getPrefix() + ", Data: " + message);
        }

        ArrayList<WebSocket> connectionList = channelConnectionList.get(service.getPrefix());

        if (connectionList == null) {
            logger.trace("connectionList is null, ignoring the message");
            return;
        }

        for (Iterator<WebSocket> it = connectionList.iterator(); it.hasNext(); ) {
            WebSocket conn = it.next();
            try {
                conn.send(message);
            } catch (WebsocketNotConnectedException e) {
                logger.warn("WebsocketNotConnectedException: Removing connection from list");
                it.remove();
            }
        }
    }

    private String getToken(List<NameValuePair> params) {
        for (NameValuePair pair : params) {
            if (pair.getName().equals("access_token")) {
                return pair.getValue();
            }
        }
        return null;
    }

    private ArrayList<WebSocket> getConnectionListForChannel(String uri) {
        ArrayList<WebSocket> connectionList = channelConnectionList.get(uri);
        if (connectionList == null) {
            connectionList = new ArrayList<>();
        }
        return connectionList;
    }

    private void addConnectionToChannel(String uri, WebSocket conn) {
        ArrayList<WebSocket> connectionList = getConnectionListForChannel(uri);
        connectionList.add(conn);
        channelConnectionList.put(uri, connectionList);
    }

    private void removeConnectionFromChannel(String uri, WebSocket conn) {
        ArrayList<WebSocket> connectionList = getConnectionListForChannel(uri);
        connectionList.remove(conn);
        channelConnectionList.put(uri, connectionList);
    }
}
