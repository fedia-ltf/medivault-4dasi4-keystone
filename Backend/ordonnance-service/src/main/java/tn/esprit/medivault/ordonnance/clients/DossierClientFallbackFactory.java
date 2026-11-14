package tn.esprit.medivault.ordonnance.clients;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.ordonnance.exceptions.ServiceIndisponibleException;

@Component
public class DossierClientFallbackFactory implements FallbackFactory<DossierClient> {

    @Override
    public DossierClient create(Throwable cause) {
        return patientId -> {
            throw new ServiceIndisponibleException("dossier-service indisponible : " + cause.getMessage());
        };
    }
}