package tn.esprit.medivault.soindomicile.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * Infirmier d'une pharmacie, géré par le pharmacien.
 * Il n'a pas de compte : il ne se connecte pas, il est seulement affecté aux visites.
 */
@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Infirmier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String prenom;
    private String nom;
    private String telephone;
    private String pharmacieId;       // la pharmacie où il travaille

    // Temps de disponibilité : l'infirmier ne peut être affecté qu'entre ces deux heures
    private LocalTime heureDebut;
    private LocalTime heureFin;

    /** "Sonia Ben Ali" : copié dans la visite au moment de l'affectation. */
    public String getNomComplet() {
        return prenom + " " + nom;
    }

    /** L'heure de la visite tombe-t-elle dans ses horaires ? */
    public boolean disponibleA(LocalTime heure) {
        return !heure.isBefore(heureDebut) && !heure.isAfter(heureFin);
    }
}
