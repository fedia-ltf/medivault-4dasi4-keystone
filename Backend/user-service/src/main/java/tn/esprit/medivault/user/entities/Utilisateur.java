package tn.esprit.medivault.user.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Compte MediVault. Patients et administrateurs sont des Utilisateur ;
 * médecins, pharmaciens et laborantins sont des Professionnel (même collection, champ _class).
 * L'id (String) est l'identifiant utilisé par tous les autres microservices (patientId, medecinId…).
 */
@Document(collection = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Utilisateur {

    @Id
    private String id;

    private String keycloakId;

    private String nom;

    private String prenom;

    @Indexed(unique = true)
    private String email;

    private String telephone;

    private Role role;

    @lombok.Builder.Default
    private boolean actif = true;

    @CreatedDate
    private Instant dateCreation;
}
