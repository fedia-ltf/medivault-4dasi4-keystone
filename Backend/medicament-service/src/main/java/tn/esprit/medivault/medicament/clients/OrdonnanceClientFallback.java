package tn.esprit.medivault.medicament.clients;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.OrdonnanceDTO;

import java.util.List;

// FALLBACK = plan B (Resilience4j / circuit breaker).
// Utilisé quand ordonnance-service est indisponible (arrêté, trop lent, erreurs répétées) :
// au lieu de planter, on renvoie une réponse prévue -> une liste vide.
// Résultat : le retrait du médicament continue sans bloquer.
// @Component : Spring crée cet objet pour pouvoir l'utiliser.
@Component
@Slf4j
public class OrdonnanceClientFallback implements OrdonnanceClient {

    @Override
    public List<OrdonnanceDTO> getOrdonnances(Long medicamentId, String statut) {
        log.warn("ordonnance-service indisponible : impossible de vérifier les ordonnances du médicament {}", medicamentId);
        return List.of();
    }
}
