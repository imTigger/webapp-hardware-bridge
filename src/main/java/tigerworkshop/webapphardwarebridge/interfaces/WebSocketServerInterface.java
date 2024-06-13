package tigerworkshop.webapphardwarebridge.interfaces;


public interface WebSocketServerInterface {
    void onDataReceived(String channel, String message);

    void registerService(WebSocketServiceInterface service);

    void unregisterService(WebSocketServiceInterface service);
}
