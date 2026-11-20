package tn.esprit.medivault.user.entities;

/**
 * Même valeur dans user-service, rendezvous-service et dans les événements.
 */
public enum TypeProfessionnel {
    MEDECIN,
    PHARMACIEN,
    LABORANTIN;

    public static TypeProfessionnel depuis(Role role) {
        return TypeProfessionnel.valueOf(role.name());
    }
}
