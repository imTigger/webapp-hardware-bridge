package tigerworkshop.webapphardwarebridge.websocketservices;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.PrinterService;

public class PrinterWebSocketService implements WebSocketServiceInterface {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketServerInterface server = null;
    private Gson gson = new Gson();

    public PrinterWebSocketService() {
        logger.info("Starting PrinterWebSocketService");
    }

    @Override
    public String getPrefix() {
        return "/printer";
    }

    @Override
    public void onDataReceived(String message) {
        try {
            if (message.startsWith("[")) {
                PrintDocument[] printDocuments = gson.fromJson(message, PrintDocument[].class);
                for (PrintDocument printDocument : printDocuments) {
                    try {
                        DocumentService.getInstance().prepareDocument(printDocument);
                        PrinterService.getInstance().printDocument(printDocument);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } else if (message.startsWith("{")) {
                PrintDocument printDocument = gson.fromJson(message, PrintDocument.class);
                try {
                    DocumentService.getInstance().prepareDocument(printDocument);
                    PrinterService.getInstance().printDocument(printDocument);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            } else {
                throw new Exception("Unknown input: " + message);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
    }
}
