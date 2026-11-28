package tn.esprit.medivault.analyse.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medivault.analyse.client.DossierClient;
import tn.esprit.medivault.analyse.dto.AccesResponse;
import tn.esprit.medivault.analyse.dto.AnalyseResponse;
import tn.esprit.medivault.analyse.dto.LigneResultatRequest;
import tn.esprit.medivault.analyse.dto.ResultatRequest;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.entity.ResultatAnalyse;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.event.ConsultationAjouteeEvent;
import tn.esprit.medivault.analyse.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.analyse.event.RendezVousConfirmeEvent;
import tn.esprit.medivault.analyse.event.ResultatDisponibleEvent;
import tn.esprit.medivault.analyse.exception.AccesRefuseException;
import tn.esprit.medivault.analyse.exception.AnalyseNotFoundException;
import tn.esprit.medivault.analyse.exception.DonneesInvalidesException;
import tn.esprit.medivault.analyse.exception.StatutInvalideException;
import tn.esprit.medivault.analyse.messaging.AnalyseEventPublisher;
import tn.esprit.medivault.analyse.repository.AnalyseRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyseServiceImpl implements IAnalyseService {

    /** Valeur de typeProfessionnel d'un laboratoire, identique dans user-service et rendezvous-service. */
    static final String TYPE_LABORATOIRE = "LABORANTIN";

    private final AnalyseRepository analyseRepository;
    private final DossierClient dossierClient;
    private final AnalyseEventPublisher eventPublisher;

    // ---------- API REST ----------

    @Override
    @Transactional(readOnly = true)
    public List<AnalyseResponse> rechercher(StatutAnalyse statut, String laboratoireId) {
        List<Analyse> analyses;
        if (statut != null && laboratoireId != null) {
            analyses = analyseRepository.findByStatutAndLaboratoireIdOrderByDateDemandeAsc(statut, laboratoireId);
        } else if (statut != null) {
            analyses = analyseRepository.findByStatutOrderByDateDemandeAsc(statut);
        } else if (laboratoireId != null) {
            analyses = analyseRepository.findByLaboratoireIdOrderByDateDemandeAsc(laboratoireId);
        } else {
            analyses = analyseRepository.findAll();
        }
        return analyses.stream().map(AnalyseResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyseResponse trouverParId(Long id) {
        return AnalyseResponse.from(charger(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyseResponse> analysesDuPatient(String patientId) {
        return analyseRepository.findByPatientIdOrderByDateDemandeDesc(patientId).stream()
                .map(AnalyseResponse::from).toList();
    }

    /**
     * Saisie du résultat par le laboratoire, dans cet ordre :
     * RG7 (EN_COURS, sinon 409) → RG10 (laboratoire affecté, sinon 403) →
     * appel synchrone à dossier-service : justification d'accès toujours ACTIVE (sinon 403 ; dossier indisponible → 503)
     * → résultats enregistrés avec leur interprétation, analyse TERMINEE, publication de ResultatDisponible.
     */
    @Override
    @Transactional
    public AnalyseResponse saisirResultat(Long analyseId, ResultatRequest request) {
        Analyse analyse = charger(analyseId);

        if (analyse.getStatut() != StatutAnalyse.EN_COURS) {
            throw new StatutInvalideException("RG7 : saisie impossible, l'analyse " + analyseId
                    + " est " + analyse.getStatut() + " (elle doit être EN_COURS)");
        }
        if (!request.laboratoireId().equals(analyse.getLaboratoireId())) {
            throw new AccesRefuseException("RG10 : seul le laboratoire affecté par le rendez-vous ("
                    + analyse.getLaboratoireId() + ") peut saisir ce résultat");
        }

        AccesResponse acces = dossierClient.verifierAcces(analyse.getPatientId(), request.laboratoireId());
        if (acces == null || !acces.autorise()) {
            throw new AccesRefuseException("La justification d'accès du laboratoire au dossier du patient"
                    + " n'est plus active (retirée ou expirée)");
        }

        for (LigneResultatRequest ligne : request.resultats()) {
            if (ligne.referenceMin() > ligne.referenceMax()) {
                throw new DonneesInvalidesException("Référence invalide pour " + ligne.parametre()
                        + " : la borne basse dépasse la borne haute");
            }
            analyse.ajouterResultat(ResultatAnalyse.builder()
                    .parametre(ligne.parametre())
                    .valeur(ligne.valeur())
                    .unite(ligne.unite())
                    .valeurReference(format(ligne.referenceMin()) + "–" + format(ligne.referenceMax()))
                    .interpretation(InterpretationCalculator.interpreter(ligne.valeur(),
                            ligne.referenceMin(), ligne.referenceMax()))
                    .build());
        }
        analyse.setStatut(StatutAnalyse.TERMINEE);
        analyse.setDateResultat(LocalDateTime.now());
        Analyse terminee = analyseRepository.save(analyse);

        eventPublisher.publierResultatDisponible(new ResultatDisponibleEvent(terminee.getId(),
                terminee.getPatientId(), terminee.getMedecinId(), terminee.getLaboratoireId(),
                terminee.getTypeAnalyse(), terminee.getDateResultat()));
        return AnalyseResponse.from(terminee);
    }

    // ---------- Événements RabbitMQ ----------

    /** ConsultationAjoutee : une analyse EN_ATTENTE par analyse demandée (sans doublon si le message est rejoué). */
    @Override
    @Transactional
    public void creerDepuisConsultation(ConsultationAjouteeEvent event) {
        if (event.analysesDemandees() == null || event.analysesDemandees().isEmpty()) {
            return;
        }
        for (String type : event.analysesDemandees()) {
            if (type == null || type.isBlank()
                    || analyseRepository.existsByConsultationIdAndTypeAnalyse(event.consultationId(), type)) {
                continue;
            }
            Analyse analyse = analyseRepository.save(Analyse.builder()
                    .consultationId(event.consultationId())
                    .patientId(event.patientId())
                    .medecinId(event.medecinId())
                    .typeAnalyse(type)
                    .statut(StatutAnalyse.EN_ATTENTE)
                    .dateDemande(event.date() != null ? event.date() : LocalDateTime.now())
                    .build());
            log.info("Analyse {} ({}) créée EN_ATTENTE pour le patient {}", analyse.getId(), type, event.patientId());
        }
    }

    /** RendezVousConfirme (laboratoire) : l'analyse réservée passe EN_COURS, affectée au laboratoire. */
    @Override
    @Transactional
    public void affecterLaboratoire(RendezVousConfirmeEvent event) {
        if (!estRendezVousLaboratoire(event.typeProfessionnel(), event.analyseId())) {
            return;
        }
        analyseRepository.findById(event.analyseId()).ifPresentOrElse(analyse -> {
            if (analyse.getStatut() != StatutAnalyse.EN_ATTENTE || !analyse.getPatientId().equals(event.patientId())) {
                log.warn("RendezVousConfirme {} ignoré : analyse {} {} ou d'un autre patient",
                        event.rendezVousId(), analyse.getId(), analyse.getStatut());
                return;
            }
            analyse.setStatut(StatutAnalyse.EN_COURS);
            analyse.setLaboratoireId(event.professionnelId());
            log.info("Analyse {} EN_COURS, affectée au laboratoire {}", analyse.getId(), event.professionnelId());
        }, () -> log.warn("RendezVousConfirme {} ignoré : analyse {} inconnue", event.rendezVousId(), event.analyseId()));
    }

    /** RendezVousAnnule (laboratoire) : l'analyse repasse EN_ATTENTE et le laboratoire est retiré. */
    @Override
    @Transactional
    public void annulerAffectation(RendezVousAnnuleEvent event) {
        if (!estRendezVousLaboratoire(event.typeProfessionnel(), event.analyseId())) {
            return;
        }
        analyseRepository.findById(event.analyseId()).ifPresent(analyse -> {
            if (analyse.getStatut() != StatutAnalyse.EN_COURS
                    || !event.professionnelId().equals(analyse.getLaboratoireId())) {
                log.warn("RendezVousAnnule {} ignoré : analyse {} {}", event.rendezVousId(),
                        analyse.getId(), analyse.getStatut());
                return;
            }
            analyse.setStatut(StatutAnalyse.EN_ATTENTE);
            analyse.setLaboratoireId(null);
            log.info("Analyse {} repassée EN_ATTENTE (rendez-vous {} annulé)", analyse.getId(), event.rendezVousId());
        });
    }

    private boolean estRendezVousLaboratoire(String typeProfessionnel, Long analyseId) {
        return TYPE_LABORATOIRE.equals(typeProfessionnel) && analyseId != null;
    }

    private Analyse charger(Long id) {
        return analyseRepository.findById(id).orElseThrow(() -> new AnalyseNotFoundException(id));
    }

    private static String format(double valeur) {
        return String.format(Locale.FRANCE, "%.2f", valeur);
    }
}
