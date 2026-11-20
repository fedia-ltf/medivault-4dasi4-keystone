package tn.esprit.medivault.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import tn.esprit.medivault.user.exceptions.UtilisateurNotFoundException;
import tn.esprit.medivault.user.messaging.UserEventPublisher;
import tn.esprit.medivault.user.repositories.UtilisateurRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UtilisateurServiceImpl implements IUtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final UserEventPublisher eventPublisher;

    /**
     * 1. contrôles (email unique, champs du rôle) ; 2. compte Keycloak + rôle (Admin API) ;
     * 3. enregistrement MongoDB ; 4. publication de UtilisateurCree (dossier crée le dossier du patient).
     */
    @Override
    public UtilisateurResponse creer(UtilisateurRequest request) {
        String email = request.email().trim().toLowerCase();
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailDejaUtiliseException(email);
        }
        verifierChampsDuRole(request);
        if (keycloakAdminClient.isEnabled() && (request.motDePasse() == null || request.motDePasse().isBlank())) {
            throw new DonneesInvalidesException("Le mot de passe est obligatoire pour créer le compte Keycloak");
        }

        String keycloakId = keycloakAdminClient.creerCompte(email, request.prenom(), request.nom(),
                request.motDePasse(), request.role());

        Utilisateur utilisateur = construire(request, email, keycloakId);
        Utilisateur enregistre;
        try {
            enregistre = utilisateurRepository.save(utilisateur);
        } catch (RuntimeException e) {
            keycloakAdminClient.supprimerCompte(keycloakId);
            throw e;
        }

        eventPublisher.publierUtilisateurCree(new UtilisateurCreeEvent(
                enregistre.getId(), enregistre.getRole().name(), enregistre.getNom(), enregistre.getPrenom()));
        log.info("Compte {} créé ({})", enregistre.getId(), enregistre.getRole());
        return UtilisateurResponse.from(enregistre);
    }

    @Override
    public UtilisateurResponse trouverParId(String id) {
        return UtilisateurResponse.from(charger(id));
    }

    @Override
    public List<UtilisateurResponse> lister(Role role) {
        List<Utilisateur> utilisateurs = role == null
                ? utilisateurRepository.findAll()
                : utilisateurRepository.findByRole(role);
        return utilisateurs.stream().map(UtilisateurResponse::from).toList();
    }

    /** Un compte désactivé est refusé par les autres services (ex. medicament : pharmacie active ?). */
    @Override
    public UtilisateurResponse changerStatut(String id, boolean actif) {
        Utilisateur utilisateur = charger(id);
        keycloakAdminClient.definirActif(utilisateur.getKeycloakId(), actif);
        utilisateur.setActif(actif);
        return UtilisateurResponse.from(utilisateurRepository.save(utilisateur));
    }

    private Utilisateur charger(String id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new UtilisateurNotFoundException("Aucun utilisateur avec l'id " + id));
    }

    private void verifierChampsDuRole(UtilisateurRequest r) {
        switch (r.role()) {
            case MEDECIN, LABORANTIN -> {
                exiger(r.nomEtablissement(), "nomEtablissement (cabinet ou laboratoire)");
                exiger(r.adresse(), "adresse");
                exiger(r.ville(), "ville");
                verifierHoraires(r);
            }
            case PHARMACIEN -> {
                exiger(r.nomEtablissement(), "nomEtablissement (nom de la pharmacie)");
                exiger(r.numeroAgrement(), "numeroAgrement");
                exiger(r.adresse(), "adresse");
                exiger(r.ville(), "ville");
            }
            default -> {
                // PATIENT et ADMIN : aucun champ professionnel
            }
        }
    }

    private void verifierHoraires(UtilisateurRequest r) {
        if (r.joursTravail() == null || r.joursTravail().isEmpty()) {
            throw new DonneesInvalidesException("joursTravail est obligatoire pour un " + r.role());
        }
        if (r.heureDebut() == null || r.heureFin() == null) {
            throw new DonneesInvalidesException("heureDebut et heureFin sont obligatoires pour un " + r.role());
        }
        if (!r.heureDebut().isBefore(r.heureFin())) {
            throw new DonneesInvalidesException("heureDebut doit être avant heureFin");
        }
    }

    private void exiger(String valeur, String champ) {
        if (valeur == null || valeur.isBlank()) {
            throw new DonneesInvalidesException("Le champ " + champ + " est obligatoire pour ce rôle");
        }
    }

    private Utilisateur construire(UtilisateurRequest r, String email, String keycloakId) {
        if (r.role().estProfessionnel()) {
            return Professionnel.builder()
                    .keycloakId(keycloakId)
                    .nom(r.nom().trim())
                    .prenom(r.prenom().trim())
                    .email(email)
                    .telephone(r.telephone())
                    .role(r.role())
                    .actif(true)
                    .type(TypeProfessionnel.depuis(r.role()))
                    .nomEtablissement(r.nomEtablissement())
                    .numeroAgrement(r.numeroAgrement())
                    .adresse(r.adresse())
                    .ville(r.ville() == null ? null : r.ville().trim())
                    .joursTravail(r.joursTravail() == null ? null : r.joursTravail().stream().distinct().sorted().toList())
                    .heureDebut(r.heureDebut())
                    .heureFin(r.heureFin())
                    .build();
        }
        return Utilisateur.builder()
                .keycloakId(keycloakId)
                .nom(r.nom().trim())
                .prenom(r.prenom().trim())
                .email(email)
                .telephone(r.telephone())
                .role(r.role())
                .actif(true)
                .build();
    }
}
