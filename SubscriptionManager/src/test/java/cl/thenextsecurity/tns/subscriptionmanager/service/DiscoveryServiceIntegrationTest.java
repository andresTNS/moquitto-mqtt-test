package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.DetectedTopic;
import cl.thenextsecurity.tns.subscriptionmanager.repository.DetectedTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests de integración para DiscoveryService con Spring context y H2 in-memory.
 *
 * Verifica el ciclo completo de descubrimiento MQTT:
 * 1. runDiscovery() suscribe al wildcard "#", espera, desuscribe y procesa topics.
 * 2. processDiscoveredTopics() crea/actualiza registros en DetectedTopicRepository.
 * 3. Topics ausentes en el escaneo actual se marcan como inactivos.
 *
 * MqttService y SseService se mockean para evitar conexión real a broker y
 * complejidad de SSE. DetectedTopicRepository es real con H2 auto-configurada.
 *
 * discoveryDurationMs se acorta a 50ms via ReflectionTestUtils para tests rápidos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Tag("integration")
@DisplayName("DiscoveryService — tests de integración con Spring y H2")
class DiscoveryServiceIntegrationTest {

    @Autowired
    private DiscoveryService discoveryService;

    @Autowired
    private DetectedTopicRepository detectedTopicRepository;

    @MockitoBean
    private MqttService mqttService;

    @MockitoBean
    private SseService sseService;

    @BeforeEach
    void setUp() {
        // Acortar discoveryDurationMs a 50ms para que los tests no esperen 30 segundos
        ReflectionTestUtils.setField(discoveryService, "discoveryDurationMs", 50L);

        // Limpiar BD entre tests
        detectedTopicRepository.deleteAll();
    }

    // =========================================================================
    // runDiscovery() — ciclo completo de descubrimiento
    // =========================================================================

    @Test
    @DisplayName("runDiscovery() ciclo completo llama subscribe('#') y unsubscribe('#')")
    void runDiscovery_cicloCompleto_subscribeYUnsubscribe() {
        discoveryService.runDiscovery();

        verify(mqttService).subscribe("#");
        verify(mqttService).unsubscribe("#");
    }

    @Test
    @DisplayName("runDiscovery() después de completarse, isScanning() retorna false")
    void runDiscovery_despuesDeCompletar_isScanningEsFalse() {
        discoveryService.runDiscovery();

        assertThat(discoveryService.isScanning()).isFalse();
    }

    @Test
    @DisplayName("runDiscovery() con isScanning=true no inicia nuevo escaneo (guard clause)")
    void runDiscovery_yaEscaneando_noIniciaNuevoEscaneo() {
        // Simular que ya está escaneando
        ReflectionTestUtils.setField(discoveryService, "isScanning", true);

        discoveryService.runDiscovery();

        // subscribe() NO debe ser llamado porque el guard clause lo previene
        verify(mqttService, never()).subscribe(anyString());
    }

    // =========================================================================
    // processDiscoveredTopics() — persistencia en BD
    // =========================================================================

    @Test
    @DisplayName("processDiscoveredTopics() con topic nuevo crea registro en BD")
    void processDiscoveredTopics_topicNuevo_creaRegistroEnBD() {
        discoveryService.recordDiscoveredTopic("sensor/temperatura", "esp32-sala-01");

        discoveryService.processDiscoveredTopics();

        DetectedTopic guardado = detectedTopicRepository.findByTopicName("sensor/temperatura")
                .orElseThrow();

        assertThat(guardado.getTopicName()).isEqualTo("sensor/temperatura");
        assertThat(guardado.getLastClientId()).isEqualTo("esp32-sala-01");
        assertThat(guardado.isActive()).isTrue();
        assertThat(guardado.getMessageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("processDiscoveredTopics() con topic existente actualiza lastSeen y messageCount")
    void processDiscoveredTopics_topicExistente_actualizaLastSeenYMessageCount() {
        // Insertar topic existente con messageCount=5
        DetectedTopic existente = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .lastClientId("esp32-old")
                .messageCount(5)
                .lastSeen(LocalDateTime.now().minusDays(1))
                .build();
        detectedTopicRepository.save(existente);

        // Registrar el mismo topic en el escaneo actual
        discoveryService.recordDiscoveredTopic("sensor/temperatura", "esp32-new");

        discoveryService.processDiscoveredTopics();

        DetectedTopic actualizado = detectedTopicRepository.findByTopicName("sensor/temperatura")
                .orElseThrow();

        assertThat(actualizado.getLastClientId()).isEqualTo("esp32-new");
        assertThat(actualizado.getMessageCount()).isEqualTo(6);
        assertThat(actualizado.getLastSeen()).isAfter(existente.getLastSeen());
        assertThat(actualizado.isActive()).isTrue();
    }

    @Test
    @DisplayName("processDiscoveredTopics() marca como inactivos los topics ausentes en escaneo actual")
    void processDiscoveredTopics_topicAusente_marcaComoInactivo() {
        // Insertar topic activo que NO aparecerá en el escaneo actual
        DetectedTopic ausente = DetectedTopic.builder()
                .topicName("sensor/old")
                .lastClientId("esp32-old")
                .active(true)
                .build();
        detectedTopicRepository.save(ausente);

        // Escaneo actual registra un topic diferente
        discoveryService.recordDiscoveredTopic("sensor/new", "esp32-new");

        discoveryService.processDiscoveredTopics();

        DetectedTopic ausenteActualizado = detectedTopicRepository.findByTopicName("sensor/old")
                .orElseThrow();

        assertThat(ausenteActualizado.isActive()).isFalse();
    }

    // =========================================================================
    // schedulePeriodicDiscovery() — configuración enabled/disabled
    // =========================================================================

    @Test
    @DisplayName("schedulePeriodicDiscovery() con discoveryEnabled=false no ejecuta descubrimiento")
    void schedulePeriodicDiscovery_discoveryDisabled_noEjecuta() {
        discoveryService.updateConfig(false, 300000L, 30000L);

        discoveryService.schedulePeriodicDiscovery();

        // Si discovery está disabled, no debe llamar a subscribe()
        // (el método lanza un virtual thread, pero la lógica interna lo previene)
        verify(mqttService, never()).subscribe(anyString());
    }
}
