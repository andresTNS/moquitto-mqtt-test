package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import cl.thenextsecurity.tns.subscriptionmanager.repository.MqttMessageRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageStorageService {

    private final SubscriptionRepository subscriptionRepository;
    private final MqttMessageRepository mqttMessageRepository;
    private final SseService sseService;

    /**
     * Persiste un mensaje MQTT solo si existe una suscripción activa para el topic.
     * Las fechas se asignan automáticamente via @PrePersist en la entidad.
     * Notifica a los clientes SSE tras guardar exitosamente.
     */
    @Transactional
    public void saveMessage(String topic, String clientId, String payload, int qos) {
        boolean tieneSubscripcionActiva = subscriptionRepository.findByTopicName(topic)
                .map(s -> s.getActive())
                .orElse(false);

        if (!tieneSubscripcionActiva) {
            log.debug("Mensaje ignorado — sin suscripción activa para topic: {}", topic);
            return;
        }

        MqttMessage mensaje = MqttMessage.builder()
                .topic(topic)
                .clientId(clientId)
                .payload(payload)
                .qos(qos)
                .build();

        MqttMessage guardado = mqttMessageRepository.save(mensaje);
        log.debug("Mensaje guardado — topic: {}, clientId: {}", topic, clientId);

        sseService.sendMessage(guardado);
    }
}
