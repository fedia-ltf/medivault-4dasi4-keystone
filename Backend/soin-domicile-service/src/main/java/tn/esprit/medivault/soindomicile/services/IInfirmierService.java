package tn.esprit.medivault.soindomicile.services;

import tn.esprit.medivault.soindomicile.dto.InfirmierRequest;
import tn.esprit.medivault.soindomicile.entities.Infirmier;

import java.util.List;

public interface IInfirmierService {
    List<Infirmier> lister(String pharmacieId);
    Infirmier getById(Long id);
    Infirmier ajouter(InfirmierRequest r);
    Infirmier modifier(Long id, InfirmierRequest r);
    void supprimer(Long id);
}
