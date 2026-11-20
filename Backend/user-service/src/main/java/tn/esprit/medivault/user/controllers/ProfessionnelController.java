package tn.esprit.medivault.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.medivault.user.dto.DisponibilitesResponse;
import tn.esprit.medivault.user.dto.SignatureResponse;
import tn.esprit.medivault.user.dto.UtilisateurResponse;
import tn.esprit.medivault.user.entities.TypeProfessionnel;
import tn.esprit.medivault.user.services.IProfessionnelService;

import java.util.List;

@RestController
@RequestMapping("/api/professionnels")
@RequiredArgsConstructor
@Tag(name = "Professionnels", description = "Médecins, pharmacies et laboratoires : recherche, horaires, signature")
public class ProfessionnelController {

    private final IProfessionnelService professionnelService;

    @GetMapping
    @Operation(summary = "Rechercher des professionnels actifs par type et par ville")
    @ApiResponse(responseCode = "200", description = "Liste (éventuellement vide)")
    @ApiResponse(responseCode = "400", description = "Type inconnu")
    public List<UtilisateurResponse> rechercher(@RequestParam(required = false) TypeProfessionnel type,
                                                @RequestParam(required = false) String ville) {
        return professionnelService.rechercher(type, ville);
    }

    @GetMapping("/{id}/disponibilites")
    @Operation(summary = "Jours et heures de travail (appelé par rendezvous-service)")
    @ApiResponse(responseCode = "200", description = "Horaires du médecin ou du laboratoire")
    @ApiResponse(responseCode = "404", description = "Professionnel introuvable ou sans agenda")
    public DisponibilitesResponse disponibilites(@PathVariable String id) {
        return professionnelService.disponibilites(id);
    }

    @GetMapping("/{id}/signature")
    @Operation(summary = "Signature du médecin (appelé par ordonnance-service)")
    @ApiResponse(responseCode = "200", description = "Signature en data URL base64")
    @ApiResponse(responseCode = "404", description = "Médecin introuvable ou signature absente")
    public SignatureResponse signature(@PathVariable String id) {
        return professionnelService.signature(id);
    }

    @PutMapping(value = "/{id}/signature", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Enregistrer ou remplacer la signature du médecin (PNG ou JPEG, 200 Ko max)")
    @ApiResponse(responseCode = "200", description = "Signature enregistrée")
    @ApiResponse(responseCode = "400", description = "Fichier vide, trop lourd ou format non autorisé")
    @ApiResponse(responseCode = "404", description = "Médecin introuvable")
    public SignatureResponse enregistrerSignature(@PathVariable String id,
                                                  @RequestPart("fichier") MultipartFile fichier) {
        return professionnelService.enregistrerSignature(id, fichier);
    }
}
