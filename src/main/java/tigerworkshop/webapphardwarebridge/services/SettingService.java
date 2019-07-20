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

public class SettingService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SettingService.class.getName());
    private static final String SETTING_FILENAME = "setting.json";
    private static final String SETTING_FALLBACK_FILENAME = "setting.default.json";
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
                setting = new Setting();
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

    public Setting getSetting() {
        return setting;
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
