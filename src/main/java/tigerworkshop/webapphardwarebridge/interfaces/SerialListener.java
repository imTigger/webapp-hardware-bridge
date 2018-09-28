package tigerworkshop.webapphardwarebridge.interfaces;

import tigerworkshop.webapphardwarebridge.services.SerialService;

public interface SerialListener {
    void onStart(SerialService serialService);

    void onDataReceived(SerialService serialService, String receivedData);
}
