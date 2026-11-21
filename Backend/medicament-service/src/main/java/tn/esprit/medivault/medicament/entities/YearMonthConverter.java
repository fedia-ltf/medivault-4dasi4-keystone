package tn.esprit.medivault.medicament.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

// JPA ne sait pas toujours enregistrer un YearMonth : on le stocke sous forme de texte "2026-11".
// Le texte "AAAA-MM" se trie correctement dans l'ordre chronologique.
@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth mois) {
        return mois == null ? null : mois.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String texte) {
        return texte == null ? null : YearMonth.parse(texte);
    }
}