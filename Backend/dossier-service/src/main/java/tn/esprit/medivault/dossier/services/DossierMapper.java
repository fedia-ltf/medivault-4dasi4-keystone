package tn.esprit.medivault.dossier.services;

import org.springframework.stereotype.Component;
import tn.esprit.medivault.dossier.dto.*;
import tn.esprit.medivault.dossier.entities.AccesDossier;
import tn.esprit.medivault.dossier.entities.Consultation;
import tn.esprit.medivault.dossier.entities.Dossier;

import java.util.List;

@Component
public class DossierMapper {

    public DossierResponse toDossierResponse(Dossier dossier, List<ConsultationResponse> consultations) {
        return new DossierResponse(
                dossier.getId(),
                dossier.getPatientId(),
                dossier.getGroupeSanguin(),
                dossier.getAllergies(),
                dossier.getAntecedents(),
                consultations
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
