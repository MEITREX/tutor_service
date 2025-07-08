package de.unistuttgart.iste.meitrex.tutor_service.config;

import de.unistuttgart.iste.meitrex.tutor_service.client.DocProcAIServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DocProcAIServiceConfig {

    @Value("${docProcAIService.url}")
    private String docProcAIServiceUrl;

    @Bean
    public DocProcAIServiceClient DocProcAIServiceClient(){
        final WebClient webClient = WebClient.builder().baseUrl(docProcAIServiceUrl).build();
        final GraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();
        return new DocProcAIServiceClient(graphQlClient);

    }

}
