package tigerworkshop.webapphardwarebridge.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.printing.PDFPrintable;
import org.apache.pdfbox.printing.Scaling;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.utils.AnnotatedPrintable;
import tigerworkshop.webapphardwarebridge.utils.ImagePrintable;

import javax.imageio.ImageIO;
import javax.print.*;
import javax.print.attribute.AttributeSet;
import javax.print.attribute.HashPrintServiceAttributeSet;
import javax.print.attribute.standard.PrinterName;
import java.awt.*;
import java.awt.print.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PrinterService {

    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class.getName());
    private static final PrinterService instance = new PrinterService();

    private PrinterService() {
    }

    public static PrinterService getInstance() {
        return instance;
    }

    /**
     * Prints a PrintDocument
     */
    public void printDocument(PrintDocument printDocument) throws Exception {
        logger.info(printDocument.toString());
        try {
            if (!printDocument.getRawContent().isEmpty()) {
                printRaw(printDocument.getRawContent().getBytes());
            } else if (isImage(printDocument)) {
                printImage(printDocument);
            } else if (isPDF(printDocument)) {
                printPDF(printDocument);
            } else {
                throw new Exception("Unknown file type: " + printDocument.getUrl());
            }
        } catch (Exception e) {
            logger.error("Document Print Error, document deleted!", e);
            DocumentService.deleteFileFromUrl(printDocument.getUrl());
            throw e;
        }
    }

    /**
     * Return all printDocument services
     */
    public ArrayList<String> listPrinters() {
        logger.trace("listPrinters::");
        ArrayList<String> printerList = new ArrayList<>();
        PrintService[] printServices = PrinterJob.lookupPrintServices();
        for (PrintService printService : printServices) {
            printerList.add(printService.getName());
        }
        return printerList;
    }

    /**
     * Return name of mapped printer
     */
    private String findMappedPrinter(String type) {
        logger.trace("findMappedPrinter::" + type);
        return SettingService.getInstance().getMappedPrinter(type);
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
    private void printRaw(byte[] b) throws PrinterException, IOException {
        try {
            AttributeSet attrSet = new HashPrintServiceAttributeSet(new PrinterName("ZH380", null)); //EPSON TM-U220 ReceiptE4

            DocPrintJob job = PrintServiceLookup.lookupPrintServices(null, attrSet)[0].createPrintJob();
            //PrintServiceLookup.lookupDefaultPrintService().createPrintJob();

            DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
            Doc doc = new SimpleDoc(b, flavor, null);

            job.print(doc, null);
            System.out.println("Done !");
        } catch (javax.print.PrintException pex) {
            System.out.println("Printer Error " + pex.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
