package tigerworkshop.webapphardwarebridge.interfaces;

import tigerworkshop.webapphardwarebridge.BridgeWebSocketServer;

public interface WebSocketServiceInterface {
    String getPrefix();

    void onDataReceived(String message);

    void setServer(BridgeWebSocketServer server);
}
