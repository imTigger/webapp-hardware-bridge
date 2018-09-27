package tigerworkshop.webapphardwarebridge;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.PrinterService;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class BridgeWebSocketServer extends org.java_websocket.server.WebSocketServer {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Gson gson = new Gson();

    private HashMap<String, ArrayList<WebSocket>> channelClientList = new HashMap<>();
    private HashMap<String, String> serialMappings = new HashMap<>();

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

        conn.setAttachment(uri);

        logger.info(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " connected to " + handshake.getResourceDescriptor());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        logger.info(conn + " disconnected");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        logger.info("onMessage: " + conn + ": " + message);

        if (conn.getAttachment().equals(printerPrefix)) {
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

    public void updateSerial(String s, String port) {
        ArrayList<WebSocket> clientList = channelClientList.get(getSerialChannel(port));

        if (clientList != null) {
            for (Iterator<WebSocket> it = clientList.iterator(); it.hasNext(); ) {
                WebSocket conn = it.next();
                try {
                    conn.send(s);
                } catch (WebsocketNotConnectedException e) {
                    logger.warn("WebsocketNotConnectedException: Removing client from list - " + conn.getRemoteSocketAddress());
                    it.remove();
                }
            }
        }
    }

    public String getSerialChannel(String port) {
        String key = serialMappings.get(port);
        if (key == null) {
            return null;
        } else {
            return serialPrefix + key;
        }
    }

    public void addSerialMapping(String key, String port) {
        serialMappings.put(port, key);
    }
}
