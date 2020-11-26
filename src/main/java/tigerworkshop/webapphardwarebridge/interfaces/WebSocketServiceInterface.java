package tigerworkshop.webapphardwarebridge.interfaces;

import java.nio.ByteBuffer;

public interface WebSocketServiceInterface {
    void setServer(WebSocketServerInterface server);

    void start();

    void stop();

    void onDataReceived(String message);

    void onDataReceived(ByteBuffer message);
}
