package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

// ENTITÉ n°2 de mon service.
// Une Interaction = "le médicament A et le médicament B sont dangereux ensemble".
// C'est une classe d'association : elle relie 2 médicaments ET elle a ses propres
// attributs (gravite, description). C'est pour ça que c'est une entité à part,
// et pas un simple attribut de Medicament.
//
// @Entity : cette classe devient une table "interaction" dans la base H2.
// Lombok (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder) :
//   génère automatiquement les getters/setters, les constructeurs et le "builder"
//   (Interaction.builder().gravite(...).build()) -> on n'écrit pas ce code à la main.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Interaction {

    // @Id : clé primaire. @GeneratedValue(IDENTITY) : la base donne l'id automatiquement (1, 2, 3...).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Enumerated(STRING) : on stocke le texte "GRAVE" dans la base (et pas un numéro 0,1,2).
    // nullable = false : colonne obligatoire (NOT NULL).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gravite gravite;

    // Explication du risque, ex. "risque hémorragique"
    private String description;

    // @ManyToOne : plusieurs interactions peuvent concerner le même médicament.
    // Dans la table, ça crée une colonne clé étrangère medicament_a_id.
    // optional = false : une interaction a toujours ses 2 médicaments.
    @ManyToOne(optional = false)
    private Medicament medicament_a;

    @ManyToOne(optional = false)
    private Medicament medicament_b;

    // Logique métier dans l'entité : seule une interaction CONTRE_INDIQUEE bloque (RG4)
    public boolean estBloquante() {
        return gravite == Gravite.CONTRE_INDIQUEE;
    }

    // Retourne l'autre médicament de l'interaction
    // (si on donne A, on reçoit B ; si on donne B, on reçoit A)
    public Medicament autreQue(Medicament medicament) {
        return medicament_a.getId().equals(medicament.getId()) ? medicament_b : medicament_a;
    }
}
