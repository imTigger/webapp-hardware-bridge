package tigerworkshop.webapphardwarebridge;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.PrinterService;
import tigerworkshop.webapphardwarebridge.services.SerialService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BridgeWebSocketServer extends WebSocketServer implements WebSocketServerInterface {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Gson gson = new Gson();

    private HashMap<String, ArrayList<WebSocket>> channelClientList = new HashMap<>();
    private HashMap<String, SerialService> serialServices = new HashMap<>();

    private String serialPrefix = "/serial/";
    private String printerPrefix = "/printer";
    private ArrayList<WebSocketServiceInterface> services = new ArrayList<>();

    public BridgeWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String uri = handshake.getResourceDescriptor();

        ArrayList<WebSocket> clientList = channelClientList.get(uri);
        if (clientList == null) {
            clientList = new ArrayList<>();
        }
        clientList.add(conn);
        channelClientList.put(uri, clientList);

        if (uri.equals(printerPrefix)) {
            conn.send("Ready");
        }

        logger.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + handshake.getResourceDescriptor());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info(conn + " disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("onMessage: " + conn + ": " + message);

        if (conn.getResourceDescriptor().equals(printerPrefix)) {
            logger.info("Attempt to print: " + message);

            try {
                if (message.startsWith("[")) {
                    PrintDocument[] printDocuments = gson.fromJson(message, PrintDocument[].class);
                    for (PrintDocument printDocument : printDocuments) {
                        try {
                            DocumentService.getInstance().prepareDocument(printDocument);
                            PrinterService.getInstance().printDocument(printDocument);
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    }
                } else {
                    PrintDocument printDocument = gson.fromJson(message, PrintDocument.class);
                    try {
                        DocumentService.getInstance().prepareDocument(printDocument);
                        PrinterService.getInstance().printDocument(printDocument);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        for (WebSocketServiceInterface service : services) {
            if (conn.getResourceDescriptor().startsWith(service.getPrefix())) {
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

        ArrayList<WebSocket> clientList = channelClientList.get(service.getPrefix());

        if (clientList == null) {
            return;
        }

        for (Iterator<WebSocket> it = clientList.iterator(); it.hasNext(); ) {
            WebSocket conn = it.next();
            try {
                conn.send(message);
            } catch (WebsocketNotConnectedException e) {
                logger.warn("WebsocketNotConnectedException: Removing client from list - " + conn.getRemoteSocketAddress());
                it.remove();
            }
        }
    }
}
