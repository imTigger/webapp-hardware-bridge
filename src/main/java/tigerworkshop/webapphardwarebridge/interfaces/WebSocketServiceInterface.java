package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    void setServer(WebSocketServerInterface server);

    void start();

    void onDataReceived(String message);
}
