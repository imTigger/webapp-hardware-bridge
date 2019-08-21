package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    void setServer(WebSocketServerInterface server);

    void start();

    void stop();

    void onDataReceived(String message);
}
