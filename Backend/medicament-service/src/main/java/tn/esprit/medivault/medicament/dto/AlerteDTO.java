package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.medicament.entities.Gravite;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDTO {

    private TypeAlerte type;
    private Long medicament_id;
    // Renseigné uniquement pour une alerte d'interaction
    private Long medicament_associe_id;
    private Gravite gravite;
    // true si l'alerte rend la prescription impossible (RG4 / RG6)
    private boolean bloquante;
    private String message;
}
