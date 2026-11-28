import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { RendezVousScene } from '../components/rendez-vous-scene';
import { EspacePatientComponent } from '../components/espace-patient';
import { EspaceMedecinComponent } from '../components/espace-medecin';
import { EspaceLaborantinComponent } from '../components/espace-laborantin';
import { RdvConfirmesComponent } from '../components/rdv-confirmes';
import { AuthService } from '../../../core/auth.service';

// Page principale du module Rendez-vous :
// Reconnaissance automatique du rôle et affichage de l'espace adapté
@Component({
  selector: 'mv-rendez-vous-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterLink,
    Icon,
    RendezVousScene,
    EspacePatientComponent,
    EspaceMedecinComponent,
    EspaceLaborantinComponent,
    RdvConfirmesComponent
  ],
  template: `
    <!-- En-tête dynamique commun avec identification automatique -->
    <section class="enter relative overflow-hidden rounded-[2rem] aurora-dark grain p-8 text-white sm:p-12 mb-6">
      <div class="absolute inset-0 grid-lines"></div>
      <div class="pointer-events-none absolute inset-y-0 right-0 hidden w-[52%] md:block">
        <mv-rendez-vous-scene class="h-full w-full" />
      </div>
      <div class="relative max-w-xl">
        <div class="grid h-14 w-14 place-items-center rounded-2xl bg-aqua-400 text-ink-950">
          <mv-icon name="calendar" [size]="26" />
        </div>
        <p class="mt-6 font-mono text-xs text-aqua-300">rendezvous-service · Port 8087</p>
        <h2 class="mt-2 font-display text-5xl sm:text-6xl leading-none">Rendez-vous</h2>
        <p class="mt-4 text-base sm:text-lg text-white/60">
          Prise de rendez-vous, gestion des créneaux et suivi des consultations en temps réel.
        </p>

        <!-- Badge du rôle détecté dynamiquement -->
        <div class="mt-6 flex flex-wrap items-center gap-3">
          <span class="chip bg-white/15 px-3.5 py-1.5 text-white backdrop-blur flex items-center gap-2">
            <mv-icon [name]="badgeRole().icon" [size]="15" />
            Compte actif : <strong>{{ auth.nom() }}</strong>
          </span>
          <span class="chip {{ badgeRole().style }} px-3.5 py-1.5 font-bold uppercase tracking-wider text-xs">
            Rôle : {{ auth.role() || 'Non identifié' }}
          </span>
        </div>
      </div>
    </section>

    <!-- Affichage exclusif de l'interface correspondant au rôle détecté -->
    @switch (auth.role()) {
      @case ('PATIENT') {
        <mv-espace-patient />
      }
      @case ('MEDECIN') {
        <mv-espace-medecin />
      }
      @case ('LABORANTIN') {
        <mv-espace-laborantin />
      }
      @case ('ADMIN') {
        <!-- Vue superviseur / administration -->
        <div class="space-y-6">
          <div class="card p-6 bg-slate-50 border-slate-200">
            <h3 class="font-display text-2xl text-ink-900">Supervision Administrateur · Flux des Rendez-vous</h3>
            <p class="text-sm text-ink-500">Vue globale sur toutes les réservations et événements RabbitMQ du système.</p>
          </div>
          <mv-rdv-confirmes />
        </div>
      }
      @default {
        <!-- Page de sécurité : Rôle absent, non autorisé ou inconnu -->
        <div class="card p-12 text-center enter">
          <div class="grid h-16 w-16 place-items-center rounded-2xl bg-rose-50 text-rose-600 mx-auto mb-4">
            <mv-icon name="alert" [size]="32" />
          </div>
          <h3 class="font-display text-3xl text-ink-900">Accès non autorisé</h3>
          <p class="text-ink-500 mt-2 max-w-md mx-auto text-sm">
            Votre compte n'a pas les autorisations nécessaires pour accéder à ce module.
          </p>
          <a routerLink="/app/dashboard" class="btn btn-soft px-6 py-2.5 text-sm mt-6 inline-flex">
            Retourner au tableau de bord
          </a>
        </div>
      }
    }

    <!-- Pied de page -->
    <div class="mt-8 flex items-center justify-between border-t border-ink-100 pt-4">
      <a routerLink="/app/dashboard" class="btn btn-soft px-5 py-2.5 text-sm">
        <mv-icon name="chevron-left" [size]="16" /> Retour au tableau de bord
      </a>
      <p class="text-xs text-ink-400">MediVault · Système sécurisé RBAC (Role-Based Access Control)</p>
    </div>
  `
})
export class RendezVousPage {
  protected auth = inject(AuthService);

  protected badgeRole = computed(() => {
    const role = this.auth.role();
    switch (role) {
      case 'PATIENT':
        return { icon: 'user', style: 'bg-teal-400/25 text-teal-200 border border-teal-300/30' };
      case 'MEDECIN':
        return { icon: 'stethoscope', style: 'bg-aqua-400/25 text-aqua-200 border border-aqua-300/30' };
      case 'LABORANTIN':
        return { icon: 'flask', style: 'bg-violet-400/25 text-violet-200 border border-violet-300/30' };
      case 'ADMIN':
        return { icon: 'shield', style: 'bg-rose-400/25 text-rose-200 border border-rose-300/30' };
      default:
        return { icon: 'lock', style: 'bg-amber-400/25 text-amber-200 border border-amber-300/30' };
    }
  });
}
