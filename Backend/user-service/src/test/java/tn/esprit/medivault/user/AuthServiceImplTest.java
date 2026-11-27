package tn.esprit.medivault.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.medivault.user.clients.EmailApiClient;
import tn.esprit.medivault.user.clients.WhatsAppApiClient;
import tn.esprit.medivault.user.clients.KeycloakAdminClient;
import tn.esprit.medivault.user.entities.JetonReinitialisation;
import tn.esprit.medivault.user.exceptions.EmailIndisponibleException;
import tn.esprit.medivault.user.exceptions.LienInvalideException;
import tn.esprit.medivault.user.repositories.JetonReinitialisationRepository;
import java.time.Instant;
import tn.esprit.medivault.user.dto.AuthDtos.InscriptionRequest;
import tn.esprit.medivault.user.dto.AuthDtos.LoginRequest;
import tn.esprit.medivault.user.dto.AuthDtos.MessageResponse;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurRequest;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.entities.Utilisateur;
import tn.esprit.medivault.user.exceptions.CompteInaccessibleException;
import tn.esprit.medivault.user.exceptions.IdentifiantsInvalidesException;
import tn.esprit.medivault.user.exceptions.KeycloakIndisponibleException;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;
import tn.esprit.medivault.user.services.AuthServiceImpl;
import tn.esprit.medivault.user.services.IUtilisateurService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// La logique de connexion, sans Keycloak ni MongoDB (ils sont simulés) : on vérifie les décisions du service.
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private KeycloakAdminClient keycloak;
    @Mock
    private UtilisateurRepository repository;
    @Mock
    private IUtilisateurService utilisateurService;
    @Mock
    private JetonReinitialisationRepository jetons;
    @Mock
    private EmailApiClient emailApi;
    @Mock
    private WhatsAppApiClient whatsApp;

    private AuthServiceImpl service;

    @BeforeEach
    void preparer() {
        service = new AuthServiceImpl(keycloak, repository, utilisateurService, new ObjectMapper(), jetons, emailApi, whatsApp);
        ReflectionTestUtils.setField(service, "urlRetourApresReinitialisation", "http://localhost:4301/login");
    }

    /** Un faux jeton JWT : en-tête.contenu.signature, avec « sub » et « email » dans le contenu. */
    private static String jwt(String sub, String email) {
        String contenu = "{\"sub\":\"" + sub + "\",\"email\":\"" + email + "\"}";
        return "e30." + Base64.getUrlEncoder().withoutPadding().encodeToString(contenu.getBytes(StandardCharsets.UTF_8)) + ".sig";
    }

    private static Map<String, Object> jetons(String sub, String email) {
        return Map.of("access_token", jwt(sub, email), "refresh_token", "refresh-1", "expires_in", 900, "refresh_expires_in", 1800);
    }

    private static Utilisateur patient(boolean actif, String keycloakId) {
        return Utilisateur.builder().id("u1").keycloakId(keycloakId).nom("Ben Ali").prenom("Ahmed")
                .email("ahmed@test.tn").role(Role.PATIENT).actif(actif).build();
    }

    // ---------------------------------------------------------------- connexion

    @Test
    void connexion_renvoieLesJetonsEtLeProfilAvecSonRole() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        when(keycloak.connexion("ahmed@test.tn", "Secret123")).thenReturn(jetons("kc-1", "ahmed@test.tn"));

        SessionResponse session = service.connexion(new LoginRequest("  Ahmed@Test.tn ", "Secret123"));

        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshToken()).isEqualTo("refresh-1");
        assertThat(session.expiresIn()).isEqualTo(900);
        assertThat(session.tokenType()).isEqualTo("Bearer");
        assertThat(session.utilisateur().role()).isEqualTo(Role.PATIENT);
        assertThat(session.utilisateur().email()).isEqualTo("ahmed@test.tn");
    }

    @Test
    void connexion_relieLeProfilAKeycloakAuPremierLogin() {
        Utilisateur compte = patient(true, null);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(compte));
        when(keycloak.connexion(anyString(), anyString())).thenReturn(jetons("kc-nouveau", "ahmed@test.tn"));
        when(repository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        service.connexion(new LoginRequest("ahmed@test.tn", "Secret123"));

        assertThat(compte.getKeycloakId()).isEqualTo("kc-nouveau");
        verify(repository).save(compte);
    }

    @Test
    void connexion_refuseUnCompteDesactiveSansInterrogerKeycloak() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(false, "kc-1")));

        assertThatThrownBy(() -> service.connexion(new LoginRequest("ahmed@test.tn", "Secret123")))
                .isInstanceOf(CompteInaccessibleException.class).hasMessageContaining("désactivé");
        verify(keycloak, never()).connexion(anyString(), anyString());
    }

    @Test
    void connexion_refuseUnCompteSansProfilEtFermeLaSessionKeycloak() {
        when(repository.findByEmailIgnoreCase("inconnu@test.tn")).thenReturn(Optional.empty());
        when(keycloak.connexion(anyString(), anyString())).thenReturn(jetons("kc-9", "inconnu@test.tn"));

        assertThatThrownBy(() -> service.connexion(new LoginRequest("inconnu@test.tn", "Secret123")))
                .isInstanceOf(CompteInaccessibleException.class);
        verify(keycloak).deconnecter("refresh-1");
    }

    @Test
    void connexion_motDePasseFaux_401() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        when(keycloak.connexion(anyString(), anyString())).thenThrow(new IdentifiantsInvalidesException("Email ou mot de passe incorrect."));

        assertThatThrownBy(() -> service.connexion(new LoginRequest("ahmed@test.tn", "mauvais")))
                .isInstanceOf(IdentifiantsInvalidesException.class);
    }

    @Test
    void connexion_keycloakDesactive_503() {
        when(repository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(keycloak.connexion(anyString(), anyString())).thenThrow(new KeycloakIndisponibleException("désactivé", null));

        assertThatThrownBy(() -> service.connexion(new LoginRequest("a@b.tn", "Secret123")))
                .isInstanceOf(KeycloakIndisponibleException.class);
    }

    // ---------------------------------------------------------------- renouvellement

    @Test
    void renouveler_refuseUnCompteDevenuInactif() {
        when(keycloak.renouveler("r")).thenReturn(jetons("kc-1", "ahmed@test.tn"));
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(false, "kc-1")));

        assertThatThrownBy(() -> service.renouveler("r")).isInstanceOf(CompteInaccessibleException.class);
        verify(keycloak).deconnecter("refresh-1");
    }

    // ---------------------------------------------------------------- inscription

    @Test
    void inscription_imposeToujoursLeRolePatient() {
        when(utilisateurService.creer(any())).thenReturn(UtilisateurResponse.from(patient(true, "kc-1")));

        service.inscription(new InscriptionRequest("Ben Ali", "Ahmed", "ahmed@test.tn", "+21620111222", "Secret123"));

        ArgumentCaptor<UtilisateurRequest> demande = ArgumentCaptor.forClass(UtilisateurRequest.class);
        verify(utilisateurService).creer(demande.capture());
        assertThat(demande.getValue().role()).isEqualTo(Role.PATIENT);
        assertThat(demande.getValue().motDePasse()).isEqualTo("Secret123");
        assertThat(demande.getValue().nomEtablissement()).isNull();
    }

    // ---------------------------------------------------------------- mot de passe oublié

    @Test
    void motDePasseOublie_compteExistant_envoieLEmail() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));

        MessageResponse reponse = service.motDePasseOublie(" AHMED@test.tn ");

        verify(keycloak).envoyerEmailReinitialisation("kc-1", "http://localhost:4301/login");
        assertThat(reponse.message()).contains("Si un compte existe");
    }

    @Test
    void motDePasseOublie_compteInconnu_memeReponseEtRienEnvoye() {
        when(repository.findByEmailIgnoreCase("inconnu@test.tn")).thenReturn(Optional.empty());

        MessageResponse reponse = service.motDePasseOublie("inconnu@test.tn");

        verify(keycloak, never()).envoyerEmailReinitialisation(anyString(), anyString());
        assertThat(reponse.message()).contains("Si un compte existe");
    }

    @Test
    void motDePasseOublie_echecDEnvoi_neRevelePasLeCompte() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        org.mockito.Mockito.doThrow(new KeycloakIndisponibleException("smtp", null))
                .when(keycloak).envoyerEmailReinitialisation(anyString(), anyString());

        MessageResponse reponse = service.motDePasseOublie("ahmed@test.tn");

        assertThat(reponse.message()).contains("Si un compte existe");
    }

    @Test
    void motDePasseOublie_sansIdentifiantKeycloak_leCherchePaEmail() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, null)));
        when(keycloak.trouverIdParEmail("ahmed@test.tn")).thenReturn("kc-trouve");

        service.motDePasseOublie("ahmed@test.tn");

        verify(keycloak).envoyerEmailReinitialisation("kc-trouve", "http://localhost:4301/login");
    }

    // ---------------------------------------------------------------- mot de passe oublié par l'API d'e-mail

    @Test
    void motDePasseOublie_apiActive_envoieUnLienAUsageUniqueSansPasserParKeycloak() {
        when(emailApi.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));

        service.motDePasseOublie("ahmed@test.tn");

        ArgumentCaptor<JetonReinitialisation> sauve = ArgumentCaptor.forClass(JetonReinitialisation.class);
        verify(jetons).save(sauve.capture());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(emailApi).envoyer(eq("ahmed@test.tn"), anyString(), anyString(), html.capture());
        // le lien est dans l'e-mail, mais la base ne garde que son empreinte SHA-256 (64 caractères), jamais le lien
        String lien = html.getValue().replaceAll("(?s).*reinitialiser-mot-de-passe\\?token=([A-Za-z0-9_-]+).*", "$1");
        assertThat(lien).hasSizeGreaterThan(30);
        assertThat(sauve.getValue().getEmpreinte()).hasSize(64).doesNotContain(lien);
        assertThat(sauve.getValue().getExpiration()).isAfter(Instant.now().plusSeconds(25 * 60));
        verify(keycloak, never()).envoyerEmailReinitialisation(anyString(), anyString());
    }

    @Test
    void motDePasseOublie_apiActive_compteDesactive_rienEnvoye() {
        when(emailApi.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(false, "kc-1")));

        service.motDePasseOublie("ahmed@test.tn");

        verify(emailApi, never()).envoyer(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void motDePasseOublie_apiActive_echecDEnvoi_replieSurKeycloakSansRevelerLeCompte() {
        when(emailApi.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        org.mockito.Mockito.doThrow(new EmailIndisponibleException("refusé", null))
                .when(emailApi).envoyer(anyString(), anyString(), anyString(), anyString());

        MessageResponse reponse = service.motDePasseOublie("ahmed@test.tn");

        assertThat(reponse.message()).contains("Si un compte existe");
        // l'API refuse : la demande ne se perd pas, Keycloak envoie l'e-mail (visible dans Mailpit)
        verify(keycloak).envoyerEmailReinitialisation("kc-1", "http://localhost:4301/login");
    }

    // ---------------------------------------------------------------- mot de passe oublié par WhatsApp

    private static Utilisateur patientAvecTelephone(String telephone) {
        Utilisateur u = patient(true, "kc-1");
        u.setTelephone(telephone);
        return u;
    }

    @Test
    void whatsApp_envoieLeLienAuNumeroInternationalSansPasserParEmail() {
        when(whatsApp.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patientAvecTelephone("22 123 456")));

        MessageResponse reponse = service.motDePasseOublie("ahmed@test.tn", "whatsapp");

        ArgumentCaptor<String> texte = ArgumentCaptor.forClass(String.class);
        verify(whatsApp).envoyer(eq("+21622123456"), texte.capture());
        assertThat(texte.getValue()).contains("reinitialiser-mot-de-passe?token=");
        verify(jetons).save(any(JetonReinitialisation.class));
        verify(emailApi, never()).envoyer(anyString(), anyString(), anyString(), anyString());
        verify(keycloak, never()).envoyerEmailReinitialisation(anyString(), anyString());
        assertThat(reponse.message()).contains("message");
    }

    @Test
    void whatsApp_compteSansNumero_replieSurLEmail() {
        when(whatsApp.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patientAvecTelephone(null)));

        service.motDePasseOublie("ahmed@test.tn", "WHATSAPP");

        verify(whatsApp, never()).envoyer(anyString(), anyString());
        verify(keycloak).envoyerEmailReinitialisation("kc-1", "http://localhost:4301/login");
    }

    @Test
    void whatsApp_nonConfigure_replieSurLEmail() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patientAvecTelephone("22123456")));

        service.motDePasseOublie("ahmed@test.tn", "WHATSAPP");

        verify(whatsApp, never()).envoyer(anyString(), anyString());
        verify(keycloak).envoyerEmailReinitialisation("kc-1", "http://localhost:4301/login");
    }

    @Test
    void whatsApp_refuseParTwilio_replieSurLEmail() {
        when(whatsApp.isActif()).thenReturn(true);
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patientAvecTelephone("22123456")));
        org.mockito.Mockito.doThrow(new EmailIndisponibleException("refusé", null)).when(whatsApp).envoyer(anyString(), anyString());

        service.motDePasseOublie("ahmed@test.tn", "WHATSAPP");

        verify(keycloak).envoyerEmailReinitialisation("kc-1", "http://localhost:4301/login");
    }

    @Test
    void numeroInternational_formatsUsuels() {
        assertThat(AuthServiceImpl.numeroInternational("22 123 456")).isEqualTo("+21622123456");
        assertThat(AuthServiceImpl.numeroInternational("+216 22 123 456")).isEqualTo("+21622123456");
        assertThat(AuthServiceImpl.numeroInternational("0021622123456")).isEqualTo("+21622123456");
        assertThat(AuthServiceImpl.numeroInternational("+33612345678")).isEqualTo("+33612345678");
        assertThat(AuthServiceImpl.numeroInternational(null)).isNull();
        assertThat(AuthServiceImpl.numeroInternational("abc")).isNull();
    }

    @Test
    void reinitialiser_lienValide_changeLeMotDePasseEtInvalideLesLiens() {
        String lien = "abc";
        when(jetons.findByEmpreinte(empreinteDe(lien))).thenReturn(Optional.of(JetonReinitialisation.builder()
                .utilisateurId("u1").expiration(Instant.now().plusSeconds(600)).build()));
        when(repository.findById("u1")).thenReturn(Optional.of(patient(true, "kc-1")));

        MessageResponse reponse = service.reinitialiserMotDePasse(lien, "Nouveau123");

        verify(keycloak).changerMotDePasse("kc-1", "Nouveau123");
        verify(jetons).deleteByUtilisateurId("u1");
        assertThat(reponse.message()).contains("modifié");
    }

    @Test
    void reinitialiser_lienInconnuOuExpire_400() {
        when(jetons.findByEmpreinte(empreinteDe("inconnu"))).thenReturn(Optional.empty());
        when(jetons.findByEmpreinte(empreinteDe("vieux"))).thenReturn(Optional.of(JetonReinitialisation.builder()
                .utilisateurId("u1").expiration(Instant.now().minusSeconds(5)).build()));

        assertThatThrownBy(() -> service.reinitialiserMotDePasse("inconnu", "Nouveau123")).isInstanceOf(LienInvalideException.class);
        assertThatThrownBy(() -> service.reinitialiserMotDePasse("vieux", "Nouveau123")).isInstanceOf(LienInvalideException.class);
        verify(keycloak, never()).changerMotDePasse(anyString(), anyString());
    }

    @Test
    void reinitialiser_compteDesactive_403() {
        when(jetons.findByEmpreinte(empreinteDe("abc"))).thenReturn(Optional.of(JetonReinitialisation.builder()
                .utilisateurId("u1").expiration(Instant.now().plusSeconds(600)).build()));
        when(repository.findById("u1")).thenReturn(Optional.of(patient(false, "kc-1")));

        assertThatThrownBy(() -> service.reinitialiserMotDePasse("abc", "Nouveau123")).isInstanceOf(CompteInaccessibleException.class);
        verify(keycloak, never()).changerMotDePasse(anyString(), anyString());
    }

    private static String empreinteDe(String lien) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(lien.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- changer le mot de passe

    @Test
    void changerMotDePasse_ancienFaux_refuse() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        when(keycloak.connexion("ahmed@test.tn", "faux")).thenThrow(new IdentifiantsInvalidesException("x"));

        assertThatThrownBy(() -> service.changerMotDePasse("ahmed@test.tn", "faux", "Nouveau123"))
                .isInstanceOf(IdentifiantsInvalidesException.class).hasMessageContaining("actuel");
        verify(keycloak, never()).changerMotDePasse(anyString(), anyString());
    }

    @Test
    void changerMotDePasse_ancienJuste_modifie() {
        when(repository.findByEmailIgnoreCase("ahmed@test.tn")).thenReturn(Optional.of(patient(true, "kc-1")));
        when(keycloak.connexion("ahmed@test.tn", "Ancien123")).thenReturn(jetons("kc-1", "ahmed@test.tn"));

        MessageResponse reponse = service.changerMotDePasse("ahmed@test.tn", "Ancien123", "Nouveau123");

        verify(keycloak).changerMotDePasse("kc-1", "Nouveau123");
        assertThat(reponse.message()).contains("modifié");
    }
}
