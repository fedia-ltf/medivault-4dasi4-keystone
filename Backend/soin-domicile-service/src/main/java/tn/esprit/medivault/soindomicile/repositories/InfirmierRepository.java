package tn.esprit.medivault.soindomicile.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.medivault.soindomicile.entities.Infirmier;

import java.util.List;

public interface InfirmierRepository extends JpaRepository<Infirmier, Long> {

    List<Infirmier> findByPharmacieIdOrderByNomAscPrenomAsc(String pharmacieId);

    /** Deux infirmiers d'une même pharmacie ne portent pas le même prénom et nom. */
    boolean existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCase(String pharmacieId, String prenom, String nom);

    boolean existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCaseAndIdNot(String pharmacieId, String prenom, String nom, Long id);

    /**
     * Recherche : prénom, nom, "prénom nom" ou téléphone contenant le texte (sans tenir compte des majuscules).
     * Un texte vide renvoie tous les infirmiers ; une pharmacie vide ("") cherche dans toutes les pharmacies (admin).
     */
    @Query("""
            SELECT i FROM Infirmier i
            WHERE (:pharmacieId = '' OR i.pharmacieId = :pharmacieId)
              AND (LOWER(i.prenom) LIKE LOWER(CONCAT('%', :texte, '%'))
                OR LOWER(i.nom) LIKE LOWER(CONCAT('%', :texte, '%'))
                OR LOWER(CONCAT(i.prenom, ' ', i.nom)) LIKE LOWER(CONCAT('%', :texte, '%'))
                OR i.telephone LIKE CONCAT('%', :texte, '%'))
            ORDER BY i.nom, i.prenom""")
    List<Infirmier> rechercher(@Param("pharmacieId") String pharmacieId, @Param("texte") String texte);

    /** Nombre d'infirmiers par pharmacie (tableau de bord admin) : lignes [pharmacieId, nombre]. */
    @Query("SELECT i.pharmacieId, COUNT(i) FROM Infirmier i GROUP BY i.pharmacieId")
    List<Object[]> compterParPharmacie();
}
