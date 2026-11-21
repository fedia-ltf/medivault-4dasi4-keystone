package tn.esprit.medivault.analyse.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI analyseOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediVault - analyse-service")
                .description("Analyses demandées en consultation, affectées au laboratoire du rendez-vous, saisie des résultats")
                .version("1.0"));
    }
}
