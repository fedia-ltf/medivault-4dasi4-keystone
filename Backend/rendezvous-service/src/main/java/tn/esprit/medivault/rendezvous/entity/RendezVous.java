package tn.esprit.medivault.rendezvous.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;

/**
 * Rendez-vous pris par un patient sur un créneau donné.
 * patientId est l'identifiant String MongoDB de user-service (pas de clé étrangère).
 * analyseId est l'identifiant Long de l'analyse prescrite (renseigné seulement pour un rendez-vous au laboratoire).
 */
@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRdv statut;

    /**
     * Renseigné seulement si rendez-vous au laboratoire (pour réaliser une analyse prescrite).
     */
    @Column(name = "analyse_id")
    private Long analyseId;

    @OneToOne
    @JoinColumn(name = "creneau_id", nullable = false, unique = true)
    private Creneau creneau;

    public void annuler() {
        this.statut = StatutRdv.ANNULE;
    }

    public void terminer() {
        this.statut = StatutRdv.TERMINE;
    }
}
