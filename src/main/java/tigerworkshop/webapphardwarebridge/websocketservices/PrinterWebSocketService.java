package tigerworkshop.webapphardwarebridge.websocketservices;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.interfaces.NotificationListenerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServerInterface;
import tigerworkshop.webapphardwarebridge.interfaces.WebSocketServiceInterface;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.responses.PrintResult;
import tigerworkshop.webapphardwarebridge.services.DocumentService;
import tigerworkshop.webapphardwarebridge.services.SettingService;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;
import tigerworkshop.webapphardwarebridge.utils.ImagePrintable;

import javax.imageio.ImageIO;
import javax.print.*;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;

public class PrinterWebSocketService implements WebSocketServiceInterface {
    private static final Logger logger = LoggerFactory.getLogger(PrinterWebSocketService.class);

    private WebSocketServerInterface server = null;
    private final Gson gson = new Gson();

    private final SettingService settingService = SettingService.getInstance();
    private NotificationListenerInterface notificationListener;

    public PrinterWebSocketService() {
        logger.info("Starting PrinterWebSocketService");
    }

    public void setNotificationListener(NotificationListenerInterface notificationListener) {
        this.notificationListener = notificationListener;
    }

    @Override
    public void start() {
        server.subscribe(this, getChannel());
    }

    @Override
    public void stop() {
        logger.info("Stopping PrinterWebSocketService");
        server.unsubscribe(this, getChannel());
    }

    @Override
    public void onDataReceived(String message) {
        try {
            PrintDocument printDocument = gson.fromJson(message, PrintDocument.class);
            DocumentService.getInstance().prepareDocument(printDocument);
            printDocument(printDocument);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void onDataReceived(byte[] message) {
        logger.error("PrinterWebSocketService onDataReceived: binary data not supported");
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
    }

    private String getChannel() {
        return "/printer";
    }

    /**
     * Prints a PrintDocument
     */
    public void printDocument(PrintDocument printDocument) throws Exception {
        try {
            if (notificationListener != null) {
                notificationListener.notify("Printing " + printDocument.getType(), printDocument.getUrl(), TrayIcon.MessageType.INFO);
            }

            if (isRaw(printDocument)) {
                printRaw(printDocument);
            } else if (isImage(printDocument)) {
                printImage(printDocument);
            } else if (isPDF(printDocument)) {
                printPDF(printDocument);
            } else {
                throw new Exception("Unknown file type: " + printDocument.getUrl());
            }

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(0, printDocument.getId(), printDocument.getUrl(), "Success")));
        } catch (Exception e) {
            logger.error("Document Print Error, deleting downloaded document");
            DocumentService.deleteFileFromUrl(printDocument.getUrl());

            if (notificationListener != null) {
                notificationListener.notify("Printing Error " + printDocument.getType(), e.getMessage(), TrayIcon.MessageType.ERROR);
            }

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(1, printDocument.getId(), printDocument.getUrl(), e.getClass().getName() + " - " + e.getMessage())));

            throw e;
        }
    }

    /**
     * Return name of mapped printer
     */
    private String findMappedPrinter(String type) {
        logger.trace("findMappedPrinter::{}", type);
        return settingService.getSetting().getPrinters().get(type);
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
    private void printRaw(PrintDocument printDocument) throws PrinterException, PrintException {
        logger.debug("printRaw::{}", printDocument);
        long timeStart = System.currentTimeMillis();

        byte[] bytes = Base64.decodeBase64(printDocument.getRawContent());

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());
        Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        docPrintJob.print(doc, null);

        long timeFinish = System.currentTimeMillis();
        logger.info("Document raw printed in {} ms", timeFinish - timeStart);
    }

    /**
     * Prints image to specified printer.
     */
    private void printImage(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printImage::{}", printDocument);

        File file = DocumentService.getFileFromUrl(printDocument.getUrl());
        String path = file.getPath();
        String filename = file.getName();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());
        PageFormat pageFormat = getPageFormat(job);

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

        logger.info("Document {} printed in {} ms", filename, timeFinish - timeStart);
    }

    /**
     * Prints PDF to specified printer.
     */
    private void printPDF(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printPDF::{}", printDocument);

        File file = DocumentService.getFileFromUrl(printDocument.getUrl());
        String path = file.getPath();
        String filename = file.getName();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());

        PageFormat pageFormat = getPageFormat(job);

        try (PDDocument document = PDDocument.load(new File(path))) {
            Book book = new Book();
            for (int i = 0; i < document.getNumberOfPages(); i += 1) {
                // Rotate Page Automatically
                PageFormat eachPageFormat = (PageFormat) pageFormat.clone();

                if (settingService.getSetting().getAutoRotation()) {
                    if (document.getPage(i).getCropBox().getWidth() > document.getPage(i).getCropBox().getHeight()) {
                        logger.debug("Auto rotation result: LANDSCAPE");
                        eachPageFormat.setOrientation(PageFormat.LANDSCAPE);
                    } else {
                        logger.debug("Auto rotation result: PORTRAIT");
                        eachPageFormat.setOrientation(PageFormat.PORTRAIT);
                    }
                }

                AnnotatedPrintable printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false, settingService.getSetting().getPrinterDPI()));

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

            logger.info("Document {} printed in {} ms", path, timeFinish - timeStart);
        }
    }

    /**
     * Get PageFormat for PrinterJob
     */
    private PageFormat getPageFormat(final PrinterJob job) {
        final PageFormat pageFormat = job.defaultPage();

        logger.debug("PageFormat Size: {} x {}", pageFormat.getWidth(), pageFormat.getHeight());
        logger.debug("PageFormat Imageable Size:{} x {}, XY: {}, {}", pageFormat.getImageableWidth(), pageFormat.getImageableHeight(), pageFormat.getImageableX(), pageFormat.getImageableY());
        logger.debug("Paper Size: {} x {}", pageFormat.getPaper().getWidth(), pageFormat.getPaper().getHeight());
        logger.debug("Paper Imageable Size: {} x {}, XY: {}, {}", pageFormat.getPaper().getImageableWidth(), pageFormat.getPaper().getImageableHeight(), pageFormat.getPaper().getImageableX(), pageFormat.getPaper().getImageableY());

        // Reset Imageable Area
        if (settingService.getSetting().getResetImageableArea()) {
            logger.debug("PageFormat reset enabled");
            Paper paper = pageFormat.getPaper();
            paper.setImageableArea(0, 0, paper.getWidth(), paper.getHeight());
            pageFormat.setPaper(paper);
        }

        logger.debug("Final Paper Size: {} x {}", pageFormat.getPaper().getWidth(), pageFormat.getPaper().getHeight());
        logger.debug("Final Paper Imageable Size: {} x {}, XY: {}, {}", pageFormat.getPaper().getImageableWidth(), pageFormat.getPaper().getImageableHeight(), pageFormat.getPaper().getImageableX(), pageFormat.getPaper().getImageableY());

        return pageFormat;
    }

    /**
     * Get DocPrintJob for specified printer
     */
     private DocPrintJob getDocPrintJob(String type) throws PrinterException {
        String printerName = findMappedPrinter(type);

        if (printerName != null) {
            PrintService[] services = PrinterJob.lookupPrintServices();

            for (PrintService service : services) {
                if (service.getName().equalsIgnoreCase(printerName)) {
                    logger.info("Sending print job type: {} to printer: {}", type, service.getName());
                    return service.createPrintJob();
                }
            }
        }

         if (settingService.getSetting().isAddUnknownPrintTypeToListEnabled()) {
             settingService.addPrintTypeToList(type);
        }

        if (settingService.getSetting().getFallbackToDefaultPrinter()) {
            logger.info("No mapped print job type: {}, falling back to default printer", type);

            var printService = PrintServiceLookup.lookupDefaultPrintService();

            if (printService == null) {
                throw new PrinterException("No default printer found");
            }

            return printService.createPrintJob();
        }

         throw new PrinterException("No matched printer: " + type);
    }
}
