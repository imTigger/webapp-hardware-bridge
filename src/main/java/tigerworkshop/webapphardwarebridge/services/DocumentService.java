package tigerworkshop.webapphardwarebridge.services;

import lombok.extern.log4j.Log4j2;
import org.bouncycastle.util.encoders.Base64;
import tigerworkshop.webapphardwarebridge.Constants;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.utils.DownloadUtil;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@Log4j2
public class DocumentService {
    private static final DocumentService instance = new DocumentService();
    private static final ConfigService CONFIG_SERVICE = ConfigService.getInstance();

    private DocumentService() {
        File directory = new File(Constants.DOCUMENT_PATH);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static DocumentService getInstance() {
        return instance;
    }

    public static void decodeBase64(String base64, String urlString) throws Exception {
        byte[] bytes = Base64.decode(base64);

        try (OutputStream stream = Files.newOutputStream(Paths.get(getPathFromUrl(urlString)))) {
            stream.write(bytes);
        } catch (
                Exception e) {
            log.error("Failed to extract file from base64", e);
            throw e;
        }
    }

    public static void download(String urlString) throws Exception {
        DownloadUtil.file(urlString, getPathFromUrl(urlString), true, CONFIG_SERVICE.getConfig().getDownloader().isIgnoreTLSCertificateError(), CONFIG_SERVICE.getConfig().getDownloader().getTimeout());
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
        return Constants.DOCUMENT_PATH + filename;
    }

    public void prepareDocument(PrintDocument printDocument) throws Exception {
        if (printDocument.getRawContent() != null && !printDocument.getRawContent().isEmpty()) {
            return;
        }

        if (printDocument.getUrl() == null && printDocument.getFileContent() == null) {
            throw new Exception("URL is null");
        }

        if (printDocument.getFileContent() != null) {
            decodeBase64(printDocument.getFileContent(), printDocument.getUrl());
        } else {
            download(printDocument.getUrl());
        }
    }
}
