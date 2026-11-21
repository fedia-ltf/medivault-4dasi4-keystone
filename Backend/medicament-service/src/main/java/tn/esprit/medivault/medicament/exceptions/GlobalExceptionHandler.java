package tn.esprit.medivault.medicament.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// GESTION CENTRALE DES ERREURS.
// Toutes les erreurs sont renvoyées au format Problem Details (RFC 9457) :
// { "type", "title", "status", "detail", "instance" } + propriétés éventuelles.
// @RestControllerAdvice : cette classe "surveille" tous les controllers ; quand une exception
//   sort d'un controller, la bonne méthode ci-dessous est appelée et renvoie une réponse propre.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 404 : ce qu'on cherche n'existe pas
    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail handleNotFound(RessourceIntrouvableException ex) {
        return probleme(HttpStatus.NOT_FOUND, "Introuvable", ex.getMessage());
    }

    // 409 : doublon, ou opération impossible dans l'état actuel
    @ExceptionHandler(ConflitException.class)
    public ProblemDetail handleConflit(ConflitException ex) {
        return probleme(HttpStatus.CONFLICT, "Conflit", ex.getMessage());
    }

    // 409 aussi si la base refuse (contrainte d'unicité) : deux requêtes identiques en même temps
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrite(DataIntegrityViolationException ex) {
        log.warn("Contrainte de base violée : {}", ex.getMostSpecificCause().getMessage());
        return probleme(HttpStatus.CONFLICT, "Conflit", "Cette donnée existe déjà ou est encore utilisée.");
    }

    // 409 : deux personnes ont modifié le même quota en même temps
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleVerrou(ObjectOptimisticLockingFailureException ex) {
        return probleme(HttpStatus.CONFLICT, "Modification simultanée", "Cette donnée vient d'être modifiée par quelqu'un d'autre. Réessayez.");
    }

    // 503 : ordonnance-service ou user-service ne répond pas
    @ExceptionHandler(ServiceIndisponibleException.class)
    public ProblemDetail handleIndisponible(ServiceIndisponibleException ex) {
        log.warn("Service externe indisponible : {}", ex.getMessage());
        return probleme(HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible", ex.getMessage());
    }

    // 400 : erreurs de validation (@Valid) avec le détail des champs invalides
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(e -> erreurs.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Données invalides");
        problem.setProperty("erreurs", erreurs);
        return ResponseEntity.badRequest().body(problem);
    }

    // 400 : JSON mal formé, valeur d'enum inconnue (categorie = "AUTRE"), mois mal écrit...
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(probleme(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Corps de la requête illisible ou valeur non autorisée (catégorie, mois au format AAAA-MM...)"));
    }

    // Filet de sécurité : toute autre erreur imprévue -> 500, sans montrer les détails au client
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAutres(Exception ex) {
        log.error("Erreur inattendue", ex);
        return probleme(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Erreur interne du serveur");
    }

    private ProblemDetail probleme(HttpStatus statut, String titre, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statut, detail);
        problem.setTitle(titre);
        return problem;
    }
}