package cl.thenextsecurity.tns.subscriptionmanager.repository;

import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTopicName(String topicName);

    List<Subscription> findByActiveTrue();

    boolean existsByTopicName(String topicName);

    void deleteByTopicName(String topicName);
}
