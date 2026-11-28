package tn.esprit.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

// Sécurité de la Gateway, point d'entrée unique de MediVault :
//  - toute requête venant de l'extérieur doit porter un jeton JWT Keycloak valide (Authorization: Bearer ...),
//    vérifié avec les clés publiques du realm medivault (spring.security.oauth2.resourceserver.jwt.jwk-set-uri) ;
//  - sont publiques : la connexion / l'inscription / le mot de passe oublié, Swagger et la santé de la Gateway.
// Les microservices s'appellent entre eux directement (OpenFeign + Eureka, RabbitMQ) : ils ne passent pas par ici.
// Les droits fins par rôle restent vérifiés dans chaque service (ex. user-service : ADMIN pour créer un compte).
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] ROUTES_PUBLIQUES = {
            "/api/auth/**",
            "/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/v3/api-docs/**", "/docs/**",
            "/actuator/health/**", "/actuator/info"
    };

    @Bean
    public SecurityWebFilterChain chaineDeSecurite(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(acces -> acces
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .pathMatchers(ROUTES_PUBLIQUES).permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(serveur -> serveur.jwt(Customizer.withDefaults()))
                .build();
    }

    // CORS géré ICI seulement (aucun @CrossOrigin dans les services) : même les réponses 401 portent l'en-tête,
    // pour que le front Angular sache que son jeton a expiré
    @Bean
    public CorsConfigurationSource corsConfigurationSource(@Value("${medivault.cors.allowed-origins}") List<String> origines) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origines);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
