package tigerworkshop.webapphardwarebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import io.javalin.http.ContentType;
import io.javalin.plugin.bundled.CorsPluginConfig;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.dtos.PrintServiceDTO;
import tigerworkshop.webapphardwarebridge.dtos.SerialPortDTO;
import tigerworkshop.webapphardwarebridge.interfaces.GUIInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.utils.CertificateGenerator;

import javax.print.PrintService;
import java.awt.*;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class WebAPIServer {
    private static final WebAPIServer server = new WebAPIServer(null);
    private static final ConfigService configService = ConfigService.getInstance();

    private Javalin javalinServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GUIInterface guiInterface;

    public WebAPIServer(GUIInterface guiInterface) {
        this.guiInterface = guiInterface;
    }

    public static void main(String[] args) {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        log.info("Web API Server started");

        Config.WebApiServer webConfig = configService.getConfig().getWebApiServer();

        javalinServer = Javalin.create(cfg -> {
                    cfg.showJavalinBanner = false;
                    cfg.staticFiles.add(staticFiles -> staticFiles.directory = "web");
                    cfg.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));

                    if (webConfig.getTls().isEnabled()) {
                        if (webConfig.getTls().isSelfSigned()) {
                            log.info("TLS Enabled with self-signed certificate");

                            CertificateGenerator.generateSelfSignedCertificate(webConfig.getAddress(), webConfig.getTls().getCert(), webConfig.getTls().getKey());

                            log.info("For first time setup, open in browser and trust the certificate: {}", webConfig.getUri().replace("http", "https"));
                        }

                        SslPlugin plugin = new SslPlugin(conf -> {
                            conf.insecure = false;
                            conf.securePort = webConfig.getPort();
                            conf.pemFromPath(webConfig.getTls().getCert(), webConfig.getTls().getKey());
                        });
                        cfg.registerPlugin(plugin);
                    }
                })
                .before(ctx -> {
                    if (webConfig.getAuthentication().isEnabled()) {
                        try {
                            // Bearer Token
                            if (Optional.ofNullable(ctx.header("Authorization")).orElse("").endsWith(webConfig.getAuthentication().getToken())) {
                                return;
                            }

                            // Basic Auth
                            if (ctx.basicAuthCredentials() != null && Objects.equals(ctx.basicAuthCredentials().getPassword(), webConfig.getAuthentication().getToken())) {
                                return;
                            }
                        } catch (Exception e) {}

                        ctx.header("WWW-Authenticate", "Basic realm=\"Token required\"");
                        ctx.res().sendError(401, "Token mismatch");
                    }
                })
                .get("/config.json", ctx -> {
                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .put("/config.json", ctx -> {
                    configService.loadFromJson(ctx.body());
                    configService.save();

                    guiInterface.notify("Setting", "Setting saved successfully", TrayIcon.MessageType.INFO);

                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .get("/system/printers.json", ctx -> {
                    ArrayList<PrintServiceDTO> dtos = new ArrayList<>();
                    for (PrintService service : PrinterJob.lookupPrintServices()) {
                        dtos.add(new PrintServiceDTO(service.getName(), ""));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .get("/system/serials.json", ctx -> {
                    ArrayList<SerialPortDTO> dtos = new ArrayList<>();
                    for (SerialPort port : SerialPort.getCommPorts()) {
                        dtos.add(new SerialPortDTO(port.getSystemPortName(), port.getPortDescription(), port.getManufacturer()));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .post("/system/restart.json", ctx -> {
                    guiInterface.restart();
                })
                .start(configService.getConfig().getWebApiServer().getBind(), configService.getConfig().getWebApiServer().getPort());
    }

    public void stop() {
        javalinServer.stop();
    }
}
