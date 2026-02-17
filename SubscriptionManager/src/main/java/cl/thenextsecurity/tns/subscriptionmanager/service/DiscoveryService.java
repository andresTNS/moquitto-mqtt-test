package cl.thenextsecurity.tns.subscriptionmanager.service;

import cl.thenextsecurity.tns.subscriptionmanager.entity.DetectedTopic;
import cl.thenextsecurity.tns.subscriptionmanager.repository.DetectedTopicRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class DiscoveryService {

    private final DetectedTopicRepository detectedTopicRepository;
    private final SseService sseService;

    // Inyección @Lazy para romper la dependencia circular con MqttService
    private MqttService mqttService;

    @Value("${mqtt.discovery.enabled:true}")
    private boolean discoveryEnabled;

    @Value("${mqtt.discovery.duration:30000}")
    private long discoveryDurationMs;

    // volatile garantiza visibilidad entre threads (MqttService lo consulta constantemente)
    private volatile boolean isScanning = false;

    private final Set<String> discoveredTopics = ConcurrentHashMap.newKeySet();
    private final Map<String, String> discoveredClientIdsByTopic = new ConcurrentHashMap<>();

    public DiscoveryService(DetectedTopicRepository detectedTopicRepository, SseService sseService) {
        this.detectedTopicRepository = detectedTopicRepository;
        this.sseService = sseService;
    }

    @Autowired
    @Lazy
    public void setMqttService(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    /**
     * Consultado por MqttService en cada mensaje recibido para decidir
     * si redirigir al proceso de descubrimiento o al almacenamiento normal.
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * Registra un topic observado durante el escaneo activo.
     * Llamado desde MqttService.messageArrived() cuando isScanning=true.
     */
    public void recordDiscoveredTopic(String topic, String clientId) {
        discoveredTopics.add(topic);
        if (clientId != null && !clientId.isBlank()) {
            discoveredClientIdsByTopic.put(topic, clientId);
        }
    }

    /**
     * Ejecuta el ciclo completo de descubrimiento:
     * suscribe al wildcard "#", espera, desuscribe y procesa resultados.
     * Diseñado para ejecutarse en un thread separado (no bloquea el scheduler).
     */
    public void runDiscovery() {
        if (isScanning) {
            log.warn("Escaneo ya en curso, se omite nueva solicitud");
            return;
        }

        log.info("Iniciando escaneo de descubrimiento MQTT...");
        isScanning = true;
        discoveredTopics.clear();
        discoveredClientIdsByTopic.clear();

        try {
            mqttService.subscribe("#");
            Thread.sleep(discoveryDurationMs);
            mqttService.unsubscribe("#");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Escaneo interrumpido", e);
        } catch (Exception e) {
            log.error("Error durante el escaneo MQTT", e);
        } finally {
            isScanning = false;
        }

        processDiscoveredTopics();
        sseService.sendTopicsUpdated();
        log.info("Escaneo finalizado. Topics encontrados: {}", discoveredTopics.size());
    }

    /**
     * Lanza un escaneo periódico en un virtual thread para no bloquear
     * el thread del scheduler de Spring mientras dure el Thread.sleep().
     */
    @Scheduled(fixedRateString = "${mqtt.discovery.interval:300000}")
    public void schedulePeriodicDiscovery() {
        if (!discoveryEnabled) {
            return;
        }
        Thread.ofVirtual().start(this::runDiscovery);
    }

    /**
     * Actualiza o crea registros en detected_topics según los topics observados.
     * Marca como inactivos los topics que no aparecieron en este escaneo.
     */
    @Transactional
    public void processDiscoveredTopics() {
        for (String topic : discoveredTopics) {
            String clientId = discoveredClientIdsByTopic.get(topic);

            detectedTopicRepository.findByTopicName(topic).ifPresentOrElse(
                    existing -> {
                        existing.setActive(true);
                        existing.setLastClientId(clientId);
                        existing.setMessageCount(existing.getMessageCount() + 1);
                        // lastSeen se actualiza con la fecha actual
                        existing.setLastSeen(java.time.LocalDateTime.now());
                        detectedTopicRepository.save(existing);
                    },
                    () -> {
                        // firstDetected y lastSeen se asignan via @PrePersist
                        DetectedTopic nuevo = DetectedTopic.builder()
                                .topicName(topic)
                                .lastClientId(clientId)
                                .build();
                        detectedTopicRepository.save(nuevo);
                    }
            );
        }

        // Marcar como inactivos los topics que no aparecieron en este escaneo
        detectedTopicRepository.findByActiveTrue().stream()
                .filter(t -> !discoveredTopics.contains(t.getTopicName()))
                .forEach(t -> {
                    t.setActive(false);
                    detectedTopicRepository.save(t);
                });
    }
}
