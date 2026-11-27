package tn.esprit.medivault.user.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Lien « mot de passe oublié » envoyé par e-mail (ajouté : envoi par l'API d'un service d'e-mail).
 * On ne garde jamais le jeton lui-même, seulement son empreinte SHA-256 : si la base fuit, les liens ne sont pas utilisables.
 * MongoDB supprime tout seul le document à la date d'expiration (index TTL).
 */
@Document(collection = "jetons_reinitialisation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JetonReinitialisation {

    @Id
    private String id;

    @Indexed(unique = true)
    private String empreinte;

    @Indexed
    private String utilisateurId;

    @Indexed(direction = IndexDirection.ASCENDING, expireAfterSeconds = 0)
    private Instant expiration;
}
