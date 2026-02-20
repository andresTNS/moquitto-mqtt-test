package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.entity.Subscription;
import cl.thenextsecurity.tns.subscriptionmanager.repository.SubscriptionRepository;
import cl.thenextsecurity.tns.subscriptionmanager.service.MqttService;
import cl.thenextsecurity.tns.subscriptionmanager.service.SseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@DisplayName("SubscriptionController — tests de capa web")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private MqttService mqttService;

    @MockitoBean
    private SseService sseService;

    // =========================================================================
    // POST /subscriptions/save
    // =========================================================================

    @Test
    @DisplayName("POST /subscriptions/save retorna redirect a /")
    void save_retornaRedirectAlDashboard() throws Exception {
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/subscriptions/save")
                        .param("selectedTopics", "sensor/temperatura"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /subscriptions/save con topic nuevo llama save() y subscribe()")
    void save_conTopicNuevo_guardaYSuscribe() throws Exception {
        // No hay suscripciones actuales en BD
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/subscriptions/save")
                        .param("selectedTopics", "sensor/temperatura"));

        verify(subscriptionRepository).save(any(Subscription.class));
        verify(mqttService).subscribe(eq("sensor/temperatura"));
    }

    @Test
    @DisplayName("POST /subscriptions/save con topic existente no seleccionado llama deleteByTopicName() y unsubscribe()")
    void save_conTopicExistenteNoSeleccionado_eliminaYDesuscribe() throws Exception {
        Subscription existente = Subscription.builder()
                .topicName("sensor/humedad")
                .active(true)
                .build();

        // "sensor/humedad" está en BD pero NO en selectedTopics
        when(subscriptionRepository.findAll()).thenReturn(List.of(existente));

        mockMvc.perform(post("/subscriptions/save")
                        .param("selectedTopics", "sensor/temperatura"));

        verify(mqttService).unsubscribe(eq("sensor/humedad"));
        verify(subscriptionRepository).deleteByTopicName(eq("sensor/humedad"));
    }

    @Test
    @DisplayName("POST /subscriptions/save sin selectedTopics elimina todos los existentes")
    void save_sinSelectedTopics_eliminaTodosLosExistentes() throws Exception {
        Subscription existente = Subscription.builder()
                .topicName("sensor/temperatura")
                .active(true)
                .build();

        when(subscriptionRepository.findAll()).thenReturn(List.of(existente));

        // No se envía ningún selectedTopics (ningún checkbox marcado)
        mockMvc.perform(post("/subscriptions/save"));

        verify(mqttService).unsubscribe(eq("sensor/temperatura"));
        verify(subscriptionRepository).deleteByTopicName(eq("sensor/temperatura"));
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST /subscriptions/save siempre llama sendTopicsUpdated() en SseService")
    void save_siempreLlamaSendTopicsUpdated() throws Exception {
        when(subscriptionRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(post("/subscriptions/save"));

        verify(sseService).sendTopicsUpdated();
    }
}
