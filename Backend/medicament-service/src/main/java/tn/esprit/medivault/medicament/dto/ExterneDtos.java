package tn.esprit.medivault.medicament.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// Vues simplifiées de ce que renvoient les autres microservices (appels Feign).
// On garde seulement les champs utiles ; ignoreUnknown = true : si l'autre service ajoute des champs,
// on les ignore au lieu de planter (les services restent indépendants).
public final class ExterneDtos {

    private ExterneDtos() {
    }

    // ordonnance-service : GET /api/ordonnances/code/{code}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ordonnance(Long id, String code, String patientId, String statut, List<Ligne> lignes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ligne(Long medicamentId, Integer quantite, Integer quantiteDelivree) {
    }

    // user-service : GET /api/utilisateurs/{id}  ->  le champ actif (true / false)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Utilisateur(String id, boolean actif) {
    }
}
