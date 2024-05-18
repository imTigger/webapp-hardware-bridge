package tigerworkshop.webapphardwarebridge.services;

import org.bouncycastle.util.encoders.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.utils.DownloadUtil;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class DocumentService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    private static final DocumentService instance = new DocumentService();
    private static final SettingService settingService = SettingService.getInstance();

    private DocumentService() {
        File directory = new File(Config.DOCUMENT_PATH);
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
            logger.error("Failed to extract file from base64", e);
            throw e;
        }
    }

    public static void download(String urlString) throws Exception {
        DownloadUtil.file(urlString, getPathFromUrl(urlString), true, settingService.getSetting().getIgnoreTLSCertificateErrorEnabled(), settingService.getSetting().getDownloadTimeout());
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
            decodeBase64(printDocument.getFileContent(), printDocument.getUrl());
        } else {
            download(printDocument.getUrl());
        }
    }
}
