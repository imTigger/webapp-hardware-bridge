package tigerworkshop.webapphardwarebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fazecast.jSerialComm.SerialPort;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.plugin.bundled.CorsPluginConfig;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.PrintServiceDTO;
import tigerworkshop.webapphardwarebridge.dtos.SerialPortDTO;
import tigerworkshop.webapphardwarebridge.interfaces.GUIListenerInterface;
import tigerworkshop.webapphardwarebridge.services.ConfigService;

import java.awt.*;
import java.awt.print.PrinterJob;
import java.util.ArrayList;

@Log4j2
public class WebAPIServer {
    private static final WebAPIServer server = new WebAPIServer(null);
    private static final ConfigService configService = ConfigService.getInstance();

    private Javalin javalinServer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final GUIListenerInterface guiListener;

    public WebAPIServer(GUIListenerInterface guiListener) {
        this.guiListener = guiListener;
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

        javalinServer = Javalin.create(config -> {
                    config.showJavalinBanner = false;
                    config.staticFiles.add(staticFiles -> staticFiles.directory = "web");
                    config.bundledPlugins.enableCors(cors -> cors.addRule(CorsPluginConfig.CorsRule::anyHost));
                })
                .get("/config.json", ctx -> {
                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .put("/config.json", ctx -> {
                    configService.loadFromJson(ctx.body());
                    configService.save();

                    guiListener.notify("Setting", "Setting saved successfully", TrayIcon.MessageType.INFO);

                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .get("/system/printers.json", ctx -> {
                    var dtos = new ArrayList<PrintServiceDTO>();
                    for (var service : PrinterJob.lookupPrintServices()) {
                        dtos.add(new PrintServiceDTO(service.getName(), ""));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .get("/system/serials.json", ctx -> {
                    var dtos = new ArrayList<>();
                    for (var port : SerialPort.getCommPorts()) {
                        dtos.add(new SerialPortDTO(port.getSystemPortName(), port.getPortDescription(), port.getManufacturer()));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .post("/system/restart.json", ctx -> {
                    guiListener.restart();
                })
                .start(configService.getConfig().getWebApiServer().getBind(), configService.getConfig().getWebApiServer().getPort());
    }

    public void stop() {
        javalinServer.stop();
    }
}
