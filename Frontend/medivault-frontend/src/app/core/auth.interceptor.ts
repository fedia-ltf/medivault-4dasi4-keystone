import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Routes qui ne demandent pas de jeton (connexion, inscription, mot de passe oublié…) */
const PUBLIQUES = ['/api/auth/login', '/api/auth/refresh', '/api/auth/logout', '/api/auth/inscription', '/api/auth/mot-de-passe-oublie', '/api/auth/reinitialiser-mot-de-passe'];

// Pour chaque appel de l'API quand quelqu'un est connecté :
//  - propage l'identité et le rôle vers les microservices (en-têtes X-User-Id et X-User-Role) ;
//  - ajoute « Authorization: Bearer <jeton> » quand la personne s'est connectée par Keycloak.
// Si le jeton a expiré (401), il est renouvelé une fois avec le refreshToken, puis la requête est rejouée.
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }

  const utilisateur = auth.utilisateur();
  const identite: Record<string, string> = {};
  if (utilisateur?.id) identite['X-User-Id'] = utilisateur.id;
  if (utilisateur?.role) identite['X-User-Role'] = utilisateur.role;

  if (PUBLIQUES.some(p => req.url.startsWith(p)) || !auth.aUneSession()) {
    // pas de session Keycloak (ou route publique) : seulement l'identité, et le jeton éventuel d'une connexion hors ligne
    const jetonHorsLigne = !PUBLIQUES.some(p => req.url.startsWith(p)) ? utilisateur?.token : undefined;
    const entetes = jetonHorsLigne ? { ...identite, Authorization: `Bearer ${jetonHorsLigne}` } : identite;
    return next(Object.keys(entetes).length ? req.clone({ setHeaders: entetes }) : req);
  }

  const avec = (jeton: string | null) => req.clone({ setHeaders: jeton ? { ...identite, Authorization: `Bearer ${jeton}` } : identite });
  const envoyer = () => next(avec(auth.jeton())).pipe(
    catchError((e: HttpErrorResponse) => {
      if (e.status !== 401) return throwError(() => e);
      return auth.rafraichir().pipe(switchMap(ok => (ok ? next(avec(auth.jeton())) : throwError(() => e))));
    })
  );
  // jeton bientôt expiré : on le renouvelle avant l'appel plutôt que d'attendre un 401
  return auth.bientotExpire() ? auth.rafraichir().pipe(switchMap(() => envoyer())) : envoyer();
};
