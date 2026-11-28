import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mv-acces-refuse',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon],
  template: `
    <div class="grid min-h-[70vh] place-items-center p-6 text-center">
      <div class="max-w-md enter">
        <div class="grid h-16 w-16 place-items-center rounded-3xl bg-rose-50 text-rose-600 mx-auto mb-6 shadow-sm">
          <mv-icon name="shield" [size]="32" />
        </div>
        <span class="chip bg-rose-100 text-rose-700 text-xs font-bold uppercase tracking-wider px-3 py-1">
          Erreur 403 · Accès Refusé
        </span>
        <h1 class="mt-4 font-display text-4xl text-ink-900 leading-tight">Espace non autorisé</h1>
        <p class="mt-3 text-ink-500 text-sm leading-relaxed">
          Votre compte est identifié en tant que
          <strong class="text-ink-800">{{ auth.role() || 'Inconnu' }}</strong>.
          Vous n'avez pas les permissions nécessaires pour accéder à cette ressource.
        </p>

        <div class="mt-8 flex flex-col sm:flex-row items-center justify-center gap-3">
          <button (click)="auth.redirectionApresConnexion()" class="btn btn-primary px-6 py-3 text-sm w-full sm:w-auto">
            <mv-icon name="arrow-right" [size]="16" /> Aller à mon espace
          </button>
          <a routerLink="/login" (click)="auth.deconnexion()" class="btn btn-soft px-5 py-3 text-sm w-full sm:w-auto">
            Changer de compte
          </a>
        </div>
      </div>
    </div>
  `
})
export class AccesRefusePage {
  protected auth = inject(AuthService);
}
