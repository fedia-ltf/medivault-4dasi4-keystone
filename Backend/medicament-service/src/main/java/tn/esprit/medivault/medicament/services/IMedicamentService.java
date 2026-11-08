package tn.esprit.medivault.medicament.services;

import tn.esprit.medivault.medicament.dto.MedicamentRequest;
import tn.esprit.medivault.medicament.dto.VerificationRequest;
import tn.esprit.medivault.medicament.dto.VerificationResponse;
import tn.esprit.medivault.medicament.entities.Medicament;

import java.util.List;

public interface IMedicamentService {

    List<Medicament> getCatalogue();

    Medicament ajouterMedicament(MedicamentRequest request);

    VerificationResponse verifierCompatibilite(VerificationRequest request);

    void retirerMedicament(Long id, String motif);
}
