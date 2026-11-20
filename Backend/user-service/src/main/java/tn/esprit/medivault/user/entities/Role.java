package tn.esprit.medivault.user.entities;

/**
 * Rôles Keycloak de MediVault (l'infirmier n'a pas de compte : il est géré par la pharmacie).
 */
public enum Role {
    PATIENT,
    MEDECIN,
    PHARMACIEN,
    LABORANTIN,
    ADMIN;

    public boolean estProfessionnel() {
        return this == MEDECIN || this == PHARMACIEN || this == LABORANTIN;
    }
}
