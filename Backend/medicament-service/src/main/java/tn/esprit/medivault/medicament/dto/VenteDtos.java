package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tn.esprit.medivault.medicament.entities.VenteSensible;

import java.time.LocalDateTime;
import java.time.YearMonth;

public final class VenteDtos {

    private VenteDtos() {
    }

    // Ce que le pharmacien (ou soin-domicile pour une injection) envoie pour déclarer une vente.
    // Le patient n'est pas demandé : il vient de l'ordonnance.
    public record Requete(
            @NotBlank(message = "La pharmacie est obligatoire") String pharmacieId,
            @NotBlank(message = "Le code de l'ordonnance est obligatoire") String ordonnanceCode,
            @NotNull(message = "Le médicament est obligatoire") Long medicamentId,
            @Min(value = 1, message = "La quantité doit être d'au moins 1") int quantite) {
    }

    public record Reponse(Long id, String pharmacieId, Long medicamentId, String medicamentNom, YearMonth mois,
                          String patientId, Long ordonnanceId, String ordonnanceCode, int quantite, LocalDateTime date) {

        public static Reponse de(VenteSensible v) {
            var a = v.getAttribution();
            return new Reponse(v.getId(), a.getPharmacieId(), a.getMedicament().getId(), a.getMedicament().getNomCommercial(),
                    a.getMois(), v.getPatientId(), v.getOrdonnanceId(), v.getOrdonnanceCode(), v.getQuantite(), v.getDate());
        }
    }
}