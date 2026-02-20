package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import cl.thenextsecurity.tns.subscriptionmanager.repository.MqttMessageRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MessageStorageService sin contexto Spring ni BD.
 * Se verifican los 5 comportamientos clave de saveMessage():
 * 1. Guarda la entidad con topic e IMEI correctos cuando la suscripción es activa.
 * 2. Preserva el payload GPS completo del FMC920 (incluyendo coordenadas latlng).
 * 3. Notifica al SSE con el mensaje guardado.
 * 4. No guarda ni notifica cuando el topic no tiene suscripción en BD.
 * 5. No guarda ni notifica cuando la suscripción existe pero está inactiva.
 *
 * Datos reales del dispositivo FMC920 conectado a AWS IoT Core:
 * IMEI 865413057599200, topic 865413057599200/data, coordenadas GPS reales.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageStorageService — tests unitarios de saveMessage()")
class MessageStorageServiceTest {

    // =========================================================================
    // Datos reales del dispositivo FMC920 en producción
    // =========================================================================

    private static final String IMEI       = "865413057599200";
    private static final String TOPIC_REAL = IMEI + "/data";

    /**
     * Payload real enviado por el FMC920 via AWS IoT Core.
     * latlng contiene la posición GPS del dispositivo (lat,lng).
     */
    private static final String PAYLOAD_REAL = """
            {
              "state": {
                "reported": {
                  "11317": "01",
                  "ts": 1771538837021,
                  "pr": 0,
                  "latlng": "-33.393940,-70.557262",
                  "alt": 728,
                  "ang": 166,
                  "sat": 17,
                  "sp": 0,
                  "evt": 11317
                }
              }
            }""";

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private MqttMessageRepository  mqttMessageRepository;
    @Mock private SseService             sseService;

    @InjectMocks
    private MessageStorageService messageStorageService;

    private Subscription suscripcionActiva() {
        return Subscription.builder()
                .topicName(TOPIC_REAL)
                .active(true)
                .build();
    }

    // =========================================================================
    // Happy path — suscripción activa
    // =========================================================================

    @Test
    @DisplayName("saveMessage con suscripción activa guarda la entidad con topic e IMEI correctos")
    void saveMessage_conSuscripcionActiva_guardaConTopicEImei() {
        when(subscriptionRepository.findByTopicName(TOPIC_REAL))
                .thenReturn(Optional.of(suscripcionActiva()));
        when(mqttMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        messageStorageService.saveMessage(TOPIC_REAL, IMEI, PAYLOAD_REAL, 1);

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttMessageRepository).save(captor.capture());

        assertThat(captor.getValue().getTopic()).isEqualTo(TOPIC_REAL);
        assertThat(captor.getValue().getClientId()).isEqualTo(IMEI);
        assertThat(captor.getValue().getQos()).isEqualTo(1);
    }

    @Test
    @DisplayName("saveMessage con suscripción activa preserva el payload GPS completo del FMC920")
    void saveMessage_conSuscripcionActiva_preservaPayloadGpsFmc920() {
        when(subscriptionRepository.findByTopicName(TOPIC_REAL))
                .thenReturn(Optional.of(suscripcionActiva()));
        when(mqttMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        messageStorageService.saveMessage(TOPIC_REAL, IMEI, PAYLOAD_REAL, 1);

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(mqttMessageRepository).save(captor.capture());

        String payload = captor.getValue().getPayload();
        assertThat(payload).contains("latlng");
        assertThat(payload).contains("-33.393940,-70.557262");
        assertThat(payload).contains("1771538837021"); // timestamp del FMC920
    }

    @Test
    @DisplayName("saveMessage con suscripción activa notifica SSE con el mensaje guardado")
    void saveMessage_conSuscripcionActiva_notificaSseConMensajeGuardado() {
        when(subscriptionRepository.findByTopicName(TOPIC_REAL))
                .thenReturn(Optional.of(suscripcionActiva()));
        when(mqttMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        messageStorageService.saveMessage(TOPIC_REAL, IMEI, PAYLOAD_REAL, 1);

        ArgumentCaptor<MqttMessage> captor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(sseService).sendMessageJson(captor.capture());

        assertThat(captor.getValue().getPayload()).contains("latlng");
    }

    // =========================================================================
    // Casos negativos — sin suscripción o inactiva
    // =========================================================================

    @Test
    @DisplayName("saveMessage sin suscripción en BD no guarda ni notifica SSE")
    void saveMessage_sinSuscripcionEnBD_noGuardaNiNotificaSSE() {
        when(subscriptionRepository.findByTopicName(TOPIC_REAL))
                .thenReturn(Optional.empty());

        messageStorageService.saveMessage(TOPIC_REAL, IMEI, PAYLOAD_REAL, 1);

        verify(mqttMessageRepository, never()).save(any());
        verify(sseService, never()).sendMessageJson(any());
    }

    @Test
    @DisplayName("saveMessage con suscripción inactiva (active=false) no guarda ni notifica SSE")
    void saveMessage_conSuscripcionInactiva_noGuardaNiNotificaSSE() {
        Subscription inactiva = Subscription.builder()
                .topicName(TOPIC_REAL)
                .active(false)
                .build();
        when(subscriptionRepository.findByTopicName(TOPIC_REAL))
                .thenReturn(Optional.of(inactiva));

        messageStorageService.saveMessage(TOPIC_REAL, IMEI, PAYLOAD_REAL, 1);

        verify(mqttMessageRepository, never()).save(any());
        verify(sseService, never()).sendMessageJson(any());
    }
}
