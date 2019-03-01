package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    String getPrefix();

    void onDataReceived(String message);

    void setServer(WebSocketServerInterface server);
}
