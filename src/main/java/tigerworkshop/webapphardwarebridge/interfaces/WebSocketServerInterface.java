package tigerworkshop.webapphardwarebridge.interfaces;


public interface WebSocketServerInterface {
    void onDataReceived(WebSocketServiceInterface service, String message);
}
