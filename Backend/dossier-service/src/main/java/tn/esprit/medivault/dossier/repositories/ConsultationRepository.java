package tn.esprit.medivault.dossier.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.dossier.entities.Consultation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

    List<Consultation> findByDossierIdOrderByDateDesc(Long dossierId);

    Optional<Consultation> findByIdAndDossierId(Long id, Long dossierId);

    /**
     * Requête paginée avec filtres optionnels null-safe.
     * Tri par date décroissante géré via le Pageable passé par le service.
     */
    @Query("""
            SELECT c FROM Consultation c
            WHERE c.dossier.id = :dossierId
              AND (:medecinId IS NULL OR c.medecinId = :medecinId)
              AND (:du IS NULL OR CAST(c.date AS localdate) >= :du)
              AND (:au IS NULL OR CAST(c.date AS localdate) <= :au)
            """)
    Page<Consultation> findFiltered(
            @Param("dossierId") Long dossierId,
            @Param("medecinId") String medecinId,
            @Param("du") LocalDate du,
            @Param("au") LocalDate au,
            Pageable pageable
    );
}
