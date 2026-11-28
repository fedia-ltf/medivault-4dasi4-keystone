import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';

export type Role = 'PATIENT' | 'MEDECIN' | 'PHARMACIEN' | 'LABORANTIN' | 'ADMIN';

export interface Utilisateur {
  nom: string;
  email: string;
  role: Role;
  /** Identifiant du compte dans le user-service */
  id?: string;
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

/** Ce que renvoie POST /api/auth/login (user-service) */
interface ProfilApi { id: string; nom: string; prenom: string; email: string; role: Role }
interface SessionApi { accessToken: string; refreshToken: string; expiresIn: number; refreshExpiresIn: number; utilisateur: ProfilApi }
/** Les jetons gardés dans le navigateur */
interface Session { accessToken: string; refreshToken: string; expireA: number }

export interface Inscription { nom: string; prenom: string; email: string; telephone?: string; motDePasse: string }

const CLE = 'mv.utilisateur';
const CLE_SESSION = 'mv.session';

// Annuaire des comptes de démonstration, utilisé seulement par connexion() (mode hors ligne, sans serveur de connexion).
// La connexion normale est connexionReelle() : Keycloak vérifie le mot de passe et le rôle vient du compte.
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

// Authentification : le mot de passe est vérifié par Keycloak (via POST /api/auth/login du user-service),
// le rôle vient du compte.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly courant = signal<Utilisateur | null>(this.lire<Utilisateur>(CLE));
  private readonly session = signal<Session | null>(this.lire<Session>(CLE_SESSION));
  private renouvellement$: Observable<boolean> | null = null;

  readonly utilisateur = this.courant.asReadonly();
  readonly connecte = computed(() => this.courant() !== null);
  readonly role = computed(() => this.courant()?.role ?? null);
  readonly id = computed(() => this.courant()?.id ?? null);
  readonly nom = computed(() => this.courant()?.nom ?? '');
  readonly initiales = computed(() =>
    (this.courant()?.nom ?? '?').split(' ').map(p => p[0]).join('').slice(0, 2).toUpperCase()
  );

  // ---------------------------------------------------------------- connexion réelle (Keycloak)

  /** Email + mot de passe : Keycloak vérifie, le user-service renvoie les jetons et le profil avec son rôle. */
  connexionReelle(email: string, motDePasse: string, resterConnecte: boolean): Observable<Utilisateur> {
    return this.http.post<SessionApi>('/api/auth/login', { email, motDePasse }).pipe(
      tap(s => this.ouvrir(s, resterConnecte)),
      map(() => this.courant()!)
    );
  }

  /** Crée un compte patient. La connexion se fait ensuite avec connexionReelle. */
  inscription(donnees: Inscription): Observable<unknown> {
    return this.http.post('/api/auth/inscription', donnees);
  }

  /** « Mot de passe oublié » : le serveur envoie l'e-mail (même réponse si le compte n'existe pas). */
  motDePasseOublie(email: string, canal: 'EMAIL' | 'WHATSAPP' = 'EMAIL'): Observable<string> {
    return this.http.post<{ message: string }>('/api/auth/mot-de-passe-oublie', { email, canal }).pipe(map(r => r.message));
  }

  /** Choisit un nouveau mot de passe avec le lien reçu par e-mail (à usage unique). */
  reinitialiserMotDePasse(token: string, nouveauMotDePasse: string): Observable<string> {
    return this.http.post<{ message: string }>('/api/auth/reinitialiser-mot-de-passe', { token, nouveauMotDePasse }).pipe(map(r => r.message));
  }

  // ---------------------------------------------------------------- jetons

  /** Le jeton à envoyer dans l'en-tête « Authorization » */
  jeton(): string | null {
    return this.session()?.accessToken ?? null;
  }

  aUneSession(): boolean {
    return this.session() !== null;
  }

  /** Le jeton expire dans moins de 20 secondes (ou l'est déjà) */
  bientotExpire(): boolean {
    const s = this.session();
    return !!s && Date.now() > s.expireA - 20_000;
  }

  /** Demande de nouveaux jetons avec le refreshToken. Une seule demande à la fois, partagée. */
  rafraichir(): Observable<boolean> {
    const actuelle = this.session();
    if (!actuelle) return of(false);
    this.renouvellement$ ??= this.http.post<SessionApi>('/api/auth/refresh', { refreshToken: actuelle.refreshToken }).pipe(
      tap(s => this.ouvrir(s, this.estPersistant())),
      map(() => true),
      catchError(() => {
        this.effacer();
        this.router.navigateByUrl('/login');
        return of(false);
      }),
      finalize(() => (this.renouvellement$ = null)),
      shareReplay(1)
    );
    return this.renouvellement$;
  }

  deconnexion() {
    const s = this.session();
    if (s) {
      // on ferme aussi la session côté Keycloak ; si ça échoue, la déconnexion locale a quand même lieu
      this.http.post('/api/auth/logout', { refreshToken: s.refreshToken }).pipe(catchError(() => of(null))).subscribe();
    }
    this.effacer();
  }

  // ---------------------------------------------------------------- rôles et redirection

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

  // ---------------------------------------------------------------- mode hors ligne (démonstration)

  /**
   * Connexion automatique SANS serveur de connexion : le rôle et l'identifiant sont déduits de l'e-mail
   * (annuaire de démonstration) ou d'un jeton JWT fourni. Pas de jeton Keycloak, pas de renouvellement.
   * La page de connexion utilise connexionReelle().
   */
  connexion(email: string, tokenOptionnel?: string): Utilisateur {
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

  // ---------------------------------------------------------------- stockage

  private ouvrir(s: SessionApi, persistant: boolean) {
    const u: Utilisateur = {
      id: s.utilisateur.id,
      nom: `${s.utilisateur.prenom} ${s.utilisateur.nom}`.trim(),
      email: s.utilisateur.email,
      role: s.utilisateur.role
    };
    const session: Session = { accessToken: s.accessToken, refreshToken: s.refreshToken, expireA: Date.now() + s.expiresIn * 1000 };
    this.ecrire(CLE, u, persistant);
    this.ecrire(CLE_SESSION, session, persistant);
    this.courant.set(u);
    this.session.set(session);
  }

  private sauvegarderEtEmettre(u: Utilisateur) {
    this.effacer();
    this.ecrire(CLE, u, true);
    this.courant.set(u);
  }

  private effacer() {
    for (const stock of [localStorage, sessionStorage]) {
      try { stock.removeItem(CLE); stock.removeItem(CLE_SESSION); } catch { /* stockage indisponible */ }
    }
    this.courant.set(null);
    this.session.set(null);
  }

  /** « Rester connecté » : localStorage (survit à la fermeture du navigateur) sinon sessionStorage */
  private ecrire(cle: string, valeur: unknown, persistant: boolean) {
    const [garde, autre] = persistant ? [localStorage, sessionStorage] : [sessionStorage, localStorage];
    try {
      garde.setItem(cle, JSON.stringify(valeur));
      autre.removeItem(cle);
    } catch { /* stockage indisponible */ }
  }

  private estPersistant(): boolean {
    try { return localStorage.getItem(CLE_SESSION) !== null; } catch { return false; }
  }

  private lire<T>(cle: string): T | null {
    for (const stock of [localStorage, sessionStorage]) {
      try {
        const brut = stock.getItem(cle);
        if (brut) return JSON.parse(brut) as T;
      } catch { /* valeur illisible : on l'ignore */ }
    }
    return null;
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
