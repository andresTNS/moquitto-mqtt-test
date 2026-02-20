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

@DisplayName("Subscription — tests de entidad")
class SubscriptionTest {

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

        Subscription sub = Subscription.builder()
                .topicName("sensor/temperatura")
                .active(true)
                .createdAt(ahora)
                .build();

        assertThat(sub.getTopicName()).isEqualTo("sensor/temperatura");
        assertThat(sub.getActive()).isTrue();
        assertThat(sub.getCreatedAt()).isEqualTo(ahora);
    }

    // =========================================================================
    // Valores por defecto (@Builder.Default)
    // =========================================================================

    @Test
    @DisplayName("@Builder.Default establece active=true cuando no se especifica")
    void builderDefault_activeTrueWhenNoEspecificado() {
        Subscription sub = Subscription.builder()
                .topicName("sensor/temperatura")
                .build();

        assertThat(sub.getActive()).isTrue();
    }

    // =========================================================================
    // @PrePersist
    // =========================================================================

    @Test
    @DisplayName("@PrePersist asigna createdAt cuando es null")
    void prePersist_asignaCreatedAtCuandoEsNull() {
        Subscription sub = Subscription.builder()
                .topicName("sensor/temperatura")
                .build();

        assertThat(sub.getCreatedAt()).isNull();

        ReflectionTestUtils.invokeMethod(sub, "onPrePersist");

        assertThat(sub.getCreatedAt()).isNotNull();
        assertThat(sub.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("@PrePersist no sobreescribe createdAt si ya está seteado")
    void prePersist_noSobreescribeCreatedAtExistente() {
        LocalDateTime fechaOriginal = LocalDateTime.of(2024, 6, 1, 9, 0, 0);

        Subscription sub = Subscription.builder()
                .topicName("sensor/temperatura")
                .createdAt(fechaOriginal)
                .build();

        ReflectionTestUtils.invokeMethod(sub, "onPrePersist");

        assertThat(sub.getCreatedAt()).isEqualTo(fechaOriginal);
    }

    // =========================================================================
    // @EqualsAndHashCode
    // =========================================================================

    @Test
    @DisplayName("Dos objetos con el mismo topicName son iguales")
    void equals_mismoTopicName_sonIguales() {
        Subscription s1 = Subscription.builder()
                .topicName("sensor/temperatura")
                .active(true)
                .build();

        Subscription s2 = Subscription.builder()
                .topicName("sensor/temperatura")
                .active(false)
                .build();

        assertThat(s1).isEqualTo(s2);
        assertThat(s1.hashCode()).isEqualTo(s2.hashCode());
    }

    @Test
    @DisplayName("Dos objetos con distinto topicName no son iguales")
    void equals_distintoTopicName_noSonIguales() {
        Subscription s1 = Subscription.builder()
                .topicName("sensor/temperatura")
                .build();

        Subscription s2 = Subscription.builder()
                .topicName("sensor/humedad")
                .build();

        assertThat(s1).isNotEqualTo(s2);
    }

    // =========================================================================
    // Validaciones
    // =========================================================================

    @Test
    @DisplayName("@NotBlank genera ConstraintViolation cuando topicName está en blanco")
    void validacion_notBlank_topicNameEnBlanco() {
        Subscription sub = Subscription.builder()
                .topicName("   ")
                .build();

        Set<ConstraintViolation<Subscription>> violaciones = validator.validate(sub);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("topicName"));
    }

    @Test
    @DisplayName("@Size genera ConstraintViolation cuando topicName supera 500 caracteres")
    void validacion_size_topicNameMayor500Chars() {
        Subscription sub = Subscription.builder()
                .topicName("x".repeat(501))
                .build();

        Set<ConstraintViolation<Subscription>> violaciones = validator.validate(sub);

        assertThat(violaciones).isNotEmpty();
        assertThat(violaciones).anyMatch(v -> v.getPropertyPath().toString().equals("topicName"));
    }
}
