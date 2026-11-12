package tn.esprit.medivault.rendezvous.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.rendezvous.entity.RendezVous;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;

import java.util.List;
import java.util.Optional;

@Repository
public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    List<RendezVous> findByPatientId(Long patientId);

    Optional<RendezVous> findTopByPatientIdAndCreneauProfessionnelIdAndStatutOrderByCreneauDateDebutDesc(
            Long patientId, Long professionnelId, StatutRdv statut);
}

