package tn.esprit.medivault.rendezvous.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.medivault.rendezvous.enums.StatutRdv;
import tn.esprit.medivault.rendezvous.enums.TypeProfessionnel;

import java.time.LocalDateTime;

/**
 * Créneau horaire d'un professionnel (médecin ou laboratoire).
 * professionnel_id est l'identifiant du professionnel dans user-service (pas de clé étrangère).
 */
@Entity
@Table(name = "creneaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creneau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professionnel_id", nullable = false)
    private Long professionnelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_professionnel", nullable = false)
    private TypeProfessionnel typeProfessionnel;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @OneToOne(mappedBy = "creneau", cascade = CascadeType.ALL)
    private RendezVous rendezVous;

    /**
     * RG8 : Un créneau est libre tant qu'aucun rendez-vous CONFIRME ne lui est associé.
     */
    public boolean estLibre() {
        return rendezVous == null || rendezVous.getStatut() != StatutRdv.CONFIRME;
    }
}

