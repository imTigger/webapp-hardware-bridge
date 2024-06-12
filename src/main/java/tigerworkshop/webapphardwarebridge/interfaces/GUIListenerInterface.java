package tigerworkshop.webapphardwarebridge.interfaces;

import java.awt.*;

public interface GUIListenerInterface {
    void notify(String title, String message, TrayIcon.MessageType messageType);
    void restart();
}
