package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.medicament.entities.Interaction;

import java.util.List;

// Accès à la table interaction. Hérite des méthodes de base (save, findAll, findById, delete...)
// + 2 requêtes personnalisées écrites en JPQL (SQL sur les classes Java, pas sur les tables).
@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // Interactions entre médicaments appartenant TOUS LES DEUX à la liste.
    // Exemple : ids = [Coumadine, Advil, Doliprane]
    //   -> renvoie l'interaction Coumadine + Advil (les 2 sont dans la liste)
    //   -> ne renvoie pas Coumadine + Aspirine (Aspirine n'est pas dans la liste)
    // :ids est un paramètre, rempli par @Param("ids").
    @Query("select i from Interaction i where i.medicament_a.id in :ids and i.medicament_b.id in :ids")
    List<Interaction> findInteractionsEntre(@Param("ids") List<Long> ids);

    // Supprime toutes les interactions où ce médicament apparaît (côté A ou côté B).
    // Utilisé avant de retirer un médicament, sinon la clé étrangère bloquerait la suppression.
    // @Modifying : obligatoire pour une requête qui modifie (DELETE/UPDATE) au lieu de lire.
    @Modifying
    @Query("delete from Interaction i where i.medicament_a.id = :id or i.medicament_b.id = :id")
    void deleteByMedicamentId(@Param("id") Long id);
}
