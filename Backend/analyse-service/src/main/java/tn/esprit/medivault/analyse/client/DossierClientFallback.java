package tn.esprit.medivault.analyse.client;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.analyse.dto.AccesResponse;

/** Circuit ouvert ou dossier-service injoignable : aucun résultat sans justification d'accès vérifiée. */
@Component
public class DossierClientFallback implements DossierClient {

    @Override
    public AccesResponse verifierAcces(String patientId, String professionnelId) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "dossier-service indisponible : saisie du résultat refusée, réessayer plus tard");
    }
}
