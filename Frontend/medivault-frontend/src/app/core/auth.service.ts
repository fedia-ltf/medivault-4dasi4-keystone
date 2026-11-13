import { Injectable, computed, signal } from '@angular/core';

export type Role = 'PATIENT' | 'MEDECIN' | 'PHARMACIEN' | 'LABORANTIN' | 'ADMIN';

export interface Utilisateur {
  nom: string;
  email: string;
  role: Role;
}

export const ROLES: { value: Role; label: string; icon: string }[] = [
  { value: 'MEDECIN', label: 'Médecin', icon: 'stethoscope' },
  { value: 'PATIENT', label: 'Patient', icon: 'user' },
  { value: 'PHARMACIEN', label: 'Pharmacien', icon: 'cross' },
  { value: 'LABORANTIN', label: 'Laborantin', icon: 'flask' },
  { value: 'ADMIN', label: 'Admin', icon: 'shield' }
];

const CLE = 'mv.utilisateur';

// Authentification de démonstration.
// À remplacer par Keycloak (OAuth2 / JWT) lors de l'intégration de la sécurité.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly courant = signal<Utilisateur | null>(this.lire());

  readonly utilisateur = this.courant.asReadonly();
  readonly connecte = computed(() => this.courant() !== null);
  readonly initiales = computed(() =>
    (this.courant()?.nom ?? '?').split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase()
  );

  connexion(email: string, role: Role) {
    const brut = email.split('@')[0].replace(/[._-]+/g, ' ').trim() || 'Utilisateur';
    const nom = brut.split(' ').map(m => m.charAt(0).toUpperCase() + m.slice(1)).join(' ');
    const u: Utilisateur = { nom, email, role };
    localStorage.setItem(CLE, JSON.stringify(u));
    this.courant.set(u);
  }

  deconnexion() {
    localStorage.removeItem(CLE);
    this.courant.set(null);
  }

  private lire(): Utilisateur | null {
    try {
      return JSON.parse(localStorage.getItem(CLE) ?? 'null');
    } catch {
      return null;
    }
  }
}
