import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { AuthService } from '../../core/auth.service';
import { AuthLayout } from './auth-layout';

// « Mot de passe oublié » : on saisit son e-mail, Keycloak envoie un lien (valable 30 minutes) pour choisir un nouveau mot de passe.
// La réponse est la même que le compte existe ou non : on ne révèle pas quelles adresses ont un compte.
@Component({
  selector: 'mv-mot-de-passe-oublie',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, RouterLink, Icon, AuthLayout],
  template: `
    <mv-auth-layout>
      <div class="enter">
        <p class="text-sm font-semibold text-aqua-600">Pas de panique</p>
        <h2 class="mt-2 font-display text-5xl leading-none text-ink-900">Mot de passe oublié</h2>
        <p class="mt-3 text-ink-500">Entrez votre adresse e-mail : nous vous envoyons un lien pour choisir un nouveau mot de passe.</p>
      </div>

      @if (message()) {
        <div class="enter mt-8 rounded-2xl border border-emerald-100 bg-emerald-50 p-5 text-emerald-800">
          <p class="flex items-center gap-2 font-semibold"><mv-icon name="check" [size]="18" [stroke]="3" /> Demande envoyée</p>
          <p class="mt-2 text-sm leading-relaxed">{{ message() }}</p>
          <p class="mt-2 text-sm leading-relaxed">
            @if (canal() === 'WHATSAPP') {
              Le lien est valable 30 minutes. Sans numéro WhatsApp enregistré sur votre compte, il est envoyé par e-mail.
            } @else {
              Le lien est valable 30 minutes. Pensez à regarder dans vos courriers indésirables.
            }
          </p>
        </div>
        <a routerLink="/login" class="btn btn-ink mt-6 w-full py-4 text-[15px]">Retour à la connexion <mv-icon name="arrow-right" [size]="18" /></a>
      } @else {
        <form class="enter [animation-delay:150ms] mt-8 space-y-4" [class.shake]="secoue()" (ngSubmit)="envoyer()" novalidate>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Adresse e-mail</span>
            <span class="relative block">
              <mv-icon name="mail" [size]="18" class="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-ink-300" />
              <input class="field pl-11" type="email" name="email" autocomplete="email" placeholder="prenom.nom&#64;medivault.tn" [(ngModel)]="email" />
            </span>
          </label>

          <div>
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Recevoir le lien par</span>
            <div class="grid grid-cols-2 gap-1.5 rounded-2xl border border-white bg-white/70 p-1.5">
              @for (c of canaux; track c.valeur) {
                <button type="button" (click)="canal.set(c.valeur)" [attr.aria-pressed]="canal() === c.valeur"
                        class="rounded-xl py-2.5 text-sm font-semibold transition-colors"
                        [class.bg-ink-950]="canal() === c.valeur" [class.text-white]="canal() === c.valeur"
                        [class.text-ink-500]="canal() !== c.valeur">{{ c.libelle }}</button>
              }
            </div>
          </div>

          @if (erreur()) {
            <p class="enter flex items-center gap-2 rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">
              <mv-icon name="alert" [size]="16" /> {{ erreur() }}
            </p>
          }

          <button type="submit" [disabled]="chargement()" class="btn btn-ink shine relative mt-2 w-full overflow-hidden py-4 text-[15px] disabled:cursor-wait">
            @if (chargement()) {
              <span class="h-5 w-5 animate-spin rounded-full border-2 border-white/25 border-t-aqua-300"></span> Envoi…
            } @else {
              Envoyer le lien <mv-icon name="mail" [size]="18" />
            }
          </button>
        </form>

        <p class="mt-8 text-center text-sm text-ink-500">
          Vous vous en souvenez ? <a routerLink="/login" class="font-semibold text-aqua-600 hover:text-aqua-700">Se connecter</a>
        </p>
      }
    </mv-auth-layout>
  `
})
export class MotDePasseOubliePage {
  private auth = inject(AuthService);

  protected email = '';
  protected readonly canaux = [
    { valeur: 'EMAIL' as const, libelle: 'E-mail' },
    { valeur: 'WHATSAPP' as const, libelle: 'WhatsApp' }
  ];
  protected canal = signal<'EMAIL' | 'WHATSAPP'>('EMAIL');
  protected chargement = signal(false);
  protected message = signal('');
  protected erreur = signal('');
  protected secoue = signal(false);

  protected envoyer() {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email.trim())) {
      this.erreur.set('Adresse e-mail invalide.');
      this.secoue.set(true);
      setTimeout(() => this.secoue.set(false), 650);
      return;
    }
    this.erreur.set('');
    this.chargement.set(true);
    this.auth.motDePasseOublie(this.email.trim(), this.canal()).subscribe({
      next: m => { this.chargement.set(false); this.message.set(m); },
      error: (e: HttpErrorResponse) => {
        this.chargement.set(false);
        this.erreur.set(e.status === 0 || e.status === 404 || e.status === 502
          ? 'Impossible de joindre le serveur de comptes.' : 'Envoi impossible pour le moment. Réessayez plus tard.');
      }
    });
  }
}
