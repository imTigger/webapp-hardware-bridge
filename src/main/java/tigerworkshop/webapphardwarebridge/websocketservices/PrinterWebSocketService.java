package tigerworkshop.webapphardwarebridge.websocketservices;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;
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
    private Logger logger = LoggerFactory.getLogger(getClass());
    private WebSocketServerInterface server = null;
    private Gson gson = new Gson();

    public PrinterWebSocketService() {
        logger.info("Starting PrinterWebSocketService");
    }

    @Override
    public void onDataReceived(String message) {
        try {
            PrintDocument printDocument = gson.fromJson(message, PrintDocument.class);
            try {
                DocumentService.getInstance().prepareDocument(printDocument);
                printDocument(printDocument);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void setServer(WebSocketServerInterface server) {
        this.server = server;
        server.subscribe(this, getChannel());
    }

    private String getChannel() {
        return "/printer";
    }

    /**
     * Prints a PrintDocument
     */
    public void printDocument(PrintDocument printDocument) throws Exception {
        logger.info(printDocument.toString());
        try {
            if (isRaw(printDocument)) {
                printRaw(printDocument);
            } else if (isImage(printDocument)) {
                printImage(printDocument);
            } else if (isPDF(printDocument)) {
                printPDF(printDocument);
            } else {
                throw new Exception("Unknown file type: " + printDocument.getUrl());
            }

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(0, printDocument.getId(), "Success")));
        } catch (Exception e) {
            logger.error("Document Print Error, document deleted!", e);
            DocumentService.deleteFileFromUrl(printDocument.getUrl());

            server.onDataReceived(getChannel(), gson.toJson(new PrintResult(1, printDocument.getId(), e.getClass().getName() + " - " + e.getMessage())));

            throw e;
        }
    }

    /**
     * Return name of mapped printer
     */
    private String findMappedPrinter(String type) {
        logger.trace("findMappedPrinter::" + type);
        return SettingService.getInstance().getMappedPrinter(type);
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
        logger.debug("printRaw::" + printDocument);
        long timeStart = System.currentTimeMillis();

        byte[] bytes = Base64.decodeBase64(printDocument.getRawContent());

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());
        Doc doc = new SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
        docPrintJob.print(doc, null);

        long timeFinish = System.currentTimeMillis();
        logger.info("Document raw printed in " + (timeFinish - timeStart) + "ms");
    }

    /**
     * Prints image to specified printer.
     */
    private void printImage(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printImage::" + printDocument);

        String filename = DocumentService.getFileFromUrl(printDocument.getUrl()).getPath();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());
        PageFormat pageFormat = job.defaultPage();

        logger.trace("Paper Size: " + pageFormat.getWidth() + " x " + pageFormat.getHeight());
        logger.trace("Imageable Size:" + pageFormat.getImageableWidth() + " x " + pageFormat.getImageableHeight());

        Paper paper = pageFormat.getPaper();
        double width = pageFormat.getWidth();
        double height = pageFormat.getHeight();
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        pageFormat.setPaper(paper);

        Image image = ImageIO.read(new File(filename));

        Book book = new Book();
        AnnotatedPrintable printable = new AnnotatedPrintable(new ImagePrintable(image));

        for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
            printable.addAnnotation(printDocumentExtra);
        }

        book.append(printable, pageFormat);

        job.setPageable(book);
        job.setJobName("WebApp Hardware Bridge Image");
        job.print();

        long timeFinish = System.currentTimeMillis();
        logger.info("Document " + filename + " printed in " + (timeFinish - timeStart) + "ms");
    }

    /**
     * Prints PDF to specified printer.
     */
    private void printPDF(PrintDocument printDocument) throws PrinterException, IOException {
        logger.debug("printPDF::" + printDocument);

        String filename = DocumentService.getFileFromUrl(printDocument.getUrl()).getPath();

        long timeStart = System.currentTimeMillis();

        DocPrintJob docPrintJob = getDocPrintJob(printDocument.getType());

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintService(docPrintJob.getPrintService());
        PageFormat pageFormat = job.defaultPage();

        logger.trace("Paper Size: " + pageFormat.getWidth() + " x " + pageFormat.getHeight());
        logger.trace("Imageable Size:" + pageFormat.getImageableWidth() + " x " + pageFormat.getImageableHeight());

        Paper paper = pageFormat.getPaper();
        double width = pageFormat.getWidth();
        double height = pageFormat.getHeight();

        // Reset Imageable Area
        paper.setSize(width, height);
        paper.setImageableArea(0, 0, width, height);
        pageFormat.setPaper(paper);

        PDDocument document = PDDocument.load(new File(filename));

        Book book = new Book();
        for (int i = 0; i < document.getNumberOfPages(); i += 1) {
            // Rotate Page Automatically
            if (Config.PDF_AUTO_ROTATE) {
                if (document.getPage(i).getCropBox().getWidth() > document.getPage(i).getCropBox().getHeight()) {
                    pageFormat.setOrientation(PageFormat.LANDSCAPE);
                } else {
                    pageFormat.setOrientation(PageFormat.PORTRAIT);
                }
            }

            AnnotatedPrintable printable;
            if (System.getProperty("os.name").contains("Mac OS X")) {
                printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT, false, 203));
            } else {
                printable = new AnnotatedPrintable(new PDFPrintable(document, Scaling.SHRINK_TO_FIT));
            }

            for (AnnotatedPrintable.AnnotatedPrintableAnnotation printDocumentExtra : printDocument.getExtras()) {
                printable.addAnnotation(printDocumentExtra);
            }
            book.append(printable, pageFormat);
        }

        job.setPageable(book);
        job.setJobName("WebApp Hardware Bridge PDF");
        job.print();

        long timeFinish = System.currentTimeMillis();
        logger.info("Document " + filename + " printed in " + (timeFinish - timeStart) + "ms");

        document.close();
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
                    logger.info("Creating print job to printer: " + service.getName());
                    return service.createPrintJob();
                }
            }
        }

        if (SettingService.getInstance().getFallbackToDefaultPrinter()) {
            logger.info("No matched printer: " + printerName + ", falling back to default printer");
            return PrintServiceLookup.lookupDefaultPrintService().createPrintJob();
        } else {
            throw new PrinterException("No matched printer: " + type);
        }
    }
}
