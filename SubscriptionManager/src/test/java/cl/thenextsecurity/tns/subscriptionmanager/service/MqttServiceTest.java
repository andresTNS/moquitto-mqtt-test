package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.config.AwsIotSslConfig;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para MqttService sin contexto Spring.
 * Se testea extractClientIdFromTopic() de forma indirecta a través de
 * messageArrived(), que es público y llama al método privado internamente.
 * Nota: @PostConstruct connect() no se invoca con @InjectMocks, por lo que
 * no se intenta ninguna conexión real al broker MQTT.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MqttService — tests unitarios de extractClientIdFromTopic")
class MqttServiceTest {

    @Mock
    private DiscoveryService discoveryService;

    @Mock
    private MessageStorageService messageStorageService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AwsIotSslConfig awsIotSslConfig;

    @InjectMocks
    private MqttService mqttService;

    @BeforeEach
    void setUp() {
        // Modo normal (no escaneo) → los mensajes van a messageStorageService
        when(discoveryService.isScanning()).thenReturn(false);
    }

    private MqttMessage mensaje(String payload) {
        return new MqttMessage(payload.getBytes());
    }

    // =========================================================================
    // extractClientIdFromTopic — 4 casos
    // =========================================================================

    @Test
    @DisplayName("Topic formato Teltonika IMEI/data retorna el IMEI como clientId")
    void messageArrived_topicTeltonika_extraeImeiComoClientId() throws Exception {
        mqttService.messageArrived("865413057599200/data", mensaje("{}"));

        verify(messageStorageService).saveMessage(
                eq("865413057599200/data"), eq("865413057599200"), any(), anyInt());
    }

    @Test
    @DisplayName("Topic con múltiples segmentos retorna solo el primer segmento como clientId")
    void messageArrived_topicMultiplesSegmentos_extraePrimerSegmento() throws Exception {
        mqttService.messageArrived("865413057599200/data/gps", mensaje("{}"));

        verify(messageStorageService).saveMessage(
                eq("865413057599200/data/gps"), eq("865413057599200"), any(), anyInt());
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
