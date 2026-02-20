package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.repository.DetectedTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para DiscoveryService sin contexto Spring ni BD.
 * Se verifican los métodos de gestión de estado y registro de topics
 * observados durante el escaneo MQTT.
 *
 * DiscoveryService gestiona el descubrimiento periódico de topics MQTT
 * mediante suscripción al wildcard "#" y registro de los mensajes recibidos
 * durante una ventana de tiempo (discoveryDurationMs).
 *
 * Estado interno accedido via ReflectionTestUtils:
 * - isScanning (volatile boolean)
 * - discoveredTopics (Set<String>)
 * - discoveredClientIdsByTopic (Map<String, String>)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscoveryService — tests unitarios de gestión de estado")
class DiscoveryServiceTest {

    @Mock private DetectedTopicRepository detectedTopicRepository;
    @Mock private SseService              sseService;
    @Mock private MqttService             mqttService;

    private DiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        discoveryService = new DiscoveryService(detectedTopicRepository, sseService);
        // MqttService se inyecta via @Lazy setter, simular inyección manual
        discoveryService.setMqttService(mqttService);
    }

    // =========================================================================
    // Estado inicial
    // =========================================================================

    @Test
    @DisplayName("isScanning() en estado inicial retorna false")
    void isScanning_estadoInicial_retornaFalse() {
        assertThat(discoveryService.isScanning()).isFalse();
    }

    // =========================================================================
    // recordDiscoveredTopic() — registro de topics observados
    // =========================================================================

    @Test
    @DisplayName("recordDiscoveredTopic con topic válido lo agrega a discoveredTopics")
    void recordDiscoveredTopic_topicValido_agregaADiscoveredTopics() {
        discoveryService.recordDiscoveredTopic("sensor/temperatura", "esp32-sala-01");

        @SuppressWarnings("unchecked")
        Set<String> discoveredTopics = (Set<String>) ReflectionTestUtils.getField(
                discoveryService, "discoveredTopics");

        assertThat(discoveredTopics).contains("sensor/temperatura");
    }

    @Test
    @DisplayName("recordDiscoveredTopic con clientId válido lo almacena en el mapa")
    void recordDiscoveredTopic_conClientIdValido_almacenaEnMapa() {
        discoveryService.recordDiscoveredTopic("sensor/temperatura", "esp32-sala-01");

        @SuppressWarnings("unchecked")
        Map<String, String> clientIdsByTopic = (Map<String, String>) ReflectionTestUtils.getField(
                discoveryService, "discoveredClientIdsByTopic");

        assertThat(clientIdsByTopic).containsEntry("sensor/temperatura", "esp32-sala-01");
    }

    @Test
    @DisplayName("recordDiscoveredTopic con clientId null no almacena en el mapa")
    void recordDiscoveredTopic_clientIdNulo_noAlmacenaEnMapa() {
        discoveryService.recordDiscoveredTopic("sensor/temperatura", null);

        @SuppressWarnings("unchecked")
        Map<String, String> clientIdsByTopic = (Map<String, String>) ReflectionTestUtils.getField(
                discoveryService, "discoveredClientIdsByTopic");

        assertThat(clientIdsByTopic).doesNotContainKey("sensor/temperatura");
    }

    @Test
    @DisplayName("recordDiscoveredTopic con clientId en blanco no almacena en el mapa")
    void recordDiscoveredTopic_clientIdEnBlanco_noAlmacenaEnMapa() {
        discoveryService.recordDiscoveredTopic("sensor/temperatura", "   ");

        @SuppressWarnings("unchecked")
        Map<String, String> clientIdsByTopic = (Map<String, String>) ReflectionTestUtils.getField(
                discoveryService, "discoveredClientIdsByTopic");

        assertThat(clientIdsByTopic).doesNotContainKey("sensor/temperatura");
    }

    // =========================================================================
    // updateConfig() — configuración en runtime
    // =========================================================================

    @Test
    @DisplayName("updateConfig() modifica los 3 campos de configuración internos")
    void updateConfig_modificaCamposInternos() {
        discoveryService.updateConfig(false, 60000L, 10000L);

        assertThat(discoveryService.isDiscoveryEnabled()).isFalse();
        assertThat(discoveryService.getDiscoveryIntervalMs()).isEqualTo(60000L);
        assertThat(discoveryService.getDiscoveryDurationMs()).isEqualTo(10000L);
    }
}
