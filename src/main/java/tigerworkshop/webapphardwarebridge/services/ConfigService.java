package tigerworkshop.webapphardwarebridge.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;

import java.io.File;
import java.io.IOException;

@Log4j2
public class ConfigService {
    @Getter
    private static final ConfigService instance = new ConfigService();

    private static final String CONFIG_FILENAME = "config.json";
    private static final String CONFIG_DEFAULT_FILENAME = "config.default.json";
    private static final String PRINTER_PLACEHOLDER = "";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Getter
    private Config config = null;

    private ConfigService() {
        try {
            loadFromFile(CONFIG_FILENAME);
        } catch (Exception e) {
            log.warn("Failed loading config file", e);
            try {
                log.warn("Loading default config file", e);
                loadFromFile(CONFIG_DEFAULT_FILENAME);
                save();
            } catch (Exception ex) {
                log.error("Failed loading default config file", ex);
                System.exit(1);
            }
        }
    }

    public void loadFromJson(String json) throws JsonProcessingException {
        log.info("Loading config from JSON: {}", json);
        config = objectMapper.readValue(json, Config.class);
    }

    private void loadFromFile(String filename) throws IOException {
        log.info("Loading config from file: {}", filename);
        config = objectMapper.readValue(new File(filename), Config.class);
    }

    public void save() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(CONFIG_FILENAME), config);
        } catch (Exception e) {
            log.error("Failed to save config file", e);
            System.exit(1);
        }
    }

    public void addPrintTypeToList(String printType) {
        config.getPrinter().getMappings().add(new Config.PrinterMapping(printType, PRINTER_PLACEHOLDER, false, true, 0));
        save();
    }
}
