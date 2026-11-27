import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Routes qui ne demandent pas de jeton (connexion, inscription, mot de passe oublié…) */
const PUBLIQUES = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout', '/api/auth/inscription', '/api/auth/mot-de-passe-oublie'];

// Ajoute « Authorization: Bearer <jeton> » à chaque appel de l'API quand quelqu'un est connecté par Keycloak.
// Si le jeton a expiré (401), il est renouvelé une fois avec le refreshToken, puis la requête est rejouée.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith('/api/') || PUBLIQUES.some(p => req.url.startsWith(p)) || !auth.aUneSession()) {
    return next(req);
  }
  const avec = (jeton: string | null) => (jeton ? req.clone({ setHeaders: { Authorization: `Bearer ${jeton}` } }) : req);
  const envoyer = () => next(avec(auth.jeton())).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status !== 401) return throwError(() => e);
      return auth.rafraichir().pipe(switchMap(ok => (ok ? next(avec(auth.jeton())) : throwError(() => e))));
    })
  );
  // jeton bientôt expiré : on le renouvelle avant l'appel plutôt que d'attendre un 401
  return auth.bientotExpire() ? auth.rafraichir().pipe(switchMap(() => envoyer())) : envoyer();
};
