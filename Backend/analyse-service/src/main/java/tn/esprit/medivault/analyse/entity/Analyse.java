package tn.esprit.medivault.analyse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyse demandée par un médecin pendant une consultation.
 * patientId, medecinId et laboratoireId sont les id (String) de user-service : pas de clé étrangère entre bases.
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

    /** Consultation d'origine (événement ConsultationAjoutee) : évite de créer deux fois la même analyse. */
    private Long consultationId;

    private String patientId;

    private String medecinId;

    /** Renseigné à la réception de RendezVousConfirme (rendez-vous au laboratoire) — RG10. */
    private String laboratoireId;

    private String typeAnalyse;

    @Enumerated(EnumType.STRING)
    private StatutAnalyse statut;

    private LocalDateTime dateDemande;

    private LocalDateTime dateResultat;

    /** Composition : un résultat n'existe pas sans son analyse (0..* : aucun tant qu'elle n'est pas TERMINEE). */
    @OneToMany(mappedBy = "analyse", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ResultatAnalyse> resultats = new ArrayList<>();

    public void ajouterResultat(ResultatAnalyse resultat) {
        resultat.setAnalyse(this);
        resultats.add(resultat);
    }
}
