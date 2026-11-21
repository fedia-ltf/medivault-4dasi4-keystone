package tn.esprit.medivault.medicament.repositories;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.medivault.medicament.entities.Attribution;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

// JpaSpecificationExecutor : permet de filtrer avec des critères facultatifs (pharmacie, médicament, mois, statut).
public interface AttributionRepository extends JpaRepository<Attribution, Long>, JpaSpecificationExecutor<Attribution> {

    boolean existsByPharmacieIdAndMedicamentIdAndMois(String pharmacieId, Long medicamentId, YearMonth mois);

    boolean existsByMedicamentId(Long medicamentId);

    List<Attribution> findByMois(YearMonth mois);

    List<Attribution> findByMedicamentIdAndMois(Long medicamentId, YearMonth mois);

    // Verrou d'écriture : pendant qu'une vente est enregistrée, aucune autre vente ne peut lire ni modifier
    // ce quota. Cela évite de vendre plus que le quota si deux ventes arrivent en même temps (RG2).
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Attribution a where a.pharmacieId = :pharmacieId and a.medicament.id = :medicamentId and a.mois = :mois")
    Optional<Attribution> trouverPourVente(@Param("pharmacieId") String pharmacieId,
                                           @Param("medicamentId") Long medicamentId,
                                           @Param("mois") YearMonth mois);
}