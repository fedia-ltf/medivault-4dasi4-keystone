package tn.esprit.medivault.soindomicile.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record VisiteUpdateRequest(@NotNull @Future LocalDateTime date, @NotBlank String adresse) {
}
