package tn.esprit.medivault.analyse.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyse demandée par un médecin pendant une consultation.
 * patient_id, medecin_id et laboratoire_id sont des identifiants d'autres services (pas de clé étrangère).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analyse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long patientId;

    private Long medecinId;

    /** Renseigné à la réception de RendezVousConfirme (rendez-vous au laboratoire) — RG10. */
    private Long laboratoireId;

    private String typeAnalyse;

    @Enumerated(EnumType.STRING)
    private StatutAnalyse statut;

    /** Composition : un résultat n'existe pas sans son analyse. */
    @OneToMany(mappedBy = "analyse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResultatAnalyse> resultats = new ArrayList<>();
}
