import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { roleGuard } from './medicaments.guard';

// Routes du module Médicaments (préfixe : /app/medicaments).
// Chaque rôle a son espace : l'adresse /app/medicaments oriente vers le bon.
export const MEDICAMENTS_ROUTES: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => {
      const role = inject(AuthService).utilisateur()?.role;
      return role === 'ADMIN' ? 'gestion' : role === 'PHARMACIEN' ? 'pharmacien' : 'catalogue';
    }
  },
  {
    path: 'gestion',
    canActivate: [roleGuard('ADMIN')],
    data: { title: 'Gestion des médicaments' },
    loadComponent: () => import('./pages/admin/medicaments-admin-page').then(m => m.MedicamentsAdminPage)
  },
  {
    path: 'pharmacien',
    canActivate: [roleGuard('PHARMACIEN', 'ADMIN')],
    data: { title: 'Espace pharmacien' },
    loadComponent: () => import('./pages/pharmacien/pharmacien-page').then(m => m.PharmacienPage)
  },
  {
    path: 'catalogue',
    data: { title: 'Catalogue des médicaments' },
    loadComponent: () => import('./pages/consultation/consultation-page').then(m => m.ConsultationPage)
  }
];
