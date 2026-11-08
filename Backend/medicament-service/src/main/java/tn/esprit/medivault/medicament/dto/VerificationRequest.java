package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    @NotEmpty(message = "La liste des médicaments ne doit pas être vide")
    private List<@NotNull Long> medicament_ids;

    // Allergies du patient exprimées en classes thérapeutiques (ex. "Pénicilline")
    private List<String> allergies = new ArrayList<>();
}
