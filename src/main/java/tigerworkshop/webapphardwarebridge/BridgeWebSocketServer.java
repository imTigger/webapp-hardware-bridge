package tigerworkshop.webapphardwarebridge;

import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.utils.ConnectionAttachment;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BridgeWebSocketServer extends WebSocketServer implements WebSocketServerInterface {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private HashMap<String, ArrayList<WebSocket>> channelConnectionList = new HashMap<>();

    private ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

    public BridgeWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();
        connection.setAttachment(new ConnectionAttachment(uri, null));
        addConnectionToChannel(uri, connection);

        logger.info(connection.getRemoteSocketAddress().toString() + " connected to " + uri);
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        removeConnectionFromChannel(((ConnectionAttachment) connection.getAttachment()).getUri(), connection);
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
