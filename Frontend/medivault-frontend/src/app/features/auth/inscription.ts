import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { AuthService } from '../../core/auth.service';
import { AuthLayout } from './auth-layout';

// Inscription libre : on ne peut créer qu'un compte PATIENT. Les médecins, pharmaciens et laborantins
// sont créés par l'administrateur. Après l'inscription, la connexion se fait automatiquement.
@Component({
  selector: 'mv-inscription',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, AuthLayout],
  template: `
    <mv-auth-layout>
      <div class="enter">
        <p class="text-sm font-semibold text-aqua-600">Bienvenue</p>
        <h2 class="mt-2 font-display text-5xl leading-none text-ink-900">Créer un compte</h2>
        <p class="mt-3 text-ink-500">Un compte patient, pour suivre vos ordonnances et vos rendez-vous.</p>
      </div>

      <form class="enter [animation-delay:150ms] mt-8 space-y-4" [class.shake]="secoue()" (ngSubmit)="valider()" novalidate>
        <div class="grid gap-4 sm:grid-cols-2">
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Prénom</span>
            <input class="field" name="prenom" autocomplete="given-name" [(ngModel)]="prenom" />
          </label>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Nom</span>
            <input class="field" name="nom" autocomplete="family-name" [(ngModel)]="nom" />
          </label>
        </div>

        <label class="block">
          <span class="mb-1.5 block text-sm font-semibold text-ink-700">Adresse e-mail</span>
          <span class="relative block">
            <mv-icon name="mail" [size]="18" class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-300" />
            <input class="field pl-11" type="email" name="email" autocomplete="email" placeholder="prenom.nom&#64;exemple.tn" [(ngModel)]="email" />
          </span>
        </label>

        <label class="block">
          <span class="mb-1.5 block text-sm font-semibold text-ink-700">Téléphone <span class="font-normal text-ink-400">(facultatif)</span></span>
          <input class="field" type="tel" name="tel" autocomplete="tel" placeholder="+216 20 111 222" [(ngModel)]="telephone" />
        </label>

        <label class="block">
          <span class="mb-1.5 block text-sm font-semibold text-ink-700">Mot de passe</span>
          <span class="relative block">
            <mv-icon name="lock" [size]="18" class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-300" />
            <input class="field pl-11 pr-12" [type]="voir() ? 'text' : 'password'" name="mdp" autocomplete="new-password"
                   placeholder="8 caractères, une lettre et un chiffre" [(ngModel)]="motDePasse" />
            <button type="button" (click)="voir.set(!voir())"
                    class="absolute right-3 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-ink-400 hover:bg-ink-50 hover:text-ink-700"
                    [attr.aria-label]="voir() ? 'Masquer le mot de passe' : 'Afficher le mot de passe'">
              <mv-icon [name]="voir() ? 'eye-off' : 'eye'" [size]="18" />
            </button>
          </span>
        </label>

        <label class="block">
          <span class="mb-1.5 block text-sm font-semibold text-ink-700">Confirmer le mot de passe</span>
          <input class="field" [type]="voir() ? 'text' : 'password'" name="confirmation" autocomplete="new-password" [(ngModel)]="confirmation" />
        </label>

        @if (erreur()) {
          <p class="enter flex items-center gap-2 rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">
            <mv-icon name="alert" [size]="16" /> {{ erreur() }}
          </p>
        }

        <button type="submit" [disabled]="chargement() || succes()" class="btn btn-ink shine relative mt-2 w-full overflow-hidden py-4 text-[15px] disabled:cursor-wait">
          @if (succes()) {
            <span class="grid h-6 w-6 place-items-center rounded-full bg-aqua-400 text-ink-950 enter"><mv-icon name="check" [size]="15" [stroke]="3" /></span>
            Compte créé
          } @else if (chargement()) {
            <span class="h-5 w-5 animate-spin rounded-full border-2 border-white/25 border-t-aqua-300"></span>
            Création du compte…
          } @else {
            Créer mon compte <mv-icon name="arrow-right" [size]="18" />
          }
        </button>
      </form>

      <p class="mt-8 text-center text-sm text-ink-500">
        Déjà un compte ? <a routerLink="/login" class="font-semibold text-aqua-600 hover:text-aqua-700">Se connecter</a>
      </p>
    </mv-auth-layout>
  `
})
export class InscriptionPage {
  private auth = inject(AuthService);
  private router = inject(Router);

  protected prenom = '';
  protected nom = '';
  protected email = '';
  protected telephone = '';
  protected motDePasse = '';
  protected confirmation = '';
  protected voir = signal(false);
  protected chargement = signal(false);
  protected succes = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);

  protected valider() {
    const probleme = this.verifier();
    if (probleme) return this.refuser(probleme);
    this.erreur.set('');
    this.chargement.set(true);
    const donnees = {
      prenom: this.prenom.trim(), nom: this.nom.trim(), email: this.email.trim(), motDePasse: this.motDePasse,
      ...(this.telephone.trim() ? { telephone: this.telephone.trim() } : {})
    };
    this.auth.inscription(donnees).subscribe({
      next: () => this.connecter(donnees.email),
      error: (e: HttpErrorResponse) => {
        this.chargement.set(false);
        this.refuser(this.messageDe(e));
      }
    });
  }

  /** Le compte vient d'être créé : on ouvre directement la session */
  private connecter(email: string) {
    this.auth.connexionReelle(email, this.motDePasse, true).subscribe({
      next: () => {
        this.chargement.set(false);
        this.succes.set(true);
        setTimeout(() => this.router.navigateByUrl('/app/dashboard'), 900);
      },
      error: () => {
        // compte créé mais connexion impossible : on renvoie vers la page de connexion
        this.chargement.set(false);
        this.router.navigateByUrl('/login');
      }
    });
  }

  private verifier(): string {
    if (!this.prenom.trim() || !this.nom.trim()) return 'Le prénom et le nom sont obligatoires.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) return 'Adresse e-mail invalide.';
    if (this.telephone.trim() && !/^\+?[0-9 ]{8,15}$/.test(this.telephone.trim())) return 'Téléphone invalide (8 à 15 chiffres).';
    if (this.motDePasse.length < 8) return 'Le mot de passe doit contenir au moins 8 caractères.';
    if (!/[A-Za-z]/.test(this.motDePasse) || !/\d/.test(this.motDePasse)) return 'Le mot de passe doit contenir au moins une lettre et un chiffre.';
    if (this.motDePasse !== this.confirmation) return 'Les deux mots de passe ne sont pas identiques.';
    return '';
  }

  private messageDe(e: HttpErrorResponse): string {
    if (e.status === 409) return 'Cette adresse e-mail est déjà utilisée.';
    if (e.status === 400) return Object.values<string>(e.error?.erreurs ?? {})[0] ?? e.error?.detail ?? 'Données invalides.';
    if (e.status === 503) return 'Le serveur de comptes est indisponible. Réessayez dans un instant.';
    if (e.status === 0 || e.status === 404 || e.status === 502) return 'Impossible de joindre le serveur de comptes.';
    return 'Inscription impossible pour le moment.';
  }

  private refuser(message: string) {
    this.erreur.set(message);
    this.secoue.set(true);
    setTimeout(() => this.secoue.set(false), 650);
  }
}
