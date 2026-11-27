import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { AuthService } from '../../core/auth.service';
import { AuthLayout } from './auth-layout';

// Page ouverte depuis le lien de l'e-mail « mot de passe oublié » : /reinitialiser-mot-de-passe?token=...
// On choisit un nouveau mot de passe ; le lien ne sert qu'une fois et expire au bout de 30 minutes.
@Component({
  selector: 'mv-reinitialiser-mot-de-passe',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, AuthLayout],
  template: `
    <mv-auth-layout>
      <div class="enter">
        <p class="text-sm font-semibold text-aqua-600">Dernière étape</p>
        <h2 class="mt-2 font-display text-5xl leading-none text-ink-900">Nouveau mot de passe</h2>
        <p class="mt-3 text-ink-500">Choisissez un mot de passe d'au moins 8 caractères, avec une lettre et un chiffre.</p>
      </div>

      @if (!token) {
        <div class="enter mt-8 rounded-2xl border border-rose-100 bg-rose-50 p-5 text-rose-800">
          <p class="flex items-center gap-2 font-semibold"><mv-icon name="alert" [size]="18" /> Lien incomplet</p>
          <p class="mt-2 text-sm leading-relaxed">Ouvrez le lien tel qu'il est écrit dans l'e-mail, ou refaites une demande.</p>
        </div>
        <a routerLink="/mot-de-passe-oublie" class="btn btn-ink mt-6 w-full py-4 text-[15px]">Refaire une demande</a>
      } @else if (fini()) {
        <div class="enter mt-8 rounded-2xl border border-emerald-100 bg-emerald-50 p-5 text-emerald-800">
          <p class="flex items-center gap-2 font-semibold"><mv-icon name="check" [size]="18" [stroke]="3" /> Mot de passe modifié</p>
          <p class="mt-2 text-sm leading-relaxed">Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.</p>
        </div>
        <a routerLink="/login" class="btn btn-ink mt-6 w-full py-4 text-[15px]">Se connecter <mv-icon name="arrow-right" [size]="18" /></a>
      } @else {
        <form class="enter [animation-delay:150ms] mt-8 space-y-4" [class.shake]="secoue()" (ngSubmit)="valider()" novalidate>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Nouveau mot de passe</span>
            <span class="relative block">
              <mv-icon name="lock" [size]="18" class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-300" />
              <input class="field pl-11" type="password" name="nouveau" autocomplete="new-password" placeholder="••••••••" [(ngModel)]="nouveau" />
            </span>
          </label>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Confirmer le mot de passe</span>
            <span class="relative block">
              <mv-icon name="lock" [size]="18" class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-300" />
              <input class="field pl-11" type="password" name="confirmation" autocomplete="new-password" placeholder="••••••••" [(ngModel)]="confirmation" />
            </span>
          </label>

          @if (erreur()) {
            <p class="enter flex items-center gap-2 rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">
              <mv-icon name="alert" [size]="16" /> {{ erreur() }}
            </p>
          }

          <button type="submit" [disabled]="chargement()" class="btn btn-ink shine relative mt-2 w-full overflow-hidden py-4 text-[15px] disabled:cursor-wait">
            @if (chargement()) {
              <span class="h-5 w-5 animate-spin rounded-full border-2 border-white/25 border-t-aqua-300"></span> Enregistrement…
            } @else {
              Enregistrer <mv-icon name="check" [size]="18" />
            }
          </button>
        </form>
      }
    </mv-auth-layout>
  `
})
export class ReinitialiserMotDePassePage {
  private auth = inject(AuthService);
  protected token = inject(ActivatedRoute).snapshot.queryParamMap.get('token') ?? '';

  protected nouveau = '';
  protected confirmation = '';
  protected chargement = signal(false);
  protected fini = signal(false);
  protected erreur = signal('');
  protected secoue = signal(false);

  protected valider() {
    if (this.nouveau.length < 8 || !/[A-Za-z]/.test(this.nouveau) || !/\d/.test(this.nouveau)) {
      return this.refuser('8 caractères minimum, avec au moins une lettre et un chiffre.');
    }
    if (this.nouveau !== this.confirmation) {
      return this.refuser('Les deux mots de passe ne sont pas identiques.');
    }
    this.erreur.set('');
    this.chargement.set(true);
    this.auth.reinitialiserMotDePasse(this.token, this.nouveau).subscribe({
      next: () => { this.chargement.set(false); this.fini.set(true); },
      error: (e: HttpErrorResponse) => {
        this.chargement.set(false);
        this.refuser(e.status === 0 || e.status === 502 || e.status === 503
          ? 'Serveur injoignable pour le moment. Réessayez.'
          : e.error?.detail ?? 'Lien invalide ou expiré : refaites une demande.');
      }
    });
  }

  private refuser(message: string) {
    this.erreur.set(message);
    this.secoue.set(true);
    setTimeout(() => this.secoue.set(false), 650);
  }
}
