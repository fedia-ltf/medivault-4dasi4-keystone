package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

// Une tentative de vente REFUSÉE : qui (pharmacie), quoi (médicament, ordonnance, quantité), quand et pourquoi.
// Comme une vente, elle n'est jamais modifiée ni supprimée : c'est la trace qu'un inspecteur peut consulter.
// Le nom du médicament est copié (snapshot) : l'historique reste lisible même si le médicament est renommé,
// et aucune clé étrangère ne bloque la suppression d'un médicament.
@Entity
@Table(name = "tentative_refusee",
        indexes = @Index(name = "idx_tentative_pharmacie_date", columnList = "pharmacie_id, date"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TentativeRefusee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pharmacie_id", nullable = false)
    private String pharmacieId;

    @Column(nullable = false)
    private Long medicamentId;

    @Column(nullable = false)
    private String medicamentNom;

    // Le code tel que la pharmacie l'a saisi (peut être faux : c'est justement un motif de refus)
    private String ordonnanceCode;

    @Column(nullable = false)
    private int quantite;

    @Column(nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotifRefus motif;

    @Column(length = 500)
    private String detail;
}
