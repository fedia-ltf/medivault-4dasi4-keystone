import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from '../../core/auth.service';

// Réserve une page du module à certains rôles. Les autres sont renvoyés vers /app/soin-domicile,
// qui les oriente vers leur espace (même principe que le module Médicaments).
// La vraie protection se fera côté back avec Keycloak : ceci masque seulement les écrans.
export const roleGuard = (...roles: Role[]): CanActivateFn => () => {
  const role = inject(AuthService).utilisateur()?.role;
  return role && roles.includes(role) ? true : inject(Router).createUrlTree(['/app/soin-domicile']);
};
