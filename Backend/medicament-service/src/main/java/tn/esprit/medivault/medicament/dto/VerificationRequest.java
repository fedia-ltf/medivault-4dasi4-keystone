package tn.esprit.medivault.medicament.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// DTO d'entrée de POST /api/medicaments/compatibilite.
// Exemple de JSON :
// { "medicament_ids": [3, 7], "allergies": ["Pénicilline"] }
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationRequest {

    // @NotEmpty : la liste doit contenir au moins 1 id. @NotNull sur chaque élément : pas d'id null.
    @NotEmpty(message = "La liste des médicaments ne doit pas être vide")
    private List<@NotNull Long> medicament_ids;

    // Allergies du patient exprimées en classes thérapeutiques (ex. "Pénicilline").
    // Facultatif : liste vide par défaut.
    private List<String> allergies = new ArrayList<>();
}
