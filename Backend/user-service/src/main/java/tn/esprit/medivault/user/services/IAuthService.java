package tn.esprit.medivault.user.services;

import tn.esprit.medivault.user.dto.AuthDtos.InscriptionRequest;
import tn.esprit.medivault.user.dto.AuthDtos.LoginRequest;
import tn.esprit.medivault.user.dto.AuthDtos.MessageResponse;
import tn.esprit.medivault.user.dto.AuthDtos.SessionResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;

/** Connexion, inscription et mot de passe : le mot de passe n'est jamais stocké ici, Keycloak en est le seul gardien. */
public interface IAuthService {

    SessionResponse connexion(LoginRequest request);

    SessionResponse renouveler(String refreshToken);

    void deconnexion(String refreshToken);

    /** Crée un compte PATIENT (Keycloak + MongoDB + événement UtilisateurCree). */
    UtilisateurResponse inscription(InscriptionRequest request);

    /** Envoie l'e-mail de réinitialisation. Même réponse que le compte existe ou non (on ne révèle pas les comptes). */
    MessageResponse motDePasseOublie(String email);

    /** Choisit un nouveau mot de passe grâce au lien reçu par e-mail (à usage unique, 30 minutes). */
    MessageResponse reinitialiserMotDePasse(String lien, String nouveauMotDePasse);

    UtilisateurResponse moi(String email);

    MessageResponse changerMotDePasse(String email, String ancienMotDePasse, String nouveauMotDePasse);
}
