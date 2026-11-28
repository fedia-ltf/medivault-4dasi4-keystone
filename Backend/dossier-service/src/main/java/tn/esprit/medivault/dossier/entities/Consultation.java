package tn.esprit.medivault.dossier.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "dossier")
@EqualsAndHashCode(exclude = "dossier")
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medecinId;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(nullable = false)
    private String motif;

    private String diagnostic;

    @Column(length = 2000)
    private String observations;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "consultation_analyses", joinColumns = @JoinColumn(name = "consultation_id"))
    @Column(name = "analyse")
    @Builder.Default
    private List<String> analysesDemandees = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dossier_id")
    private Dossier dossier;
}
