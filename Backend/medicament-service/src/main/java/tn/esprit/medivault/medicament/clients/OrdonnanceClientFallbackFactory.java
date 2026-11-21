package tn.esprit.medivault.medicament.clients;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.ExterneDtos;
import tn.esprit.medivault.medicament.exceptions.ObjetExterneIntrouvableException;
import tn.esprit.medivault.medicament.exceptions.ServiceIndisponibleException;

// PLAN B de l'appel vers ordonnance-service. Le "factory" reçoit la cause de l'échec, ce qui permet de distinguer :
//   - 404 : l'ordonnance n'existe pas -> règle métier (RG1), ce n'est pas une panne
//   - tout le reste (service arrêté, trop lent, circuit ouvert) -> 503 : pas de vérification, pas de vente
@Component
@Slf4j
public class OrdonnanceClientFallbackFactory implements FallbackFactory<OrdonnanceClient> {

    @Override
    public OrdonnanceClient create(Throwable cause) {
        return code -> {
            if (cause instanceof FeignException.NotFound) {
                throw new ObjetExterneIntrouvableException("Ordonnance introuvable : " + code);
            }
            log.warn("ordonnance-service indisponible ({})", cause.toString());
            throw new ServiceIndisponibleException("ordonnance-service", cause);
        };
    }
}