package tn.esprit.medivault.dossier.messaging.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UtilisateurCree(
        String utilisateurId,
        String role,
        String nom,
        String prenom
) {}
