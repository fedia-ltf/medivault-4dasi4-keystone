package tn.esprit.medivault.medicament.events;

import java.time.LocalDateTime;

// Alerte « quota bientôt épuisé » : publiée UNE fois, quand une vente fait passer un quota de moins de 80 % à 80 % ou plus.
// Clé de routage quota.bientot.epuise. medicament-service la publie ; le notification-service pourra prévenir
// le pharmacien et l'administrateur (personne ne l'écoute encore : voir README).
public record QuotaBientotEpuise(String pharmacieId, Long medicamentId, String medicamentNom,
                                 int pourcentageUtilise, int reste, LocalDateTime date) {
}
