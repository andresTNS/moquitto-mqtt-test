package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.entity.DetectedTopic;
import cl.thenextsecurity.tns.subscriptionmanager.entity.MqttMessage;
import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import cl.thenextsecurity.tns.subscriptionmanager.repository.DetectedTopicRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.MqttMessageRepository;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import cl.thenextsecurity.tns.subscriptionmanager.service.DiscoveryService;
import cl.thenextsecurity.tns.subscriptionmanager.service.MqttService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@DisplayName("HomeController — tests de capa web")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MqttService mqttService;

    @MockitoBean
    private DiscoveryService discoveryService;

    @MockitoBean
    private DetectedTopicRepository detectedTopicRepository;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private MqttMessageRepository mqttMessageRepository;

    // =========================================================================
    // GET /
    // =========================================================================

    @Test
    @DisplayName("GET / retorna HTTP 200 y vista index")
    void index_retornaVistaIndex() throws Exception {
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(detectedTopicRepository.findAllByOrderByLastSeenDesc()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("GET / incluye mqttConnected=true en el modelo cuando el broker está conectado")
    void index_modeloContieneEstadoBrokerConectado() throws Exception {
        when(mqttService.isConnected()).thenReturn(true);
        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(detectedTopicRepository.findAllByOrderByLastSeenDesc()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("mqttConnected", true));
    }

    @Test
    @DisplayName("GET / incluye detectedTopics en el modelo")
    void index_modeloContieneDetectedTopics() throws Exception {
        DetectedTopic topic = DetectedTopic.builder()
                .topicName("sensor/temperatura")
                .lastSeen(LocalDateTime.now())
                .firstDetected(LocalDateTime.now())
                .build();

        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of());
        when(detectedTopicRepository.findAllByOrderByLastSeenDesc()).thenReturn(List.of(topic));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("detectedTopics"));
    }

    @Test
    @DisplayName("GET / incluye subscriptions y subscribedTopics en el modelo")
    void index_modeloContieneSubscriptionsYSubscribedTopics() throws Exception {
        Subscription sub = Subscription.builder()
                .topicName("sensor/temperatura")
                .active(true)
                .build();

        when(subscriptionRepository.findByActiveTrue()).thenReturn(List.of(sub));
        when(detectedTopicRepository.findAllByOrderByLastSeenDesc()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("subscriptions"))
                .andExpect(model().attributeExists("subscribedTopics"));
    }

    // =========================================================================
    // GET /history
    // =========================================================================

    @Test
    @DisplayName("GET /history sin filtros retorna HTTP 200, vista history y llama findAll")
    void history_sinFiltros_retornaVistaHistory() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findAll(any(Pageable.class))).thenReturn(paginaVacia);

        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"));

        verify(mqttMessageRepository).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("GET /history?topic=sensor llama findByTopic con el topic correcto")
    void history_conFiltroTopic_llamaFindByTopic() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findByTopic(eq("sensor/temperatura"), any(Pageable.class)))
                .thenReturn(paginaVacia);

        mockMvc.perform(get("/history").param("topic", "sensor/temperatura"))
                .andExpect(status().isOk());

        verify(mqttMessageRepository).findByTopic(eq("sensor/temperatura"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /history?clientId=esp32 llama findByClientId con el clientId correcto")
    void history_conFiltroClientId_llamaFindByClientId() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findByClientId(eq("esp32"), any(Pageable.class)))
                .thenReturn(paginaVacia);

        mockMvc.perform(get("/history").param("clientId", "esp32"))
                .andExpect(status().isOk());

        verify(mqttMessageRepository).findByClientId(eq("esp32"), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /history?from=...&to=... llama findByReceivedAtBetween con las fechas parseadas")
    void history_conFiltroFechas_llamaFindByReceivedAtBetween() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findByReceivedAtBetween(any(), any(), any(Pageable.class)))
                .thenReturn(paginaVacia);

        mockMvc.perform(get("/history")
                        .param("from", "2025-01-01T00:00")
                        .param("to",   "2025-01-31T23:59"))
                .andExpect(status().isOk());

        verify(mqttMessageRepository).findByReceivedAtBetween(any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /history?topic=sensor&from=...&to=... llama findByTopicAndReceivedAtBetween")
    void history_conTopicYFechas_llamaFindByTopicAndFechas() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findByTopicAndReceivedAtBetween(eq("sensor/temperatura"), any(), any(), any(Pageable.class)))
                .thenReturn(paginaVacia);

        mockMvc.perform(get("/history")
                        .param("topic", "sensor/temperatura")
                        .param("from",  "2025-01-01T00:00")
                        .param("to",    "2025-01-31T23:59"))
                .andExpect(status().isOk());

        verify(mqttMessageRepository).findByTopicAndReceivedAtBetween(
                eq("sensor/temperatura"), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /history preserva el filtro topic en el modelo para la paginación")
    void history_preservaFiltroTopicEnModelo() throws Exception {
        Page<MqttMessage> paginaVacia = new PageImpl<>(List.of());
        when(mqttMessageRepository.findByTopic(any(), any(Pageable.class))).thenReturn(paginaVacia);

        mockMvc.perform(get("/history").param("topic", "sensor/temperatura"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filterTopic", "sensor/temperatura"));
    }
}
