package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.text.Normalizer;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom_commercial;

    @Column(nullable = false)
    private String dci;

    @Column(nullable = false)
    private String classe_therapeutique;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormeMedicament forme;

    private String dosage;

    // Statistique de consommation, mise à jour par l'événement OrdonnanceDelivree
    @ColumnDefault("0")
    @Column(nullable = false)
    private int nb_delivrances;

    // Comparaison sans tenir compte de la casse ni des accents ("Penicilline" = "Pénicilline")
    public boolean estDeLaClasse(String classe) {
        if (classe == null || classe_therapeutique == null) {
            return false;
        }
        return normaliser(classe_therapeutique).equals(normaliser(classe));
    }

    public void enregistrerDelivrance() {
        nb_delivrances++;
    }

    private static String normaliser(String texte) {
        String sansAccents = Normalizer.normalize(texte.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sansAccents.toLowerCase();
    }
}
