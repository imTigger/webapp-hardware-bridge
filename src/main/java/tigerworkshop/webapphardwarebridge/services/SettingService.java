package tigerworkshop.webapphardwarebridge.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.responses.Setting;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;

public class SettingService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SettingService.class.getName());
    private static final String SETTING_FILENAME = "setting.json";
    private static final String SETTING_FALLBACK_FILENAME = "setting.json.example";
    private static SettingService instance = new SettingService();
    private Setting setting = null;

    private SettingService() {
        load();
    }

    public static SettingService getInstance() {
        return instance;
    }

    public void load() {
        try {
            loadCurrent();
        } catch (Exception e) {
            try {
                loadDefault();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        }
    }

    public void loadCurrent() throws IOException {
        loadFile(SETTING_FILENAME);
    }

    public void loadDefault() throws IOException {
        loadFile(SETTING_FALLBACK_FILENAME);
    }

    private void loadFile(String filename) throws IOException {
        JsonReader reader = new JsonReader(new FileReader(filename));
        Gson gson = new Gson();
        setting = gson.fromJson(reader, Setting.class);
        reader.close();
    }

    public String getAddress() {
        return setting.getAddress();
    }

    public int getPort() {
        return setting.getPort();
    }

    public String getToken() {
        return setting.getToken();
    }

    public boolean getTokenAuthenticationEnabled() {
        return setting.getTokenAuthenticationEnabled();
    }

    public boolean getFallbackToDefaultPrinter() {
        return setting.getFallbackToDefaultPrinter();
    }

    public HashMap<String, String> getSerials() {
        return setting.getSerials();
    }

    public void setSerials(HashMap<String, String> serials) {
        setting.setSerials(serials);
    }

    public HashMap<String, String> getPrinters() {
        return setting.getPrinters();
    }

    public void setPrinters(HashMap<String, String> printers) {
        setting.setPrinters(printers);
    }

    public String getMappedSerial(String key) {
        return setting.getSerials().get(key);
    }

    public String getMappedPrinter(String key) {
        return setting.getPrinters().get(key);
    }

    public void save() {
        try {
            Writer writer = new FileWriter(SETTING_FILENAME);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(setting, writer);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
