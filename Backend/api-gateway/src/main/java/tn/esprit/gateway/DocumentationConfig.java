package tn.esprit.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

// Swagger unifié : http://localhost:9000/swagger-ui.html montre la documentation des 7 microservices.
// Chaque document OpenAPI est lu chez le service (trouvé par Eureka) puis adapté pour que « Try it out »
// passe lui aussi par la Gateway :
//   - servers = « / » : les requêtes de test partent vers la Gateway, pas vers le port du service ;
//   - bouton « Authorize » : on colle le jeton JWT Keycloak (obtenu par POST /api/auth/login).
// Ces routes sont écrites en Java (et non dans application.properties) car modifier le corps d'une réponse
// (ModifyResponseBody) n'est possible qu'ici ; les routes métier restent dans application.properties.
@Configuration
public class DocumentationConfig {

    /** nom affiché dans Swagger → nom du service dans Eureka */
    static final Map<String, String> SERVICES = new LinkedHashMap<>();
    static {
        SERVICES.put("medicament", "MEDICAMENT-SERVICE");
        SERVICES.put("user", "USER-SERVICE");
        SERVICES.put("dossier", "DOSSIER-SERVICE");
        SERVICES.put("ordonnance", "ORDONNANCE-SERVICE");
        SERVICES.put("soin-domicile", "SOIN-DOMICILE-SERVICE");
        SERVICES.put("analyse", "ANALYSE-SERVICE");
        SERVICES.put("rendezvous", "RENDEZVOUS-SERVICE");
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean
    public RouteLocator routesDocumentation(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        SERVICES.forEach((nom, service) -> routes.route("docs-" + nom, r -> r
                .path("/docs/" + nom)
                .filters(f -> f
                        .setPath("/v3/api-docs")
                        .modifyResponseBody(String.class, String.class, (echange, corps) -> Mono.justOrEmpty(adapter(corps))))
                .uri("lb://" + service)));
        return routes.build();
    }

    /** Le document OpenAPI du service, réglé pour passer par la Gateway avec un jeton JWT */
    static String adapter(String corps) {
        if (corps == null || corps.isBlank()) {
            return corps;
        }
        try {
            JsonNode lu = JSON.readTree(corps);
            if (!(lu instanceof ObjectNode doc)) {
                return corps;
            }
            doc.putArray("servers").addObject().put("url", "/").put("description", "API Gateway");
            ObjectNode composants = doc.get("components") instanceof ObjectNode c ? c : doc.putObject("components");
            ObjectNode schemas = composants.get("securitySchemes") instanceof ObjectNode s ? s : composants.putObject("securitySchemes");
            schemas.putObject("jwt").put("type", "http").put("scheme", "bearer").put("bearerFormat", "JWT")
                    .put("description", "Jeton Keycloak : POST /api/auth/login (service user) puis copier accessToken");
            doc.putArray("security").addObject().putArray("jwt");
            return JSON.writeValueAsString(doc);
        } catch (RuntimeException e) {
            // document illisible : on le renvoie tel quel plutôt que de casser la page
            return corps;
        }
    }
}
