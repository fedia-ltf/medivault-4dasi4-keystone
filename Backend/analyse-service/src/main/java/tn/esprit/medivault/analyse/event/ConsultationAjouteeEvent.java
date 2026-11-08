package tn.esprit.medivault.analyse.event;

import java.util.List;

/** Consommé (clé consultation.ajoutee) : crée une analyse EN_ATTENTE par analyse demandée. */
public record ConsultationAjouteeEvent(Long consultationId, Long patientId, Long medecinId,
                                       List<String> analysesDemandees) {
}
