package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gravite gravite;

    private String description;

    @ManyToOne(optional = false)
    private Medicament medicament_a;

    @ManyToOne(optional = false)
    private Medicament medicament_b;

    public boolean estBloquante() {
        return gravite == Gravite.CONTRE_INDIQUEE;
    }

    // Retourne l'autre médicament de l'interaction
    public Medicament autreQue(Medicament medicament) {
        return medicament_a.getId().equals(medicament.getId()) ? medicament_b : medicament_a;
    }
}
