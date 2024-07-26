package tigerworkshop.webapphardwarebridge.interfaces;

import java.awt.*;

public interface GUIInterface {
    void notify(String title, String message, TrayIcon.MessageType messageType);
    void restart();
}
