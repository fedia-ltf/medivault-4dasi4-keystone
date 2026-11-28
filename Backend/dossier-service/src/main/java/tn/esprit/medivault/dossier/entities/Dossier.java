package tn.esprit.medivault.dossier.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossiers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"consultations", "acces", "liens"})
@EqualsAndHashCode(exclude = {"consultations", "acces", "liens"})
public class Dossier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private GroupeSanguin groupeSanguin = GroupeSanguin.INCONNU;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dossier_allergies", joinColumns = @JoinColumn(name = "dossier_id"))
    @Column(name = "allergie")
    @Builder.Default
    private List<String> allergies = new ArrayList<>();

    @Column(length = 2000)
    private String antecedents;

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consultation> consultations = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccesDossier> acces = new ArrayList<>();

    @OneToMany(mappedBy = "dossier", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LienDossier> liens = new ArrayList<>();

    public Dossier(String patientId) {
        this.patientId = patientId;
        this.groupeSanguin = GroupeSanguin.INCONNU;
        this.allergies = new ArrayList<>();
        this.consultations = new ArrayList<>();
        this.acces = new ArrayList<>();
        this.liens = new ArrayList<>();
    }
}
