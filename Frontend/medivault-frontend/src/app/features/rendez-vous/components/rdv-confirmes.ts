import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { RendezVous } from '../models/rendezvous.model';

// Composant 04 : Événement RendezVousConfirme & Suivi des rendez-vous
@Component({
  selector: 'mv-rdv-confirmes',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon],
  template: `
    <div class="card p-6">
      <div class="flex items-center gap-3 mb-6">
        <div class="grid h-10 w-10 place-items-center rounded-xl bg-violet-50 text-violet-600">
          <mv-icon name="activity" [size]="20" />
        </div>
        <div>
          <h3 class="font-bold text-ink-900 text-lg">Événements & Rendez-vous Confirmés</h3>
          <p class="text-sm text-ink-400">Flux des réservations publiées vers RabbitMQ (dossier, analyse, notification)</p>
        </div>
        <button (click)="charger()" class="ml-auto btn btn-soft px-4 py-2 text-xs">
          <mv-icon name="refresh" [size]="14" [class.animate-spin]="chargement()" /> Actualiser
        </button>
      </div>

      <div class="grid gap-3 sm:grid-cols-3 mb-6">
        <div class="rounded-xl border border-ink-100 bg-ink-50/40 p-4">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Confirmés</p>
          <p class="font-display text-3xl font-bold text-emerald-600 mt-1">{{ totalConfirmes() }}</p>
        </div>
        <div class="rounded-xl border border-ink-100 bg-ink-50/40 p-4">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Honorés / Terminés</p>
          <p class="font-display text-3xl font-bold text-teal-600 mt-1">{{ totalTermines() }}</p>
        </div>
        <div class="rounded-xl border border-ink-100 bg-ink-50/40 p-4">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Annulés</p>
          <p class="font-display text-3xl font-bold text-rose-600 mt-1">{{ totalAnnules() }}</p>
        </div>
      </div>

      @if (chargement()) {
        <div class="space-y-2">
          @for (i of [1,2,3]; track i) {
            <div class="h-16 rounded-xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
          }
        </div>
      } @else if (liste().length === 0) {
        <div class="py-12 text-center text-ink-400">
          <p>Aucun rendez-vous enregistré.</p>
        </div>
      } @else {
        <div class="overflow-x-auto">
          <table class="w-full text-sm text-left">
            <thead>
              <tr class="border-b border-ink-100 text-xs font-semibold text-ink-400 uppercase">
                <th class="py-3 px-3">ID</th>
                <th class="py-3 px-3">Patient</th>
                <th class="py-3 px-3">Professionnel</th>
                <th class="py-3 px-3">Date</th>
                <th class="py-3 px-3">Motif</th>
                <th class="py-3 px-3 text-right">Statut</th>
              </tr>
            </thead>
            <tbody>
              @for (r of liste(); track r.id) {
                <tr class="border-b border-ink-50 hover:bg-ink-50/40 transition">
                  <td class="py-3 px-3 font-mono text-xs font-semibold text-ink-700">#{{ r.id }}</td>
                  <td class="py-3 px-3 font-medium text-ink-900">{{ r.patientId }}</td>
                  <td class="py-3 px-3 text-ink-600">{{ r.professionnelId || '-' }} ({{ r.typeProfessionnel }})</td>
                  <td class="py-3 px-3 text-ink-500 text-xs">{{ formatDate(r.dateDebut) }}</td>
                  <td class="py-3 px-3 text-ink-600 text-xs">{{ r.motif }}</td>
                  <td class="py-3 px-3 text-right">
                    <span class="chip text-xs {{ tonBadge(r.statut) }}">{{ r.statut }}</span>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `
})
export class RdvConfirmesComponent {
  protected api = inject(RendezvousService);
  protected liste = signal<RendezVous[]>([]);
  protected chargement = signal(true);

  constructor() {
    this.charger();
  }

  charger() {
    this.chargement.set(true);
    this.api.tousLesRendezVous().subscribe(l => {
      this.liste.set(l);
      this.chargement.set(false);
    });
  }

  totalConfirmes() {
    return this.liste().filter(r => r.statut === 'CONFIRME').length;
  }

  totalTermines() {
    return this.liste().filter(r => r.statut === 'TERMINE').length;
  }

  totalAnnules() {
    return this.liste().filter(r => r.statut === 'ANNULE').length;
  }

  protected formatDate(d: string) {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  protected tonBadge(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-100 text-emerald-700' : (s === 'ANNULE' ? 'bg-rose-100 text-rose-700' : 'bg-slate-100 text-slate-700');
  }
}
