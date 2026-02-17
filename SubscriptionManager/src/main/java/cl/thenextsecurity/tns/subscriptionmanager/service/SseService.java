package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class SseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Registra un nuevo cliente SSE y configura su limpieza automática
     * al completarse, expirar o producirse un error.
     */
    public SseEmitter addEmitter() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        log.debug("Nuevo cliente SSE conectado. Total activos: {}", emitters.size());
        return emitter;
    }

    /**
     * Envía un mensaje MQTT recibido a todos los clientes SSE conectados.
     * El browser lo recibe como evento "message" y lo agrega al panel.
     */
    public void sendMessage(MqttMessage message) {
        String contenido = String.format("[%s] %s | cliente: %s → %s",
                message.getReceivedAt(),
                message.getTopic(),
                message.getClientId(),
                message.getPayload());
        broadcast("message", contenido);
    }

    /**
     * Envía un mensaje MQTT en formato JSON a todos los clientes SSE conectados.
     * Estructura esperada por dashboard.js:
     * { "topic": "...", "clientId": "...", "payload": "...", "timestamp": "..." }
     */
    public void sendMessageJson(MqttMessage message) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("topic", message.getTopic());
        datos.put("clientId", message.getClientId());
        datos.put("payload", message.getPayload());
        datos.put("timestamp", message.getReceivedAt() != null ? message.getReceivedAt().toString() : "");

        try {
            String json = objectMapper.writeValueAsString(datos);
            broadcast("message", json);
        } catch (JsonProcessingException e) {
            log.error("Error al serializar mensaje MQTT a JSON para SSE", e);
        }
    }

    /**
     * Notifica a todos los clientes SSE que la lista de topics fue actualizada.
     * El browser responde ejecutando location.reload().
     */
    public void sendTopicsUpdated() {
        broadcast("topics-updated", "refresh");
    }

    private void broadcast(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                deadEmitters.add(emitter);
            }
        });

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            log.debug("Eliminados {} emitters muertos. Activos: {}", deadEmitters.size(), emitters.size());
        }
    }
}
