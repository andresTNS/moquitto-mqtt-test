package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriptions",
        indexes = {
                @Index(name = "idx_subscriptions_active", columnList = "active")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "topicName")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del topic no puede estar vacío")
    @Size(max = 500, message = "El nombre del topic no puede superar 500 caracteres")
    @Column(name = "topic_name", nullable = false, unique = true, length = 500)
    private String topicName;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Auto-asigna la fecha de creación antes de persistir por primera vez.
     * Evita que el servicio deba asignarla manualmente.
     */
    @PrePersist
    protected void onPrePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
