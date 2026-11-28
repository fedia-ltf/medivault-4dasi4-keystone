package tn.esprit.medivault.ordonnance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneOrdonnanceDTO {
    @NotNull
    private Long medicamentId;
    @NotBlank
    private String nomMedicament;
    @NotBlank
    private String posologie;
    @Min(1)
    private int dureeJours;
    @Min(1)
    private int quantite;
}