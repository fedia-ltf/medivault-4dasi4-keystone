package tn.esprit.medivault.user.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.Utilisateur;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends MongoRepository<Utilisateur, String> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilisateur> findByEmailIgnoreCase(String email);

    List<Utilisateur> findByRole(Role role);
}
