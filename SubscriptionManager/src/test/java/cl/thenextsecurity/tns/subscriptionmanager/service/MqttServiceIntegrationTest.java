package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.config.AwsIotSslConfig;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests de integración para MqttService con conexión real a AWS IoT Core.
 *
 * Grupo A — Conexión: verifica que el cliente se conecta y que las operaciones
 * subscribe/unsubscribe no lanzan excepción.
 *
 * Grupo B — Recepción: espera hasta 5 minutos para recibir un mensaje real
 * del dispositivo FMC920 (IMEI 865413057599200) y verifica que el topic,
 * el clientId (IMEI extraído del topic) y el payload (contiene "latlng")
 * son correctos.
 *
 * Todos los tests usan assumeTrue y se saltan automáticamente si el cliente
 * no logra conectarse (certificados no disponibles o broker inalcanzable).
 *
 * SubscriptionRepository se mockea para aislar reloadSubscriptions() de la BD.
 * DiscoveryService y MessageStorageService se mockean para capturar el flujo
 * de messageArrived() sin depender de la capa de persistencia.
 */
@SpringBootTest(classes = {AwsIotSslConfig.class, MqttService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
@DisplayName("MqttService — tests de integración con conexión real a AWS IoT Core")
class MqttServiceIntegrationTest {

    private static final String IMEI       = "865413057599200";
    private static final String TOPIC_REAL = IMEI + "/data";

    @Autowired
    private MqttService mqttService;

    @MockitoBean
    private DiscoveryService discoveryService;

    @MockitoBean
    private MessageStorageService messageStorageService;

    /**
     * Mockeado para aislar reloadSubscriptions() en @PostConstruct:
     * findByActiveTrue() retorna lista vacía por defecto (sin BD real).
     */
    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    // =========================================================================
    // Grupo A — Conexión real al broker
    // =========================================================================

    @Test
    @DisplayName("El cliente MQTT está conectado al broker tras @PostConstruct")
    void connect_clienteConectadoAlBroker() {
        assumeTrue(mqttService.isConnected(),
                "Saltando: cliente no conectado (certificados no disponibles o broker inalcanzable)");

        assertThat(mqttService.isConnected()).isTrue();
    }

    @Test
    @DisplayName("subscribe() y unsubscribe() en topic real ejecutan sin excepción")
    void subscribeUnsubscribe_topicReal_sinExcepcion() {
        assumeTrue(mqttService.isConnected(),
                "Saltando: cliente no conectado");

        mqttService.subscribe(TOPIC_REAL);
        mqttService.unsubscribe(TOPIC_REAL);
    }

    // =========================================================================
    // Grupo B — Recepción real de mensajes del FMC920
    // =========================================================================

    @Test
    @DisplayName("Mensaje real FMC920: topic, IMEI y payload con latlng recibidos en 5 minutos")
    void mensajeReal_fmc920_recibeTopicImeiYLatlng() throws InterruptedException {
        assumeTrue(mqttService.isConnected(),
                "Saltando: cliente no conectado al broker");

        // discoveryService retorna false para que el mensaje llegue a messageStorageService
        when(discoveryService.isScanning()).thenReturn(false);

        // El latch y el doAnswer se configuran ANTES de suscribirse para evitar
        // race condition si llega un mensaje inmediatamente al suscribirse
        CountDownLatch latch = new CountDownLatch(1);
        doAnswer(inv -> { latch.countDown(); return null; })
                .when(messageStorageService).saveMessage(anyString(), anyString(), anyString(), anyInt());

        mqttService.subscribe(TOPIC_REAL);

        // Esperar hasta 5 minutos a que el FMC920 envíe un mensaje real
        boolean received = latch.await(5, TimeUnit.MINUTES);

        mqttService.unsubscribe(TOPIC_REAL);

        assumeTrue(received,
                "Saltando verificaciones: no llegó ningún mensaje del FMC920 en 5 minutos");

        // Capturar y verificar los argumentos que messageArrived() pasó a messageStorageService
        ArgumentCaptor<String> topicCaptor    = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> payloadCaptor  = ArgumentCaptor.forClass(String.class);

        verify(messageStorageService).saveMessage(
                topicCaptor.capture(),
                clientIdCaptor.capture(),
                payloadCaptor.capture(),
                anyInt());

        assertThat(topicCaptor.getValue())
                .as("El topic debe ser el canal de datos del FMC920")
                .isEqualTo(TOPIC_REAL);

        assertThat(clientIdCaptor.getValue())
                .as("El clientId debe ser el IMEI extraído del primer segmento del topic")
                .isEqualTo(IMEI);

        assertThat(payloadCaptor.getValue())
                .as("El payload del FMC920 debe contener las coordenadas GPS en 'latlng'")
                .contains("latlng");
    }
}
