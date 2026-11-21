package tn.esprit.medivault.analyse.exception;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// Erreurs au format Problem Details (RFC 9457), comme les autres microservices.
// Le 503 du fallback (ResponseStatusException) est déjà converti par ResponseEntityExceptionHandler.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(AnalyseNotFoundException.class)
    public ProblemDetail handleNotFound(AnalyseNotFoundException ex) {
        return probleme(HttpStatus.NOT_FOUND, "Analyse introuvable", ex.getMessage());
    }

    @ExceptionHandler(StatutInvalideException.class)
    public ProblemDetail handleStatut(StatutInvalideException ex) {
        return probleme(HttpStatus.CONFLICT, "Statut de l'analyse incompatible", ex.getMessage());
    }

    @ExceptionHandler(AccesRefuseException.class)
    public ProblemDetail handleAcces(AccesRefuseException ex) {
        return probleme(HttpStatus.FORBIDDEN, "Accès refusé", ex.getMessage());
    }

    @ExceptionHandler(DonneesInvalidesException.class)
    public ProblemDetail handleInvalide(DonneesInvalidesException ex) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Valeur non autorisée pour le paramètre « " + ex.getName() + " »");
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
