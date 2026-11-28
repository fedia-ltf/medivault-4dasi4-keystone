import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { Hologram } from '../../shared/hologram';
import { AuthService, ROLES, Role } from '../../core/auth.service';

@Component({
  selector: 'mv-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, Logo, Hologram],
  templateUrl: './login.html'
})
export class LoginPage {
  protected auth = inject(AuthService);

  protected readonly roles = ROLES;
  protected role = signal<Role>('MEDECIN');
  protected roleIndex = computed(() => this.roles.findIndex(r => r.value === this.role()));

  protected email = 'karim.trabelsi@medivault.tn';
  protected motDePasse = 'medivault';
  protected voirMotDePasse = signal(false);
  protected chargement = signal(false);
  protected succes = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);
  protected roleDetecte = signal<string | null>(null);

  protected choisirRole(r: Role) {
    this.role.set(r);
    this.remplirCompteParRole(r);
  }

  protected remplirCompteParRole(r: Role) {
    const comptes: Record<Role, { email: string; pass: string }> = {
      MEDECIN: { email: 'karim.trabelsi@medivault.tn', pass: 'medivault' },
      PATIENT: { email: 'ahmed.benali@medivault.tn', pass: 'medivault' },
      LABORANTIN: { email: 'contact@biolab.tn', pass: 'medivault' },
      PHARMACIEN: { email: 'pharmacie.centrale@medivault.tn', pass: 'medivault' },
      ADMIN: { email: 'admin@medivault.tn', pass: 'medivault' }
    };
    const c = comptes[r];
    this.email = c.email;
    this.motDePasse = c.pass;
  }

  protected connexion() {
    this.erreur.set('');
    const emailOk = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email);
    if (!emailOk || this.motDePasse.length < 4) {
      this.erreur.set(!emailOk ? 'Adresse e-mail invalide.' : 'Le mot de passe doit contenir au moins 4 caractères.');
      this.secoue.set(true);
      setTimeout(() => this.secoue.set(false), 650);
      return;
    }

    this.chargement.set(true);

    setTimeout(() => {
      this.chargement.set(false);
      this.succes.set(true);

      // Authentification et détection du rôle
      const utilisateur = this.auth.connexion(this.email);
      this.roleDetecte.set(utilisateur.role);

      // Redirection automatique vers l'espace du rôle
      setTimeout(() => {
        this.auth.redirectionApresConnexion();
      }, 700);
    }, 800);
  }
}
