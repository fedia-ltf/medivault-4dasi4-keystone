package tn.esprit.medivault.user.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.medivault.user.entities.Professionnel;
import tn.esprit.medivault.user.entities.TypeProfessionnel;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Recherche des professionnels avec des filtres optionnels (type, ville).
 * Les patients et administrateurs (sans champ "type") sont exclus.
 */
@Repository
@RequiredArgsConstructor
public class ProfessionnelSearchRepository {

    private final MongoTemplate mongoTemplate;

    public List<Professionnel> rechercher(TypeProfessionnel type, String ville) {
        // un seul critère sur "type" : MongoDB n'accepte pas deux expressions sur le même champ
        Criteria criteres = (type != null ? Criteria.where("type").is(type) : Criteria.where("type").exists(true))
                .and("actif").is(true);
        if (ville != null && !ville.isBlank()) {
            criteres = criteres.and("ville").regex("^" + Pattern.quote(ville.trim()) + "$", "i");
        }
        Query query = new Query(criteres).with(Sort.by("nom", "prenom"));
        return mongoTemplate.find(query, Professionnel.class, "utilisateurs");
    }
}
