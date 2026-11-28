package tn.esprit.medivault.medicament.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration de Swagger (la page http://localhost:8084/swagger-ui.html).
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medicamentOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediVault - medicament-service")
                .description("Médicaments sensibles : quota mensuel par pharmacie, vente justifiée par ordonnance, sanction au 2e avertissement")
                .version("2.0"));
    }
}
