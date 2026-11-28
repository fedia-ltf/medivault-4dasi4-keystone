import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { RendezVous } from '../models/rendezvous.model';

// Composant 02 : Prise de rendez-vous - formulaire de réservation
@Component({
  selector: 'mv-prise-rdv',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="card p-6">
      <div class="flex items-center gap-3 mb-6">
        <div class="grid h-10 w-10 place-items-center rounded-xl bg-teal-50 text-teal-600">
          <mv-icon name="plus" [size]="20" />
        </div>
        <div>
          <h3 class="font-bold text-ink-900 text-lg">Prise de rendez-vous</h3>
          <p class="text-sm text-ink-400">Réserver un créneau pour un patient</p>
        </div>
      </div>

      @if (rdvCree()) {
        <div class="rounded-2xl bg-emerald-50 border border-emerald-100 p-5 text-center">
          <div class="grid h-12 w-12 place-items-center rounded-2xl bg-emerald-100 text-emerald-600 mx-auto mb-3">
            <mv-icon name="check" [size]="24" />
          </div>
          <p class="font-bold text-emerald-800 text-lg">Rendez-vous confirmé !</p>
          <p class="text-sm text-emerald-600 mt-1">RDV #{{ rdvCree()!.id }} · {{ formatDate(rdvCree()!.dateDebut) }}</p>
          <p class="text-xs text-emerald-500 mt-1">Statut : {{ rdvCree()!.statut }}</p>
          <button (click)="rdvCree.set(null); reset()" class="btn btn-soft mt-4 px-6 py-2.5 text-sm">
            Nouveau rendez-vous
          </button>
        </div>
      } @else {
        <form class="space-y-4" (ngSubmit)="reserver()">
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="block">
              <span class="mb-1.5 block text-sm font-semibold text-ink-700">ID Créneau *</span>
              <input class="field" type="number" placeholder="1" [(ngModel)]="form.creneauId" name="creneau" required />
              <p class="mt-1 text-xs text-ink-400">Trouvez l'ID dans l'Agenda ci-dessus</p>
            </label>
            <label class="block">
              <span class="mb-1.5 block text-sm font-semibold text-ink-700">ID Patient *</span>
              <input class="field" placeholder="patient-001" [(ngModel)]="form.patientId" name="patient" required />
            </label>
          </div>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">Motif de la consultation *</span>
            <input class="field" placeholder="Consultation de routine, suivi, urgence…" [(ngModel)]="form.motif" name="motif" required />
          </label>
          <label class="block">
            <span class="mb-1.5 block text-sm font-semibold text-ink-700">ID Analyse <span class="font-normal text-ink-400">(obligatoire si laboratoire)</span></span>
            <input class="field" type="number" placeholder="Laisser vide si médecin" [(ngModel)]="form.analyseId" name="analyse" />
          </label>

          @if (erreur()) {
            <p class="rounded-xl bg-rose-50 px-4 py-3 text-sm font-medium text-rose-700">{{ erreur() }}</p>
          }

          <button type="submit" class="btn btn-primary shine w-full py-3.5 text-sm" [disabled]="envoi()">
            @if (envoi()) {
              <mv-icon name="refresh" [size]="17" class="animate-spin" /> Réservation en cours…
            } @else {
              <mv-icon name="check" [size]="17" /> Confirmer le rendez-vous
            }
          </button>
        </form>
      }
    </div>
  `
})
export class PriseRdvComponent {
  protected api = inject(RendezvousService);
  protected rdvCree = signal<RendezVous | null>(null);
  protected envoi = signal(false);
  protected erreur = signal('');

  protected form = { creneauId: null as number | null, patientId: '', motif: '', analyseId: null as number | null };

  reserver() {
    if (!this.form.creneauId || !this.form.patientId.trim() || !this.form.motif.trim()) {
      this.erreur.set('Les champs Créneau, Patient et Motif sont obligatoires.');
      return;
    }
    this.erreur.set('');
    this.envoi.set(true);
    this.api.reserver({
      creneauId: this.form.creneauId!,
      patientId: this.form.patientId.trim(),
      motif: this.form.motif.trim(),
      analyseId: this.form.analyseId || null
    }).subscribe({
      next: rdv => { this.rdvCree.set(rdv); this.envoi.set(false); },
      error: () => { this.erreur.set('Erreur lors de la réservation. Vérifiez que le créneau est disponible.'); this.envoi.set(false); }
    });
  }

  reset() {
    this.form = { creneauId: null, patientId: '', motif: '', analyseId: null };
  }

  protected formatDate(d: string) {
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' });
  }
}
