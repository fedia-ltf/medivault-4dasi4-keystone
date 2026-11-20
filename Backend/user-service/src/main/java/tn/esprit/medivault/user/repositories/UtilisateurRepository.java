package tn.esprit.medivault.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.Utilisateur;

import java.util.List;

public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {

    boolean existsByEmailIgnoreCase(String email);

    List<Utilisateur> findByRole(Role role);
}
