package tigerworkshop.webapphardwarebridge.websocketservices;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.interfaces.GUIInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.responses.PrintResult;
import tigerworkshop.webapphardwarebridge.services.ConfigService;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;
import tigerworkshop.webapphardwarebridge.utils.ImagePrintable;

import javax.imageio.ImageIO;
import javax.print.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Log4j2
public class PrinterWebSocketService implements WebSocketServiceInterface {
    private WebSocketServerInterface server;
    private final GUIInterface guiInterface;

    private static final ConfigService configService = ConfigService.getInstance();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public PrinterWebSocketService(GUIInterface newGUIInterface) {
        log.info("Starting PrinterWebSocketService");

        this.guiInterface = newGUIInterface;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void onDataReceived(String message) {
        try {
            PrintDocument printDocument = objectMapper.readValue(message, PrintDocument.class);
            DocumentService.getInstance().prepareDocument(printDocument);
            printDocument(printDocument);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void onDataReceived(byte[] message) {
        log.error("PrinterWebSocketService onDataReceived: binary data not supported");
    }

    @Override
    public void onRegister(WebSocketServerInterface server) {
        this.server = server;
    }

    @Override
    public void onUnregister() {
        this.server = null;
    }

    @Override
    public String getChannel() {
        return "/printer";
    }

    /**
     * Prints a PrintDocument
     */
    public void printDocument(PrintDocument printDocument) throws Exception {
        var printerSearchResult = searchPrinterForType(printDocument.getType());

        try {
            if (guiInterface != null) {
                guiInterface.notify("Printing " + printDocument.getType(), printDocument.getUrl(), TrayIcon.MessageType.INFO);
            }

            if (isRaw(printDocument)) {
                printRaw(printDocument, printerSearchResult);
            } else if (isImage(printDocument)) {
                printImage(printDocument, printerSearchResult);
            } else if (isPDF(printDocument)) {
                printPDF(printDocument, printerSearchResult);
            } else {
                throw new Exception("Unknown file type: " + printDocument.getUrl());
            }

            server.onDataReceived(getChannel(), objectMapper.writeValueAsString(new PrintResult(true, "Success", printDocument.getId(), printerSearchResult.getName())));
        } catch (Exception e) {
            log.error("Document Print Error, deleting downloaded document");
            DocumentService.deleteFileFromUrl(printDocument.getUrl());

            if (guiInterface != null) {
                guiInterface.notify("Printing Error " + printDocument.getType(), e.getMessage(), TrayIcon.MessageType.ERROR);
            }

            server.onDataReceived(getChannel(), objectMapper.writeValueAsString(new PrintResult(false, e.getMessage(), printDocument.getId(), printerSearchResult.getName())));

            throw e;
        }
    }

    /**
     * Return if PrintDocument is raw
     */
    private Boolean isRaw(PrintDocument printDocument) {
        return printDocument.getRawContent() != null && !printDocument.getRawContent().isEmpty();
    }

    /**
     * Return if PrintDocument is image
     */
    private Boolean isImage(PrintDocument printDocument) {
        String url = printDocument.getUrl();
        String filename = url.substring(url.lastIndexOf("/") + 1);

        return filename.matches("^.*\\.(jpg|jpeg|png|gif)$");
    }

    /**
     * Return if PrintDocument is PDF
     */
    private Boolean isPDF(PrintDocument printDocument) {
        String url = printDocument.getUrl();
        String filename = url.substring(url.lastIndexOf("/") + 1);

        return filename.matches("^.*\\.(pdf)$");
    }

    /**
     * Prints raw bytes to specified printer.
     */
    private void printRaw(PrintDocument printDocument, PrinterSearchResult printerSearchResult) throws PrinterException, PrintException {
        log.debug("printRaw::{}", printDocument);
        long timeStart = System.currentTimeMillis();

        byte[] bytes = Base64.decodeBase64(printDocument.getRawContent());

        DocPrintJob docPrintJob = searchPrinterForType(printDocument.getType()).getDocPrintJob();
        Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        docPrintJob.print(doc, null);

        long timeFinish = System.currentTimeMillis();
        log.info("Document raw printed in {} ms", timeFinish - timeStart);
    }

    /**
     * Prints image to specified printer.
     */
    private void printImage(PrintDocument printDocument, PrinterSearchResult printerSearchResult) throws PrinterException, IOException {
        log.debug("printImage::{}", printDocument);

        File file = DocumentService.getFileFromUrl(printDocument.getUrl());
        String path = file.getPath();
        String filename = file.getName();

        long timeStart = System.currentTimeMillis();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(printerSearchResult.getDocPrintJob().getPrintService());

        var pageFormat = getPageFormat(job, printerSearchResult);

        Image image = ImageIO.read(new File(path));

        Book book = new Book();
        AnnotatedPrintable printable = new AnnotatedPrintable(new ImagePrintable(image));

        for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
            printable.addAnnotation(printDocumentExtra);
        }

        book.append(printable, pageFormat);

        job.setPageable(book);
        job.setJobName(filename);
        job.setCopies(printDocument.getQty());
        job.print();

        long timeFinish = System.currentTimeMillis();

        log.info("Document {} printed in {} ms", filename, timeFinish - timeStart);
    }

    /**
     * Prints PDF to specified printer.
     */
    private void printPDF(PrintDocument printDocument, PrinterSearchResult printerSearchResult) throws PrinterException, IOException {
        log.debug("printPDF::{}", printDocument);

        File file = DocumentService.getFileFromUrl(printDocument.getUrl());
        String path = file.getPath();
        String filename = file.getName();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = printerSearchResult.getDocPrintJob();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());

        var pageFormat = getPageFormat(job, printerSearchResult);

        try (PDDocument document = PDDocument.load(new File(path))) {
            Book book = new Book();
            for (int i = 0; i < document.getNumberOfPages(); i += 1) {
                // Rotate Page Automatically
                PageFormat eachPageFormat = (PageFormat) pageFormat.clone();

                if (printerSearchResult.getMapping().isAutoRotate()) {
                    if (document.getPage(i).getCropBox().getWidth() > document.getPage(i).getCropBox().getHeight()) {
                        log.debug("Auto rotation result: LANDSCAPE");
                        eachPageFormat.setOrientation(PageFormat.LANDSCAPE);
                    } else {
                        log.debug("Auto rotation result: PORTRAIT");
                        eachPageFormat.setOrientation(PageFormat.PORTRAIT);
                    }
                }

                AnnotatedPrintable printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false, printerSearchResult.getMapping().getForceDPI()));

                for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
                    printable.addAnnotation(printDocumentExtra);
                }
                book.append(printable, eachPageFormat);
            }

            job.setPageable(book);
            job.setJobName(filename);
            job.setCopies(printDocument.getQty());
            job.print();

            long timeFinish = System.currentTimeMillis();

            log.info("Document {} printed in {} ms", path, timeFinish - timeStart);
        }
    }

    private PageFormat getPageFormat(PrinterJob job, PrinterSearchResult printerSearchResult) {
        final PageFormat pageFormat = job.defaultPage();

        log.debug("PageFormat Size: {} x {}", pageFormat.getWidth(), pageFormat.getHeight());
        log.debug("PageFormat Imageable Size:{} x {}, XY: {}, {}", pageFormat.getImageableWidth(), pageFormat.getImageableHeight(), pageFormat.getImageableX(), pageFormat.getImageableY());
        log.debug("Paper Size: {} x {}", pageFormat.getPaper().getWidth(), pageFormat.getPaper().getHeight());
        log.debug("Paper Imageable Size: {} x {}, XY: {}, {}", pageFormat.getPaper().getImageableWidth(), pageFormat.getPaper().getImageableHeight(), pageFormat.getPaper().getImageableX(), pageFormat.getPaper().getImageableY());

        // Reset Imageable Area
        if (printerSearchResult.getMapping().isResetImageableArea()) {
            log.debug("PageFormat reset enabled");
            Paper paper = pageFormat.getPaper();
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pageFormat.setPaper(paper);
        }

        log.debug("Final Paper Size: {} x {}", pageFormat.getPaper().getWidth(), pageFormat.getPaper().getHeight());
        log.debug("Final Paper Imageable Size: {} x {}, XY: {}, {}", pageFormat.getPaper().getImageableWidth(), pageFormat.getPaper().getImageableHeight(), pageFormat.getPaper().getImageableX(), pageFormat.getPaper().getImageableY());

        return pageFormat;
    }

    /**
     * Get PrinterSearchResult for specified type
     */
    private PrinterSearchResult searchPrinterForType(String type) throws PrinterException {
        Optional<Config.PrinterMapping> printerMappingOptional = configService.getConfig().getPrinter().getMappings().stream().filter(it -> it.getType().equals(type)).findFirst();

        if (printerMappingOptional.isPresent()) {
            Config.PrinterMapping printerMapping = printerMappingOptional.get();
            PrintService[] printServices = PrinterJob.lookupPrintServices();

            for (PrintService printService : printServices) {
                if (printService.getName().equalsIgnoreCase(printerMapping.getName())) {
                    log.info("Sending print job type: {} to printer: {}", type, printService.getName());

                    return new PrinterSearchResult(printService.getName(), printerMapping, printService.createPrintJob(), false);
                }
            }
        }

         if (configService.getConfig().getPrinter().isAutoAddUnknownType()) {
             configService.addPrintTypeToList(type);
        }

         if (configService.getConfig().getPrinter().isFallbackToDefault()) {
             log.info("No mapped print job type: {}, falling back to default printer", type);

            var printService = PrintServiceLookup.lookupDefaultPrintService();

            if (printService == null) {
                throw new PrinterException("No default printer found");
            }

             return new PrinterSearchResult(printService.getName(), new Config.PrinterMapping(), printService.createPrintJob(), true);
        }

         throw new PrinterException("No matched printer: " + type);
    }

    @Getter
    @AllArgsConstructor
    private static class PrinterSearchResult {
        private String name;
        private Config.PrinterMapping mapping;
        private DocPrintJob docPrintJob;
        private Boolean isDefault;
    }
}
