package tn.esprit.medivault.dossier.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.dossier.entities.Dossier;

import java.util.Optional;

@Repository
public interface DossierRepository extends JpaRepository<Dossier, Long> {

    Optional<Dossier> findByPatientId(String patientId);

    boolean existsByPatientId(String patientId);
}
