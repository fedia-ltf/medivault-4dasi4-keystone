package tn.esprit.medivault.ordonnance.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrdonnanceCreeeEvent {
    private Long ordonnanceId;
    private String code;
    private Long patientId;
    private Long medecinId;
    private LocalDate dateEmission;
}