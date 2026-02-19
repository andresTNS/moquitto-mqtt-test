package cl.thenextsecurity.tns.subscriptionmanager.config;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import javax.net.ssl.SSLSocketFactory;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para AwsIotSslConfig sin contexto Spring.
 * Los certificados se generan programáticamente en archivos temporales
 * para no depender de los certs reales de AWS (que están gitignoreados).
 */
@DisplayName("AwsIotSslConfig — tests unitarios (sin contexto Spring)")
class AwsIotSslConfigTest {

    @TempDir
    Path tempDir;

    private AwsIotSslConfig sslConfig;
    private Path caFile;
    private Path certFile;
    private Path keyFile;

    @BeforeEach
    void setUp() throws Exception {
        sslConfig = new AwsIotSslConfig();

        // Generar par de claves RSA 2048
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();

        // Construir certificado autofirmado con BouncyCastle
        X500Name subject = new X500Name("CN=test-device");
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 86400000L); // +24h

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(
                new JcaX509v3CertificateBuilder(
                        subject, BigInteger.ONE, notBefore, notAfter,
                        subject, keyPair.getPublic())
                        .build(signer));

        // Escribir CA cert en PEM (BEGIN CERTIFICATE)
        caFile = tempDir.resolve("ca.pem");
        try (JcaPEMWriter w = new JcaPEMWriter(new FileWriter(caFile.toFile()))) {
            w.writeObject(cert);
        }

        // Escribir device cert — mismo autofirmado para simplificar el test
        certFile = tempDir.resolve("cert.pem");
        try (JcaPEMWriter w = new JcaPEMWriter(new FileWriter(certFile.toFile()))) {
            w.writeObject(cert);
        }

        // Escribir clave privada en formato PKCS#1 (BEGIN RSA PRIVATE KEY)
        // BouncyCastle lee este formato nativo sin conversión
        keyFile = tempDir.resolve("key.pem");
        PrivateKeyInfo pkInfo = PrivateKeyInfo.getInstance(keyPair.getPrivate().getEncoded());
        RSAPrivateKey rsaKey = RSAPrivateKey.getInstance(pkInfo.parsePrivateKey());
        try (JcaPEMWriter w = new JcaPEMWriter(new FileWriter(keyFile.toFile()))) {
            w.writeObject(new PemObject("RSA PRIVATE KEY", rsaKey.getEncoded()));
        }

        // Inyectar rutas via reflexión — los campos @Value de Lombok son privados
        ReflectionTestUtils.setField(sslConfig, "caPath",   caFile.toString());
        ReflectionTestUtils.setField(sslConfig, "certPath", certFile.toString());
        ReflectionTestUtils.setField(sslConfig, "keyPath",  keyFile.toString());
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    @DisplayName("Retorna SSLSocketFactory no nula con certificados autofirmados válidos")
    void getSslSocketFactory_conCertsValidos_retornaFactoryNoNula() throws Exception {
        SSLSocketFactory factory = sslConfig.getSslSocketFactory();
        assertThat(factory).isNotNull();
    }

    // =========================================================================
    // Rutas inexistentes
    // =========================================================================

    @Test
    @DisplayName("Lanza excepción cuando la ruta del CA no existe")
    void getSslSocketFactory_conCaInexistente_lanzaExcepcion() {
        ReflectionTestUtils.setField(sslConfig, "caPath", "/ruta/inexistente/ca.pem");
        assertThatThrownBy(sslConfig::getSslSocketFactory).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Lanza excepción cuando la ruta del certificado no existe")
    void getSslSocketFactory_conCertInexistente_lanzaExcepcion() {
        ReflectionTestUtils.setField(sslConfig, "certPath", "/ruta/inexistente/cert.pem");
        assertThatThrownBy(sslConfig::getSslSocketFactory).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Lanza excepción cuando la ruta de la clave privada no existe")
    void getSslSocketFactory_conKeyInexistente_lanzaExcepcion() {
        ReflectionTestUtils.setField(sslConfig, "keyPath", "/ruta/inexistente/key.pem");
        assertThatThrownBy(sslConfig::getSslSocketFactory).isInstanceOf(Exception.class);
    }
}
