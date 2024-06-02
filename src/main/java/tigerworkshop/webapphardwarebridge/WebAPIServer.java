package tigerworkshop.webapphardwarebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.PrintServiceDTO;
import tigerworkshop.webapphardwarebridge.services.ConfigService;

import javax.print.PrintService;
import java.awt.print.PrinterJob;
import java.util.ArrayList;

@Log4j2
public class WebAPIServer {
    private static final WebAPIServer server = new WebAPIServer();

    private Javalin javalinServer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    ConfigService configService = ConfigService.getInstance();

    public WebAPIServer() {

    }

    public static void main(String[] args) {
        server.start();
    }

    public void start() {
        log.info("Web API Server started");

        javalinServer = Javalin.create(config -> config.staticFiles.add(staticFiles -> {
                    staticFiles.hostedPath = "/";
                    staticFiles.directory = "web";
                }))
                .get("/config.json", ctx -> {
                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .put("/config.json", ctx -> {
                    configService.loadFromJson(ctx.body());
                    configService.save();
                    ctx.contentType(ContentType.APPLICATION_JSON).result(configService.getConfig().toJson());
                })
                .get("/system/printers.json", ctx -> {
                    PrintService[] services = PrinterJob.lookupPrintServices();
                    var dtos = new ArrayList<PrintServiceDTO>();

                    for (var service : services) {
                        dtos.add(new PrintServiceDTO(service.getName(), ""));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .get("/system/serials.json", ctx -> {
                    ctx.result("Serials");
                })
                .start(configService.getConfig().getWebApiServer().getBind(), configService.getConfig().getWebApiServer().getPort());
    }

    public void stop() {
        javalinServer.stop();
    }
}
