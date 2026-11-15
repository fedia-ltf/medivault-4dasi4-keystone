package tn.esprit.medivault.soindomicile.clients;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.soindomicile.exceptions.RegleMetierException;

@Component
public class MedicamentClientFallback implements FallbackFactory<MedicamentClient> {
    @Override
    public MedicamentClient create(Throwable cause) {
        return vente -> {
            if (cause instanceof FeignException.Conflict) {
                throw new RegleMetierException("Quota de médicaments sensibles atteint");     // 409
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "medicament-service indisponible : acceptation refusée");                   // 503
        };
    }
}
