package tigerworkshop.webapphardwarebridge;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.SerialListener;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.PrinterService;
import tigerworkshop.webapphardwarebridge.services.SerialService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BridgeWebSocketServer extends WebSocketServer implements SerialListener {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Gson gson = new Gson();

    private HashMap<String, ArrayList<WebSocket>> channelClientList = new HashMap<>();
    private HashMap<String, SerialService> serialServices = new HashMap<>();

    private String serialPrefix = "/serial/";
    private String printerPrefix = "/printer";

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
                PrintDocument[] printDocuments = gson.fromJson(message, PrintDocument[].class);
                for (PrintDocument printDocument : printDocuments) {
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

        if (conn.getResourceDescriptor().startsWith(serialPrefix)) {
            logger.info("Attempt to send: " + message);

            String mappingKey = conn.getResourceDescriptor().replace(serialPrefix, "");
            SerialService serialService = serialServices.get(mappingKey);
            if (serialService != null) {
                serialService.send(message.getBytes());
            } else {
                logger.warn("serialService is null");
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

    @Override
    public void onStart(SerialService serialService) {
        serialServices.put(serialService.getMappingKey(), serialService);
    }

    @Override
    public void onDataReceived(SerialService serialService, String receivedData) {
        ArrayList<WebSocket> clientList = channelClientList.get(serialPrefix + serialService.getMappingKey());

        if (clientList == null) {
            return;
        }

        for (Iterator<WebSocket> it = clientList.iterator(); it.hasNext(); ) {
            WebSocket conn = it.next();
            try {
                conn.send(receivedData);
            } catch (WebsocketNotConnectedException e) {
                logger.warn("WebsocketNotConnectedException: Removing client from list - " + conn.getRemoteSocketAddress());
                it.remove();
            }
        }
    }
}
