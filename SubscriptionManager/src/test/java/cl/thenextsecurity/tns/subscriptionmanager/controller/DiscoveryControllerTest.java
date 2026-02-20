package cl.thenextsecurity.tns.subscriptionmanager.controller;

import cl.thenextsecurity.tns.subscriptionmanager.service.DiscoveryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DiscoveryController.class)
@DisplayName("DiscoveryController — tests de capa web")
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscoveryService discoveryService;

    // =========================================================================
    // POST /discovery/scan
    // =========================================================================

    @Test
    @DisplayName("POST /discovery/scan retorna redirect a /")
    void scanNow_retornaRedirectAlDashboard() throws Exception {
        mockMvc.perform(post("/discovery/scan"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /discovery/scan llama runDiscovery() en background")
    void scanNow_llamaRunDiscoveryEnBackground() throws Exception {
        mockMvc.perform(post("/discovery/scan"));

        // runDiscovery() se ejecuta en virtual thread; timeout(1000) espera hasta 1s
        verify(discoveryService, timeout(1000)).runDiscovery();
    }

    // =========================================================================
    // POST /discovery/configure
    // =========================================================================

    @Test
    @DisplayName("POST /discovery/configure retorna redirect a /")
    void configure_retornaRedirectAlDashboard() throws Exception {
        mockMvc.perform(post("/discovery/configure")
                        .param("enabled",  "true")
                        .param("interval", "300000")
                        .param("duration", "30000"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("POST /discovery/configure sin 'enabled' usa false por defecto (checkbox desmarcado)")
    void configure_sinEnabled_usaFalsePorDefecto() throws Exception {
        mockMvc.perform(post("/discovery/configure")
                .param("interval", "300000")
                .param("duration", "30000"));

        // El checkbox HTML no envía valor cuando está desmarcado → defaultValue="false"
        verify(discoveryService).updateConfig(false, 300000L, 30000L);
    }

    @Test
    @DisplayName("POST /discovery/configure llama updateConfig() con los valores correctos")
    void configure_llamaUpdateConfigConValoresCorrectos() throws Exception {
        mockMvc.perform(post("/discovery/configure")
                        .param("enabled",  "true")
                        .param("interval", "600000")
                        .param("duration", "45000"));

        verify(discoveryService).updateConfig(true, 600000L, 45000L);
    }
}
