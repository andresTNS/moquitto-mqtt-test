package cl.thenextsecurity.tns.subscriptionmanager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "El topic no puede estar vacío")
    @Size(max = 500, message = "El topic no puede superar 500 caracteres")
    @Column(nullable = false, length = 500)
    private String topic;

    @Column(name = "client_id", length = 255)
    @Builder.Default
    private String clientId = "unknown";

    @NotBlank(message = "El payload no puede estar vacío")
    @ToString.Exclude
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column
    private Integer qos;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    /**
     * Auto-asigna la fecha de recepción y normaliza clientId antes de persistir.
     * Garantiza que nunca queden valores nulos en campos obligatorios.
     */
    @PrePersist
    protected void onPrePersist() {
        if (receivedAt == null) {
            receivedAt = LocalDateTime.now();
        }
        if (clientId == null || clientId.isBlank()) {
            clientId = "unknown";
        }
    }
}
