package tigerworkshop.webapphardwarebridge.utils;

import lombok.extern.log4j.Log4j2;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.regex.Pattern;

@Log4j2
public class CertificateGenerator {
    private static final String CERTIFICATE_ALGORITHM = "RSA";
    private static final String CERTIFICATE_ISSUER = "CN=127.0.0.1";
    private static final String CERTIFICATE_DOMAIN = "CN=127.0.0.1";
    private static final int CERTIFICATE_BITS = 2048;

    private static final String IPV4_REGEX = "(([0-1]?[0-9]{1,2}\\.)|(2[0-4][0-9]\\.)|(25[0-5]\\.)){3}(([0-1]?[0-9]{1,2})|(2[0-4][0-9])|(25[0-5]))";
    private static final Pattern IPV4_PATTERN = Pattern.compile(IPV4_REGEX);

    public static void generateSelfSignedCertificate(String address, String certificatePath, String keyPath) {
        Security.addProvider(new BouncyCastleProvider());

        if (!isCertificateAndKeyExist(certificatePath, keyPath)) {
            try {
                log.info("Certificate or private key does not exist, attempt to generate.");

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(CERTIFICATE_ALGORITHM);
                keyPairGenerator.initialize(CERTIFICATE_BITS, new SecureRandom());
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                X500Name issuer = new X500Name(CERTIFICATE_ISSUER);
                X500Name subject = new X500Name(CERTIFICATE_DOMAIN);
                BigInteger serialNumber = new BigInteger(64, new SecureRandom());
                Date validFrom = new Date();
                Date validTo = new Date(System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 365 * 10));
                SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());
                ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").setProvider(new BouncyCastleProvider()).build(keyPair.getPrivate());

                X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(issuer, serialNumber, validFrom, validTo, subject, subPubKeyInfo);

                final GeneralNames subjectAltNames;
                if (IPV4_PATTERN.matcher(address).matches()) {
                    subjectAltNames = new GeneralNames(new GeneralName(GeneralName.iPAddress, address));
                } else {
                    subjectAltNames = new GeneralNames(new GeneralName(GeneralName.dNSName, address));
                }
                certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);

                X509CertificateHolder certificateHolder = certificateBuilder.build(signer);
                X509Certificate cert = new JcaX509CertificateConverter().getCertificate(certificateHolder);

                log.info("Certificate and private key generated.");

                File directory = new File("tls");
                if (!directory.isDirectory()) {
                    directory.mkdir();
                }

                saveCert(cert, certificatePath);
                saveKey(keyPair.getPrivate(), keyPath);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.info("Certificate and private key already exists.");
        }
    }

    public static Boolean isCertificateAndKeyExist(String certificatePath, String keyPath) {
        File certificate = new File(certificatePath);
        File privateKey = new File(keyPath);

        return certificate.exists() && privateKey.exists();
    }

    private static void saveCert(X509Certificate cert, String certificatePath) {
        try {
            JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(certificatePath));
            writer.writeObject(cert);
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void saveKey(PrivateKey key, String keyPath) {
        try {
            JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(keyPath));
            writer.writeObject(new JcaPKCS8Generator(key, null));
            writer.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

}
