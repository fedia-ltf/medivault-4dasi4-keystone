package tn.esprit.medivault.user.dto;

/**
 * Publié sur RabbitMQ (exchange medivault.events, clé utilisateur.cree).
 * dossier-service crée un dossier vide quand role = PATIENT.
 */
public record UtilisateurCreeEvent(
        String utilisateurId,
        String role,
        String nom,
        String prenom
) {
}
