package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MqttMessage — tests de entidad")
class MqttMessageTest {

    private Validator validator;

    @BeforeEach
    void configurarValidador() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    // =========================================================================
    // Builder
    // =========================================================================

    @Test
    @DisplayName("Builder crea el objeto con todos los campos correctamente")
    void builder_creaObjetoConTodosLosCampos() {
        LocalDateTime ahora = LocalDateTime.now();

        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .clientId("esp32-sala-01")
                .payload("{\"value\": 25.3, \"client_id\": \"esp32-sala-01\"}")
                .qos(1)
                .receivedAt(ahora)
                .build();

        assertThat(mensaje.getTopic()).isEqualTo("sensor/temperatura");
        assertThat(mensaje.getClientId()).isEqualTo("esp32-sala-01");
        assertThat(mensaje.getPayload()).isEqualTo("{\"value\": 25.3, \"client_id\": \"esp32-sala-01\"}");
        assertThat(mensaje.getQos()).isEqualTo(1);
        assertThat(mensaje.getReceivedAt()).isEqualTo(ahora);
    }

    // =========================================================================
    // Valores por defecto (@Builder.Default)
    // =========================================================================

    @Test
    @DisplayName("@Builder.Default establece clientId=\"unknown\" cuando no se especifica")
    void builderDefault_clientIdUnknownWhenNoEspecificado() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .build();

        assertThat(mensaje.getClientId()).isEqualTo("unknown");
    }

    // =========================================================================
    // @PrePersist
    // =========================================================================

    @Test
    @DisplayName("@PrePersist asigna receivedAt cuando es null")
    void prePersist_asignaReceivedAtCuandoEsNull() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .build();

        assertThat(mensaje.getReceivedAt()).isNull();

        ReflectionTestUtils.invokeMethod(mensaje, "onPrePersist");

        assertThat(mensaje.getReceivedAt()).isNotNull();
        assertThat(mensaje.getReceivedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("@PrePersist no sobreescribe receivedAt si ya está seteado")
    void prePersist_noSobreescribeReceivedAtExistente() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2024, 3, 20, 14, 45, 0);

        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .receivedAt(fechaOriginal)
                .build();

        ReflectionTestUtils.invokeMethod(mensaje, "onPrePersist");

        assertThat(mensaje.getReceivedAt()).isEqualTo(fechaOriginal);
    }

    @Test
    @DisplayName("@PrePersist normaliza clientId null a \"unknown\"")
    void prePersist_normalizaClientIdNullAUnknown() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .clientId(null)
                .build();

        ReflectionTestUtils.invokeMethod(mensaje, "onPrePersist");

        assertThat(mensaje.getClientId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("@PrePersist normaliza clientId en blanco a \"unknown\"")
    void prePersist_normalizaClientIdEnBlancoAUnknown() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .clientId("   ")
                .build();

        ReflectionTestUtils.invokeMethod(mensaje, "onPrePersist");

        assertThat(mensaje.getClientId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("@PrePersist no modifica clientId cuando es un valor válido")
    void prePersist_noModificaClientIdValido() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("{\"value\": 25.3}")
                .clientId("esp32-sala-01")
                .build();

        ReflectionTestUtils.invokeMethod(mensaje, "onPrePersist");

        assertThat(mensaje.getClientId()).isEqualTo("esp32-sala-01");
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    @Test
    @DisplayName("@NotBlank genera ConstraintViolation cuando topic está en blanco")
    void validacion_notBlank_topicEnBlanco() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("   ")
                .payload("{\"value\": 25.3}")
                .build();

        Set<ConstraintViolation<MqttMessage>> violaciones = validator.validate(mensaje);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("topic"));
    }

    @Test
    @DisplayName("@NotBlank genera ConstraintViolation cuando payload está en blanco")
    void validacion_notBlank_payloadEnBlanco() {
        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .payload("   ")
                .build();

        Set<ConstraintViolation<MqttMessage>> violaciones = validator.validate(mensaje);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("payload"));
    }

    // =========================================================================
    // @ToString.Exclude
    // =========================================================================

    @Test
    @DisplayName("@ToString.Exclude excluye el payload del toString()")
    void toString_noIncluyePayload() {
        String payloadSecreto = "{\"client_id\": \"esp32\", \"temperatura\": 99.9}";

        MqttMessage mensaje = MqttMessage.builder()
                .topic("sensor/temperatura")
                .clientId("esp32")
                .payload(payloadSecreto)
                .build();

        assertThat(mensaje.toString()).doesNotContain(payloadSecreto);
    }
}
