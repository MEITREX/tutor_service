package de.unistuttgart.iste.meitrex.tutor_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration()
@ConfigurationProperties(prefix = "ollama")
@Setter
@Getter
public class OllamaConfig {

    private String url;

}
