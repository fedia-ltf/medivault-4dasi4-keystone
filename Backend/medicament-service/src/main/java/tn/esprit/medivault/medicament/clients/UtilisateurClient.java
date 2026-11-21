package tn.esprit.medivault.medicament.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import tn.esprit.medivault.medicament.dto.ExterneDtos;

// CLIENT FEIGN vers user-service (documentation : "Synchrone 2") : la pharmacie est-elle active ?
@FeignClient(name = "user-service", fallbackFactory = UtilisateurClientFallbackFactory.class)
public interface UtilisateurClient {

    // GET /api/utilisateurs/{id} : le compte avec son champ actif (true / false)
    @GetMapping("/api/utilisateurs/{id}")
    ExterneDtos.Utilisateur parId(@PathVariable("id") String id);
}