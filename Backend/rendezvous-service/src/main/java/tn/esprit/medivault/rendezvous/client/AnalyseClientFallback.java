package tn.esprit.medivault.rendezvous.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.rendezvous.dto.AnalyseResponse;

/**
 * Fallback Resilience4j si analyse-service est indisponible.
 * Justification : Pas de rendez-vous au laboratoire sans prescription vérifiée (fail-closed, 503).
 */
@Component
public class AnalyseClientFallback implements AnalyseClient {

    @Override
    public AnalyseResponse getAnalyseById(Long id) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "analyse-service indisponible : réservation au laboratoire refusée, réessayer plus tard");
    }
}

