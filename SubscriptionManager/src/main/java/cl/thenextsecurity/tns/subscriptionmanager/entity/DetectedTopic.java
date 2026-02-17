package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "detected_topics",
        indexes = {
                @Index(name = "idx_detected_topics_active", columnList = "active"),
                @Index(name = "idx_detected_topics_last_seen", columnList = "last_seen")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "topicName")
public class DetectedTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del topic no puede estar vacío")
    @Size(max = 500, message = "El nombre del topic no puede superar 500 caracteres")
    @Column(name = "topic_name", nullable = false, unique = true, length = 500)
    private String topicName;

    @Column(name = "last_client_id")
    private String lastClientId;

    @Column(name = "first_detected", nullable = false)
    private LocalDateTime firstDetected;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "message_count", nullable = false)
    @Builder.Default
    private Long messageCount = 0L;

    /**
     * Auto-asigna las fechas de detección antes de persistir por primera vez.
     * Evita que el servicio deba asignarlas manualmente.
     */
    @PrePersist
    protected void onPrePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (firstDetected == null) {
            firstDetected = now;
        }
        if (lastSeen == null) {
            lastSeen = now;
        }
    }
}
