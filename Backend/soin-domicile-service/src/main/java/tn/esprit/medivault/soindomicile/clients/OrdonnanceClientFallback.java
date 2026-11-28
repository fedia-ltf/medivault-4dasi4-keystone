package tn.esprit.medivault.soindomicile.clients;

import feign.FeignException;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.medivault.soindomicile.exceptions.OrdonnanceInvalideException;

@Component
public class OrdonnanceClientFallback implements FallbackFactory<OrdonnanceClient> {
    @Override
    public OrdonnanceClient create(Throwable cause) {
        return code -> {
            if (cause instanceof FeignException.NotFound) {
                throw new OrdonnanceInvalideException("Ordonnance " + code + " introuvable");   // 422
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "ordonnance-service indisponible : soin refusé, réessayez plus tard");     // 503
        };
    }
}
