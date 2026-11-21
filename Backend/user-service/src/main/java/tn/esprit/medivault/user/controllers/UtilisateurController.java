package tn.esprit.medivault.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.user.dto.StatutRequest;
import tn.esprit.medivault.user.dto.UtilisateurRequest;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.Role;
import tn.esprit.medivault.user.services.IUtilisateurService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "Utilisateurs", description = "Comptes MediVault (patients, professionnels, administrateurs)")
public class UtilisateurController {

    private final IUtilisateurService utilisateurService;

    @PostMapping
    @Operation(summary = "Créer un compte (Keycloak + profil) et publier UtilisateurCree")
    @ApiResponse(responseCode = "201", description = "Compte créé")
    @ApiResponse(responseCode = "400", description = "Données invalides ou champs du rôle manquants")
    @ApiResponse(responseCode = "409", description = "Email déjà utilisé")
    @ApiResponse(responseCode = "503", description = "Keycloak indisponible")
    public ResponseEntity<UtilisateurResponse> creer(@Valid @RequestBody UtilisateurRequest request) {
        UtilisateurResponse cree = utilisateurService.creer(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(cree.id()).toUri();
        return ResponseEntity.created(location).body(cree);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un utilisateur (utilisé par les autres services : compte actif ?)")
    @ApiResponse(responseCode = "200", description = "Utilisateur trouvé")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    public UtilisateurResponse trouver(@PathVariable String id) {
        return utilisateurService.trouverParId(id);
    }

    @GetMapping
    @Operation(summary = "Lister les comptes, filtrés par rôle (administrateur)")
    @ApiResponse(responseCode = "200", description = "Liste des comptes")
    public List<UtilisateurResponse> lister(@RequestParam(required = false) Role role) {
        return utilisateurService.lister(role);
    }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Activer ou désactiver un compte (administrateur)")
    @ApiResponse(responseCode = "200", description = "Statut modifié")
    @ApiResponse(responseCode = "400", description = "Corps invalide")
    @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    @ApiResponse(responseCode = "503", description = "Keycloak indisponible")
    public UtilisateurResponse changerStatut(@PathVariable String id, @Valid @RequestBody StatutRequest request) {
        return utilisateurService.changerStatut(id, request.actif());
    }
}
