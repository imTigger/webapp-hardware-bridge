package tigerworkshop.webapphardwarebridge.interfaces;

import java.awt.*;

public interface NotificationListenerInterface {
    void notify(String title, String message, TrayIcon.MessageType messageType);
}
