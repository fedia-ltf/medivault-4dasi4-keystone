package tn.esprit.medivault.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.medivault.user.clients.KeycloakAdminClient;
import tn.esprit.medivault.user.dto.UtilisateurCreeEvent;
import tn.esprit.medivault.user.dto.UtilisateurRequest;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.entities.Utilisateur;
import tn.esprit.medivault.user.exceptions.DonneesInvalidesException;
import tn.esprit.medivault.user.exceptions.EmailDejaUtiliseException;
import tn.esprit.medivault.user.messaging.UserEventPublisher;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;
import tn.esprit.medivault.user.services.UtilisateurServiceImpl;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository repository;
    @Mock
    private KeycloakAdminClient keycloak;
    @Mock
    private UserEventPublisher publisher;
    @InjectMocks
    private UtilisateurServiceImpl service;

    private UtilisateurRequest patient() {
        return new UtilisateurRequest("Ben Ali", "Ahmed", "Ahmed@Test.tn", "+216 20111222", "motdepasse",
                Role.PATIENT, null, null, null, null, null, null, null);
    }

    private UtilisateurRequest medecin(LocalTime debut, LocalTime fin) {
        return new UtilisateurRequest("Trabelsi", "Karim", "karim@test.tn", null, "motdepasse",
                Role.MEDECIN, "Cabinet", null, "12 rue", "Tunis",
                List.of(DayOfWeek.MONDAY), debut, fin);
    }

    @Test
    void creerPatient_enregistreEtPublieUtilisateurCree() {
        when(repository.existsByEmailIgnoreCase("ahmed@test.tn")).thenReturn(false);
        when(repository.save(any(Utilisateur.class))).thenAnswer(inv -> {
            Utilisateur u = inv.getArgument(0);
            u.setId("u1");
            return u;
        });

        UtilisateurResponse reponse = service.creer(patient());

        assertThat(reponse.id()).isEqualTo("u1");
        assertThat(reponse.email()).isEqualTo("ahmed@test.tn");
        assertThat(reponse.type()).isNull();
        ArgumentCaptor<UtilisateurCreeEvent> event = ArgumentCaptor.forClass(UtilisateurCreeEvent.class);
        verify(publisher).publierUtilisateurCree(event.capture());
        assertThat(event.getValue().role()).isEqualTo("PATIENT");
        assertThat(event.getValue().utilisateurId()).isEqualTo("u1");
    }

    @Test
    void creerMedecin_creeUnProfessionnelDeTypeMedecin() {
        when(repository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        service.creer(medecin(LocalTime.of(8, 0), LocalTime.of(16, 0)));

        ArgumentCaptor<Utilisateur> sauve = ArgumentCaptor.forClass(Utilisateur.class);
        verify(repository).save(sauve.capture());
        assertThat(sauve.getValue()).isInstanceOf(Professionnel.class);
        assertThat(((Professionnel) sauve.getValue()).getType()).isEqualTo(TypeProfessionnel.MEDECIN);
    }

    @Test
    void creer_emailDejaUtilise_409() {
        when(repository.existsByEmailIgnoreCase("ahmed@test.tn")).thenReturn(true);

        assertThatThrownBy(() -> service.creer(patient())).isInstanceOf(EmailDejaUtiliseException.class);
        verify(repository, never()).save(any());
        verify(publisher, never()).publierUtilisateurCree(any());
    }

    @Test
    void creerMedecin_horairesIncoherents_400() {
        assertThatThrownBy(() -> service.creer(medecin(LocalTime.of(17, 0), LocalTime.of(8, 0))))
                .isInstanceOf(DonneesInvalidesException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void creerPharmacien_sansAgrement_400() {
        UtilisateurRequest pharmacien = new UtilisateurRequest("Mansour", "Sami", "ph@test.tn", null, "motdepasse",
                Role.PHARMACIEN, "Pharmacie Centrale", null, "30 avenue", "Tunis", null, null, null);

        assertThatThrownBy(() -> service.creer(pharmacien)).isInstanceOf(DonneesInvalidesException.class);
    }
}
