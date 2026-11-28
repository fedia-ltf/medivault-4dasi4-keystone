package tn.esprit.medivault.medicament.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.medivault.medicament.entities.MedicamentSensible;

public interface MedicamentSensibleRepository extends JpaRepository<MedicamentSensible, Long> {

    boolean existsByNomCommercialIgnoreCase(String nomCommercial);

    boolean existsByNomCommercialIgnoreCaseAndIdNot(String nomCommercial, Long id);
}




//le repository. C’est la classe qui parle à la base (lire, écrire, supprimer) sans que tu écrives de SQL.
//interface : ena juste ktebt il contrat , spring howa ili ya3mel il code fel demarrage


//À la création, on refuse un médicament dont le nom existe déjà (erreur 409).
//À la modification, on peut garder son propre nom, mais pas prendre celui d’un autre : d’où le AndIdNot.