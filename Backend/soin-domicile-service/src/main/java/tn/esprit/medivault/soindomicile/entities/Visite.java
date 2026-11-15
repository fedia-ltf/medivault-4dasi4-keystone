package tn.esprit.medivault.soindomicile.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Visite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;
    private String pharmacieId;
    private String pharmacienId;      // rempli quand le pharmacien accepte
    // Infirmier affecté par le pharmacien (il n'a pas de compte : on garde seulement son nom et son téléphone)
    private String infirmierNom;
    private String infirmierTelephone;
    private LocalDateTime date;
    private String adresse;
    // Position GPS envoyée par le navigateur du patient (facultative)
    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    private TypeSoin typeSoin;

    private Long ordonnanceId;        // obligatoire pour une injection ou une prise de sang

    @Enumerated(EnumType.STRING)
    private StatutVisite statut;

    // Rapport de l'infirmier, saisi par le pharmacien
    private String tension;           // ex. "13/8"
    private Double glycemie;          // en g/L
    private boolean injectionFaite;
    private String dose;              // ex. "10 UI"
    @Column(length = 1000)
    private String observations;
    private LocalDateTime dateRapport;

    // Ajoutés au diagramme
    private Long medicamentId;        // médicament injecté (vente, MedicamentRetire)
    private boolean medicamentSensible;

    // Association « génère » : null pour une demande ponctuelle
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    @JsonIgnore
    private PlanSuivi plan;

    /** RG4 : seuil au format "tension>14/9;glycemie>1.8". */
    public boolean estAnormale(String seuil) {
        if (seuil == null || seuil.isBlank()) return false;
        for (String partie : seuil.replace(" ", "").toLowerCase().split(";")) {
            if (partie.startsWith("tension>") && tension != null) {
                String[] l = partie.substring(8).split("/");
                String[] m = tension.split("/");
                if (Integer.parseInt(m[0].trim()) > Integer.parseInt(l[0])
                        || Integer.parseInt(m[1].trim()) > Integer.parseInt(l[1])) return true;
            }
            if (partie.startsWith("glycemie>") && glycemie != null
                    && glycemie > Double.parseDouble(partie.substring(9).replace(',', '.'))) return true;
        }
        return false;
    }

    /** RG5 : annulation jusqu'à 2 h avant la visite. */
    public boolean peutEtreAnnulee(LocalDateTime maintenant) {
        return statut != null && statut.estActive()
                && Duration.between(maintenant, date).toMinutes() >= 120;
    }
}
