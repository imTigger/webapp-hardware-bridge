package tigerworkshop.webapphardwarebridge.interfaces;


public interface WebSocketServerInterface {
    void onDataReceived(String channel, String message);

    void subscribe(WebSocketServiceInterface service, String channel);

    void unsubscribe(WebSocketServiceInterface service, String channel);
}
