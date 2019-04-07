package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    void onDataReceived(String message);
    void setServer(WebSocketServerInterface server);
}
