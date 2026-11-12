package tn.esprit.medivault.rendezvous.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;

/**
 * Rendez-vous pris par un patient sur un créneau donné.
 * patient_id est l'identifiant du patient dans user-service (pas de clé étrangère).
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
    private Long patientId;

    @Column(nullable = false)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRdv statut;

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

