package tn.esprit.medivault.analyse.dto;

import tn.esprit.medivault.analyse.entity.ResultatAnalyse;
import tn.esprit.medivault.analyse.enums.Interpretation;

public record ResultatResponse(Long id, String parametre, double valeur, String unite,
                               String valeurReference, Interpretation interpretation) {

    public static ResultatResponse from(ResultatAnalyse r) {
        return new ResultatResponse(r.getId(), r.getParametre(), r.getValeur(), r.getUnite(),
                r.getValeurReference(), r.getInterpretation());
    }
}
