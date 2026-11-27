import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { Hologram } from '../../shared/hologram';
import { AuthService } from '../../core/auth.service';

// Connexion réelle : le mot de passe est vérifié par Keycloak, le rôle vient du compte (plus de compte de démonstration ici)
@Component({
  selector: 'mv-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, Logo, Hologram],
  templateUrl: './login.html'
})
export class LoginPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  protected email = '';
  protected motDePasse = '';
  protected resterConnecte = true;
  protected voirMotDePasse = signal(false);
  protected chargement = signal(false);
  protected succes = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);

  protected connexion() {
    this.erreur.set('');
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email);
    if (!emailOk || !this.motDePasse) {
      this.refuser(!emailOk ? 'Adresse e-mail invalide.' : 'Entrez votre mot de passe.');
      return;
    }
    this.chargement.set(true);
    this.auth.connexionReelle(this.email.trim(), this.motDePasse, this.resterConnecte).subscribe({
      next: () => this.reussir(),
      error: (e: HttpErrorResponse) => this.echec(e)
    });
  }

  private reussir() {
    this.chargement.set(false);
    this.succes.set(true);
    setTimeout(() => this.router.navigateByUrl('/app/dashboard'), 900);
  }

  private echec(e: HttpErrorResponse) {
    this.chargement.set(false);
    if (e.status === 401 || e.status === 403) {
      this.refuser(e.error?.detail ?? 'Email ou mot de passe incorrect.');
    } else if (e.status === 400) {
      this.refuser(Object.values<string>(e.error?.erreurs ?? {})[0] ?? 'Données invalides.');
    } else if (e.status === 0 || e.status === 404 || e.status === 502 || e.status === 503) {
      this.refuser('Serveur de connexion injoignable (user-service ou Keycloak éteint).');
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
