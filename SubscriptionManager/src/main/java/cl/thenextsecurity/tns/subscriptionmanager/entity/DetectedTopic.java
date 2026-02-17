package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.persistence.*;
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
public class DetectedTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "topic_name", nullable = false, unique = true, length = 500)
    private String topicName;

    @Column(name = "last_client_id", length = 255)
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
}