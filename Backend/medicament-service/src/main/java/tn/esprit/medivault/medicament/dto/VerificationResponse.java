package tn.esprit.medivault.medicament.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {

    private boolean compatible;
    private List<AlerteDTO> alertes;
}
