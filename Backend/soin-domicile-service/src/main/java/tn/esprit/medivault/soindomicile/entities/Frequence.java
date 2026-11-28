package tn.esprit.medivault.soindomicile.entities;

import java.time.LocalDate;

public enum Frequence {
    QUOTIDIENNE, HEBDOMADAIRE, MENSUELLE;

    public LocalDate suivante(LocalDate date) {
        return switch (this) {
            case QUOTIDIENNE -> date.plusDays(1);
            case HEBDOMADAIRE -> date.plusWeeks(1);
            case MENSUELLE -> date.plusMonths(1);
        };
    }
}
