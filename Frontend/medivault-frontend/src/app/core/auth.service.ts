import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, map, of, shareReplay, tap } from 'rxjs';

export type Role = 'PATIENT' | 'MEDECIN' | 'PHARMACIEN' | 'LABORANTIN' | 'ADMIN';

export interface Utilisateur {
  nom: string;
  email: string;
  role: Role;
  /** Identifiant du compte dans le user-service (absent en mode démonstration) */
  id?: string;
}

export const ROLES: { value: Role; label: string; icon: string }[] = [
  { value: 'MEDECIN', label: 'Médecin', icon: 'stethoscope' },
  { value: 'PATIENT', label: 'Patient', icon: 'user' },
  { value: 'PHARMACIEN', label: 'Pharmacien', icon: 'cross' },
  { value: 'LABORANTIN', label: 'Laborantin', icon: 'flask' },
  { value: 'ADMIN', label: 'Admin', icon: 'shield' }
];

/** Ce que renvoie POST /api/auth/login (user-service) */
interface ProfilApi { id: string; nom: string; prenom: string; email: string; role: Role }
interface SessionApi { accessToken: string; refreshToken: string; expiresIn: number; refreshExpiresIn: number; utilisateur: ProfilApi }
/** Les jetons gardés dans le navigateur */
interface Session { accessToken: string; refreshToken: string; expireA: number }

export interface Inscription { nom: string; prenom: string; email: string; telephone?: string; motDePasse: string }

const CLE = 'mv.utilisateur';
const CLE_SESSION = 'mv.session';

// Authentification : le mot de passe est vérifié par Keycloak (via POST /api/auth/login du user-service),
// le rôle vient du compte. Si le serveur de connexion n'est pas joignable, la page de connexion propose le
// mode démonstration (connexion simulée, sans jeton).
@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private router = inject(Router);

  private readonly courant = signal<Utilisateur | null>(this.lire<Utilisateur>(CLE));
  private readonly session = signal<Session | null>(this.lire<Session>(CLE_SESSION));
  private renouvellement$: Observable<boolean> | null = null;

  readonly utilisateur = this.courant.asReadonly();
  readonly connecte = computed(() => this.courant() !== null);
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
  motDePasseOublie(email: string): Observable<string> {
    return this.http.post<{ message: string }>('/api/auth/mot-de-passe-oublie', { email }).pipe(map(r => r.message));
  }

  /** Choisit un nouveau mot de passe avec le lien reçu par e-mail (à usage unique). */
  reinitialiserMotDePasse(token: string, nouveauMotDePasse: string): Observable<string> {
    return this.http.post<{ message: string }>('/api/auth/reinitialiser-mot-de-passe', { token, nouveauMotDePasse }).pipe(map(r => r.message));
  }

  // ---------------------------------------------------------------- jetons

  /** Le jeton à envoyer dans l'en-tête « Authorization » (absent en mode démonstration) */
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

  // ---------------------------------------------------------------- mode démonstration

  /** Connexion simulée (serveur de connexion absent) : pas de jeton, le rôle est celui qu'on choisit. */
  connexion(email: string, role: Role) {
    const brut = email.split('@')[0].replace(/[._-]+/g, ' ').trim() || 'Utilisateur';
    const nom = brut.split(' ').map(m => m.charAt(0).toUpperCase() + m.slice(1)).join(' ');
    this.effacer();
    this.ecrire(CLE, { nom, email, role } satisfies Utilisateur, true);
    this.courant.set({ nom, email, role });
  }

  deconnexion() {
    const s = this.session();
    if (s) {
      // on ferme aussi la session côté Keycloak ; si ça échoue, la déconnexion locale a quand même lieu
      this.http.post('/api/auth/logout', { refreshToken: s.refreshToken }).pipe(catchError(() => of(null))).subscribe();
    }
    this.effacer();
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
}
