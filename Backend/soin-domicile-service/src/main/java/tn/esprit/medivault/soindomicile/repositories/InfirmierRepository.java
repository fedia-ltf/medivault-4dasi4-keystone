package tn.esprit.medivault.soindomicile.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.medivault.soindomicile.entities.Infirmier;

import java.util.List;

public interface InfirmierRepository extends JpaRepository<Infirmier, Long> {

    List<Infirmier> findByPharmacieIdOrderByNomAscPrenomAsc(String pharmacieId);

    /** Deux infirmiers d'une même pharmacie ne portent pas le même prénom et nom. */
    boolean existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCase(String pharmacieId, String prenom, String nom);

    boolean existsByPharmacieIdAndPrenomIgnoreCaseAndNomIgnoreCaseAndIdNot(String pharmacieId, String prenom, String nom, Long id);
}
