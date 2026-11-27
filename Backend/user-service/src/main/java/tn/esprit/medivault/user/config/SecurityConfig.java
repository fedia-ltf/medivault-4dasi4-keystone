package tn.esprit.medivault.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sécurité du user-service : Spring Security + jeton JWT de Keycloak (le rôle est lu dans le jeton).
 *
 * Interrupteur : medivault.security.enabled
 *   false (par défaut) : tout est ouvert, comme avant (tests, développement seul, autres équipes) ;
 *   true               : il faut un jeton valide, et le rôle décide de ce qu'on a le droit de faire (profil « keycloak »).
 *
 * Plus tard (Gateway) : la Gateway pourra aussi vérifier le jeton avant de router ; ce contrôle reste utile ici.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Set<String> ROLES = Set.of("PATIENT", "MEDECIN", "PHARMACIEN", "LABORANTIN", "ADMIN");

    @Bean
    @ConditionalOnProperty(name = "medivault.security.enabled", havingValue = "true")
    public SecurityFilterChain filtreSecurise(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // connexion, inscription, mot de passe oublié : ouverts à tous
                        .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/api/auth/inscription", "/api/auth/mot-de-passe-oublie").permitAll()
                        .requestMatchers("/actuator/health/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // lectures appelées par les autres services (pharmacie active ? horaires ? signature ?)
                        .requestMatchers(HttpMethod.GET, "/api/utilisateurs/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/professionnels", "/api/professionnels/**").permitAll()
                        // le médecin dépose sa signature
                        .requestMatchers(HttpMethod.PUT, "/api/professionnels/*/signature").hasAnyRole("MEDECIN", "ADMIN")
                        // créer des comptes, lister les comptes, activer/désactiver : administrateur seulement
                        .requestMatchers("/api/utilisateurs", "/api/utilisateurs/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(convertisseurDeRoles()))
                        .authenticationEntryPoint((req, res, e) -> ecrire(res, objectMapper, HttpStatus.UNAUTHORIZED,
                                "Non authentifié", "Connectez-vous pour accéder à cette ressource.")))
                .exceptionHandling(e -> e.accessDeniedHandler((req, res, ex) -> ecrire(res, objectMapper, HttpStatus.FORBIDDEN,
                        "Accès refusé", "Votre rôle ne permet pas cette opération.")));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "medivault.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain filtreOuvert(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }

    /** Vérifie la signature des jetons avec les clés publiques de Keycloak. */
    @Bean
    @ConditionalOnProperty(name = "medivault.security.enabled", havingValue = "true")
    public JwtDecoder jwtDecoder(@Value("${medivault.security.jwk-set-uri}") String jwkSetUri) {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /** Le rôle MediVault est dans le jeton Keycloak : realm_access.roles -> ROLE_ADMIN, ROLE_MEDECIN… */
    private JwtAuthenticationConverter convertisseurDeRoles() {
        JwtAuthenticationConverter convertisseur = new JwtAuthenticationConverter();
        convertisseur.setJwtGrantedAuthoritiesConverter(jwt -> {
            Map<String, Object> acces = jwt.getClaim("realm_access");
            Object roles = acces == null ? null : acces.get("roles");
            if (!(roles instanceof Collection<?> liste)) {
                return List.<GrantedAuthority>of();
            }
            return liste.stream().map(Object::toString).filter(ROLES::contains)
                    .<GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        });
        return convertisseur;
    }

    private static void ecrire(HttpServletResponse res, ObjectMapper mapper, HttpStatus statut, String titre, String detail) throws IOException {
        ProblemDetail probleme = ProblemDetail.forStatusAndDetail(statut, detail);
        probleme.setTitle(titre);
        res.setStatus(statut.value());
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), probleme);
    }
}
