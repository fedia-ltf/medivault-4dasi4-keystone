package tn.esprit.medivault.dossier.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.dossier.entities.LienDossier;
import tn.esprit.medivault.dossier.entities.TypeLien;

import java.util.List;

@Repository
public interface LienDossierRepository extends JpaRepository<LienDossier, Long> {

    boolean existsByDossierIdAndTypeAndReferenceId(Long dossierId, TypeLien type, String referenceId);

    List<LienDossier> findByDossierIdOrderByDateDesc(Long dossierId);
}
