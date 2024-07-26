package tigerworkshop.webapphardwarebridge.services;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import tigerworkshop.webapphardwarebridge.dtos.Config;
import tigerworkshop.webapphardwarebridge.responses.PrintDocument;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Log4j2
public class DocumentService {
    @Getter
    private static final DocumentService instance = new DocumentService();
    private static final Config.Downloader downloaderConfig = ConfigService.getInstance().getConfig().getDownloader();

    public File prepareDocument(PrintDocument printDocument) throws Exception {
        FileUtils.forceMkdir(new File(downloaderConfig.getPath()));

        if (printDocument.getUrl() == null && printDocument.getFileContent() == null) {
            throw new Exception("Both URL and File Content are null");
        }

        File output = getOutputFile(printDocument);
        if (printDocument.getFileContent() != null) {
            byte[] bytes = Base64.getDecoder().decode(printDocument.getFileContent());
            Files.write(output.toPath(), bytes);
        } else {
            URL url = new URL(printDocument.getUrl());
            download(url, getOutputFile(printDocument));
        }

        return output;
    }

    public void deleteDocument(PrintDocument printDocument) throws IOException {
        FileUtils.deleteQuietly(getOutputFile(printDocument));
    }

    private File getOutputFile(PrintDocument printDocument) throws MalformedURLException {
        File output;
        if (printDocument.getFileContent() != null) {
            output = new File(downloaderConfig.getPath() + "/" + printDocument.getUuid() + "-" + printDocument.getUrl());
        } else {
            URL url = new URL(printDocument.getUrl());
            output = new File(downloaderConfig.getPath() + "/" + printDocument.getUuid() + "-" + FilenameUtils.getName(url.getPath()));
        }
        return output;
    }

    private void download(URL url, File outputFile) throws Exception {
        log.info("Downloading file from: {}", url);

        long timeStart = System.currentTimeMillis();

        if (downloaderConfig.isIgnoreTLSCertificateError()) {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return null;
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }

                    }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }

        URLConnection urlConnection = url.openConnection();
        urlConnection.setConnectTimeout((int) downloaderConfig.getTimeout() * 1000);
        urlConnection.setReadTimeout((int) downloaderConfig.getTimeout() * 1000);
        urlConnection.connect();

        int contentLength = urlConnection.getContentLength();
        int responseCode;
        if (urlConnection instanceof HttpsURLConnection) {
            responseCode = ((HttpsURLConnection) urlConnection).getResponseCode();
        } else {
            responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
        }

        log.trace("Content Length: {}", contentLength);
        log.trace("Response Code: {}", responseCode);

        // Status code mismatch
        if (responseCode != 200) {
            throw new IOException("HTTP Status Code: " + responseCode);
        }

        FileUtils.copyInputStreamToFile(urlConnection.getInputStream(), outputFile);

        long timeFinish = System.currentTimeMillis();
        log.info("File {} downloaded in {} ms", outputFile.getName(), timeFinish - timeStart);
    }
}
