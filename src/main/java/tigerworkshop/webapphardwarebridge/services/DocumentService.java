package tigerworkshop.webapphardwarebridge.services;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.utils.DownloadUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class DocumentService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DocumentService.class.getName());
    private static DocumentService instance = new DocumentService();

    private DocumentService() {
        File directory = new File(Config.DOCUMENT_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static DocumentService getInstance() {
        return instance;
    }

    public static void extract(String base64, String urlString) throws Exception {
        byte[] bytes = Base64.decode(base64);

        try (OutputStream stream = new FileOutputStream(getPathFromUrl(urlString))) {
            stream.write(bytes);
        }
    }

    public static void download(String urlString) throws Exception {
        DownloadUtil.file(urlString, getPathFromUrl(urlString), true);
    }

    public static File getFileFromUrl(String urlString) {
        return new File(getPathFromUrl(urlString));
    }

    public static void deleteFileFromUrl(String urlString) {
        getFileFromUrl(urlString).delete();
    }

    public static String getPathFromUrl(String urlString) {
        urlString = urlString.replace(" ", "%20");
        String filename = urlString.substring(urlString.lastIndexOf("/") + 1);
        return Config.DOCUMENT_PATH + filename;
    }

    public void prepareDocument(PrintDocument printDocument) throws Exception {
        if (printDocument.getRawContent() != null && !printDocument.getRawContent().isEmpty()) {
            return;
        }

        if (printDocument.getUrl() == null && printDocument.getFileContent() == null) {
            throw new Exception("URL is null");
        }

        if (printDocument.getFileContent() != null) {
            extract(printDocument.getFileContent(), printDocument.getUrl());
        } else {
            download(printDocument.getUrl());
        }
    }
}
