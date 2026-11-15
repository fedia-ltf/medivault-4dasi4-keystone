package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.medivault.medicament.entities.Gravite;

// DTO = objet qui voyage dans le JSON (pas une table).
// Une alerte = un problème trouvé pendant la vérification de compatibilité.
// @Data (Lombok) : génère getters, setters, toString, equals, hashCode.
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteDTO {

    // ALLERGIE, INTERACTION ou MEDICAMENT_RETIRE
    private TypeAlerte type;
    // Le médicament concerné
    private Long medicament_id;
    // Renseigné uniquement pour une alerte d'interaction (l'autre médicament)
    private Long medicament_associe_id;
    // Renseigné uniquement pour une interaction
    private Gravite gravite;
    // true si l'alerte rend la prescription impossible (RG4 / RG6)
    private boolean bloquante;
    // Phrase lisible pour le médecin
    private String message;
}
