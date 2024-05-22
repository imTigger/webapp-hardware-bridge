package tigerworkshop.webapphardwarebridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.ContentType;
import lombok.extern.log4j.Log4j2;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.dtos.PrintServiceDTO;
import tigerworkshop.webapphardwarebridge.services.ConfigService;

import javax.print.PrintService;
import java.awt.print.PrinterJob;
import java.util.ArrayList;

@Log4j2
public class WebAPIServer {
    private static final WebAPIServer server = new WebAPIServer();
    private static final Javalin app = Javalin.create();

    private final ObjectMapper objectMapper = new ObjectMapper();

    ConfigService configService = ConfigService.getInstance();
    Config config = configService.getConfig();

    public WebAPIServer() {

    }

    public static void main(String[] args) {
        server.start();
    }

    public void start() {
        log.info("Web API Server started");

        app
                .get("/", ctx -> ctx.result("WebApp Hardware Bridge API Server"))
                .get("/config", ctx -> {
                    ctx.contentType(ContentType.APPLICATION_JSON).result(config.toJson());
                })
                .put("/config", ctx -> {
                    ctx.contentType(ContentType.APPLICATION_JSON).result(config.toJson());
                })
                .get("/printers", ctx -> {
                    PrintService[] services = PrinterJob.lookupPrintServices();
                    var dtos = new ArrayList<PrintServiceDTO>();

                    for (var service : services) {
                        dtos.add(new PrintServiceDTO(service.getName(), ""));
                    }

                    ctx.contentType(ContentType.APPLICATION_JSON).result(objectMapper.writeValueAsString(dtos));
                })
                .get("/serials", ctx -> {
                    ctx.result("Serials");
                })
                .start(config.getWebApiServer().getPort());
    }

    public void stop() {
        app.stop();
    }
}
