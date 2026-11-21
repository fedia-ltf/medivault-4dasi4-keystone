package tn.esprit.medivault.medicament.events;

import java.time.LocalDateTime;

// Événement de la documentation (nom EXACT, clé de routage vente.sensible.enregistree) :
// medicament-service -> ordonnance-service, qui augmente quantiteDelivree de la ligne puis met à jour le statut.
public record VenteSensibleEnregistree(Long ordonnanceId, Long medicamentId, int quantite, String pharmacieId, LocalDateTime date) {
}