package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "mqtt_messages",
        indexes = {
                @Index(name = "idx_mqtt_messages_topic", columnList = "topic"),
                @Index(name = "idx_mqtt_messages_client_id", columnList = "client_id"),
                @Index(name = "idx_mqtt_messages_received_at", columnList = "received_at"),
                @Index(name = "idx_mqtt_messages_topic_received_at", columnList = "topic, received_at")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MqttMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String topic;

    @Column(name = "client_id", length = 255)
    @Builder.Default
    private String clientId = "unknown";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column
    private Integer qos;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
