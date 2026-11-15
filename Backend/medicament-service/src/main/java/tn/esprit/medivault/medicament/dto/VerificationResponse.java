package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO de sortie de la vérification de compatibilité.
// Exemple : { "compatible": false, "alertes": [ { "type": "ALLERGIE", ... } ] }
// compatible = false dès qu'au moins une alerte est bloquante.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private boolean compatible;
    private List<AlerteDTO> alertes;
}
