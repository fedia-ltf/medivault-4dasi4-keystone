package tn.esprit.medivault.medicament.events;

import java.time.LocalDateTime;

// Événement de la documentation (nom EXACT, clé de routage pharmacie.sanction) :
// medicament-service -> notification-service, qui avertit le pharmacien (RG3).
public record SanctionPharmacie(String pharmacieId, Niveau niveau, String motif, LocalDateTime date) {

    public enum Niveau {
        AVERTISSEMENT,
        SUSPENSION
    }
}