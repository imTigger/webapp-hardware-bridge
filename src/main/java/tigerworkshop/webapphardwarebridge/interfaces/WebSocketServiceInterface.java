package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    void start();

    void stop();

    void onDataReceived(String message);

    void onDataReceived(byte[] message);

    void onRegister(WebSocketServerInterface server);

    void onUnregister();

    String getChannel();
}
