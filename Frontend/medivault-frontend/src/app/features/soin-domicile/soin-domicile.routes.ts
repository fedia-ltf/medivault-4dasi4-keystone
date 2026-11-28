import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { roleGuard } from './soin-domicile.guard';

// Routes du module Soin à domicile (préfixe : /app/soin-domicile).
//  - back-office  : /app/soin-domicile/gestion  -> administrateur (statistiques, toutes les demandes, infirmiers, plans)
//  - front-office : /app/soin-domicile/espace   -> patient, pharmacien, médecin
// L'adresse /app/soin-domicile oriente chacun vers le bon espace.
export const SOIN_DOMICILE_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => (inject(AuthService).utilisateur()?.role === 'ADMIN' ? 'gestion' : 'espace')
  },
  {
    path: 'gestion',
    canActivate: [roleGuard('ADMIN')],
    data: { title: 'Gestion du soin à domicile' },
    loadComponent: () => import('./pages/admin/soin-admin-page').then(m => m.SoinAdminPage)
  },
  {
    // pas de guard ici : la page affiche l'espace du rôle (ou un message pour les autres rôles)
    path: 'espace',
    data: { title: 'Soin à domicile' },
    loadComponent: () => import('./pages/soin-domicile-page').then(m => m.SoinDomicilePage)
  }
];
