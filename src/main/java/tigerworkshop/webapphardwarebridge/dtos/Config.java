package tigerworkshop.webapphardwarebridge.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class Config {
    private GUI gui = new GUI();
    private Server server = new Server();
    private Downloader downloader = new Downloader();
    private Printer printer = new Printer();
    private Serial serial = new Serial();

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @Data
    @NoArgsConstructor
    public static class GUI {
        private Notification notification = new Notification();
    }

    @Data
    @NoArgsConstructor
    public static class Notification {
        private boolean enabled = false;
    }

    @Data
    @NoArgsConstructor
    public static class Server {
        private String address = "127.0.0.1";
        private String bind = "127.0.0.1";
        private int port = 12212;
        private Authentication authentication = new Authentication();
        private TLS tls = new TLS();

        @JsonIgnore
        public String getUri() {
            return (tls.isEnabled() ? "https://" : "http://") + address + ":" + port;
        }
    }

    @Data
    @NoArgsConstructor
    public static class Authentication {
        private boolean enabled = false;
        private String token = null;
    }

    @Data
    @NoArgsConstructor
    public static class TLS {
        private boolean enabled = false ;
        private boolean selfSigned = true;
        private String cert = "tls/default-cert.pem";
        private String key = "tls/default-key.pem";
        private String caBundle = null;
    }

    @Data
    @NoArgsConstructor
    public static class Downloader {
        private boolean ignoreTLSCertificateError = false;
        private double timeout = 30;
        private String path = "downloads";
    }

    @Data
    @NoArgsConstructor
    public static class Printer {
        private boolean enabled = true;
        private boolean autoAddUnknownType = false;
        private boolean fallbackToDefault = false;
        private ArrayList<PrinterMapping> mappings = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    public static class Serial {
        private boolean enabled = true;
        private ArrayList<SerialMapping> mappings = new ArrayList<>();
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

        private Boolean readMultipleBytes = false;
        private String readCharset = StandardCharsets.UTF_8.toString();
    }
}