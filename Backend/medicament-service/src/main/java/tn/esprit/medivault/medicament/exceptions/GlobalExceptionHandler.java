package tn.esprit.medivault.medicament.exceptions;

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
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

// GESTION CENTRALE DES ERREURS.
// Toutes les erreurs sont renvoyées au format Problem Details (RFC 9457) :
// { "type", "title", "status", "detail", "instance" } + propriétés éventuelles
// @RestControllerAdvice : cette classe "surveille" tous les controllers ; quand une exception
//   sort d'un controller, la bonne méthode ci-dessous est appelée et renvoie une réponse propre.
// extends ResponseEntityExceptionHandler : réutilise la gestion Spring des erreurs classiques,
//   et on redéfinit (@Override) celles qu'on veut personnaliser.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // @ExceptionHandler : "si cette exception arrive, appelle cette méthode" -> 404
    @ExceptionHandler(MedicamentNotFoundException.class)
    public ProblemDetail handleNotFound(MedicamentNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Médicament introuvable");
        return problem;
    }

    // Erreurs de validation (@Valid) : on ajoute le détail des champs invalides -> 400
    // ex. "erreurs": { "nom_commercial": "Le nom commercial est obligatoire" }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.put(e.getField(), e.getDefaultMessage()));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Données invalides");
        problem.setTitle("Requête invalide");
        problem.setProperty("erreurs", erreurs);
        return ResponseEntity.badRequest().body(problem);
    }

    // JSON mal formé ou valeur d'enum inconnue (ex. forme = "CAPSULE") -> 400
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Corps de la requête illisible ou valeur non autorisée");
        problem.setTitle("Requête invalide");
        return ResponseEntity.badRequest().body(problem);
    }

    // Filet de sécurité : toute autre erreur imprévue -> 500, sans montrer les détails au client
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAutres(Exception ex) {
        log.error("Erreur inattendue", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erreur interne du serveur");
        problem.setTitle("Erreur interne");
        return problem;
    }
}
