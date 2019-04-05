package tigerworkshop.webapphardwarebridge.responses;

import java.util.HashMap;

public class Setting {
    int port = 12212;
    String token = "";
    boolean fallbackToDefaultPrinter = false;
    boolean tokenAuthenticationEnabled = false;
    HashMap<String, String> printers = new HashMap<>();
    HashMap<String, String> serials = new HashMap<>();

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
    }

    public boolean getFallbackToDefaultPrinter() {
        return fallbackToDefaultPrinter;
    }

    public boolean getTokenAuthenticationEnabled() {
        return tokenAuthenticationEnabled;
    }

    public HashMap<String, String> getPrinters() {
        return printers;
    }

    public void setPrinters(HashMap<String, String> printers) {
        this.printers = printers;
    }

    public HashMap<String, String> getSerials() {
        return serials;
    }

    public void setSerials(HashMap<String, String> serials) {
        this.serials = serials;
    }
}