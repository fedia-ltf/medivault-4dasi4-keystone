import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { Creneau, RendezVous } from '../models/rendezvous.model';
import { AuthService } from '../../../core/auth.service';

interface PraticienOption {
  id: string;
  nom: string;
  specialite: string;
  type: 'MEDECIN' | 'LABORANTIN';
  adresse: string;
  icon: string;
}

// Espace dédié pour le rôle PATIENT
// Aucune saisie d'identifiants (patientId et creneauId automatiques)
@Component({
  selector: 'mv-espace-patient',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="space-y-6">

      <!-- Bandeau Patient personnalisé -->
      <div class="card p-6 bg-gradient-to-r from-teal-500/10 via-aqua-500/5 to-transparent border-teal-200/60">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-teal-500 text-white shadow-md">
              <mv-icon name="user" [size]="28" />
            </div>
            <div>
              <span class="chip bg-teal-100 text-teal-800 text-xs font-bold uppercase tracking-wider">
                Espace Patient Sécurisé
              </span>
              <h2 class="mt-1 font-display text-3xl text-ink-900">Bienvenue, {{ auth.nom() }}</h2>
              <p class="text-sm text-ink-500">Prenez vos rendez-vous médicaux et consultez votre historique en toute simplicité.</p>
            </div>
          </div>
          <div class="flex items-center gap-2">
            <button (click)="rafraichir()" class="btn btn-soft px-4 py-2.5 text-xs">
              <mv-icon name="refresh" [size]="15" [class.animate-spin]="chargement()" /> Actualiser mes rendez-vous
            </button>
          </div>
        </div>
      </div>

      <!-- Messages de confirmation ou erreur -->
      @if (confirmation()) {
        <div class="enter rounded-2xl bg-emerald-50 border border-emerald-200 p-5 flex items-start gap-3.5 shadow-sm">
          <div class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-emerald-100 text-emerald-700">
            <mv-icon name="check" [size]="20" [stroke]="3" />
          </div>
          <div class="flex-1">
            <p class="font-bold text-emerald-900 text-base">Rendez-vous confirmé avec succès !</p>
            <p class="text-sm text-emerald-700 mt-0.5">{{ confirmation() }}</p>
            <p class="text-xs text-emerald-600 mt-1">L'accès à votre dossier médical a été automatiquement accordé au praticien.</p>
          </div>
          <button (click)="confirmation.set(null)" class="text-emerald-500 hover:text-emerald-700 text-sm">
            <mv-icon name="x" [size]="16" />
          </button>
        </div>
      }

      @if (erreur()) {
        <div class="enter rounded-2xl bg-rose-50 border border-rose-200 p-4 flex items-center gap-3 text-rose-700 text-sm">
          <mv-icon name="alert" [size]="18" /> {{ erreur() }}
        </div>
      }

      <!-- Formulaire de Prise de Rendez-vous par étapes -->
      <div class="card p-6 lg:p-8">
        <h3 class="font-display text-2xl text-ink-900 mb-6 flex items-center gap-3">
          <span class="grid h-8 w-8 place-items-center rounded-xl bg-aqua-50 text-aqua-600 text-sm font-bold">1</span>
          Prendre un nouveau rendez-vous
        </h3>

        <!-- Étape 1 : Choisir le praticien ou laboratoire -->
        <div class="mb-6">
          <label class="block text-sm font-bold text-ink-800 mb-2">Choisir un médecin ou un laboratoire :</label>
          <div class="grid gap-3 sm:grid-cols-2">
            @for (p of praticiens; track p.id) {
              <div
                (click)="selectionnerPraticien(p)"
                class="cursor-pointer rounded-2xl border-2 p-4 transition-all flex items-center gap-3.5 {{ praticienChoisi()?.id === p.id ? 'border-teal-500 bg-teal-50/40 shadow-sm ring-2 ring-teal-400/20' : 'border-ink-100 hover:border-ink-200 bg-white' }}">
                <div class="grid h-12 w-12 shrink-0 place-items-center rounded-xl {{ p.type === 'MEDECIN' ? 'bg-aqua-100 text-aqua-700' : 'bg-violet-100 text-violet-700' }}">
                  <mv-icon [name]="p.icon" [size]="22" />
                </div>
                <div class="flex-1">
                  <div class="flex items-center justify-between">
                    <p class="font-bold text-ink-900 text-sm">{{ p.nom }}</p>
                    <span class="chip text-[11px] {{ p.type === 'MEDECIN' ? 'bg-aqua-50 text-aqua-700' : 'bg-violet-50 text-violet-700' }}">
                      {{ p.type }}
                    </span>
                  </div>
                  <p class="text-xs text-ink-500 mt-0.5">{{ p.specialite }} · {{ p.adresse }}</p>
                </div>
              </div>
            }
          </div>
        </div>

        <!-- Étape 2 : Consulter les créneaux disponibles et en choisir un -->
        @if (praticienChoisi(); as praticien) {
          <div class="mb-6 enter">
            <div class="flex items-center justify-between mb-2">
              <label class="block text-sm font-bold text-ink-800">
                Créneaux disponibles avec {{ praticien.nom }} :
              </label>
              @if (chargementCreneaux()) {
                <span class="text-xs text-ink-400 flex items-center gap-1.5">
                  <span class="h-3 w-3 animate-spin rounded-full border-2 border-teal-500 border-t-transparent"></span>
                  Recherche des disponibilités…
                </span>
              }
            </div>

            @if (creneauxLibres().length === 0 && !chargementCreneaux()) {
              <div class="rounded-xl border border-dashed border-ink-200 p-6 text-center text-ink-400 text-sm">
                Aucun créneau libre pour le moment. Veuillez réessayer plus tard ou choisir un autre professionnel.
              </div>
            } @else {
              <div class="grid gap-2 sm:grid-cols-3 lg:grid-cols-4">
                @for (c of creneauxLibres(); track c.id) {
                  <button
                    type="button"
                    (click)="creneauChoisi.set(c)"
                    class="rounded-xl border-2 p-3 text-left transition-all cursor-pointer {{ creneauChoisi()?.id === c.id ? 'border-teal-600 bg-teal-500 text-white shadow-md' : 'border-ink-100 bg-white hover:border-teal-300 hover:bg-teal-50/30' }}">
                    <p class="text-xs font-semibold {{ creneauChoisi()?.id === c.id ? 'text-teal-100' : 'text-ink-400' }}">
                      {{ formaterJour(c.dateDebut) }}
                    </p>
                    <p class="font-bold text-base mt-0.5 {{ creneauChoisi()?.id === c.id ? 'text-white' : 'text-ink-900' }}">
                      {{ formaterHeure(c.dateDebut) }} – {{ formaterHeure(c.dateFin) }}
                    </p>
                  </button>
                }
              </div>
            }
          </div>
        }

        <!-- Étape 3 : Saisie du motif et validation -->
        @if (creneauChoisi(); as creneau) {
          <form class="space-y-4 pt-4 border-t border-ink-100 enter" (ngSubmit)="confirmerReservation()">
            <div class="rounded-xl bg-teal-50/60 border border-teal-100 p-4 flex items-center gap-3">
              <mv-icon name="calendar" [size]="20" class="text-teal-600 shrink-0" />
              <p class="text-sm text-teal-900">
                Créneau sélectionné : <strong>{{ formaterDateComplete(creneau.dateDebut) }}</strong> avec <strong>{{ praticienChoisi()?.nom }}</strong>
              </p>
            </div>

            <div class="grid gap-4 sm:grid-cols-2">
              <label class="block">
                <span class="mb-1 block text-sm font-semibold text-ink-700">Motif de votre consultation *</span>
                <input
                  class="field"
                  placeholder="Ex : Consultation générale, toux, contrôle annuel…"
                  [(ngModel)]="motif"
                  name="motif"
                  required />
              </label>

              <!-- Si laboratoire : sélection de l'analyse prescrite -->
              @if (praticienChoisi()?.type === 'LABORANTIN') {
                <label class="block enter">
                  <span class="mb-1 block text-sm font-semibold text-ink-700">Analyse prescrite (Ordonnance médicale) *</span>
                  <select class="field" [(ngModel)]="analyseId" name="analyse" required>
                    <option [ngValue]="null">-- Choisir votre prescription médicale --</option>
                    <option [ngValue]="2">Analyse #2 · NFS (Bilan sanguin complet) · Prescrit par Dr Karim</option>
                    <option [ngValue]="1">Analyse #1 · Glycémie à jeun · Prescrit par Dr Karim</option>
                  </select>
                </label>
              } @else {
                <label class="block">
                  <span class="mb-1 block text-sm font-semibold text-ink-700">Description ou précision (facultatif)</span>
                  <input
                    class="field"
                    placeholder="Remarques éventuelles pour le médecin"
                    [(ngModel)]="description"
                    name="description" />
                </label>
              }
            </div>

            <button
              type="submit"
              [disabled]="envoi()"
              class="btn btn-primary shine py-3.5 px-8 text-sm mt-2 w-full sm:w-auto">
              @if (envoi()) {
                <mv-icon name="refresh" [size]="16" class="animate-spin" /> Enregistrement en cours…
              } @else {
                <mv-icon name="check" [size]="16" /> Confirmer ma réservation
              }
            </button>
          </form>
        }
      </div>

      <!-- Section : Mes Rendez-vous & Annulation -->
      <div class="card p-6 lg:p-8">
        <div class="flex items-center justify-between mb-6">
          <div>
            <h3 class="font-display text-2xl text-ink-900">Mes Rendez-vous</h3>
            <p class="text-sm text-ink-400">Consultez l'historique et gérez vos rendez-vous médicaux</p>
          </div>
          <span class="chip bg-ink-50 text-ink-600 text-xs font-semibold">
            {{ mesRdv().length }} rendez-vous au total
          </span>
        </div>

        @if (chargement()) {
          <div class="space-y-3">
            @for (i of [1,2,3]; track i) {
              <div class="h-20 rounded-2xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
            }
          </div>
        } @else if (mesRdv().length === 0) {
          <div class="py-12 text-center text-ink-400 text-sm">
            Vous n'avez aucun rendez-vous pour le moment. Utilisez le formulaire ci-dessus pour réserver votre première consultation !
          </div>
        } @else {
          <div class="space-y-3">
            @for (r of mesRdv(); track r.id) {
              <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4 rounded-2xl border border-ink-100 bg-white p-5 transition hover:border-teal-200 hover:shadow-sm">
                <div class="flex items-center gap-4">
                  <div class="grid h-12 w-12 shrink-0 place-items-center rounded-2xl {{ tonStatutBg(r.statut) }}">
                    <mv-icon [name]="r.typeProfessionnel === 'LABORANTIN' ? 'flask' : 'stethoscope'" [size]="22" />
                  </div>
                  <div>
                    <div class="flex items-center gap-2">
                      <p class="font-bold text-ink-900 text-base">
                        {{ formaterDateComplete(r.dateDebut) }}
                      </p>
                      <span class="chip text-xs {{ tonStatutChip(r.statut) }}">{{ r.statut }}</span>
                    </div>
                    <p class="text-sm text-ink-600 mt-0.5">
                      <strong>{{ r.professionnelId === 'labo-biolab' ? 'Laboratoire BioLab' : 'Dr Karim Trabelsi' }}</strong>
                      · Motif : {{ r.motif }}
                    </p>
                    @if (r.analyseId) {
                      <p class="text-xs text-violet-600 mt-0.5 font-medium">
                        🔬 Prescription médicale d'analyse #{{ r.analyseId }} associée
                      </p>
                    }
                  </div>
                </div>

                <div class="flex items-center gap-2">
                  @if (r.statut === 'CONFIRME') {
                    <button
                      type="button"
                      (click)="annulerRdv(r.id)"
                      class="btn btn-soft text-rose-600 hover:bg-rose-50 px-4 py-2.5 text-xs font-semibold">
                      <mv-icon name="x" [size]="14" /> Annuler ce rendez-vous
                    </button>
                  }
                </div>
              </div>
            }
          </div>
        }
      </div>

    </div>
  `
})
export class EspacePatientComponent {
  protected auth = inject(AuthService);
  protected api = inject(RendezvousService);

  protected praticiens: PraticienOption[] = [
    {
      id: 'medecin-karim',
      nom: 'Dr Karim Trabelsi',
      specialite: 'Médecin Généraliste',
      type: 'MEDECIN',
      adresse: '12 rue de Marseille, Tunis',
      icon: 'stethoscope'
    },
    {
      id: 'labo-biolab',
      nom: 'Laboratoire BioLab',
      specialite: 'Analyses médicales & Prélèvements',
      type: 'LABORANTIN',
      adresse: '5 avenue Habib Bourguiba, Ariana',
      icon: 'flask'
    }
  ];

  protected praticienChoisi = signal<PraticienOption | null>(null);
  protected creneauxLibres = signal<Creneau[]>([]);
  protected creneauChoisi = signal<Creneau | null>(null);
  protected chargementCreneaux = signal(false);

  protected motif = '';
  protected description = '';
  protected analyseId: number | null = null;
  protected envoi = signal(false);
  protected confirmation = signal<string | null>(null);
  protected erreur = signal<string | null>(null);

  protected mesRdv = signal<RendezVous[]>([]);
  protected chargement = signal(true);

  constructor() {
    this.rafraichir();
    // Sélectionner Dr Karim par défaut pour une prise en main immédiate
    this.selectionnerPraticien(this.praticiens[0]);
  }

  selectionnerPraticien(p: PraticienOption) {
    this.praticienChoisi.set(p);
    this.creneauChoisi.set(null);
    this.chargementCreneaux.set(true);

    this.api.creneauxLibres(p.id).subscribe(creneaux => {
      this.creneauxLibres.set(creneaux);
      this.chargementCreneaux.set(false);
    });
  }

  confirmerReservation() {
    const creneau = this.creneauChoisi();
    const praticien = this.praticienChoisi();
    const patientId = this.auth.id();

    if (!creneau || !patientId || !this.motif.trim()) {
      this.erreur.set('Veuillez renseigner le motif de votre consultation.');
      return;
    }

    if (praticien?.type === 'LABORANTIN' && !this.analyseId) {
      this.erreur.set('Veuillez sélectionner l\'analyse prescrite par votre médecin pour réserver au laboratoire.');
      return;
    }

    this.erreur.set(null);
    this.envoi.set(true);

    this.api.reserver({
      creneauId: creneau.id,
      patientId: patientId,
      motif: this.motif.trim(),
      analyseId: this.analyseId
    }).subscribe({
      next: rdv => {
        this.confirmation.set(`Rendez-vous réservé le ${this.formaterDateComplete(rdv.dateDebut)}.`);
        this.envoi.set(false);
        this.motif = '';
        this.description = '';
        this.analyseId = null;
        this.creneauChoisi.set(null);
        this.rafraichir();
        if (praticien) this.selectionnerPraticien(praticien);
      },
      error: () => {
        this.erreur.set('Impossible de confirmer ce rendez-vous. Le créneau est peut-être déjà réservé.');
        this.envoi.set(false);
      }
    });
  }

  annulerRdv(id: number) {
    this.api.annuler(id).subscribe({
      next: () => {
        this.confirmation.set(`Le rendez-vous #${id} a été annulé avec succès.`);
        this.rafraichir();
        const p = this.praticienChoisi();
        if (p) this.selectionnerPraticien(p);
      },
      error: () => this.erreur.set('Impossible d\'annuler ce rendez-vous.')
    });
  }

  rafraichir() {
    const patientId = this.auth.id();
    if (!patientId) return;

    this.chargement.set(true);
    this.api.parPatient(patientId).subscribe(rdvs => {
      this.mesRdv.set(rdvs);
      this.chargement.set(false);
    });
  }

  protected formaterJour(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short' });
  }

  protected formaterHeure(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  protected formaterDateComplete(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' });
  }

  protected tonStatutBg(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-50 text-emerald-600' : (s === 'ANNULE' ? 'bg-rose-50 text-rose-600' : 'bg-slate-100 text-slate-600');
  }

  protected tonStatutChip(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-100 text-emerald-800' : (s === 'ANNULE' ? 'bg-rose-100 text-rose-800' : 'bg-slate-100 text-slate-700');
  }
}
