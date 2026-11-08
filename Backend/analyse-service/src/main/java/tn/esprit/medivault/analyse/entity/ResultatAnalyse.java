package tn.esprit.medivault.analyse.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import tn.esprit.medivault.analyse.enums.Interpretation;

/** Un paramètre mesuré par le laboratoire (ex. glycémie : 1,30 g/L). */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultatAnalyse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String parametre;

    private double valeur;

    private String unite;

    private String valeurReference;

    @Enumerated(EnumType.STRING)
    private Interpretation interpretation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "analyse_id")
    @JsonIgnore
    private Analyse analyse;
}
