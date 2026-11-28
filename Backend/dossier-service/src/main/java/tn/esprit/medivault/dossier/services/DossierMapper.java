package tn.esprit.medivault.dossier.services;

import org.springframework.stereotype.Component;
import tn.esprit.medivault.dossier.dto.*;
import tn.esprit.medivault.dossier.entities.AccesDossier;
import tn.esprit.medivault.dossier.entities.Consultation;
import tn.esprit.medivault.dossier.entities.Dossier;
import tn.esprit.medivault.dossier.entities.LienDossier;

import java.util.Comparator;
import java.util.List;

@Component
public class DossierMapper {

    public DossierResponse toDossierResponse(Dossier dossier, List<ConsultationResponse> consultations) {
        List<LienDossierResponse> liens = (dossier.getLiens() != null ? dossier.getLiens() : List.<LienDossier>of())
                .stream()
                .sorted(Comparator.comparing(LienDossier::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toLienDossierResponse)
                .toList();

        return new DossierResponse(
                dossier.getId(),
                dossier.getPatientId(),
                dossier.getGroupeSanguin(),
                dossier.getAllergies(),
                dossier.getAntecedents(),
                consultations,
                liens
        );
    }

    public DossierResponse toDossierResponse(Dossier dossier, List<ConsultationResponse> consultations, List<LienDossierResponse> liens) {
        return new DossierResponse(
                dossier.getId(),
                dossier.getPatientId(),
                dossier.getGroupeSanguin(),
                dossier.getAllergies(),
                dossier.getAntecedents(),
                consultations,
                liens
        );
    }

    public LienDossierResponse toLienDossierResponse(LienDossier l) {
        return new LienDossierResponse(
                l.getId(),
                l.getType(),
                l.getReferenceId(),
                l.getDate(),
                l.getAuteurId(),
                l.getLibelle()
        );
    }

    public ConsultationResponse toConsultationResponse(Consultation c) {
        return new ConsultationResponse(
                c.getId(),
                c.getMedecinId(),
                c.getDate(),
                c.getMotif(),
                c.getDiagnostic(),
                c.getObservations(),
                c.getAnalysesDemandees()
        );
    }

    public AllergiesResponse toAllergiesResponse(Dossier dossier) {
        return new AllergiesResponse(
                dossier.getPatientId(),
                dossier.getAllergies()
        );
    }

    public AccesResponse toAccesResponse(AccesDossier acces) {
        return new AccesResponse(
                acces.getId(),
                acces.getProfessionnelId(),
                acces.getStatut(),
                acces.getDateAccord(),
                acces.getDateRetrait()
        );
    }
}
