import { Routes } from '@angular/router';
import { inject } from '@angular/core';
import { authGuard } from './core/auth.guard';
import { AuthService } from './core/auth.service';
import { roleGuard } from './core/role.guard';

// Routes principales. Chaque module garde ses propres routes dans
// features/<module>/<module>.routes.ts : ce fichier n'a pas à être modifié.
export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/landing/landing').then(m => m.LandingPage) },
  { path: 'login', loadComponent: () => import('./features/auth/login').then(m => m.LoginPage) },
  { path: 'inscription', loadComponent: () => import('./features/auth/inscription').then(m => m.InscriptionPage) },
  { path: 'mot-de-passe-oublie', loadComponent: () => import('./features/auth/mot-de-passe-oublie').then(m => m.MotDePasseOubliePage) },
  { path: 'reinitialiser-mot-de-passe', loadComponent: () => import('./features/auth/reinitialiser-mot-de-passe').then(m => m.ReinitialiserMotDePassePage) },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/shell').then(m => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        // Chaque rôle a son tableau de bord, protégé par son guard
        path: 'dashboard',
        children: [
          { path: '', pathMatch: 'full', redirectTo: () => (inject(AuthService).utilisateur()?.role ?? 'medecin').toLowerCase() },
          ...(['ADMIN', 'PHARMACIEN', 'MEDECIN', 'PATIENT', 'LABORANTIN'] as const).map(role => ({
            path: role.toLowerCase(),
            canActivate: [roleGuard(role)],
            data: { title: 'Tableau de bord' },
            loadComponent: () => import('./features/dashboard/dashboard').then(m => m.DashboardPage)
          }))
        ]
      },
      { path: 'medicaments', loadChildren: () => import('./features/medicaments/medicaments.routes').then(m => m.MEDICAMENTS_ROUTES) },
      { path: 'dossiers', loadChildren: () => import('./features/dossiers/dossiers.routes').then(m => m.DOSSIERS_ROUTES) },
      { path: 'ordonnances', loadChildren: () => import('./features/ordonnances/ordonnances.routes').then(m => m.ORDONNANCES_ROUTES) },
      { path: 'pharmacie', loadChildren: () => import('./features/pharmacie/pharmacie.routes').then(m => m.PHARMACIE_ROUTES) },
      { path: 'analyses', loadChildren: () => import('./features/analyses/analyses.routes').then(m => m.ANALYSES_ROUTES) },
      { path: 'rendez-vous', loadChildren: () => import('./features/rendez-vous/rendez-vous.routes').then(m => m.RENDEZ_VOUS_ROUTES) }
    ]
  },
  { path: '**', redirectTo: '' }
];
