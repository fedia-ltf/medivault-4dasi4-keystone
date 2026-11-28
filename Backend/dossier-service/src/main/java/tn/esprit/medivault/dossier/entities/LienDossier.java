package tn.esprit.medivault.dossier.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "liens_dossier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dossier_id", "type", "reference_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "dossier")
@EqualsAndHashCode(exclude = "dossier")
public class LienDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeLien type;

    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    private LocalDateTime date;

    private String auteurId;

    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
