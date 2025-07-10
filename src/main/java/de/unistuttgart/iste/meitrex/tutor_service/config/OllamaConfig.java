package de.unistuttgart.iste.meitrex.tutor_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration()
@Getter
public class OllamaConfig {

    @Value("${ollama.url}")
    private String url;

}
