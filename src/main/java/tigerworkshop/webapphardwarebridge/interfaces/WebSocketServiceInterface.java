package tigerworkshop.webapphardwarebridge.interfaces;

public interface WebSocketServiceInterface {
    void start();

    void stop();

    void messageToService(String message);

    void messageToService(byte[] message);

    void onRegister(WebSocketServerInterface server);

    void onUnregister();

    String getChannel();
}
