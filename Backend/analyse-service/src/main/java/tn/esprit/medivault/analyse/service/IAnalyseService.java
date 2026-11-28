package tn.esprit.medivault.analyse.service;

import tn.esprit.medivault.analyse.dto.AnalyseResponse;
import tn.esprit.medivault.analyse.dto.ResultatRequest;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.event.ConsultationAjouteeEvent;
import tn.esprit.medivault.analyse.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.analyse.event.RendezVousConfirmeEvent;

import java.util.List;

public interface IAnalyseService {

    List<AnalyseResponse> rechercher(StatutAnalyse statut, String laboratoireId);

    AnalyseResponse trouverParId(Long id);

    List<AnalyseResponse> analysesDuPatient(String patientId);

    AnalyseResponse saisirResultat(Long analyseId, ResultatRequest request);

    void creerDepuisConsultation(ConsultationAjouteeEvent event);

    void affecterLaboratoire(RendezVousConfirmeEvent event);

    void annulerAffectation(RendezVousAnnuleEvent event);
}
