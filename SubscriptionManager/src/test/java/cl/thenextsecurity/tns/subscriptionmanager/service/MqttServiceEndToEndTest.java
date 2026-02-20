package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import cl.thenextsecurity.tns.subscriptionmanager.repository.MqttMessageRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test end-to-end: MqttService → MessageStorageService → MySQL.
 *
 * Verifica el flujo completo con broker AWS IoT Core y base de datos reales:
 * 1. El cliente MQTT recibe un mensaje real del FMC920.
 * 2. messageArrived() extrae el IMEI del topic y llama a MessageStorageService.
 * 3. MessageStorageService persiste el registro en MySQL porque existe una
 *    suscripción activa para el topic del FMC920.
 * 4. El test verifica el registro en BD: topic, IMEI como clientId, latlng
 *    en el payload y receivedAt no nulo.
 *
 * DiscoveryService se mockea para que isScanning() retorne false siempre
 * y para evitar que el @Scheduled de descubrimiento interfiera durante los
 * 5 minutos de espera del test.
 *
 * El @BeforeEach inserta una suscripción activa si no existe, y el @AfterEach
 * limpia los mensajes creados durante el test y la suscripción si fue creada
 * por este test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Tag("integration")
@DisplayName("MqttService → MessageStorageService → MySQL — test end-to-end con broker real")
class MqttServiceEndToEndTest {

    private static final String IMEI       = "865413057599200";
    private static final String TOPIC_REAL = IMEI + "/data";

    @Autowired private MqttService            mqttService;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private MqttMessageRepository  mqttMessageRepository;

    /**
     * Mockeado para garantizar isScanning()=false durante todo el test
     * y para evitar que el @Scheduled de descubrimiento compita con la
     * recepción de mensajes del FMC920.
     */
    @MockitoBean
    private DiscoveryService discoveryService;

    private LocalDateTime testStart;
    private boolean suscripcionCreadaPorElTest;

    @BeforeEach
    void setUp() {
        testStart = LocalDateTime.now();

        // Insertar suscripción activa si no existe, para que MessageStorageService
        // no descarte los mensajes del FMC920 por falta de suscripción
        if (!subscriptionRepository.existsByTopicName(TOPIC_REAL)) {
            subscriptionRepository.save(Subscription.builder()
                    .topicName(TOPIC_REAL)
                    .active(true)
                    .build());
            suscripcionCreadaPorElTest = true;
        } else {
            suscripcionCreadaPorElTest = false;
        }
    }

    @AfterEach
    void tearDown() {
        // Eliminar solo los mensajes creados durante este test (por receivedAt >= testStart)
        List<MqttMessage> mensajesDelTest = mqttMessageRepository
                .findByTopicAndReceivedAtBetween(TOPIC_REAL, testStart, LocalDateTime.now(), Pageable.unpaged())
                .getContent();
        mqttMessageRepository.deleteAll(mensajesDelTest);

        // Eliminar la suscripción solo si la creamos nosotros
        if (suscripcionCreadaPorElTest) {
            subscriptionRepository.deleteByTopicName(TOPIC_REAL);
        }
    }

    @Test
    @DisplayName("Mensaje real FMC920 se persiste en MySQL con topic, IMEI y latlng correctos")
    void mensajeRealFmc920_persisteEnMysqlConDatosCorrectos() throws InterruptedException {
        assumeTrue(mqttService.isConnected(),
                "Saltando: cliente no conectado al broker (certificados no disponibles)");

        long countAntes = mqttMessageRepository.countByTopic(TOPIC_REAL);

        mqttService.subscribe(TOPIC_REAL);

        // Polling cada segundo hasta 5 minutos esperando que el FMC920 envíe un mensaje
        Instant timeout = Instant.now().plusSeconds(300);
        while (mqttMessageRepository.countByTopic(TOPIC_REAL) <= countAntes) {
            if (Instant.now().isAfter(timeout)) break;
            Thread.sleep(1000);
        }

        mqttService.unsubscribe(TOPIC_REAL);

        boolean received = mqttMessageRepository.countByTopic(TOPIC_REAL) > countAntes;
        assumeTrue(received,
                "Saltando verificaciones: no llegó ningún mensaje del FMC920 en 5 minutos");

        // Recuperar el mensaje persistido durante el test
        List<MqttMessage> mensajes = mqttMessageRepository
                .findByTopicAndReceivedAtBetween(TOPIC_REAL, testStart, LocalDateTime.now(), Pageable.unpaged())
                .getContent();

        assertThat(mensajes).isNotEmpty();

        MqttMessage mensaje = mensajes.get(0);
        assertThat(mensaje.getTopic())
                .as("El topic persistido debe ser el canal de datos del FMC920")
                .isEqualTo(TOPIC_REAL);
        assertThat(mensaje.getClientId())
                .as("El clientId debe ser el IMEI extraído del topic por MqttService")
                .isEqualTo(IMEI);
        assertThat(mensaje.getPayload())
                .as("El payload del FMC920 debe contener las coordenadas GPS en 'latlng'")
                .contains("latlng");
        assertThat(mensaje.getReceivedAt())
                .as("receivedAt debe ser asignado automáticamente por @PrePersist")
                .isNotNull();
    }
}
