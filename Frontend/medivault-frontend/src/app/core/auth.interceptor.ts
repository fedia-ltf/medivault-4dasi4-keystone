import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

/**
 * Intercepteur HTTP : propage automatiquement l'identité et le rôle certifié
 * de l'utilisateur connecté vers les microservices via les en-têtes sécurisés.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const utilisateur = auth.utilisateur();

  if (utilisateur) {
    const headers: Record<string, string> = {
      'X-User-Id': utilisateur.id,
      'X-User-Role': utilisateur.role
    };

    if (utilisateur.token) {
      headers['Authorization'] = `Bearer ${utilisateur.token}`;
    }

    const authReq = req.clone({
      setHeaders: headers
    });
    return next(authReq);
  }

  return next(req);
};
