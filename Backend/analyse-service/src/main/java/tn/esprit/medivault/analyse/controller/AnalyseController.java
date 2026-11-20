package tn.esprit.medivault.analyse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.medivault.analyse.dto.AnalyseResponse;
import tn.esprit.medivault.analyse.dto.ResultatRequest;
import tn.esprit.medivault.analyse.enums.StatutAnalyse;
import tn.esprit.medivault.analyse.service.IAnalyseService;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
@Tag(name = "Analyses", description = "Analyses demandées en consultation et résultats du laboratoire")
public class AnalyseController {

    private final IAnalyseService analyseService;

    @GetMapping
    @Operation(summary = "Analyses à traiter par un laboratoire (filtres statut et laboratoireId facultatifs)")
    @ApiResponse(responseCode = "200", description = "Liste (éventuellement vide)")
    @ApiResponse(responseCode = "400", description = "Statut inconnu")
    public List<AnalyseResponse> rechercher(@RequestParam(required = false) StatutAnalyse statut,
                                            @RequestParam(required = false) String laboratoireId) {
        return analyseService.rechercher(statut, laboratoireId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Une analyse (lue aussi par rendezvous-service pour vérifier une prescription)")
    @ApiResponse(responseCode = "200", description = "Analyse trouvée")
    @ApiResponse(responseCode = "404", description = "Analyse introuvable")
    public AnalyseResponse trouver(@PathVariable Long id) {
        return analyseService.trouverParId(id);
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Analyses d'un patient, les plus récentes d'abord")
    @ApiResponse(responseCode = "200", description = "Liste (éventuellement vide)")
    public List<AnalyseResponse> analysesDuPatient(@PathVariable String patientId) {
        return analyseService.analysesDuPatient(patientId);
    }

    @PostMapping("/{id}/resultats")
    @Operation(summary = "Saisir le résultat (RG7, RG10, justification d'accès vérifiée auprès de dossier-service)")
    @ApiResponse(responseCode = "201", description = "Résultat enregistré, analyse TERMINEE, ResultatDisponible publié")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @ApiResponse(responseCode = "403", description = "Laboratoire non affecté (RG10) ou accès retiré / expiré")
    @ApiResponse(responseCode = "404", description = "Analyse introuvable")
    @ApiResponse(responseCode = "409", description = "Analyse pas EN_COURS (RG7)")
    @ApiResponse(responseCode = "503", description = "dossier-service indisponible : saisie refusée (fail-closed)")
    public ResponseEntity<AnalyseResponse> saisirResultat(@PathVariable Long id,
                                                          @Valid @RequestBody ResultatRequest request) {
        AnalyseResponse terminee = analyseService.saisirResultat(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/analyses/{id}").buildAndExpand(terminee.id()).toUri();
        return ResponseEntity.created(location).body(terminee);
    }
}
