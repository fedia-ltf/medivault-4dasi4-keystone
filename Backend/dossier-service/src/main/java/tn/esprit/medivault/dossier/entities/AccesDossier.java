package tn.esprit.medivault.dossier.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acces_dossier", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"dossier_id", "professionnel_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "dossier")
@EqualsAndHashCode(exclude = "dossier")
public class AccesDossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "professionnel_id", nullable = false)
    private String professionnelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutAcces statut;

    @Column(nullable = false)
    private LocalDateTime dateAccord;

    private LocalDateTime dateRetrait;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
