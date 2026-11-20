package tn.esprit.medivault.analyse.event;

import java.time.LocalDateTime;
import java.util.List;

/** Consommé (clé consultation.ajoutee) : crée une analyse EN_ATTENTE par analyse demandée. */
public record ConsultationAjouteeEvent(Long consultationId, String patientId, String medecinId,
                                       List<String> analysesDemandees, LocalDateTime date) {
}
