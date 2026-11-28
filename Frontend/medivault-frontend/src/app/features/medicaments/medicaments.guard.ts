import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from '../../core/auth.service';

// Réserve une page de ce module à certains rôles. Les autres sont renvoyés vers /app/medicaments,
// qui les oriente vers leur propre espace.
export const roleGuard = (...roles: Role[]): CanActivateFn => () => {
  const role = inject(AuthService).utilisateur()?.role;
  return !role || roles.includes(role) ? true : inject(Router).createUrlTree(['/app/medicaments']);
};
