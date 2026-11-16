package tn.esprit.medivault.dossier.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.dossier.entities.Consultation;

import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByDossierIdOrderByDateDesc(Long dossierId);
}
