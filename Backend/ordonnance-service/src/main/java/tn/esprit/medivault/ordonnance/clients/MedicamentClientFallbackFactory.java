package tn.esprit.medivault.ordonnance.clients;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import tn.esprit.medivault.ordonnance.exceptions.ServiceIndisponibleException;

@Component
public class MedicamentClientFallbackFactory implements FallbackFactory<MedicamentClient> {

    @Override
    public MedicamentClient create(Throwable cause) {
        return request -> {
            throw new ServiceIndisponibleException("medicament-service indisponible : " + cause.getMessage());
        };
    }
}