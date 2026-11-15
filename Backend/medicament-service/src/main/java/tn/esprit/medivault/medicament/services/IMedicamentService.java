package tn.esprit.medivault.medicament.services;

import tn.esprit.medivault.medicament.dto.MedicamentRequest;
import tn.esprit.medivault.medicament.dto.VerificationRequest;
import tn.esprit.medivault.medicament.dto.VerificationResponse;
import tn.esprit.medivault.medicament.entities.Medicament;

import java.util.List;

// INTERFACE du service : la liste de CE QUE le service sait faire (sans le "comment").
// Le controller dépend de cette interface, pas de la classe Impl.
// Avantage : couplage faible (on peut changer l'implémentation) et tests plus faciles.
public interface IMedicamentService {

    // Lire tout le catalogue
    List<Medicament> getCatalogue();

    // Ajouter un médicament à partir du DTO envoyé par l'admin
    Medicament ajouterMedicament(MedicamentRequest request);

    // RG4 + RG6 : vérifier si une liste de médicaments peut être prescrite
    VerificationResponse verifierCompatibilite(VerificationRequest request);

    // RG6 : retirer un médicament du catalogue
    void retirerMedicament(Long id, String motif);
}
