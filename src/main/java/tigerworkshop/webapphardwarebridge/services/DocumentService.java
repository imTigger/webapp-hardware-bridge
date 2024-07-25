package tigerworkshop.webapphardwarebridge.services;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.encoders.Base64;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;
import tigerworkshop.webapphardwarebridge.utils.DownloadUtil;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DocumentService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DocumentService.class.getName());
    private static final DocumentService instance = new DocumentService();
    private static final SettingService settingService = SettingService.getInstance();

    private DocumentService() {
        File directory = new File(Config.DOCUMENT_PATH);
        if (!directory.exists()) {
            boolean result = directory.mkdir();
            if (!result) {
                logger.error("Directory for documents doesn't exist and can't be created.");
            }
        }
    }

    public static DocumentService getInstance() {
        return instance;
    }

    public static void extract(String base64, String uuid, String urlString) throws Exception {
        byte[] bytes = Base64.decode(base64);

        Path filePath = getPathFromUrl(uuid, urlString);
        Files.createDirectories(filePath.getParent());
        try (OutputStream stream = Files.newOutputStream(filePath)) {
            stream.write(bytes);
        }
    }

    public static void download(String uuid, String urlString) throws Exception {
        Path filePath = getPathFromUrl(uuid, urlString);
        Files.createDirectories(filePath.getParent());
        DownloadUtil.file(urlString, filePath, settingService.getSetting().getIgnoreTLSCertificateErrorEnabled(), settingService.getSetting().getDownloadTimeout());
    }

    public static void deleteFileFromUrl(String uuid, String urlString) {
        Path filePath = getPathFromUrl(uuid, urlString);
        try {
            FileUtils.deleteDirectory(filePath.getParent().toFile());
        } catch (IOException e) {
            logger.error("Couldn't delete downloaded document.");
        }
    }

    public static Path getPathFromUrl(String uuid, String urlString) {
        urlString = urlString.replace(" ", "%20");
        String filename = urlString.substring(urlString.lastIndexOf("/") + 1);
        return Paths.get(Config.DOCUMENT_PATH + uuid + "/" + filename);
    }

    public void prepareDocument(PrintDocument printDocument) throws Exception {
        if (printDocument.getRawContent() != null && !printDocument.getRawContent().isEmpty()) {
            return;
        }

        if (printDocument.getUrl() == null && printDocument.getFileContent() == null) {
            throw new Exception("URL is null");
        }

        if (printDocument.getFileContent() != null) {
            extract(printDocument.getFileContent(), printDocument.getUuid().toString(), printDocument.getUrl());
        } else {
            download(printDocument.getUuid().toString(), printDocument.getUrl());
        }
    }
}
