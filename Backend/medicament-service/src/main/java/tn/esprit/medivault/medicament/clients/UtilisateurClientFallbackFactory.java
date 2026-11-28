package tn.esprit.medivault.medicament.clients;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.medicament.dto.ExterneDtos;
import tn.esprit.medivault.medicament.exceptions.ObjetExterneIntrouvableException;
import tn.esprit.medivault.medicament.exceptions.ServiceIndisponibleException;

import java.util.List;

// PLAN B de l'appel vers user-service : compte inconnu (404) = pharmacie non active ; panne = 503.
@Component
@Slf4j
public class UtilisateurClientFallbackFactory implements FallbackFactory<UtilisateurClient> {

    @Override
    public UtilisateurClient create(Throwable cause) {
        return new UtilisateurClient() {
            @Override
            public ExterneDtos.Utilisateur parId(String id) {
                if (cause instanceof FeignException.NotFound) {
                    throw new ObjetExterneIntrouvableException("Compte introuvable : " + id);
                }
                log.warn("user-service indisponible ({})", cause.toString());
                throw new ServiceIndisponibleException("user-service", cause);
            }

            @Override
            public List<ExterneDtos.Utilisateur> professionnels(String type) {
                throw new ServiceIndisponibleException("user-service", cause);
            }
        };
    }
}
