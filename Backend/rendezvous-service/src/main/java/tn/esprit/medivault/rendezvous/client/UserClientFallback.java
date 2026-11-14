package tn.esprit.medivault.rendezvous.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.rendezvous.dto.DisponibilitesResponse;

/**
 * Fallback Resilience4j si user-service est injoignable ou en panne.
 * Justification : Évite de générer des créneaux en dehors des horaires réels du professionnel.
 */
@Component
public class UserClientFallback implements UserClient {

    @Override
    public DisponibilitesResponse getDisponibilites(String id) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "user-service indisponible : créneaux non générés, réessayer plus tard");
    }
}
