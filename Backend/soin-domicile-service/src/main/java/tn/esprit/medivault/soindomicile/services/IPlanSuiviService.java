package tn.esprit.medivault.soindomicile.services;

import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.PlanSuivi;
import tn.esprit.medivault.soindomicile.entities.Visite;
import java.util.List;

public interface IPlanSuiviService {
    PlanSuivi creer(PlanSuiviRequest r);
    List<PlanSuivi> lister(String patientId, String medecinId);
    PlanSuivi getById(Long id);
    List<Visite> getVisites(Long id);
    PlanSuivi modifier(Long id, PlanSuiviUpdateRequest r);
    void arreter(Long id);
}
