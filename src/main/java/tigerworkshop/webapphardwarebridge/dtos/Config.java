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
    private WebSocketServer webSocketServer;
    private WebApiServer webApiServer;
    private CloudProxy cloudProxy;
    private Downloader downloader;
    private Printer printer;
    private Serial serial;

    public String toJson() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    @Data
    @NoArgsConstructor
    public static class WebSocketServer {
        private String address;
        private String bind;
        private int port;
        private Authentication authentication;
        private Tls tls;

        @JsonIgnore
        public String getUri() {
            return "ws://" + address + ":" + port;
        }
    }

    @Data
    @NoArgsConstructor
    public static class WebApiServer {
        private String address;
        private String bind;
        private int port;
        private Authentication authentication;
        private Tls tls;

        @JsonIgnore
        public String getUri() {
            return "http://" + address + ":" + port;
        }
    }

    @Data
    @NoArgsConstructor
    public static class CloudProxy {
        private boolean enabled;
        private String url;
        private int timeout;
    }

    @Data
    @NoArgsConstructor
    public static class Authentication {
        private boolean enabled;
        private String token;
    }

    @Data
    @NoArgsConstructor
    public static class Tls {
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
        private boolean autoRotate;
        private boolean resetImageableArea;
        private boolean addUnknownPrintTypeToList;
        private boolean fallbackToDefaultPrinter;
        private int printerDPI;
        private ArrayList<Mapping> mappings;
    }

    @Data
    @NoArgsConstructor
    public static class Serial {
        private boolean enabled;
        private ArrayList<Mapping> mappings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Mapping {
        private String type;
        private String name;
    }
}