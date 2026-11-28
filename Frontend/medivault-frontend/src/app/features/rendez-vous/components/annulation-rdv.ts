import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { RendezVous } from '../models/rendezvous.model';

// Composant 03 : Confirmation et annulation - permet d'annuler ou supprimer un rdv
@Component({
  selector: 'mv-annulation-rdv',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="card p-6">
      <div class="flex items-center gap-3 mb-6">
        <div class="grid h-10 w-10 place-items-center rounded-xl bg-amber-50 text-amber-600">
          <mv-icon name="x" [size]="20" />
        </div>
        <div>
          <h3 class="font-bold text-ink-900 text-lg">Annulation & Gestion</h3>
          <p class="text-sm text-ink-400">Annuler un rendez-vous et libérer le créneau</p>
        </div>
      </div>

      <div class="flex flex-wrap gap-3 mb-5">
        <label class="flex-1 min-w-48">
          <span class="mb-1 block text-sm font-semibold text-ink-700">ID Patient</span>
          <input class="field" placeholder="patient-001" [(ngModel)]="patientId" />
        </label>
        <div class="flex items-end">
          <button (click)="charger()" class="btn btn-primary px-5 py-2.5 text-sm">
            <mv-icon name="search" [size]="16" /> Charger les RDV
          </button>
        </div>
      </div>

      @if (message()) {
        <div class="mb-4 rounded-xl bg-emerald-50 border border-emerald-100 p-4 text-sm font-medium text-emerald-800 flex items-center gap-2">
          <mv-icon name="check" [size]="18" /> {{ message() }}
        </div>
      }

      @if (chargement()) {
        <div class="space-y-2">
          @for (i of [1,2,3]; track i) {
            <div class="h-16 rounded-xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
          }
        </div>
      } @else if (liste().length === 0) {
        <div class="py-10 text-center text-ink-400">
          <p>Aucun rendez-vous trouvé pour ce patient.</p>
        </div>
      } @else {
        <div class="space-y-3">
          @for (r of liste(); track r.id) {
            <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-xl border border-ink-100 bg-white p-4 transition hover:border-amber-200">
              <div class="flex items-center gap-3">
                <div class="grid h-10 w-10 shrink-0 place-items-center rounded-xl {{ tonStatut(r.statut) }}">
                  <mv-icon [name]="r.statut === 'CONFIRME' ? 'calendar' : (r.statut === 'ANNULE' ? 'x' : 'check')" [size]="18" />
                </div>
                <div>
                  <div class="flex items-center gap-2">
                    <p class="font-bold text-ink-900 text-sm">RDV #{{ r.id }}</p>
                    <span class="chip text-xs {{ tonBadge(r.statut) }}">{{ r.statut }}</span>
                  </div>
                  <p class="text-xs text-ink-500 mt-0.5">{{ r.motif }} · {{ formatDate(r.dateDebut) }}</p>
                </div>
              </div>
              <div class="flex items-center gap-2">
                @if (r.statut === 'CONFIRME') {
                  <button (click)="annuler(r.id)" class="btn btn-soft text-rose-600 hover:bg-rose-50 px-4 py-2 text-xs">
                    <mv-icon name="x" [size]="14" /> Annuler
                  </button>
                }
                <button (click)="supprimer(r.id)" class="btn btn-ghost px-3 py-2 text-ink-400 hover:text-rose-600 text-xs" title="Supprimer définitivement">
                  <mv-icon name="trash" [size]="14" />
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class AnnulationRdvComponent {
  protected api = inject(RendezvousService);
  protected liste = signal<RendezVous[]>([]);
  protected chargement = signal(false);
  protected patientId = 'patient-001';
  protected message = signal('');

  constructor() {
    this.charger();
  }

  charger() {
    if (!this.patientId.trim()) return;
    this.chargement.set(true);
    this.message.set('');
    this.api.parPatient(this.patientId.trim()).subscribe(l => {
      this.liste.set(l);
      this.chargement.set(false);
    });
  }

  annuler(id: number) {
    this.api.annuler(id).subscribe(() => {
      this.message.set(`Le rendez-vous #${id} a été annulé avec succès (événement RendezVousAnnule publié).`);
      this.charger();
    });
  }

  supprimer(id: number) {
    this.api.supprimer(id).subscribe(() => {
      this.message.set(`Le rendez-vous #${id} a été définitivement supprimé.`);
      this.charger();
    });
  }

  protected formatDate(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  protected tonStatut(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-50 text-emerald-600' : (s === 'ANNULE' ? 'bg-rose-50 text-rose-600' : 'bg-slate-50 text-slate-600');
  }

  protected tonBadge(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-100/70 text-emerald-700' : (s === 'ANNULE' ? 'bg-rose-100/70 text-rose-700' : 'bg-slate-100 text-slate-700');
  }
}
