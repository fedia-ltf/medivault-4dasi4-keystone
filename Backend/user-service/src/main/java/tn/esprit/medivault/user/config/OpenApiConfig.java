package tn.esprit.medivault.user.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI userOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediVault - user-service")
                .description("Comptes, rôles Keycloak, profils et horaires des professionnels, signature du médecin")
                .version("1.0"));
    }
}
