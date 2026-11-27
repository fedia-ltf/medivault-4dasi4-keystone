import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { Hologram } from '../../shared/hologram';
import { AuthService, ROLES, Role } from '../../core/auth.service';

// Mot de passe des comptes de démonstration créés dans Keycloak (voir Backend/user-service/keycloak)
const MOT_DE_PASSE_DEMO = 'Medivault2026!';
const COMPTES_DEMO: Record<Role, string> = {
  MEDECIN: 'karim.trabelsi@medivault.tn',
  PATIENT: 'ahmed.benali@medivault.tn',
  PHARMACIEN: 'pharmacie.centrale@medivault.tn',
  LABORANTIN: 'contact@biolab.tn',
  ADMIN: 'admin@medivault.tn'
};

@Component({
  selector: 'mv-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, Logo, Hologram],
  templateUrl: './login.html'
})
export class LoginPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  protected readonly roles = ROLES;
  /** Sert seulement à remplir un compte de démonstration (et au mode démonstration) : le vrai rôle vient du compte. */
  protected role = signal<Role>('MEDECIN');
  protected roleIndex = computed(() => this.roles.findIndex(r => r.value === this.role()));

  protected email = '';
  protected motDePasse = '';
  protected resterConnecte = true;
  protected voirMotDePasse = signal(false);
  protected chargement = signal(false);
  protected succes = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);
  /** Vrai quand le serveur de connexion ne répond pas : la connexion est alors simulée */
  protected modeDemo = signal(false);

  protected connexion() {
    this.erreur.set('');
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email);
    if (!emailOk || !this.motDePasse) {
      this.refuser(!emailOk ? 'Adresse e-mail invalide.' : 'Entrez votre mot de passe.');
      return;
    }
    this.chargement.set(true);
    // Le mot de passe est vérifié par Keycloak ; le rôle vient du compte
    this.auth.connexionReelle(this.email.trim(), this.motDePasse, this.resterConnecte).subscribe({
      next: () => this.reussir(),
      error: (e: HttpErrorResponse) => this.echec(e)
    });
  }

  protected remplirDemo() {
    this.email = COMPTES_DEMO[this.role()];
    this.motDePasse = MOT_DE_PASSE_DEMO;
  }

  private reussir() {
    this.chargement.set(false);
    this.succes.set(true);
    setTimeout(() => this.router.navigateByUrl('/app/dashboard'), 900);
  }

  private echec(e: HttpErrorResponse) {
    // serveur de connexion absent ou Keycloak éteint : on bascule en mode démonstration (connexion simulée)
    if (e.status === 0 || e.status === 404 || e.status === 502 || e.status === 503) {
      this.modeDemo.set(true);
      setTimeout(() => {
        this.auth.connexion(this.email.trim(), this.role());
        this.reussir();
      }, 600);
      return;
    }
    this.chargement.set(false);
    if (e.status === 401 || e.status === 403) {
      this.refuser(e.error?.detail ?? 'Email ou mot de passe incorrect.');
    } else if (e.status === 400) {
      this.refuser(Object.values<string>(e.error?.erreurs ?? {})[0] ?? 'Données invalides.');
    } else {
      this.refuser('Connexion impossible pour le moment. Réessayez.');
    }
  }

  private refuser(message: string) {
    this.chargement.set(false);
    this.erreur.set(message);
    this.secoue.set(true);
    setTimeout(() => this.secoue.set(false), 650);
  }
}
