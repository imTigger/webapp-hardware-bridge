package tigerworkshop.webapphardwarebridge.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;

public class TLSUtil {
    private static final String CERTIFICATE_ALGORITHM = "RSA";
    private static final String CERTIFICATE_ISSUER = "CN=WebApp Hardware Bridge";
    private static final String CERTIFICATE_DOMAIN = "CN=127.0.0.1";
    private static final int CERTIFICATE_BITS = 2048;
    private static final String CERTIFICATE_PATH = "tls/tls.crt";
    private static final String KEY_PATH = "tls/tls.key";
    private static Logger logger = LoggerFactory.getLogger("TLSUtil");

    public static SSLContext getContext() {
        File certificate = new File(CERTIFICATE_PATH);
        File privateKey = new File(KEY_PATH);

        if (!certificate.exists() || !privateKey.exists()) {
            try {
                logger.info("Cert/Key does not exist, will attempt to generate.");

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
                keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                X509Certificate cert = createCertificate(keyPair);

                saveKey(keyPair.getPrivate());
                saveCert(cert);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }

        logger.info("Creating SSL context");

        SSLContext sslContext;
        String password = "webapp-hardware-bridge";
        try {
            Security.addProvider(new BouncyCastleProvider());

            byte[] certBytes = parseDERFromPEM(getBytes(certificate), "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
            byte[] keyBytes = parseDERFromPEM(getBytes(privateKey), "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");

            X509Certificate cert = generateCertificateFromDER(certBytes);
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);

            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", cert);
            keystore.setKeyEntry("key-alias", key, password.toCharArray(), new Certificate[]{cert});

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keystore);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, password.toCharArray());

            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);


        } catch (Exception e) {
            sslContext = null;
            e.printStackTrace();
        }
        return sslContext;
    }

    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey) factory.generatePrivate(spec);
    }

    private static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private static byte[] getBytes(File file) {
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesArray;
    }

    // Method to create self-signed certificate
    private static X509Certificate createCertificate(KeyPair keyPair) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        X500Name issuer = new X500Name(CERTIFICATE_ISSUER);
        X500Name subject = new X500Name(CERTIFICATE_DOMAIN);
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        Date validFrom = new Date();
        Date validTo = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
        SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider(new BouncyCastleProvider()).build(keyPair.getPrivate());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(issuer, serialNumber, validFrom, validTo, subject, subPubKeyInfo);
        X509CertificateHolder certificateHolder = certificateBuilder.build(signer);


        return new JcaX509CertificateConverter().getCertificate(certificateHolder);
    }

    private static void saveKey(PrivateKey key) {
        try {
            JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(new File(KEY_PATH)));
            writer.writeObject(key);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveCert(X509Certificate cert) {
        try {
            JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(new File(CERTIFICATE_PATH)));
            writer.writeObject(cert);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
