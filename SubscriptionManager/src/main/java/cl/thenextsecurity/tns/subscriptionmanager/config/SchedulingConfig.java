package cl.thenextsecurity.tns.subscriptionmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Habilita el procesamiento de anotaciones @Scheduled en toda la aplicación
}
