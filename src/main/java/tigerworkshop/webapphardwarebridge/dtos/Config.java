package tigerworkshop.webapphardwarebridge.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@NoArgsConstructor
public class Config {
    private Server server;
    private Downloader downloader;
    private Printer printer;
    private Serial serial;

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @Data
    @NoArgsConstructor
    public static class Server {
        private String address;
        private String bind;
        private int port;
        private Authentication authentication;
        private TLS tls;

        @JsonIgnore
        public String getUri() {
            return "ws://" + address + ":" + port;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Authentication {
        private boolean enabled;
        private String token;
    }

    @Data
    @NoArgsConstructor
    public static class TLS {
        private boolean enabled;
        private boolean selfSigned;
        private String cert;
        private String key;
        private String caBundle;
    }

    @Data
    @NoArgsConstructor
    public static class Downloader {
        private boolean ignoreTLSCertificateError;
        private double timeout;
        private String path;
    }

    @Data
    @NoArgsConstructor
    public static class Printer {
        private boolean enabled;
        private boolean autoAddUnknownType;
        private boolean fallbackToDefault;
        private ArrayList<PrinterMapping> mappings;
    }

    @Data
    @NoArgsConstructor
    public static class Serial {
        private boolean enabled;
        private ArrayList<SerialMapping> mappings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrinterMapping {
        private String type;
        private String name;

        private boolean autoRotate = false;
        private boolean resetImageableArea = true;
        private int forceDPI = 0;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SerialMapping {
        private String type;
        private String name;

        private Integer baudRate;
        private Integer numDataBits;
        private Integer numStopBits;
        private Integer parity;
    }
}