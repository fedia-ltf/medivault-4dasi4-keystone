import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
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
  private auth = inject(AuthService);
  private router = inject(Router);

  protected readonly roles = ROLES;
  protected role = signal<Role>('MEDECIN');
  protected roleIndex = computed(() => this.roles.findIndex(r => r.value === this.role()));

  protected email = '';
  protected motDePasse = '';
  protected voirMotDePasse = signal(false);
  protected chargement = signal(false);
  protected succes = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);

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
    // Simulation de l'appel à Keycloak
    setTimeout(() => {
      this.chargement.set(false);
      this.succes.set(true);
      this.auth.connexion(this.email, this.role());
      setTimeout(() => this.router.navigateByUrl('/app/dashboard'), 900);
    }, 1100);
  }

  protected remplirDemo() {
    const demo: Record<Role, string> = {
      MEDECIN: 'karim.benali@medivault.tn',
      PATIENT: 'ahmed.trabelsi@medivault.tn',
      PHARMACIEN: 'aziz.pharma@medivault.tn',
      LABORANTIN: 'taha.labo@medivault.tn',
      ADMIN: 'admin@medivault.tn'
    };
    this.email = demo[this.role()];
    this.motDePasse = 'medivault';
  }
}
