package tn.esprit.medivault.rendezvous.service;

import tn.esprit.medivault.rendezvous.dto.CreneauResponse;
import tn.esprit.medivault.rendezvous.dto.RendezVousRequest;
import tn.esprit.medivault.rendezvous.dto.RendezVousResponse;

import java.time.LocalDate;
import java.util.List;

public interface IRendezvousService {

    List<CreneauResponse> genererCreneaux(String professionnelId);

    List<CreneauResponse> getCreneauxLibres(String professionnelId, LocalDate date);

    CreneauResponse getCreneauById(Long id);

    void supprimerCreneau(Long id);

    RendezVousResponse prendreRendezVous(RendezVousRequest request);

    RendezVousResponse annulerRendezVous(Long id);

    RendezVousResponse getRendezVousById(Long id);

    List<RendezVousResponse> getAllRendezVous();

    List<RendezVousResponse> getRendezVousByPatient(String patientId);

    List<RendezVousResponse> getRendezVousByProfessionnel(String professionnelId);

    void supprimerRendezVous(Long id);

    void traiterConsultationAjoutee(String patientId, String medecinId);
}
