package tn.esprit.medivault.analyse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.analyse.client.DossierClient;
import tn.esprit.medivault.analyse.dto.AccesResponse;
import tn.esprit.medivault.analyse.dto.AnalyseResponse;
import tn.esprit.medivault.analyse.dto.LigneResultatRequest;
import tn.esprit.medivault.analyse.dto.ResultatRequest;
import tn.esprit.medivault.analyse.entity.Analyse;
import tn.esprit.medivault.analyse.enums.Interpretation;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.event.ConsultationAjouteeEvent;
import tn.esprit.medivault.analyse.event.RendezVousAnnuleEvent;
import tn.esprit.medivault.analyse.event.RendezVousConfirmeEvent;
import tn.esprit.medivault.analyse.event.ResultatDisponibleEvent;
import tn.esprit.medivault.analyse.exception.AccesRefuseException;
import tn.esprit.medivault.analyse.exception.StatutInvalideException;
import tn.esprit.medivault.analyse.messaging.AnalyseEventPublisher;
import tn.esprit.medivault.analyse.repository.AnalyseRepository;
import tn.esprit.medivault.analyse.service.AnalyseServiceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyseServiceImplTest {

    @Mock
    private AnalyseRepository repository;
    @Mock
    private DossierClient dossierClient;
    @Mock
    private AnalyseEventPublisher publisher;
    @InjectMocks
    private AnalyseServiceImpl service;

    private Analyse analyse(StatutAnalyse statut, String laboratoireId) {
        return Analyse.builder().id(1L).consultationId(45L).patientId("patient-ahmed").medecinId("medecin-karim")
                .laboratoireId(laboratoireId).typeAnalyse("Bilan glycémique").statut(statut)
                .dateDemande(LocalDateTime.now()).build();
    }

    private ResultatRequest glycemie(String laboratoireId, double valeur) {
        return new ResultatRequest(laboratoireId,
                List.of(new LigneResultatRequest("Glycémie", valeur, "g/L", 0.70, 1.10)));
    }

    @Test
    void saisie_accesActif_terminee_interpretationEleve_publieResultatDisponible() {
        when(repository.findById(1L)).thenReturn(Optional.of(analyse(StatutAnalyse.EN_COURS, "labo-biolab")));
        when(dossierClient.verifierAcces("patient-ahmed", "labo-biolab"))
                .thenReturn(new AccesResponse("patient-ahmed", "labo-biolab", true));
        when(repository.save(any(Analyse.class))).thenAnswer(inv -> inv.getArgument(0));

        AnalyseResponse reponse = service.saisirResultat(1L, glycemie("labo-biolab", 1.30));

        assertThat(reponse.statut()).isEqualTo(StatutAnalyse.TERMINEE);
        assertThat(reponse.resultats()).singleElement()
                .satisfies(r -> {
                    assertThat(r.interpretation()).isEqualTo(Interpretation.ELEVE);
                    assertThat(r.valeurReference()).isEqualTo("0,70–1,10");
                });
        ArgumentCaptor<ResultatDisponibleEvent> event = ArgumentCaptor.forClass(ResultatDisponibleEvent.class);
        verify(publisher).publierResultatDisponible(event.capture());
        assertThat(event.getValue().medecinId()).isEqualTo("medecin-karim");
    }

    @Test
    void saisie_analyseEnAttente_rg7_409_sansAppelDossier() {
        when(repository.findById(1L)).thenReturn(Optional.of(analyse(StatutAnalyse.EN_ATTENTE, null)));

        assertThatThrownBy(() -> service.saisirResultat(1L, glycemie("labo-biolab", 1.0)))
                .isInstanceOf(StatutInvalideException.class);
        verify(dossierClient, never()).verifierAcces(anyString(), anyString());
    }

    @Test
    void saisie_autreLaboratoire_rg10_403() {
        when(repository.findById(1L)).thenReturn(Optional.of(analyse(StatutAnalyse.EN_COURS, "labo-biolab")));

        assertThatThrownBy(() -> service.saisirResultat(1L, glycemie("labo-autre", 1.0)))
                .isInstanceOf(AccesRefuseException.class).hasMessageContaining("RG10");
        verify(dossierClient, never()).verifierAcces(anyString(), anyString());
    }

    @Test
    void saisie_accesRetire_403_rienEnregistre() {
        when(repository.findById(1L)).thenReturn(Optional.of(analyse(StatutAnalyse.EN_COURS, "labo-biolab")));
        when(dossierClient.verifierAcces("patient-ahmed", "labo-biolab"))
                .thenReturn(new AccesResponse("patient-ahmed", "labo-biolab", false));

        assertThatThrownBy(() -> service.saisirResultat(1L, glycemie("labo-biolab", 1.0)))
                .isInstanceOf(AccesRefuseException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publierResultatDisponible(any());
    }

    @Test
    void saisie_dossierIndisponible_503() {
        when(repository.findById(1L)).thenReturn(Optional.of(analyse(StatutAnalyse.EN_COURS, "labo-biolab")));
        when(dossierClient.verifierAcces("patient-ahmed", "labo-biolab"))
                .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "dossier-service indisponible"));

        assertThatThrownBy(() -> service.saisirResultat(1L, glycemie("labo-biolab", 1.0)))
                .isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void consultationAjoutee_uneAnalyseEnAttenteParAnalyseDemandee() {
        when(repository.existsByConsultationIdAndTypeAnalyse(any(), anyString())).thenReturn(false);
        when(repository.save(any(Analyse.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creerDepuisConsultation(new ConsultationAjouteeEvent(45L, "patient-ahmed", "medecin-karim",
                List.of("Bilan glycémique", "NFS"), LocalDateTime.now()));

        ArgumentCaptor<Analyse> sauvees = ArgumentCaptor.forClass(Analyse.class);
        verify(repository, times(2)).save(sauvees.capture());
        assertThat(sauvees.getAllValues()).allMatch(a -> a.getStatut() == StatutAnalyse.EN_ATTENTE);
    }

    @Test
    void rendezVousConfirme_laborantin_passeEnCoursAvecLaboratoire() {
        Analyse enAttente = analyse(StatutAnalyse.EN_ATTENTE, null);
        when(repository.findById(1L)).thenReturn(Optional.of(enAttente));

        service.affecterLaboratoire(new RendezVousConfirmeEvent(7L, "patient-ahmed", "labo-biolab",
                "LABORANTIN", 1L, LocalDateTime.now()));

        assertThat(enAttente.getStatut()).isEqualTo(StatutAnalyse.EN_COURS);
        assertThat(enAttente.getLaboratoireId()).isEqualTo("labo-biolab");
    }

    @Test
    void rendezVousConfirme_medecin_ignore() {
        service.affecterLaboratoire(new RendezVousConfirmeEvent(8L, "patient-ahmed", "medecin-karim",
                "MEDECIN", null, LocalDateTime.now()));

        verify(repository, never()).findById(any());
    }

    @Test
    void rendezVousAnnule_repasseEnAttente() {
        Analyse enCours = analyse(StatutAnalyse.EN_COURS, "labo-biolab");
        when(repository.findById(1L)).thenReturn(Optional.of(enCours));

        service.annulerAffectation(new RendezVousAnnuleEvent(7L, "patient-ahmed", "labo-biolab", "LABORANTIN", 1L));

        assertThat(enCours.getStatut()).isEqualTo(StatutAnalyse.EN_ATTENTE);
        assertThat(enCours.getLaboratoireId()).isNull();
    }
}
