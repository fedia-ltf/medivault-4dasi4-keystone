package tn.esprit.medivault.analyse.service;

import tn.esprit.medivault.analyse.enums.Interpretation;

/** Compare la valeur mesurée à l'intervalle de référence (ex. 1,30 g/L pour 0,70–1,10 → ELEVE). */
public final class InterpretationCalculator {

    private InterpretationCalculator() {
    }

    public static Interpretation interpreter(double valeur, double min, double max) {
        if (valeur < min) {
            return Interpretation.BAS;
        }
        if (valeur > max) {
            return Interpretation.ELEVE;
        }
        return Interpretation.NORMAL;
    }
}
