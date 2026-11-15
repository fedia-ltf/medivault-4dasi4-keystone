package tn.esprit.medivault.soindomicile.services;

import tn.esprit.medivault.soindomicile.dto.*;
import tn.esprit.medivault.soindomicile.entities.StatutVisite;
import tn.esprit.medivault.soindomicile.entities.Visite;
import java.util.List;

public interface IVisiteService {
    Visite demander(VisiteRequest r);
    List<Visite> lister(String patientId, String pharmacieId, StatutVisite statut);
    Visite getById(Long id);
    Visite modifier(Long id, VisiteUpdateRequest r);
    void supprimer(Long id);
    Visite accepter(Long id, AcceptationRequest r);
    Visite saisirRapport(Long id, RapportRequest r);
    Visite annuler(Long id);
    int annulerInjections(Long medicamentId, String motif);
}
