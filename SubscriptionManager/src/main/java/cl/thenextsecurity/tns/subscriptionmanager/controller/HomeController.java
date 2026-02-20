package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import cl.thenextsecurity.tns.subscriptionmanager.repository.DetectedTopicRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.MqttMessageRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import cl.thenextsecurity.tns.subscriptionmanager.service.DiscoveryService;
import cl.thenextsecurity.tns.subscriptionmanager.service.MqttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HomeController {

    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private final MqttService mqttService;
    private final DiscoveryService discoveryService;
    private final DetectedTopicRepository detectedTopicRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MqttMessageRepository mqttMessageRepository;

    /**
     * Dashboard principal. Carga el estado del broker, los topics detectados
     * y las suscripciones activas para poblar la vista index.
     */
    @GetMapping("/")
    public String index(Model model) {
        var subscriptions = subscriptionRepository.findByActiveTrue();

        Set<String> subscribedTopics = subscriptions.stream()
                .map(s -> s.getTopicName())
                .collect(Collectors.toSet());

        model.addAttribute("mqttConnected",       mqttService.isConnected());
        model.addAttribute("discoveryEnabled",    discoveryService.isDiscoveryEnabled());
        model.addAttribute("discoveryInterval",   discoveryService.getDiscoveryIntervalMs());
        model.addAttribute("discoveryDuration",   discoveryService.getDiscoveryDurationMs());
        model.addAttribute("detectedTopics",      detectedTopicRepository.findAllByOrderByLastSeenDesc());
        model.addAttribute("subscribedTopics",    subscribedTopics);
        model.addAttribute("subscriptions",       subscriptions);

        return "index";
    }

    /**
     * Historial de mensajes con filtros opcionales y paginación de 50 registros.
     * Los filtros se reenvían en cada cambio de página para mantener el contexto.
     */
    @GetMapping("/history")
    public String history(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 50, Sort.by("receivedAt").descending());

        LocalDateTime fromDt = parseDateTime(from);
        LocalDateTime toDt   = parseDateTime(to);

        boolean hasTopic     = topic    != null && !topic.isBlank();
        boolean hasClientId  = clientId != null && !clientId.isBlank();
        boolean hasDateRange = fromDt   != null && toDt != null;

        Page<MqttMessage> messages;

        if (hasTopic && hasDateRange) {
            messages = mqttMessageRepository.findByTopicAndReceivedAtBetween(topic, fromDt, toDt, pageable);
        } else if (hasTopic) {
            messages = mqttMessageRepository.findByTopic(topic, pageable);
        } else if (hasClientId) {
            messages = mqttMessageRepository.findByClientId(clientId, pageable);
        } else if (hasDateRange) {
            messages = mqttMessageRepository.findByReceivedAtBetween(fromDt, toDt, pageable);
        } else {
            messages = mqttMessageRepository.findAll(pageable);
        }

        model.addAttribute("messages",      messages);
        model.addAttribute("filterTopic",   topic);
        model.addAttribute("filterClientId", clientId);
        model.addAttribute("filterFrom",    from);
        model.addAttribute("filterTo",      to);

        return "history";
    }

    /**
     * Convierte el string de un input datetime-local (yyyy-MM-ddTHH:mm)
     * a LocalDateTime. Retorna null si el valor está vacío o no es válido.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.debug("Valor de fecha-hora no parseable: '{}'", value);
            return null;
        }
    }
}
