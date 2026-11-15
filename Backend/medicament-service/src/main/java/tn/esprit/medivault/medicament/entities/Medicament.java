package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.text.Normalizer;

// ENTITÉ n°1 de mon service : un médicament du catalogue national.
// @Entity : la classe devient la table "medicament" ; chaque objet Medicament = une ligne.
// Hibernate (JPA) crée la table tout seul au démarrage (ddl-auto=update dans application.properties).
// Lombok génère getters, setters, constructeurs et builder.
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicament {

    // Clé primaire générée par la base (auto-incrément)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nom vendu en pharmacie, ex. "Augmentin". @Column(nullable = false) = NOT NULL
    @Column(nullable = false)
    private String nom_commercial;

    // DCI = nom de la molécule, ex. "Amoxicilline"
    @Column(nullable = false)
    private String dci;

    // Famille du médicament, ex. "Pénicilline". Sert à détecter les allergies (RG4).
    @Column(nullable = false)
    private String classe_therapeutique;

    // Une valeur de l'enum FormeMedicament, stockée en texte ("COMPRIME")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormeMedicament forme;

    // ex. "500 mg" (facultatif)
    private String dosage;

    // Statistique de consommation, mise à jour par l'événement OrdonnanceDelivree (RabbitMQ).
    // @ColumnDefault("0") : vaut 0 par défaut dans la base.
    @ColumnDefault("0")
    @Column(nullable = false)
    private int nb_delivrances;

    // Logique métier : ce médicament appartient-il à la classe donnée ?
    // Comparaison sans tenir compte de la casse ni des accents ("Penicilline" = "Pénicilline")
    public boolean estDeLaClasse(String classe) {
        if (classe == null || classe_therapeutique == null) {
            return false;
        }
        return normaliser(classe_therapeutique).equals(normaliser(classe));
    }

    // Appelée quand une pharmacie délivre ce médicament : compteur + 1
    public void enregistrerDelivrance() {
        nb_delivrances++;
    }

    // Outil interne : enlève les espaces au bord, les accents, et met en minuscules
    // "  Pénicilline " -> "penicilline"
    private static String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase();
    }
}
