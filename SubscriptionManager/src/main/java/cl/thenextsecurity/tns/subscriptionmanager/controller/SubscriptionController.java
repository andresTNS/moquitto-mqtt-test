package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import cl.thenextsecurity.tns.subscriptionmanager.service.MqttService;
import cl.thenextsecurity.tns.subscriptionmanager.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final MqttService mqttService;
    private final SseService sseService;

    /**
     * Guarda la selección de topics del usuario siguiendo el patrón PRG.
     * - Topics que ya existían y NO están seleccionados: se eliminan y se desuscriben del broker.
     * - Topics seleccionados que NO existían en BD: se crean y se suscriben en el broker.
     * - Topics que existían y siguen seleccionados: se mantienen sin cambios.
     * Al finalizar notifica via SSE para que los clientes conectados actualicen la vista.
     */
    @PostMapping("/subscriptions/save")
    @Transactional
    public String save(@RequestParam(required = false) List<String> selectedTopics) {
        Set<String> selected = (selectedTopics != null)
                ? Set.copyOf(selectedTopics)
                : Collections.emptySet();

        // Topics actualmente en BD
        List<Subscription> actuales = subscriptionRepository.findAll();
        Set<String> nombresActuales = actuales.stream()
                .map(Subscription::getTopicName)
                .collect(Collectors.toSet());

        // Eliminar y desuscribir los que ya no están seleccionados
        actuales.stream()
                .filter(sub -> !selected.contains(sub.getTopicName()))
                .forEach(sub -> {
                    mqttService.unsubscribe(sub.getTopicName());
                    subscriptionRepository.deleteByTopicName(sub.getTopicName());
                    log.info("Suscripción eliminada — topic: {}", sub.getTopicName());
                });

        // Crear y suscribir los nuevos seleccionados
        selected.stream()
                .filter(topicName -> !nombresActuales.contains(topicName))
                .forEach(topicName -> {
                    Subscription nueva = Subscription.builder()
                            .topicName(topicName)
                            .active(true)
                            .build();
                    subscriptionRepository.save(nueva);
                    mqttService.subscribe(topicName);
                    log.info("Suscripción creada — topic: {}", topicName);
                });

        sseService.sendTopicsUpdated();

        return "redirect:/";
    }
}
