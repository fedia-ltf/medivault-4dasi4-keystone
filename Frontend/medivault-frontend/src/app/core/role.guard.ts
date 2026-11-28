import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService, Role } from './auth.service';

/**
 * Garde de sécurité basé sur les rôles RBAC :
 * Vérifie que l'utilisateur est authentifié ET possède l'un des rôles autorisés.
 * Si le rôle est interdit ou inconnu, redirige vers la page d'accès refusé.
 *
 * S'écrit des deux façons : roleGuard('ADMIN', 'PHARMACIEN')  ou  roleGuard(['ADMIN', 'PHARMACIEN']).
 * (La vraie protection se fait côté back avec Keycloak : ceci masque les écrans.)
 */
export const roleGuard = (...rolesAutorises: (Role | Role[])[]): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = rolesAutorises.flat() as Role[];

  if (!auth.connecte()) {
    return router.createUrlTree(['/login']);
  }

  const roleUtilisateur = auth.role();
  if (roleUtilisateur && roles.includes(roleUtilisateur)) {
    return true;
  }

  // Rôle non autorisé ou manquant : affichage de la page Accès Refusé (403)
  return router.createUrlTree(['/app/acces-refuse']);
};
