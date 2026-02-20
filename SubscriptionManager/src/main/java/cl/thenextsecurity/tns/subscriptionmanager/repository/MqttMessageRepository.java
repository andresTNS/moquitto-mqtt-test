package cl.thenextsecurity.tns.subscriptionmanager.repository;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MqttMessageRepository extends JpaRepository<MqttMessage, Long> {

    Page<MqttMessage> findByTopic(String topic, Pageable pageable);

    Page<MqttMessage> findByClientId(String clientId, Pageable pageable);

    Page<MqttMessage> findByReceivedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<MqttMessage> findByTopicAndReceivedAtBetween(String topic, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<MqttMessage> findTop100ByOrderByReceivedAtDesc();

    long countByTopic(String topic);
}
