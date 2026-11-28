package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.medivault.medicament.entities.VenteSensible;

public interface VenteSensibleRepository extends JpaRepository<VenteSensible, Long>, JpaSpecificationExecutor<VenteSensible> {

    // Un quota qui a déjà des ventes ne peut plus être supprimé
    boolean existsByAttributionId(Long attributionId);

    // Combien de boîtes de ce médicament ont déjà été vendues avec cette ordonnance ?
    // (pour ne jamais dépasser la quantité prescrite)
    @Query("select coalesce(sum(v.quantite), 0) from VenteSensible v "
            + "where v.ordonnanceId = :ordonnanceId and v.attribution.medicament.id = :medicamentId")
    Long quantiteDejaVendue(@Param("ordonnanceId") Long ordonnanceId, @Param("medicamentId") Long medicamentId);
}