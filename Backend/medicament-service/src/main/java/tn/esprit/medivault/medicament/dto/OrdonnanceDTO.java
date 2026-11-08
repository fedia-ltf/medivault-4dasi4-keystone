package tn.esprit.medivault.medicament.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Vue simplifiée d'une ordonnance renvoyée par ordonnance-service
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrdonnanceDTO {

    private Long id;
    private Long patient_id;
    private String statut;
}
