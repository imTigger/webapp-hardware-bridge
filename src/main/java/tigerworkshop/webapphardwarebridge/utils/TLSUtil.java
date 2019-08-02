package tigerworkshop.webapphardwarebridge.utils;

import org.java_websocket.server.CustomSSLWebSocketServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TLSUtil {
    private static final String KEYSTORE_PASSWORD = "webapp-hardware-bridge";
    private static final String KEYSTORE_CERTIFICATE_ALIAS = "webapp-hardware-bridge-cert";
    private static final String KEYSTORE_KEY_ALIAS = "webapp-hardware-bridge-key";

    private static Logger logger = LoggerFactory.getLogger("TLSUtil");

    public static SSLContext getContext(String certificatePath, String keyPath, String caBundlePath) throws Exception {
        try {
            logger.debug("Creating SSLContext");

            File certificate = new File(certificatePath);
            File privateKey = new File(keyPath);

            X509Certificate cert = readCertificate(certificate);
            RSAPrivateKey key = readKey(privateKey);

            // Build full chain of certificate
            ArrayList<X509Certificate> fullChain = new ArrayList<>();
            fullChain.add(cert);

            if (caBundlePath != null) {
                File caBundle = new File(caBundlePath);
                ArrayList<X509Certificate> chain = readCaBundle(caBundle);
                fullChain.addAll(chain);
            }

            Certificate[] fullChainArray = new X509Certificate[fullChain.size()];
            fullChain.toArray(fullChainArray);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry(KEYSTORE_CERTIFICATE_ALIAS, cert);
            keystore.setKeyEntry(KEYSTORE_KEY_ALIAS, key, KEYSTORE_PASSWORD.toCharArray(), fullChainArray);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keystore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, KEYSTORE_PASSWORD.toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext;
        } catch (Exception e) {
            logger.error("Failed creating SSLContext:" + e.getMessage());
            throw e;
        }
    }

    // Workaround for some browsers
    public static CustomSSLWebSocketServerFactory getSecureFactory(String certificatePath, String keyPath, String caBundlePath) throws Exception {
        SSLContext sslContext = getContext(certificatePath, keyPath, caBundlePath);
        SSLEngine engine = sslContext.createSSLEngine();

        List<String> ciphers = new ArrayList<>(Arrays.asList(engine.getEnabledCipherSuites()));
        ciphers.remove("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"); // Cause problem in Firefox
        ciphers.remove("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"); // Cause problem in Firefox

        List<String> protocols = new ArrayList<>(Arrays.asList(engine.getEnabledProtocols()));
        //protocols.remove("SSLv2Hello");
        //protocols.remove("SSLv3");
        //protocols.remove("TLSv1.2"); // Cause problem in Firefox

        return new CustomSSLWebSocketServerFactory(sslContext, protocols.toArray(new String[]{}), ciphers.toArray(new String[]{}));
    }

    private static X509Certificate readCertificate(File certificate) throws Exception {
        String data = new String(getBytes(certificate));
        String[] tokens = data.split("-----BEGIN CERTIFICATE-----");
        tokens = tokens[1].split("-----END CERTIFICATE-----");
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(tokens[0])));
    }

    private static RSAPrivateKey readKey(File key) throws Exception {
        String data = new String(getBytes(key));
        String[] tokens = data.split("-----BEGIN PRIVATE KEY-----");
        tokens = tokens[1].split("-----END PRIVATE KEY-----");

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(DatatypeConverter.parseBase64Binary(tokens[0]));
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static ArrayList<X509Certificate> readCaBundle(File caBundle) throws Exception {
        if (!caBundle.exists()) {
            return new ArrayList<>();
        }

        String data = new String(getBytes(caBundle));
        String[] tokens = data.split("-----BEGIN CERTIFICATE-----");
        ArrayList<X509Certificate> result = new ArrayList<>();

        for (String token : tokens) {
            if (token.isEmpty()) continue;
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            String[] certParts = tokens[1].split("-----END CERTIFICATE-----");
            result.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(certParts[0]))));
        }

        return result;
    }

    private static byte[] getBytes(File file) throws Exception {
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis = new FileInputStream(file);
        fis.read(bytesArray);
        fis.close();

        return bytesArray;
    }
}
