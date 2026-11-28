import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

export type Role = 'PATIENT' | 'MEDECIN' | 'PHARMACIEN' | 'LABORANTIN' | 'ADMIN';

export interface Utilisateur {
  id: string;
  nom: string;
  email: string;
  role: Role;
  etablissement?: string;
  token?: string;
}

export const ROLES: { value: Role; label: string; icon: string }[] = [
  { value: 'MEDECIN', label: 'Médecin', icon: 'stethoscope' },
  { value: 'PATIENT', label: 'Patient', icon: 'user' },
  { value: 'PHARMACIEN', label: 'Pharmacien', icon: 'cross' },
  { value: 'LABORANTIN', label: 'Laborantin', icon: 'flask' },
  { value: 'ADMIN', label: 'Administrateur', icon: 'shield' }
];

const CLE = 'mv.utilisateur';

// Annuaire officiel des comptes MediVault (identique aux données de démonstration du user-service)
const ANNUAIRE_UTILISATEURS: Record<string, { id: string; nom: string; role: Role; etablissement?: string }> = {
  'ahmed.benali@medivault.tn': { id: 'patient-ahmed', nom: 'Ahmed Ben Ali', role: 'PATIENT' },
  'ahmed.trabelsi@medivault.tn': { id: 'patient-ahmed', nom: 'Ahmed Ben Ali', role: 'PATIENT' },
  'karim.trabelsi@medivault.tn': { id: 'medecin-karim', nom: 'Dr Karim Trabelsi', role: 'MEDECIN', etablissement: 'Cabinet Dr Karim Trabelsi' },
  'karim.benali@medivault.tn': { id: 'medecin-karim', nom: 'Dr Karim Trabelsi', role: 'MEDECIN', etablissement: 'Cabinet Dr Karim Trabelsi' },
  'contact@biolab.tn': { id: 'labo-biolab', nom: 'Laboratoire BioLab', role: 'LABORANTIN', etablissement: 'Laboratoire BioLab' },
  'taha.labo@medivault.tn': { id: 'labo-biolab', nom: 'Laboratoire BioLab', role: 'LABORANTIN', etablissement: 'Laboratoire BioLab' },
  'pharmacie.centrale@medivault.tn': { id: 'pharmacie-centrale', nom: 'Pharmacie Centrale', role: 'PHARMACIEN', etablissement: 'Pharmacie Centrale' },
  'aziz.pharma@medivault.tn': { id: 'pharmacie-centrale', nom: 'Pharmacie Centrale', role: 'PHARMACIEN', etablissement: 'Pharmacie Centrale' },
  'admin@medivault.tn': { id: 'admin', nom: 'Admin MediVault', role: 'ADMIN' }
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  private router = inject(Router);
  private readonly courant = signal<Utilisateur | null>(this.lire());

  readonly utilisateur = this.courant.asReadonly();
  readonly connecte = computed(() => this.courant() !== null);
  readonly role = computed(() => this.courant()?.role ?? null);
  readonly id = computed(() => this.courant()?.id ?? null);
  readonly nom = computed(() => this.courant()?.nom ?? '');
  readonly initiales = computed(() =>
    (this.courant()?.nom ?? '?').split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase()
  );

  /**
   * Connexion automatique : le rôle et l'identifiant sont déduits
   * dynamiquement sans intervention de l'utilisateur.
   */
  connexion(email: string, tokenOptionnel?: string) {
    const emailNettoye = email.trim().toLowerCase();
    let utilisateurDetecte: Utilisateur;

    // 1. Si un token JWT est fourni, on extrait les informations du payload
    if (tokenOptionnel) {
      const payloadJwt = this.extrairePayloadJwt(tokenOptionnel);
      if (payloadJwt) {
        utilisateurDetecte = {
          id: payloadJwt.sub || payloadJwt.preferred_username || 'user-' + Date.now(),
          nom: payloadJwt.name || emailNettoye.split('@')[0],
          email: emailNettoye,
          role: this.detecterRoleDepuisJwt(payloadJwt),
          token: tokenOptionnel
        };
        this.sauvegarderEtEmettre(utilisateurDetecte);
        return utilisateurDetecte;
      }
    }

    // 2. Détection automatique via l'annuaire MediVault
    const entree = ANNUAIRE_UTILISATEURS[emailNettoye];
    if (entree) {
      utilisateurDetecte = {
        id: entree.id,
        nom: entree.nom,
        email: emailNettoye,
        role: entree.role,
        etablissement: entree.etablissement
      };
    } else {
      // 3. Détection intelligente basée sur le contexte de l'adresse e-mail
      const roleDetecte = this.devinerRole(emailNettoye);
      const nomFormate = emailNettoye.split('@')[0]
        .replace(/[._-]+/g, ' ')
        .split(' ')
        .map(w => w.charAt(0).toUpperCase() + w.slice(1))
        .join(' ');

      utilisateurDetecte = {
        id: `${roleDetecte.toLowerCase()}-${Date.now().toString().slice(-4)}`,
        nom: (roleDetecte === 'MEDECIN' ? 'Dr ' : '') + nomFormate,
        email: emailNettoye,
        role: roleDetecte
      };
    }

    this.sauvegarderEtEmettre(utilisateurDetecte);
    return utilisateurDetecte;
  }

  deconnexion() {
    localStorage.removeItem(CLE);
    this.courant.set(null);
  }

  /**
   * Détermine la route de destination selon le rôle de l'utilisateur
   */
  calculerRouteParRole(role?: Role | null): string {
    const r = role ?? this.role();
    switch (r) {
      case 'PATIENT':
        return '/app/rendez-vous';
      case 'MEDECIN':
        return '/app/rendez-vous';
      case 'LABORANTIN':
        return '/app/rendez-vous';
      case 'PHARMACIEN':
        return '/app/pharmacie';
      case 'ADMIN':
        return '/app/dashboard';
      default:
        return '/app/acces-refuse';
    }
  }

  /**
   * Redirection automatique vers le tableau de bord après connexion
   */
  redirectionApresConnexion() {
    this.router.navigateByUrl('/app/dashboard');
  }

  hasRole(...rolesAutorises: Role[]): boolean {
    const r = this.role();
    return r !== null && rolesAutorises.includes(r);
  }

  private sauvegarderEtEmettre(u: Utilisateur) {
    localStorage.setItem(CLE, JSON.stringify(u));
    this.courant.set(u);
  }

  private lire(): Utilisateur | null {
    try {
      const data = localStorage.getItem(CLE);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  }

  private extrairePayloadJwt(token: string): any {
    try {
      const parties = token.split('.');
      if (parties.length !== 3) return null;
      return JSON.parse(atob(parties[1]));
    } catch {
      return null;
    }
  }

  private detecterRoleDepuisJwt(payload: any): Role {
    const roles: string[] = [
      ...(payload.realm_access?.roles || []),
      ...(payload.roles || []),
      ...(payload.resource_access?.['medivault']?.roles || [])
    ].map((r: string) => r.toUpperCase());

    if (roles.includes('MEDECIN') || roles.includes('DOCTOR')) return 'MEDECIN';
    if (roles.includes('LABORANTIN') || roles.includes('LAB') || roles.includes('LABORATOIRE')) return 'LABORANTIN';
    if (roles.includes('PHARMACIEN') || roles.includes('PHARMACY')) return 'PHARMACIEN';
    if (roles.includes('ADMIN') || roles.includes('ADMINISTRATEUR')) return 'ADMIN';
    return 'PATIENT';
  }

  private devinerRole(email: string): Role {
    if (email.includes('medecin') || email.includes('dr.') || email.includes('doctor')) return 'MEDECIN';
    if (email.includes('labo') || email.includes('analyse')) return 'LABORANTIN';
    if (email.includes('pharma')) return 'PHARMACIEN';
    if (email.includes('admin')) return 'ADMIN';
    return 'PATIENT';
  }
}
