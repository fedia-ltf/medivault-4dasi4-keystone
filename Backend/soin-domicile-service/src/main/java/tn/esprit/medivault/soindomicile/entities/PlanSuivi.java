package tn.esprit.medivault.soindomicile.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PlanSuivi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String patientId;
    private String medecinId;

    @Enumerated(EnumType.STRING)
    private TypeSoin typeSoin;

    @Enumerated(EnumType.STRING)
    private Frequence frequence;

    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String seuilAlerte;       // ex. "tension>14/9;glycemie>1.8"

    // Ajoutés au diagramme : recopiés dans chaque visite générée
    private String pharmacieId;
    private String adresse;
    private LocalTime heureVisite;

    // 1 plan génère 0..* visites
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    @JsonIgnore
    @Builder.Default
    private List<Visite> visites = new ArrayList<>();

    /** RG1 : génère une visite par date, de dateDebut à dateFin. */
    public List<Visite> genererVisites(LocalDateTime aPartirDe) {
        List<Visite> liste = new ArrayList<>();
        for (LocalDate jour = dateDebut; !jour.isAfter(dateFin); jour = frequence.suivante(jour)) {
            LocalDateTime date = jour.atTime(heureVisite);
            if (date.isAfter(aPartirDe)) {
                liste.add(Visite.builder()
                        .patientId(patientId).pharmacieId(pharmacieId).adresse(adresse)
                        .typeSoin(typeSoin).date(date)
                        .statut(StatutVisite.DEMANDEE).plan(this)
                        .build());
            }
        }
        return liste;
    }

    /** Un plan est en cours tant que sa date de fin n'est pas passée. */
    public boolean estEnCours() {
        return !dateFin.isBefore(LocalDate.now());
    }
}
