package tigerworkshop.webapphardwarebridge.utils;

import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;
import tigerworkshop.webapphardwarebridge.Config;

import javax.net.ssl.HttpsURLConnection;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class DownloadUtil {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DownloadUtil.class.getName());

    public static long file(String urlString, String path, Boolean overwrite) throws Exception {
        logger.info("Downloading file from: " + urlString);
        long timeStart = System.currentTimeMillis();

        urlString.replace(" ", "%20");

        File outputFile = new File(path);
        try {
            // File Exist, return
            if (!overwrite && outputFile.exists()) {
                long timeFinish = System.currentTimeMillis();
                logger.info("File " + path + " found on local disk in " + (timeFinish - timeStart) + "ms");
                return timeStart;
            }

            // Otherwise download it
            URL url = new URL(urlString);

            URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(Config.DOWNLOAD_TIMEOUT);
            urlConnection.setReadTimeout(Config.DOWNLOAD_TIMEOUT);
            urlConnection.connect();

            int contentLength = urlConnection.getContentLength();
            int responseCode;
            if (urlConnection instanceof HttpsURLConnection) {
                responseCode = ((HttpsURLConnection) urlConnection).getResponseCode();
            } else {
                responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
            }

            logger.trace("Content Length: " + contentLength);
            logger.trace("Response Code: " + responseCode);

            // Status code mismatch
            if (responseCode != 200) {
                throw new IOException("HTTP Status Code: " + responseCode);
            }

            FileUtils.copyInputStreamToFile(urlConnection.getInputStream(), outputFile);

            long timeFinish = System.currentTimeMillis();
            logger.info("File " + path + " downloaded in " + (timeFinish - timeStart) + "ms");

            return timeFinish - timeStart;
        } catch (Exception e) {
            throw e;
        }
    }

    public static File getFile(String path) {
        return new File(path);
    }

    public static boolean delete(String path) {
        File outputFile = getFile(path);
        return outputFile.delete();
    }
}
