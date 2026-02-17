package cl.thenextsecurity.tns.subscriptionmanager.repository;

import cl.thenextsecurity.tns.subscriptionmanager.entity.DetectedTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DetectedTopicRepository extends JpaRepository<DetectedTopic, Long> {

    Optional<DetectedTopic> findByTopicName(String topicName);

    List<DetectedTopic> findByActiveTrue();

    List<DetectedTopic> findAllByOrderByLastSeenDesc();

    boolean existsByTopicName(String topicName);
}
