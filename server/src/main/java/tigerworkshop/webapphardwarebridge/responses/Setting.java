package tigerworkshop.webapphardwarebridge.responses;

import java.util.HashMap;

public class Setting {
    int port;
    boolean fallbackToDefaultPrinter;
    HashMap<String, String> printers;
    HashMap<String, String> serials;

    public int getPort() {
        return port;
    }

    public boolean getFallbackToDefaultPrinter() {
        return fallbackToDefaultPrinter;
    }

    public HashMap<String, String> getPrinters() {
        return printers;
    }

    public HashMap<String, String> getSerials() {
        return serials;
    }

    public void setPrinters(HashMap<String, String> printers) {
        this.printers = printers;
    }

    public void setSerials(HashMap<String, String> serials) {
        this.serials = serials;
    }
}