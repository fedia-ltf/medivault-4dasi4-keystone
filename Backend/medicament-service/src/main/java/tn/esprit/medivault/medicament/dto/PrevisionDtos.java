package tn.esprit.medivault.medicament.dto;

import java.time.LocalDate;
import java.time.YearMonth;

// Prévision d'épuisement d'un quota (par pharmacie) ou d'un médicament (toutes pharmacies confondues).
public final class PrevisionDtos {

    private PrevisionDtos() {
    }

    // OK : rien à signaler · A_SURVEILLER : à ce rythme, épuisé avant la fin du mois
    // ALERTE : 80 % du quota déjà utilisé (le seuil de l'alerte RabbitMQ) · EPUISE : plus rien
    public enum Niveau {
        OK,
        A_SURVEILLER,
        ALERTE,
        EPUISE
    }

    public record Quota(Long attributionId, String pharmacieId, Long medicamentId, String medicamentNom, YearMonth mois,
                        int quantiteAttribuee, int quantiteVendue, int reste, int pourcentageUtilise,
                        double rythmeParJour, LocalDate dateEpuisementPrevue, boolean epuisementAvantFinDeMois,
                        Niveau niveau, String message) {
    }

    public record Medicament(Long medicamentId, String medicamentNom, YearMonth mois, int pharmacies,
                             int quantiteAttribuee, int quantiteVendue, int reste, int pourcentageUtilise,
                             double rythmeParJour, LocalDate dateEpuisementPrevue, boolean epuisementAvantFinDeMois,
                             Niveau niveau, int pharmaciesEnAlerte, String message) {
    }
}
