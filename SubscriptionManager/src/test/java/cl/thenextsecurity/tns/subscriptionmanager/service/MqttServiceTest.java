package cl.thenextsecurity.tns.subscriptionmanager.service;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para MqttService sin contexto Spring.
 * Se testea extractClientIdFromTopic() indirectamente a través de messageArrived().
 * MqttService se crea manualmente — @PostConstruct no se invoca, sin conexión real.
 * subscriptionRepository y awsIotSslConfig no son necesarios aquí: messageArrived()
 * no los usa. Los tests de integración con conexión real cubren esos componentes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MqttService — tests unitarios de extractClientIdFromTopic")
class MqttServiceTest {

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

    @Mock private DiscoveryService      discoveryService;
    @Mock private MessageStorageService messageStorageService;

    private MqttService mqttService;

    @BeforeEach
    void setUp() {
        // Crear directamente: subscriptionRepository y awsIotSslConfig son null
        // porque messageArrived() no los utiliza.
        mqttService = new MqttService(discoveryService, messageStorageService, null, null);
        // lenient: aplica solo a los 4 tests de abajo, no a posibles tests futuros
        lenient().when(discoveryService.isScanning()).thenReturn(false);
    }

    private MqttMessage mensaje(String payload) {
        return new MqttMessage(payload.getBytes());
    }

    // =========================================================================
    // extractClientIdFromTopic — 4 casos con datos reales del FMC920
    // =========================================================================

    @Test
    @DisplayName("Mensaje real FMC920: IMEI extraído del topic y payload GPS pasado íntegro")
    void messageArrived_mensajeRealFmc920_extraeImeiYPasaPayloadCompleto() throws Exception {
        mqttService.messageArrived(TOPIC_REAL, mensaje(PAYLOAD_REAL));

        verify(messageStorageService).saveMessage(
                eq(TOPIC_REAL), eq(IMEI), eq(PAYLOAD_REAL), anyInt());
    }

    @Test
    @DisplayName("Topic con múltiples segmentos retorna solo el primer segmento (IMEI) como clientId")
    void messageArrived_topicMultiplesSegmentos_extraePrimerSegmento() throws Exception {
        mqttService.messageArrived(IMEI + "/data/gps", mensaje("{}"));

        verify(messageStorageService).saveMessage(
                eq(IMEI + "/data/gps"), eq(IMEI), any(), anyInt());
    }

    @Test
    @DisplayName("Topic sin barra retorna 'unknown' como clientId")
    void messageArrived_topicSinBarra_retornaUnknown() throws Exception {
        mqttService.messageArrived("topicsinbarra", mensaje("{}"));

        verify(messageStorageService).saveMessage(
                eq("topicsinbarra"), eq("unknown"), any(), anyInt());
    }

    @Test
    @DisplayName("Topic null retorna 'unknown' como clientId")
    void messageArrived_topicNull_retornaUnknown() throws Exception {
        mqttService.messageArrived(null, mensaje("{}"));

        verify(messageStorageService).saveMessage(
                isNull(), eq("unknown"), any(), anyInt());
    }
}
