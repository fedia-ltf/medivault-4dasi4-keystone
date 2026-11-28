package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.NotNull;
import tn.esprit.medivault.soindomicile.entities.Frequence;
import java.time.LocalDate;
import java.time.LocalTime;

public record PlanSuiviUpdateRequest(
        @NotNull Frequence frequence,
        @NotNull LocalDate dateFin,
        @NotNull LocalTime heureVisite,
        String seuilAlerte) {
}
