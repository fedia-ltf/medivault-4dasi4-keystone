package tn.esprit.medivault.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.medivault.user.config.SecurityConfig;
import tn.esprit.medivault.user.controllers.AuthController;
import tn.esprit.medivault.user.dto.AuthDtos.MessageResponse;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.exceptions.CompteInaccessibleException;
import tn.esprit.medivault.user.exceptions.IdentifiantsInvalidesException;
import tn.esprit.medivault.user.exceptions.KeycloakIndisponibleException;
import tn.esprit.medivault.user.services.IAuthService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Les routes /api/auth : formats, validations et codes de retour (le service est simulé).
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private IAuthService service;

    private static UtilisateurResponse patient() {
        return new UtilisateurResponse("u1", "Ben Ali", "Ahmed", "ahmed@test.tn", null, Role.PATIENT, true,
                null, null, null, null, null, null, null, null, null);
    }

    @Test
    void login_200_renvoieLesJetonsEtLeRole() throws Exception {
        when(service.connexion(any())).thenReturn(new SessionResponse("acces", "refresh", 900, 1800, "Bearer", patient()));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ahmed@test.tn\",\"motDePasse\":\"Secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acces"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"))
                .andExpect(jsonPath("$.utilisateur.role").value("PATIENT"));
    }

    @Test
    void login_emailInvalide_400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"pas-un-email\",\"motDePasse\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.email").exists());
    }

    @Test
    void login_motDePasseVide_400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"motDePasse\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.motDePasse").exists());
    }

    @Test
    void login_mauvaisIdentifiants_401() throws Exception {
        when(service.connexion(any())).thenThrow(new IdentifiantsInvalidesException("Email ou mot de passe incorrect."));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"motDePasse\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Identifiants invalides"))
                .andExpect(jsonPath("$.detail").value("Email ou mot de passe incorrect."));
    }

    @Test
    void login_compteDesactive_403() throws Exception {
        when(service.connexion(any())).thenThrow(new CompteInaccessibleException("Ce compte est désactivé."));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"motDePasse\":\"Secret123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void login_keycloakDesactive_503() throws Exception {
        when(service.connexion(any())).thenThrow(new KeycloakIndisponibleException("Keycloak est désactivé", null));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"motDePasse\":\"Secret123\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void inscription_201() throws Exception {
        when(service.inscription(any())).thenReturn(patient());

        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"Ben Ali\",\"prenom\":\"Ahmed\",\"email\":\"ahmed@test.tn\",\"telephone\":\"+21620111222\",\"motDePasse\":\"Secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void inscription_motDePasseTropFaible_400() throws Exception {
        for (String faible : new String[]{"court1", "motdepassesanschiffre", "12345678"}) {
            mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nom\":\"A\",\"prenom\":\"B\",\"email\":\"a@b.tn\",\"motDePasse\":\"" + faible + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreurs.motDePasse").exists());
        }
    }

    @Test
    void inscription_neLitPasLeRoleDansLaRequete() throws Exception {
        when(service.inscription(any())).thenReturn(patient());

        // un visiteur qui essaie de s'inscrire « ADMIN » : le champ est ignoré, le service impose PATIENT
        mockMvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"A\",\"prenom\":\"B\",\"email\":\"a@b.tn\",\"motDePasse\":\"Secret123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        verify(service).inscription(any());
    }

    @Test
    void motDePasseOublie_202_etEmailInvalide_400() throws Exception {
        when(service.motDePasseOublie("a@b.tn", null)).thenReturn(new MessageResponse("Si un compte existe…"));
        when(service.motDePasseOublie("a@b.tn", "WHATSAPP")).thenReturn(new MessageResponse("message WhatsApp"));

        mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Si un compte existe…"));
        mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"canal\":\"WHATSAPP\"}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.message").value("message WhatsApp"));
        mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"canal\":\"PIGEON\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/mot-de-passe-oublie").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nimporte quoi\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reinitialiser_200_lienInvalide400_motDePasseFaible400() throws Exception {
        when(service.reinitialiserMotDePasse("lien-ok", "Nouveau123")).thenReturn(new MessageResponse("modifié"));
        when(service.reinitialiserMotDePasse("lien-mort", "Nouveau123"))
                .thenThrow(new tn.esprit.medivault.user.exceptions.LienInvalideException("Ce lien est invalide ou a expiré"));

        mockMvc.perform(post("/api/auth/reinitialiser-mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"lien-ok\",\"nouveauMotDePasse\":\"Nouveau123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.message").value("modifié"));
        mockMvc.perform(post("/api/auth/reinitialiser-mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"lien-mort\",\"nouveauMotDePasse\":\"Nouveau123\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/reinitialiser-mot-de-passe").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"lien-ok\",\"nouveauMotDePasse\":\"abc\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_204() throws Exception {
        mockMvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"r\"}"))
                .andExpect(status().isNoContent());
        verify(service).deconnexion("r");
    }

    @Test
    void moi_sansJeton_401() throws Exception {
        mockMvc.perform(get("/api/auth/moi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Identifiants invalides"));
    }
}
