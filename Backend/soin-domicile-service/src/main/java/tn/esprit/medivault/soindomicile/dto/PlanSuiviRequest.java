package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.*;
import tn.esprit.medivault.soindomicile.entities.Frequence;
import tn.esprit.medivault.soindomicile.entities.TypeSoin;
import java.time.LocalDate;
import java.time.LocalTime;

public record PlanSuiviRequest(
        @NotBlank String patientId,
        @NotBlank String medecinId,
        @NotBlank String pharmacieId,
        @NotBlank String adresse,
        @NotNull TypeSoin typeSoin,
        @NotNull Frequence frequence,
        @NotNull @FutureOrPresent LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        @NotNull LocalTime heureVisite,
        String seuilAlerte) {
}
