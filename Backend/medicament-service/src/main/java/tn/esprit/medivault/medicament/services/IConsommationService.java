package tn.esprit.medivault.medicament.services;

import tn.esprit.medivault.medicament.dto.OrdonnanceDelivreeEvent;

// Service séparé pour la partie "statistiques de consommation"
// (appelé par le listener RabbitMQ, pas par le controller).
public interface IConsommationService {

    // Incrémente nb_delivrances de chaque médicament délivré
    void enregistrerDelivrance(OrdonnanceDelivreeEvent event);
}
