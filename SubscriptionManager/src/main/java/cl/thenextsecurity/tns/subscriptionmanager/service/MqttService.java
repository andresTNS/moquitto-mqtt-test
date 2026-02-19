package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.config.AwsIotSslConfig;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Gestiona la conexión con el broker MQTT e implementa MqttCallback
 * para recibir mensajes y redirigirlos según el estado del sistema:
 * - isScanning=true  → DiscoveryService (registro de topics)
 * - isScanning=false → MessageStorageService (persistencia)
 *
 * Nota: MqttMessage en este archivo se refiere a org.eclipse.paho.client.mqttv3.MqttMessage,
 * no a la entidad cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage.
 */
@Service
@Slf4j
public class MqttService implements MqttCallback {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.client.id}")
    private String mqttClientId;

    private final DiscoveryService discoveryService;
    private final MessageStorageService messageStorageService;
    private final SubscriptionRepository subscriptionRepository;
    private final AwsIotSslConfig awsIotSslConfig;
    private final ObjectMapper objectMapper;

    private MqttClient client;

    public MqttService(DiscoveryService discoveryService,
                       MessageStorageService messageStorageService,
                       SubscriptionRepository subscriptionRepository,
                       AwsIotSslConfig awsIotSslConfig) {
        this.discoveryService = discoveryService;
        this.messageStorageService = messageStorageService;
        this.subscriptionRepository = subscriptionRepository;
        this.awsIotSslConfig = awsIotSslConfig;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Se ejecuta al arrancar la aplicación: conecta al broker,
     * configura el callback y recarga las suscripciones activas desde BD.
     */
    @PostConstruct
    public void connect() {
        try {
            client = new MqttClient(brokerUrl, mqttClientId);
            client.setCallback(this);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            options.setSocketFactory(awsIotSslConfig.getSslSocketFactory());

            client.connect(options);
            log.info("Conectado al broker MQTT: {}", brokerUrl);

            reloadSubscriptions();
        } catch (MqttException e) {
            log.error("Error al conectar con el broker MQTT: {}", brokerUrl, e);
        } catch (Exception e) {
            log.error("Error al configurar TLS para AWS IoT Core: {}", e.getMessage(), e);
        }
    }

    /**
     * Suscribe el cliente a todos los topics con suscripción activa en BD.
     * Garantiza que un reinicio no pierda la configuración del usuario.
     */
    private void reloadSubscriptions() {
        subscriptionRepository.findByActiveTrue().forEach(sub -> {
            try {
                client.subscribe(sub.getTopicName(), 1);
                log.info("Suscripción recargada — topic: {}", sub.getTopicName());
            } catch (MqttException e) {
                log.error("Error al recargar suscripción — topic: {}", sub.getTopicName(), e);
            }
        });
    }

    /**
     * Suscribe al cliente a un topic específico con QoS 1.
     * Usado por DiscoveryService para el wildcard "#" durante el escaneo.
     */
    public void subscribe(String topic) {
        try {
            client.subscribe(topic, 1);
            log.info("Suscrito al topic: {}", topic);
        } catch (MqttException e) {
            log.error("Error al suscribirse al topic: {}", topic, e);
        }
    }

    /**
     * Indica si el cliente MQTT está actualmente conectado al broker.
     * Usado por HomeController para el indicador de estado en la vista.
     */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /**
     * Desuscribe al cliente de un topic específico.
     * Usado por DiscoveryService al finalizar el escaneo.
     */
    public void unsubscribe(String topic) {
        try {
            client.unsubscribe(topic);
            log.info("Desuscrito del topic: {}", topic);
        } catch (MqttException e) {
            log.error("Error al desuscribirse del topic: {}", topic, e);
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        String clientId = extractClientIdFromTopic(topic);

        if (discoveryService.isScanning()) {
            // Modo descubrimiento: registrar topic, descartar mensaje
            discoveryService.recordDiscoveredTopic(topic, clientId);
            return;
        }

        // Modo normal: persistir si existe suscripción activa
        messageStorageService.saveMessage(topic, clientId, payload, message.getQos());
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.error("Conexión con broker MQTT perdida: {}", cause.getMessage());
        // automaticReconnect=true gestiona la reconexión automáticamente
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // No publicamos mensajes, no se requiere implementación
    }

    /**
     * Extrae el identificador del cliente desde el topic MQTT.
     * Para dispositivos Teltonika el topic tiene formato "{IMEI}/data",
     * por lo que el primer segmento es el IMEI del dispositivo.
     * Retorna "unknown" si el topic no contiene el separador "/".
     */
    private String extractClientIdFromTopic(String topic) {
        if (topic != null && topic.contains("/")) {
            return topic.split("/")[0];
        }
        return "unknown";
    }
}
