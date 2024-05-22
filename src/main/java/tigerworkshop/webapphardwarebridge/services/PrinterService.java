package tigerworkshop.webapphardwarebridge.services;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class PrinterService {
    @Getter
    private static final PrinterService instance = new PrinterService();

    private PrinterService() {
    }
}
