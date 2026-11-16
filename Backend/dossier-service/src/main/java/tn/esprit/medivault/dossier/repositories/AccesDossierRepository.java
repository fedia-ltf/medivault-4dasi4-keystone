package tn.esprit.medivault.dossier.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.dossier.entities.AccesDossier;
import tn.esprit.medivault.dossier.entities.StatutAcces;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccesDossierRepository extends JpaRepository<AccesDossier, Long> {

    Optional<AccesDossier> findByDossierIdAndProfessionnelId(Long dossierId, String professionnelId);

    Optional<AccesDossier> findByDossierPatientIdAndProfessionnelId(String patientId, String professionnelId);

    boolean existsByDossierIdAndProfessionnelIdAndStatut(Long dossierId, String professionnelId, StatutAcces statut);

    List<AccesDossier> findByDossierIdAndStatut(Long dossierId, StatutAcces statut);
}
