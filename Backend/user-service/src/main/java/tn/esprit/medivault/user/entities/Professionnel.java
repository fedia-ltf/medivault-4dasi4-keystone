package tn.esprit.medivault.user.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Médecin, pharmacien (compte de la pharmacie) ou laborantin (compte du laboratoire).
 * MongoDB : les champs propres au professionnel n'existent que sur ces documents.
 */
@Document(collection = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Professionnel extends Utilisateur {

    private TypeProfessionnel type;

    private String nomEtablissement;

    private String numeroAgrement;

    private String adresse;

    private String ville;

    /** Horaires utilisés par rendezvous-service pour générer les créneaux. */
    private List<DayOfWeek> joursTravail;

    private LocalTime heureDebut;

    private LocalTime heureFin;

    /**
     * Signature du médecin (image encodée en data URL base64), reprise par ordonnance-service
     * sur l'ordonnance. Uniquement pour le type MEDECIN.
     */
    private String signatureImage;
}
