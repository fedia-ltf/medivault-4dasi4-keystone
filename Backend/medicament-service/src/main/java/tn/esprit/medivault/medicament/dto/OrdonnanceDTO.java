package tn.esprit.medivault.medicament.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Vue simplifiée d'une ordonnance renvoyée par ordonnance-service (via Feign).
// On garde seulement les champs qui nous intéressent.
// @JsonIgnoreProperties(ignoreUnknown = true) : si ordonnance-service envoie d'autres champs,
//   on les ignore au lieu de planter. Comme ça, les deux services restent indépendants.
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdonnanceDTO {

    private Long id;
    private Long patient_id;
    private String statut;
}
