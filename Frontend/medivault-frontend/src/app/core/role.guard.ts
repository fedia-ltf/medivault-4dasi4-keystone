import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

// Réserve une route à certains rôles ; les autres sont renvoyés vers leur tableau de bord.
// (La vraie protection se fait côté back avec Keycloak : ceci masque seulement les écrans.)
export const roleGuard = (...roles: Role[]): CanActivateFn => () => {
  const role = inject(AuthService).utilisateur()?.role;
  return !role || roles.includes(role) ? true : inject(Router).createUrlTree(['/app/dashboard']);
};
