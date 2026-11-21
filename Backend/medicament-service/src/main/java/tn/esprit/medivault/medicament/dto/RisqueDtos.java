package tn.esprit.medivault.medicament.dto;

import java.util.List;

// Score de risque d'une pharmacie pour un mois (0 à 100) : plus il est haut, plus elle mérite d'être surveillée.
public final class RisqueDtos {

    private RisqueDtos() {
    }

    public enum Niveau {
        FAIBLE,   // moins de 30
        MOYEN,    // 30 à 59 : à surveiller
        ELEVE     // 60 et plus : à surveiller de près
    }

    // raisons : les phrases qui expliquent le score (le score n'est jamais une boîte noire)
    public record Pharmacie(String pharmacieId, int score, Niveau niveau, int refus, int avertissements,
                            int suspensions, int tauxConsommation, List<String> raisons) {
    }
}
