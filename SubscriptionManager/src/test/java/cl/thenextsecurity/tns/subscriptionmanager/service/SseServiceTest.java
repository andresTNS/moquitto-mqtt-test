package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Tests unitarios para SseService sin contexto Spring.
 *
 * SseService gestiona las conexiones Server-Sent Events para actualizar el
 * dashboard en tiempo real cuando:
 * - Llegan nuevos mensajes MQTT (sendMessageJson)
 * - La lista de suscripciones cambia (sendTopicsUpdated)
 *
 * Los emitters se registran en una CopyOnWriteArrayList y se configuran con
 * callbacks de limpieza (onCompletion, onTimeout, onError) para eliminarlos
 * automáticamente cuando la conexión se cierra.
 *
 * Limitación de tests unitarios: no se puede verificar fácilmente el contenido
 * exacto de los eventos SSE enviados sin mockear SseEmitter.send(), por lo que
 * nos enfocamos en verificar el comportamiento de gestión de emitters y que
 * los métodos de envío no lanzan excepciones.
 */
@DisplayName("SseService — tests unitarios de gestión de emitters SSE")
class SseServiceTest {

    private SseService sseService;

    @BeforeEach
    void setUp() {
        sseService = new SseService();
    }

    // =========================================================================
    // addEmitter() — registro y configuración de emitters
    // =========================================================================

    @Test
    @DisplayName("addEmitter() crea y registra el emitter en la lista interna")
    void addEmitter_creaYRegistraEmitter() {
        SseEmitter emitter = sseService.addEmitter();

        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) ReflectionTestUtils.getField(
                sseService, "emitters");

        assertThat(emitters).hasSize(1);
        assertThat(emitters).contains(emitter);
    }

    @Test
    @DisplayName("addEmitter() retorna emitter con timeout Long.MAX_VALUE")
    void addEmitter_retornaEmitterConTimeoutMaximo() {
        SseEmitter emitter = sseService.addEmitter();

        assertThat(emitter).isNotNull();
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @DisplayName("addEmitter() configura callback onCompletion que elimina el emitter de la lista")
    void addEmitter_callbackOnCompletion_eliminaEmitterDeLaLista() {
        SseEmitter emitter = sseService.addEmitter();

        // Trigger onCompletion callback manualmente
        emitter.complete();

        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) ReflectionTestUtils.getField(
                sseService, "emitters");

        // Después de complete(), el callback debe haberlo eliminado
        assertThat(emitters).isEmpty();
    }

    @Test
    @DisplayName("addEmitter() con múltiples clientes registra todos en la lista")
    void addEmitter_multipleClientes_registraTodosEnLaLista() {
        SseEmitter emitter1 = sseService.addEmitter();
        SseEmitter emitter2 = sseService.addEmitter();
        SseEmitter emitter3 = sseService.addEmitter();

        @SuppressWarnings("unchecked")
        List<SseEmitter> emitters = (List<SseEmitter>) ReflectionTestUtils.getField(
                sseService, "emitters");

        assertThat(emitters).hasSize(3);
        assertThat(emitters).containsExactly(emitter1, emitter2, emitter3);
    }

    // =========================================================================
    // sendMessageJson() — envío de mensajes MQTT en formato JSON
    // =========================================================================

    @Test
    @DisplayName("sendMessageJson() con mensaje válido no lanza excepción")
    void sendMessageJson_conMensajeValido_noLanzaExcepcion() {
        sseService.addEmitter();

        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .clientId("esp32-sala-01")
                .payload("{\"value\": 25.3}")
                .qos(1)
                .receivedAt(LocalDateTime.now())
                .build();

        assertThatCode(() -> sseService.sendMessageJson(mensaje))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendMessageJson() sin emitters registrados no lanza excepción")
    void sendMessageJson_sinEmittersRegistrados_noLanzaExcepcion() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .clientId("esp32")
                .payload("{\"value\": 25.3}")
                .build();

        assertThatCode(() -> sseService.sendMessageJson(mensaje))
                .doesNotThrowAnyException();
    }

    // =========================================================================
    // sendTopicsUpdated() — notificación de cambios en suscripciones
    // =========================================================================

    @Test
    @DisplayName("sendTopicsUpdated() con emitters registrados no lanza excepción")
    void sendTopicsUpdated_conEmittersRegistrados_noLanzaExcepcion() {
        sseService.addEmitter();

        assertThatCode(() -> sseService.sendTopicsUpdated())
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sendTopicsUpdated() sin emitters registrados no lanza excepción")
    void sendTopicsUpdated_sinEmittersRegistrados_noLanzaExcepcion() {
        assertThatCode(() -> sseService.sendTopicsUpdated())
                .doesNotThrowAnyException();
    }
}
