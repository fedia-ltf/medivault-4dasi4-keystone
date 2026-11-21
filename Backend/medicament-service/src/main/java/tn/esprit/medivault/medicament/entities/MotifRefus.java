package tn.esprit.medivault.medicament.entities;

// Pourquoi une tentative de vente a été refusée (historique des refus, utilisé aussi par le score de risque).
public enum MotifRefus {
    PHARMACIE_INACTIVE,        // le compte de la pharmacie n'est pas actif (user-service)
    AUCUN_QUOTA,               // aucun quota attribué à cette pharmacie pour ce médicament ce mois-ci
    PHARMACIE_SUSPENDUE,       // quota suspendu après 2 avertissements (RG3)
    ORDONNANCE_NON_JUSTIFIEE,  // RG1 : ordonnance introuvable, non validée, sans ce médicament ou quantité dépassée
    QUOTA_EPUISE               // RG2 : quota du mois insuffisant
}
