package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.YearMonth;

// Le quota qu'une pharmacie reçoit pour UN médicament et UN mois (ex. 50 Subutex en novembre 2026).
// C'est l'objet central : les ventes le consomment, les avertissements s'y accumulent (RG3).
// Une pharmacie ne peut avoir qu'un seul quota par médicament et par mois (contrainte d'unicité).
@Entity
@Table(name = "attribution",
        uniqueConstraints = @UniqueConstraint(name = "uk_attribution_pharmacie_medicament_mois",
                columnNames = {"pharmacie_id", "medicament_id", "mois"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identifiant du compte du pharmacien titulaire (user-service) : c'est un texte (String)
    @Column(name = "pharmacie_id", nullable = false)
    private String pharmacieId;

    // Un médicament est attribué à 0..* pharmacies
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medicament_id", nullable = false)
    private MedicamentSensible medicament;

    @Convert(converter = YearMonthConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth mois;

    @Column(nullable = false)
    private int quantiteAttribuee;

    @Column(nullable = false)
    private int quantiteVendue;

    @Column(nullable = false)
    private int avertissements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatutAttribution statut = StatutAttribution.ACTIVE;

    // Protection contre deux modifications simultanées du même quota
    @Version
    private Long version;

    // reste = attribué - vendu (la documentation)
    public int reste() {
        return quantiteAttribuee - quantiteVendue;
    }

    public boolean estSuspendue() {
        return statut == StatutAttribution.SUSPENDUE;
    }

    // RG3 : chaque faute = 1 avertissement ; au 2e, la pharmacie est SUSPENDUE pour ce médicament
    public void ajouterAvertissement() {
        avertissements++;
        if (avertissements >= 2) {
            statut = StatutAttribution.SUSPENDUE;
        }
    }

    public void ajouterVente(int quantite) {
        quantiteVendue += quantite;
    }
}