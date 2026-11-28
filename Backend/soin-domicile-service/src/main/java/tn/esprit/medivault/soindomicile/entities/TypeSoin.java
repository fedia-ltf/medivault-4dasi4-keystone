package tn.esprit.medivault.soindomicile.entities;

public enum TypeSoin {
    INJECTION, MESURE_TENSION, GLYCEMIE, PRISE_DE_SANG;

    /** RG2 : une injection ou une prise de sang exige une ordonnance valide. */
    public boolean necessiteOrdonnance() {
        return this == INJECTION || this == PRISE_DE_SANG;
    }
}
