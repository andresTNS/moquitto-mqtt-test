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

@DisplayName("DetectedTopic — tests de entidad")
class DetectedTopicTest {

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

        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .lastClientId("esp32-sala-01")
                .firstDetected(ahora)
                .lastSeen(ahora)
                .active(true)
                .messageCount(5L)
                .build();

        assertThat(topic.getTopicName()).isEqualTo("sensor/temperatura");
        assertThat(topic.getLastClientId()).isEqualTo("esp32-sala-01");
        assertThat(topic.getFirstDetected()).isEqualTo(ahora);
        assertThat(topic.getLastSeen()).isEqualTo(ahora);
        assertThat(topic.getActive()).isTrue();
        assertThat(topic.getMessageCount()).isEqualTo(5L);
    }

    // =========================================================================
    // Valores por defecto (@Builder.Default)
    // =========================================================================

    @Test
    @DisplayName("@Builder.Default establece active=true cuando no se especifica")
    void builderDefault_activeTrueWhenNoEspecificado() {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .build();

        assertThat(topic.getActive()).isTrue();
    }

    @Test
    @DisplayName("@Builder.Default establece messageCount=0L cuando no se especifica")
    void builderDefault_messageCountCeroWhenNoEspecificado() {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .build();

        assertThat(topic.getMessageCount()).isEqualTo(0L);
    }

    // =========================================================================
    // @PrePersist
    // =========================================================================

    @Test
    @DisplayName("@PrePersist asigna firstDetected y lastSeen cuando son null")
    void prePersist_asignaFechasCuandoSonNull() {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .build();

        assertThat(topic.getFirstDetected()).isNull();
        assertThat(topic.getLastSeen()).isNull();

        ReflectionTestUtils.invokeMethod(topic, "onPrePersist");

        assertThat(topic.getFirstDetected()).isNotNull();
        assertThat(topic.getLastSeen()).isNotNull();
        assertThat(topic.getFirstDetected()).isBeforeOrEqualTo(LocalDateTime.now());
        assertThat(topic.getLastSeen()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("@PrePersist no sobreescribe firstDetected ni lastSeen si ya están seteadas")
    void prePersist_noSobreescribeFechasExistentes() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .firstDetected(fechaOriginal)
                .lastSeen(fechaOriginal)
                .build();

        ReflectionTestUtils.invokeMethod(topic, "onPrePersist");

        assertThat(topic.getFirstDetected()).isEqualTo(fechaOriginal);
        assertThat(topic.getLastSeen()).isEqualTo(fechaOriginal);
    }

    // =========================================================================
    // @EqualsAndHashCode
    // =========================================================================

    @Test
    @DisplayName("Dos objetos con el mismo topicName son iguales")
    void equals_mismoTopicName_sonIguales() {
        DetectedTopic t1 = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .messageCount(10L)
                .build();

        DetectedTopic t2 = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .messageCount(99L)
                .build();

        assertThat(t1).isEqualTo(t2);
        assertThat(t1.hashCode()).isEqualTo(t2.hashCode());
    }

    @Test
    @DisplayName("Dos objetos con distinto topicName no son iguales")
    void equals_distintoTopicName_noSonIguales() {
        DetectedTopic t1 = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .build();

        DetectedTopic t2 = DetectedTopic.builder()
                .topicName("sensor/humedad")
                .build();

        assertThat(t1).isNotEqualTo(t2);
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    @Test
    @DisplayName("@NotBlank genera ConstraintViolation cuando topicName está en blanco")
    void validacion_notBlank_topicNameEnBlanco() {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("   ")
                .build();

        Set<ConstraintViolation<DetectedTopic>> violaciones = validator.validate(topic);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("topicName"));
    }

    @Test
    @DisplayName("@Size genera ConstraintViolation cuando topicName supera 500 caracteres")
    void validacion_size_topicNameMayor500Chars() {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("a".repeat(501))
                .build();

        Set<ConstraintViolation<DetectedTopic>> violaciones = validator.validate(topic);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("topicName"));
    }
}
