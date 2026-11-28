package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import tn.esprit.medivault.medicament.entities.Attribution;
import tn.esprit.medivault.medicament.entities.StatutAttribution;

import java.time.YearMonth;
import java.util.List;

public final class AttributionDtos {

    private AttributionDtos() {
    }

    // Créer un quota pour une pharmacie. mois au format "2026-11".
    public record Requete(
            @NotBlank(message = "La pharmacie est obligatoire") String pharmacieId,
            @NotNull(message = "Le médicament est obligatoire") Long medicamentId,
            @NotNull(message = "Le mois est obligatoire") YearMonth mois,
            @Min(value = 1, message = "La quantité doit être d'au moins 1") int quantiteAttribuee) {
    }

    // Modifier un quota existant : seuls la quantité et le statut peuvent changer
    // (pharmacie, médicament et mois sont l'identité du quota).
    public record Modification(
            @Min(value = 1, message = "La quantité doit être d'au moins 1") int quantiteAttribuee,
            StatutAttribution statut) {
    }

    // Donner le même quota à plusieurs pharmacies d'un coup
    public record Lot(
            @NotNull(message = "Le médicament est obligatoire") Long medicamentId,
            @NotNull(message = "Le mois est obligatoire") YearMonth mois,
            @Min(value = 1, message = "La quantité doit être d'au moins 1") int quantiteAttribuee,
            @NotEmpty(message = "Choisissez au moins une pharmacie") List<@NotBlank String> pharmacieIds) {
    }

    public record ResultatLot(int crees, int ignores, List<Reponse> attributions, List<String> pharmaciesIgnorees) {
    }

    public record Reponse(Long id, String pharmacieId, Long medicamentId, String medicamentNom, YearMonth mois,
                          int quantiteAttribuee, int quantiteVendue, int reste, int avertissements,
                          StatutAttribution statut) {

        public static Reponse de(Attribution a) {
            return new Reponse(a.getId(), a.getPharmacieId(), a.getMedicament().getId(), a.getMedicament().getNomCommercial(),
                    a.getMois(), a.getQuantiteAttribuee(), a.getQuantiteVendue(), a.reste(), a.getAvertissements(), a.getStatut());
        }
    }

    // GET /attributions/{id}/bilan : reçu, vendu, reste, avertissements
    public record Bilan(Long attributionId, String pharmacieId, Long medicamentId, String medicament, YearMonth mois,
                        int recu, int vendu, int reste, int avertissements, StatutAttribution statut) {

        public static Bilan de(Attribution a) {
            return new Bilan(a.getId(), a.getPharmacieId(), a.getMedicament().getId(), a.getMedicament().getNomCommercial(),
                    a.getMois(), a.getQuantiteAttribuee(), a.getQuantiteVendue(), a.reste(), a.getAvertissements(), a.getStatut());
        }
    }

    // RG4 : résultat du passage au nouveau mois
    public record Renouvellement(YearMonth moisBilan, YearMonth moisNouveau, int crees, int dejaExistants, List<Bilan> bilans) {
    }

    public enum Etat {
        DISPONIBLE, BIENTOT_EPUISE, INDISPONIBLE
    }

    // GET /disponibilites?medicamentId= : où trouver ce médicament ce mois-ci
    public record Disponibilite(String pharmacieId, int reste, Etat etat) {
    }
}