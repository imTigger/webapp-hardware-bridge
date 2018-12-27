package tigerworkshop.webapphardwarebridge.interfaces;

import tigerworkshop.webapphardwarebridge.websocketservices.SerialWebSocketService;

public interface SerialListener {
    void onStart(SerialWebSocketService serialWebSocketService);

    void onDataReceived(SerialWebSocketService serialWebSocketService, String receivedData);
}
