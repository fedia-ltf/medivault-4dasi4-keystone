package tn.esprit.medivault.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.medivault.user.config.SecurityConfig;
import tn.esprit.medivault.user.controllers.AuthController;
import tn.esprit.medivault.user.controllers.ProfessionnelController;
import tn.esprit.medivault.user.controllers.UtilisateurController;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.services.IAuthService;
import tn.esprit.medivault.user.services.IProfessionnelService;
import tn.esprit.medivault.user.services.IUtilisateurService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La sécurité activée (medivault.security.enabled=true) : sans jeton 401, mauvais rôle 403, bon rôle accepté.
// Le décodeur de jeton est simulé : on fabrique un jeton « comme Keycloak » avec le rôle dans realm_access.roles.
@WebMvcTest(controllers = {UtilisateurController.class, ProfessionnelController.class, AuthController.class},
        properties = {"medivault.security.enabled=true", "medivault.security.jwk-set-uri=http://localhost:1/certs"})
@Import(SecurityConfig.class)
class SecuriteTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private IUtilisateurService utilisateurService;
    @MockitoBean
    private IProfessionnelService professionnelService;
    @MockitoBean
    private IAuthService authService;

    private static final String NOUVEAU_COMPTE = """
            {"nom":"Ben Ali","prenom":"Ahmed","email":"ahmed@test.tn","motDePasse":"motdepasse1","role":"PATIENT"}
            """;

    /** Fabrique l'en-tête « Authorization » d'une personne ayant ce rôle. */
    private String jeton(String role) {
        String valeur = "jeton-" + role;
        Instant maintenant = Instant.now();
        Jwt jwt = Jwt.withTokenValue(valeur).header("alg", "RS256").subject("kc-" + role).claim("email", role.toLowerCase() + "@medivault.tn")
                .claim("realm_access", Map.of("roles", List.of(role, "offline_access", "default-roles-medivault")))
                .issuedAt(maintenant).expiresAt(maintenant.plusSeconds(300)).build();
        when(jwtDecoder.decode(valeur)).thenReturn(jwt);
        return "Bearer " + valeur;
    }

    private static UtilisateurResponse compte() {
        return new UtilisateurResponse("u1", "Ben Ali", "Ahmed", "ahmed@test.tn", null, Role.PATIENT, true,
                null, null, null, null, null, null, null, null, null);
    }

    // ---------------------------------------------------------------- lecture réservée à l'administrateur

    @Test
    void listerLesComptes_sansJeton_401() throws Exception {
        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Non authentifié"));
    }

    @Test
    void listerLesComptes_parUnPatient_403() throws Exception {
        mockMvc.perform(get("/api/utilisateurs").header("Authorization", jeton("PATIENT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Accès refusé"));
    }

    @Test
    void listerLesComptes_parUnAdmin_200() throws Exception {
        when(utilisateurService.lister(any())).thenReturn(List.of(compte()));

        mockMvc.perform(get("/api/utilisateurs").header("Authorization", jeton("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ahmed@test.tn"));
    }

    @Test
    void jetonInvalide_401() throws Exception {
        when(jwtDecoder.decode("faux")).thenThrow(new org.springframework.security.oauth2.jwt.BadJwtException("jeton illisible"));

        mockMvc.perform(get("/api/utilisateurs").header("Authorization", "Bearer faux"))
                .andExpect(status().isUnauthorized());
    }

    // ---------------------------------------------------------------- créer et modifier des comptes : administrateur seulement

    @Test
    void creerUnCompte_parUnMedecin_403_parUnAdmin_201() throws Exception {
        when(utilisateurService.creer(any())).thenReturn(compte());

        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(NOUVEAU_COMPTE)
                        .header("Authorization", jeton("MEDECIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/utilisateurs").contentType(MediaType.APPLICATION_JSON).content(NOUVEAU_COMPTE)
                        .header("Authorization", jeton("ADMIN")))
                .andExpect(status().isCreated());
    }

    @Test
    void desactiverUnCompte_parUnPharmacien_403_parUnAdmin_200() throws Exception {
        when(utilisateurService.changerStatut(anyString(), any(Boolean.class))).thenReturn(compte());
        String corps = "{\"actif\":false}";

        mockMvc.perform(patch("/api/utilisateurs/u1/statut").contentType(MediaType.APPLICATION_JSON).content(corps)
                        .header("Authorization", jeton("PHARMACIEN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/utilisateurs/u1/statut").contentType(MediaType.APPLICATION_JSON).content(corps)
                        .header("Authorization", jeton("ADMIN")))
                .andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- lectures ouvertes (appelées par les autres services)

    @Test
    void lireUnCompte_sansJeton_200_pourLesAutresServices() throws Exception {
        when(utilisateurService.trouverParId("u1")).thenReturn(compte());

        mockMvc.perform(get("/api/utilisateurs/u1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(true));
    }

    @Test
    void rechercherDesProfessionnels_sansJeton_200() throws Exception {
        when(professionnelService.rechercher(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/professionnels")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- signature du médecin

    @Test
    void deposerUneSignature_sansJeton_401_parUnPatient_403() throws Exception {
        mockMvc.perform(put("/api/professionnels/m1/signature").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/professionnels/m1/signature").contentType(MediaType.MULTIPART_FORM_DATA)
                        .header("Authorization", jeton("PATIENT")))
                .andExpect(status().isForbidden());
    }

    // ---------------------------------------------------------------- connexion

    @Test
    void connexion_estOuverteATous() throws Exception {
        when(authService.connexion(any())).thenReturn(new SessionResponse("a", "r", 900, 1800, "Bearer", compte()));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.tn\",\"motDePasse\":\"Secret123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void monProfil_exigeUnJeton() throws Exception {
        when(authService.moi("patient@medivault.tn")).thenReturn(compte());

        mockMvc.perform(get("/api/auth/moi")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/auth/moi").header("Authorization", jeton("PATIENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"));
    }
}
