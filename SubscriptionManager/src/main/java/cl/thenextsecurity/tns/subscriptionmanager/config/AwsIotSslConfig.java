package cl.thenextsecurity.tns.subscriptionmanager.config;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.FileReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Construye el SSLSocketFactory necesario para conectar a AWS IoT Core.
 *
 * Proceso:
 * 1. Carga AmazonRootCA1.pem como TrustStore (confiar en AWS)
 * 2. Carga el certificado del dispositivo + clave privada como KeyStore
 *    (BouncyCastle lee el formato PKCS#1 nativo de AWS sin conversión)
 * 3. Construye SSLContext con TLSv1.2 (requerido por AWS IoT Core)
 *
 * Las rutas se resuelven de forma relativa al directorio de ejecución
 * de la aplicación (SubscriptionManager/).
 */
@Component
@Slf4j
public class AwsIotSslConfig {

    @Value("${mqtt.tls.ca}")
    private String caPath;

    @Value("${mqtt.tls.cert}")
    private String certPath;

    @Value("${mqtt.tls.key}")
    private String keyPath;

    /**
     * Construye y retorna el SSLSocketFactory configurado con los
     * certificados de AWS IoT Core.
     *
     * @return SSLSocketFactory listo para usar en MqttConnectOptions
     * @throws Exception si algún certificado no se puede leer o parsear
     */
    public SSLSocketFactory getSslSocketFactory() throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        // 1. Cargar CA de Amazon → TrustStore
        X509Certificate caCert;
        try (FileInputStream caInput = new FileInputStream(caPath)) {
            caCert = (X509Certificate) cf.generateCertificate(caInput);
        }

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("aws-ca", caCert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        // 2. Cargar certificado del dispositivo
        X509Certificate deviceCert;
        try (FileInputStream certInput = new FileInputStream(certPath)) {
            deviceCert = (X509Certificate) cf.generateCertificate(certInput);
        }

        // 3. Cargar clave privada PKCS#1 usando BouncyCastle
        PrivateKey privateKey;
        try (PEMParser pemParser = new PEMParser(new FileReader(keyPath))) {
            PEMKeyPair keyPair = (PEMKeyPair) pemParser.readObject();
            privateKey = new JcaPEMKeyConverter().getKeyPair(keyPair).getPrivate();
        }

        // 4. Construir KeyStore con certificado + clave privada del dispositivo
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setKeyEntry("device-key", privateKey, "".toCharArray(), new Certificate[]{deviceCert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "".toCharArray());

        // 5. Construir SSLContext con TLSv1.2 requerido por AWS IoT Core
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        log.info("SSLSocketFactory configurado correctamente para AWS IoT Core");
        return sslContext.getSocketFactory();
    }
}
