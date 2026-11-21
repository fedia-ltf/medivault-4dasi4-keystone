package tn.esprit.medivault.medicament.verification;

import java.util.List;
import java.util.Optional;

// Ce que medicament-service retient d'une ordonnance pour appliquer RG1.
public record OrdonnanceVerifiee(Long id, String code, String patientId, String statut, List<Ligne> lignes) {

    public record Ligne(Long medicamentId, Integer quantite) {
    }

    // Une ordonnance DELIVREE ou en attente de vérification ne peut pas servir.
    // VALIDEE et PARTIELLEMENT_DELIVREE (pas encore tout délivré) sont acceptées.
    public boolean valide() {
        return "VALIDEE".equals(statut) || "PARTIELLEMENT_DELIVREE".equals(statut);
    }

    public Optional<Ligne> ligne(Long medicamentId) {
        return lignes == null ? Optional.empty()
                : lignes.stream().filter(l -> medicamentId.equals(l.medicamentId())).findFirst();
    }
}