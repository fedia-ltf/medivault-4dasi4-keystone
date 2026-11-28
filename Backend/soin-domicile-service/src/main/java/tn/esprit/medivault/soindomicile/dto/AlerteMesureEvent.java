package tn.esprit.medivault.soindomicile.dto;

import java.time.LocalDateTime;

public record AlerteMesureEvent(Long visiteId, String patientId, String medecinId,
                                String tension, Double glycemie, String seuilAlerte, LocalDateTime date) {
}
