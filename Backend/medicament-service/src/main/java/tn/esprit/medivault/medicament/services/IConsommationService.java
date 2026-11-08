package tn.esprit.medivault.medicament.services;

import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;

public interface IConsommationService {

    // Incrémente nb_delivrances de chaque médicament délivré
    void enregistrerDelivrance(OrdonnanceDelivreeEvent event);
}
