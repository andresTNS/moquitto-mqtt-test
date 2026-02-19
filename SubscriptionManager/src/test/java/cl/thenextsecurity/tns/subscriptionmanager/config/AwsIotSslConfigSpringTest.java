package cl.thenextsecurity.tns.subscriptionmanager.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests de integración para AwsIotSslConfig con contexto Spring real.
 * Requieren que application.properties exista con las rutas a los certificados
 * reales de AWS IoT Core (archivo gitignoreado, solo disponible en entorno local).
 *
 * Grupo 2 — Contexto Spring: verifica que el bean y sus @Value se cargan correctamente.
 * Grupo 3 — Archivos reales: verifica que los archivos existen y tienen formato PEM válido.
 * Grupo 4 — Conexión real: verifica handshake TLS contra AWS IoT Core (puerto 8883).
 *
 * Los tests de Grupos 3 y 4 usan assumeTrue y se saltan automáticamente si los
 * archivos de certificados no están disponibles en el entorno de ejecución.
 */
@SpringBootTest(classes = AwsIotSslConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
@DisplayName("AwsIotSslConfig — tests con contexto Spring y certificados reales")
class AwsIotSslConfigSpringTest {

    @Autowired
    private AwsIotSslConfig sslConfig;

    @Value("${mqtt.tls.ca}")
    private String caPath;

    @Value("${mqtt.tls.cert}")
    private String certPath;

    @Value("${mqtt.tls.key}")
    private String keyPath;

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    // =========================================================================
    // Grupo 2 — Contexto Spring
    // =========================================================================

    @Test
    @DisplayName("El bean AwsIotSslConfig está registrado en el contexto Spring")
    void awsIotSslConfig_esBeanRegistradoEnContexto() {
        assertThat(sslConfig).isNotNull();
    }

    @Test
    @DisplayName("Las 3 rutas de certificados son inyectadas por @Value desde application.properties")
    void propiedadesTls_sonInyectadasDesdeApplicationProperties() {
        assertThat(caPath).isNotBlank();
        assertThat(certPath).isNotBlank();
        assertThat(keyPath).isNotBlank();
    }

    // =========================================================================
    // Grupo 3 — Verificación de archivos reales
    // =========================================================================

    @Test
    @DisplayName("Los 3 archivos de certificados existen en disco")
    void archivos_existenEnDisco() {
        assumeTrue(new File(caPath).exists(),   "Saltando: CA no encontrada en "   + caPath);
        assumeTrue(new File(certPath).exists(), "Saltando: cert no encontrado en " + certPath);
        assumeTrue(new File(keyPath).exists(),  "Saltando: key no encontrada en "  + keyPath);

        assertThat(new File(caPath)).exists();
        assertThat(new File(certPath)).exists();
        assertThat(new File(keyPath)).exists();
    }

    @Test
    @DisplayName("CA tiene header PEM correcto: BEGIN CERTIFICATE")
    void caFile_tieneHeaderPemCorrecto() throws Exception {
        assumeTrue(new File(caPath).exists(), "Saltando: CA no encontrada");
        assertThat(Files.readString(Path.of(caPath))).contains("BEGIN CERTIFICATE");
    }

    @Test
    @DisplayName("Certificado del dispositivo tiene header PEM correcto: BEGIN CERTIFICATE")
    void certFile_tieneHeaderPemCorrecto() throws Exception {
        assumeTrue(new File(certPath).exists(), "Saltando: cert no encontrado");
        assertThat(Files.readString(Path.of(certPath))).contains("BEGIN CERTIFICATE");
    }

    @Test
    @DisplayName("Clave privada tiene header PEM PKCS#1 correcto: BEGIN RSA PRIVATE KEY")
    void keyFile_tieneHeaderPemPkcs1() throws Exception {
        assumeTrue(new File(keyPath).exists(), "Saltando: key no encontrada");
        assertThat(Files.readString(Path.of(keyPath))).contains("BEGIN RSA PRIVATE KEY");
    }

    @Test
    @DisplayName("getSslSocketFactory() construye el factory con los certificados reales")
    void getSslSocketFactory_conCertsReales_retornaFactoryValida() throws Exception {
        assumeTrue(new File(caPath).exists(),   "Saltando: archivos de certs no disponibles");
        assumeTrue(new File(certPath).exists(), "Saltando: archivos de certs no disponibles");
        assumeTrue(new File(keyPath).exists(),  "Saltando: archivos de certs no disponibles");

        SSLSocketFactory factory = sslConfig.getSslSocketFactory();
        assertThat(factory).isNotNull();
    }

    // =========================================================================
    // Grupo 4 — Conexión real a AWS IoT Core
    // =========================================================================

    @Test
    @DisplayName("Handshake TLS exitoso con el endpoint de AWS IoT Core (puerto 8883)")
    void conexionTls_handshakeExitoso_conAwsIotCore() throws Exception {
        assumeTrue(new File(caPath).exists(),   "Saltando: archivos de certs no disponibles");
        assumeTrue(new File(certPath).exists(), "Saltando: archivos de certs no disponibles");
        assumeTrue(new File(keyPath).exists(),  "Saltando: archivos de certs no disponibles");

        // Extraer host del broker URL con formato ssl://host:8883
        String host = brokerUrl.replaceFirst("ssl://", "").replaceFirst(":.*", "");

        SSLSocketFactory factory = sslConfig.getSslSocketFactory();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, 8883)) {
            socket.setSoTimeout(5000);
            socket.startHandshake();
            assertThat(socket.isConnected()).isTrue();
        }
    }
}
