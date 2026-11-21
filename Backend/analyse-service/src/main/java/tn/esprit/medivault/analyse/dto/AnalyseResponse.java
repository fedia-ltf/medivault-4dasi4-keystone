package tn.esprit.medivault.analyse.dto;

import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Réponse de GET /api/analyses/{id}, aussi lue par rendezvous-service (id, patientId, medecinId,
 * laboratoireId, typeAnalyse, statut) pour vérifier une prescription avant réservation.
 */
public record AnalyseResponse(Long id, Long consultationId, String patientId, String medecinId,
                              String laboratoireId, String typeAnalyse, StatutAnalyse statut,
                              LocalDateTime dateDemande, LocalDateTime dateResultat,
                              List<ResultatResponse> resultats) {

    public static AnalyseResponse from(Analyse a) {
        return new AnalyseResponse(a.getId(), a.getConsultationId(), a.getPatientId(), a.getMedecinId(),
                a.getLaboratoireId(), a.getTypeAnalyse(), a.getStatut(), a.getDateDemande(), a.getDateResultat(),
                a.getResultats().stream().map(ResultatResponse::from).toList());
    }
}
