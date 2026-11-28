import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { Creneau } from '../models/rendezvous.model';

// Composant 01 : Agenda du médecin - affiche les créneaux libres d'un professionnel
@Component({
  selector: 'mv-agenda',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="card p-6">
      <div class="flex items-center gap-3 mb-6">
        <div class="grid h-10 w-10 place-items-center rounded-xl bg-aqua-50 text-aqua-600">
          <mv-icon name="calendar" [size]="20" />
        </div>
        <div>
          <h3 class="font-bold text-ink-900 text-lg">Agenda du médecin</h3>
          <p class="text-sm text-ink-400">Créneaux disponibles pour réservation</p>
        </div>
        @if (api.enLigne() === true) {
          <span class="ml-auto chip bg-emerald-50 text-emerald-700">API en ligne</span>
        } @else if (api.enLigne() === false) {
          <span class="ml-auto chip bg-amber-50 text-amber-700">Démonstration</span>
        }
      </div>

      <div class="flex flex-wrap gap-3 mb-5">
        <label class="flex-1 min-w-48">
          <span class="mb-1 block text-sm font-semibold text-ink-700">ID Professionnel</span>
          <input class="field" placeholder="prof-001" [(ngModel)]="professionnelId" />
        </label>
        <label class="flex-1 min-w-48">
          <span class="mb-1 block text-sm font-semibold text-ink-700">Date (optionnel)</span>
          <input class="field" type="date" [(ngModel)]="date" />
        </label>
        <div class="flex items-end gap-2">
          <button (click)="charger()" class="btn btn-primary px-5 py-2.5 text-sm">
            <mv-icon name="search" [size]="16" /> Chercher
          </button>
          <button (click)="generer()" class="btn btn-soft px-5 py-2.5 text-sm" title="Génère les créneaux des 7 prochains jours">
            <mv-icon name="refresh" [size]="16" /> Générer
          </button>
        </div>
      </div>

      @if (chargement()) {
        <div class="space-y-2">
          @for (i of [1,2,3,4]; track i) {
            <div class="h-14 rounded-xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
          }
        </div>
      } @else if (creneaux().length === 0) {
        <div class="py-12 text-center text-ink-400">
          <mv-icon name="calendar" [size]="40" class="mx-auto mb-3 opacity-30" />
          <p>Aucun créneau disponible. Essayez de générer des créneaux.</p>
        </div>
      } @else {
        <div class="space-y-2">
          @for (c of creneaux(); track c.id) {
            <div class="flex items-center gap-4 rounded-xl border border-ink-100 bg-white p-4 transition hover:border-aqua-200 hover:bg-aqua-50/40">
              <div class="grid h-10 w-10 shrink-0 place-items-center rounded-xl {{ c.typeProfessionnel === 'MEDECIN' ? 'bg-aqua-50 text-aqua-600' : 'bg-violet-50 text-violet-600' }}">
                <mv-icon [name]="c.typeProfessionnel === 'MEDECIN' ? 'activity' : 'flask'" [size]="18" />
              </div>
              <div class="flex-1">
                <p class="font-semibold text-ink-900 text-sm">{{ formatDate(c.dateDebut) }}</p>
                <p class="text-xs text-ink-400">{{ formatHeure(c.dateDebut) }} – {{ formatHeure(c.dateFin) }} · {{ c.typeProfessionnel }}</p>
              </div>
              <span class="chip {{ c.libre ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700' }}">
                {{ c.libre ? 'Libre' : 'Réservé' }}
              </span>
              <span class="font-mono text-xs text-ink-300">#{{ c.id }}</span>
            </div>
          }
        </div>
      }
    </div>
  `
})
export class AgendaComponent {
  protected api = inject(RendezvousService);
  protected creneaux = signal<Creneau[]>([]);
  protected chargement = signal(false);
  protected professionnelId = '';
  protected date = '';

  charger() {
    if (!this.professionnelId.trim()) return;
    this.chargement.set(true);
    this.api.creneauxLibres(this.professionnelId.trim(), this.date || undefined).subscribe(c => {
      this.creneaux.set(c);
      this.chargement.set(false);
    });
  }

  generer() {
    if (!this.professionnelId.trim()) return;
    this.chargement.set(true);
    this.api.genererCreneaux(this.professionnelId.trim()).subscribe(c => {
      this.creneaux.set(c);
      this.chargement.set(false);
    });
  }

  protected formatDate(d: string) {
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  protected formatHeure(d: string) {
    return new Date(d).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }
}
