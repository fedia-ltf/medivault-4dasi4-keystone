package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.medicament.entities.Interaction;

import java.util.List;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // Interactions entre médicaments appartenant tous les deux à la liste
    @Query("select i from Interaction i where i.medicament_a.id in :ids and i.medicament_b.id in :ids")
    List<Interaction> findInteractionsEntre(@Param("ids") List<Long> ids);

    @Modifying
    @Query("delete from Interaction i where i.medicament_a.id = :id or i.medicament_b.id = :id")
    void deleteByMedicamentId(@Param("id") Long id);
}
