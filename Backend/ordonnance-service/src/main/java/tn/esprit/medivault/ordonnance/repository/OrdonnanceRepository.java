package tn.esprit.medivault.ordonnance.repository;

import tn.esprit.medivault.ordonnance.entity.Ordonnance;
import tn.esprit.medivault.ordonnance.entity.StatutOrdonnance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdonnanceRepository extends JpaRepository<Ordonnance, Long> {
    Optional<Ordonnance> findByCode(String code);
    boolean existsByCode(String code);
    List<Ordonnance> findByPatientId(Long patientId);

    @Query("SELECT DISTINCT o FROM Ordonnance o JOIN o.lignes l WHERE l.medicamentId = :medicamentId AND o.statut = :statut")
    List<Ordonnance> findByLignesMedicamentIdAndStatut(@Param("medicamentId") Long medicamentId, @Param("statut") StatutOrdonnance statut);
}