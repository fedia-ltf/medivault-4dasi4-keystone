package tn.esprit.medivault.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tn.esprit.medivault.user.dto.DisponibilitesResponse;
import tn.esprit.medivault.user.dto.SignatureResponse;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.entities.Utilisateur;
import tn.esprit.medivault.user.exceptions.DonneesInvalidesException;
import tn.esprit.medivault.user.exceptions.UtilisateurNotFoundException;
import tn.esprit.medivault.user.repositories.ProfessionnelSearchRepository;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;
import tn.esprit.medivault.user.services.ProfessionnelServiceImpl;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfessionnelServiceImplTest {

    @Mock
    private UtilisateurRepository repository;
    @Mock
    private ProfessionnelSearchRepository searchRepository;
    @InjectMocks
    private ProfessionnelServiceImpl service;

    private Professionnel karim() {
        return Professionnel.builder().id("medecin-karim").nom("Trabelsi").prenom("Karim")
                .role(Role.MEDECIN).type(TypeProfessionnel.MEDECIN)
                .joursTravail(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))
                .heureDebut(LocalTime.of(8, 0)).heureFin(LocalTime.of(16, 0)).build();
    }

    @Test
    void disponibilites_renvoieLeContratLuParRendezvous() {
        when(repository.findById("medecin-karim")).thenReturn(Optional.of(karim()));

        DisponibilitesResponse d = service.disponibilites("medecin-karim");

        assertThat(d.typeProfessionnel()).isEqualTo("MEDECIN");
        assertThat(d.joursTravail()).containsExactly("MONDAY", "TUESDAY");
        assertThat(d.heureDebut()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void disponibilites_dUnPatient_404() {
        Utilisateur patient = Utilisateur.builder().id("patient-ahmed").role(Role.PATIENT).build();
        when(repository.findById("patient-ahmed")).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.disponibilites("patient-ahmed"))
                .isInstanceOf(UtilisateurNotFoundException.class);
    }

    @Test
    void enregistrerSignature_png_stockeeEnDataUrl() {
        when(repository.findById("medecin-karim")).thenReturn(Optional.of(karim()));
        when(repository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "sig.png", "image/png", new byte[]{1, 2, 3});

        SignatureResponse s = service.enregistrerSignature("medecin-karim", fichier);

        assertThat(s.signatureImage()).startsWith("data:image/png;base64,");
        assertThat(s.nomComplet()).isEqualTo("Dr Karim Trabelsi");
    }

    @Test
    void enregistrerSignature_formatNonAutorise_400() {
        when(repository.findById("medecin-karim")).thenReturn(Optional.of(karim()));
        MockMultipartFile fichier = new MockMultipartFile("fichier", "sig.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> service.enregistrerSignature("medecin-karim", fichier))
                .isInstanceOf(DonneesInvalidesException.class);
    }

    @Test
    void signature_absente_404() {
        when(repository.findById("medecin-karim")).thenReturn(Optional.of(karim()));

        assertThatThrownBy(() -> service.signature("medecin-karim"))
                .isInstanceOf(UtilisateurNotFoundException.class);
    }
}
