package tn.esprit.medivault.user.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// Toutes les erreurs sont renvoyées au format Problem Details (RFC 9457), comme dans medicament-service
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(UtilisateurNotFoundException.class)
    public ProblemDetail handleNotFound(UtilisateurNotFoundException ex) {
        return probleme(HttpStatus.NOT_FOUND, "Utilisateur introuvable", ex.getMessage());
    }

    @ExceptionHandler(EmailDejaUtiliseException.class)
    public ProblemDetail handleEmail(EmailDejaUtiliseException ex) {
        return probleme(HttpStatus.CONFLICT, "Email déjà utilisé", ex.getMessage());
    }

    // Course entre deux créations avec le même email : l'index unique MongoDB tranche
    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicate(DuplicateKeyException ex) {
        return probleme(HttpStatus.CONFLICT, "Email déjà utilisé", "Un compte existe déjà avec cet email");
    }

    @ExceptionHandler(DonneesInvalidesException.class)
    public ProblemDetail handleInvalide(DonneesInvalidesException ex) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ProblemDetail handleIdentifiants(IdentifiantsInvalidesException ex) {
        return probleme(HttpStatus.UNAUTHORIZED, "Identifiants invalides", ex.getMessage());
    }

    @ExceptionHandler(CompteInaccessibleException.class)
    public ProblemDetail handleCompteInaccessible(CompteInaccessibleException ex) {
        return probleme(HttpStatus.FORBIDDEN, "Compte inaccessible", ex.getMessage());
    }

    @ExceptionHandler(KeycloakIndisponibleException.class)
    public ProblemDetail handleKeycloak(KeycloakIndisponibleException ex) {
        log.error("Keycloak indisponible", ex);
        return probleme(HttpStatus.SERVICE_UNAVAILABLE, "Keycloak indisponible", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Valeur non autorisée pour le paramètre « " + ex.getName() + " »");
    }

    // Déjà gérée par ResponseEntityExceptionHandler : on surcharge pour renvoyer un 400 explicite
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                          HttpHeaders headers, HttpStatusCode status,
                                                                          WebRequest request) {
        return ResponseEntity.badRequest().body(probleme(HttpStatus.BAD_REQUEST, "Fichier trop volumineux",
                "La signature ne doit pas dépasser 200 Ko"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.put(e.getField(), e.getDefaultMessage()));
        ProblemDetail problem = probleme(HttpStatus.BAD_REQUEST, "Requête invalide", "Données invalides");
        problem.setProperty("erreurs", erreurs);
        return ResponseEntity.badRequest().body(problem);
    }

    // JSON mal formé ou valeur d'enum inconnue (ex. role = "INFIRMIER")
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        return ResponseEntity.badRequest().body(probleme(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Corps de la requête illisible ou valeur non autorisée"));
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAutres(Exception ex) {
        log.error("Erreur inattendue", ex);
        return probleme(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne", "Erreur interne du serveur");
    }

    private ProblemDetail probleme(HttpStatus status, String titre, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(titre);
        return problem;
    }
}
