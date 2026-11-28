package tn.esprit.medivault.soindomicile.entities;

/** DEMANDEE → ACCEPTEE → REALISEE ; ANNULEE tant que le soin n'est pas réalisé. */
public enum StatutVisite {
    DEMANDEE, ACCEPTEE, REALISEE, ANNULEE;

    public boolean estActive() {
        return this == DEMANDEE || this == ACCEPTEE;
    }
}
