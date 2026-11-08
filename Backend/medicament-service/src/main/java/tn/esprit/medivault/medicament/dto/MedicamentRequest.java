package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.medicament.entities.FormeMedicament;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicamentRequest {

    @NotBlank(message = "Le nom commercial est obligatoire")
    @Size(max = 100)
    private String nom_commercial;

    @NotBlank(message = "La DCI est obligatoire")
    @Size(max = 100)
    private String dci;

    @NotBlank(message = "La classe thérapeutique est obligatoire")
    @Size(max = 100)
    private String classe_therapeutique;

    @NotNull(message = "La forme est obligatoire (COMPRIME, GELULE, SIROP, INJECTABLE, POMMADE)")
    private FormeMedicament forme;

    @Size(max = 50)
    private String dosage;
}
