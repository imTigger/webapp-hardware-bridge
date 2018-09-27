package tigerworkshop.webapphardwarebridge.services;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.responses.Setting;

import java.io.FileReader;
import java.util.HashMap;

public class SettingService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SettingService.class.getName());
    private static SettingService instance = new SettingService();
    private Setting setting = null;

    private SettingService() {
        try {
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader("setting.json"));
            setting = gson.fromJson(reader, Setting.class);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static SettingService getInstance() {
        return instance;
    }

    public int getPort() {
        return setting.getPort();
    }

    public boolean getFallbackToDefaultPrinter() {
        return setting.getFallbackToDefaultPrinter();
    }

    public HashMap<String, String> getSerials() {
        return setting.getSerials();
    }

    public HashMap<String, String> getPrinters() {
        return setting.getPrinters();
    }

    public String getMappedSerial(String key) {
        return setting.getSerials().get(key);
    }

    public String getMappedPrinter(String key) {
        return setting.getPrinters().get(key);
    }
}
