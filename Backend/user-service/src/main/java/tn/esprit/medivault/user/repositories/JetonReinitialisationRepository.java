package tn.esprit.medivault.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.medivault.user.entities.JetonReinitialisation;

import java.util.Optional;

public interface JetonReinitialisationRepository extends MongoRepository<JetonReinitialisation, String> {

    Optional<JetonReinitialisation> findByEmpreinte(String empreinte);

    void deleteByUtilisateurId(String utilisateurId);
}
