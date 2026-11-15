package tn.esprit.medivault.medicament.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuration de Swagger (la page http://localhost:8084/swagger-ui.html).
// @Configuration : classe de réglages lue au démarrage.
// @Bean : l'objet renvoyé par la méthode est gardé et utilisé par Spring.
// Ici on donne juste le titre, la description et la version affichés en haut de Swagger.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI medicamentOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediVault - medicament-service")
                .description("Catalogue des médicaments, interactions et vérification de compatibilité")
                .version("1.0"));
    }
}
