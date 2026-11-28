import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { Creneau, CreneauRequest, RendezVous } from '../models/rendezvous.model';
import { AuthService } from '../../../core/auth.service';

// Espace professionnel dédié au rôle MEDECIN
// Permet au médecin de gérer ses créneaux de consultation :
// - Consulter ses créneaux
// - Ajouter manuellement un créneau horaire
// - Modifier un créneau existant
// - Supprimer un créneau libre
// - Générer automatiquement son planning de la semaine
@Component({
  selector: 'mv-espace-medecin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="space-y-6">

      <!-- En-tête Espace Médecin -->
      <div class="card p-6 bg-gradient-to-r from-aqua-500/10 via-sky-500/5 to-transparent border-aqua-200/60">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-aqua-500 text-white shadow-md">
              <mv-icon name="stethoscope" [size]="28" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span class="chip bg-aqua-100 text-aqua-800 text-xs font-bold uppercase tracking-wider">
                  Espace Cabinet Médical
                </span>
                <span class="font-mono text-xs text-ink-400">ID : {{ medecinId() }}</span>
              </div>
              <h2 class="mt-1 font-display text-3xl text-ink-900">{{ auth.nom() }}</h2>
              <p class="text-sm text-ink-500">Gestion de vos disponibilités, plages horaires de consultation et suivi des patients.</p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <button
              (click)="afficherFormulaireAjout.set(!afficherFormulaireAjout())"
              class="btn btn-primary shine px-4 py-2.5 text-xs flex items-center gap-2">
              <mv-icon [name]="afficherFormulaireAjout() ? 'x' : 'plus'" [size]="15" />
              {{ afficherFormulaireAjout() ? 'Fermer le formulaire' : 'Ajouter un créneau' }}
            </button>
            <button
              (click)="genererCreneaux()"
              [disabled]="generationEnCours()"
              class="btn btn-soft px-4 py-2.5 text-xs flex items-center gap-2">
              @if (generationEnCours()) {
                <mv-icon name="refresh" [size]="15" class="animate-spin" /> Génération…
              } @else {
                <mv-icon name="sparkles" [size]="15" /> Générer semaine (7j)
              }
            </button>
            <button (click)="chargerTout()" class="btn btn-soft px-3 py-2.5 text-xs">
              <mv-icon name="refresh" [size]="15" [class.animate-spin]="chargement()" />
            </button>
          </div>
        </div>
      </div>

      <!-- Messages de statut -->
      @if (messageSucces()) {
        <div class="enter rounded-2xl bg-emerald-50 border border-emerald-200 p-4 flex items-center justify-between gap-3 text-emerald-800 text-sm">
          <div class="flex items-center gap-3">
            <mv-icon name="check" [size]="18" />
            <span>{{ messageSucces() }}</span>
          </div>
          <button (click)="messageSucces.set(null)" class="text-emerald-600 hover:text-emerald-900">
            <mv-icon name="x" [size]="16" />
          </button>
        </div>
      }

      @if (messageErreur()) {
        <div class="enter rounded-2xl bg-rose-50 border border-rose-200 p-4 flex items-center justify-between gap-3 text-rose-800 text-sm">
          <div class="flex items-center gap-3">
            <mv-icon name="alert" [size]="18" />
            <span>{{ messageErreur() }}</span>
          </div>
          <button (click)="messageErreur.set(null)" class="text-rose-600 hover:text-rose-900">
            <mv-icon name="x" [size]="16" />
          </button>
        </div>
      }

      <!-- Formulaire d'ajout d'un nouveau créneau horaire -->
      @if (afficherFormulaireAjout()) {
        <div class="card p-6 border-2 border-aqua-300 bg-gradient-to-br from-aqua-50/40 via-white to-white enter shadow-lg">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-2">
              <div class="grid h-8 w-8 place-items-center rounded-lg bg-aqua-500 text-white">
                <mv-icon name="plus" [size]="18" />
              </div>
              <h3 class="font-bold text-ink-900 text-lg">Définir un nouvel horaire de consultation</h3>
            </div>
            <span class="text-xs text-ink-400">Ce créneau sera immédiatement ouvert à la réservation patient</span>
          </div>

          <form (ngSubmit)="ajouterCreneau()" class="space-y-4">
            <div class="grid gap-4 sm:grid-cols-3">
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Date de consultation *
                </label>
                <input
                  type="date"
                  [(ngModel)]="nouvelleDate"
                  name="nouvelleDate"
                  required
                  class="input w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Heure de début *
                </label>
                <input
                  type="time"
                  [(ngModel)]="nouvelleHeureDebut"
                  name="nouvelleHeureDebut"
                  required
                  class="input w-full text-sm" />
              </div>
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Heure de fin *
                </label>
                <input
                  type="time"
                  [(ngModel)]="nouvelleHeureFin"
                  name="nouvelleHeureFin"
                  required
                  class="input w-full text-sm" />
              </div>
            </div>

            <!-- Raccourcis rapides -->
            <div class="flex flex-wrap items-center gap-2 pt-1 text-xs">
              <span class="text-ink-400 font-medium">Plages types :</span>
              <button
                type="button"
                (click)="appliquerPreset('09:00', '09:30')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-aqua-500 transition">
                09:00 – 09:30
              </button>
              <button
                type="button"
                (click)="appliquerPreset('10:00', '10:30')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-aqua-500 transition">
                10:00 – 10:30
              </button>
              <button
                type="button"
                (click)="appliquerPreset('14:00', '14:30')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-aqua-500 transition">
                14:00 – 14:30
              </button>
              <button
                type="button"
                (click)="appliquerPreset('15:00', '15:30')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-aqua-500 transition">
                15:00 – 15:30
              </button>
            </div>

            <div class="flex items-center justify-end gap-3 pt-3 border-t border-ink-100">
              <button
                type="button"
                (click)="afficherFormulaireAjout.set(false)"
                class="btn btn-soft px-4 py-2 text-xs">
                Annuler
              </button>
              <button
                type="submit"
                [disabled]="enSauvegarde()"
                class="btn btn-primary px-5 py-2 text-xs flex items-center gap-2">
                @if (enSauvegarde()) {
                  <mv-icon name="refresh" [size]="14" class="animate-spin" /> Enregistrement…
                } @else {
                  <mv-icon name="check" [size]="14" /> Confirmer l'ajout du créneau
                }
              </button>
            </div>
          </form>
        </div>
      }

      <!-- Modale moderne de confirmation de suppression -->
      @if (creneauASupprimer()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/45 backdrop-blur-sm p-4 enter">
          <div class="card p-6 max-w-md w-full bg-white shadow-2xl border border-rose-200 text-center space-y-4">
            <div class="grid h-14 w-14 place-items-center rounded-2xl bg-rose-50 text-rose-600 mx-auto border border-rose-100">
              <mv-icon name="trash" [size]="26" />
            </div>

            <div>
              <h3 class="font-bold text-ink-900 text-xl">Supprimer ce créneau ?</h3>
              <p class="text-sm text-ink-500 mt-1">
                Vous êtes sur le point de retirer le créneau libre <strong class="text-ink-900">#{{ creneauASupprimer()!.id }}</strong>.
              </p>
              <div class="mt-3 p-3 rounded-xl bg-slate-50 border border-slate-200/60 text-xs text-ink-700 flex items-center justify-center gap-2">
                <mv-icon name="clock" [size]="14" class="text-aqua-600" />
                <span class="capitalize">{{ formaterJour(creneauASupprimer()!.dateDebut) }}</span>
                <span>·</span>
                <strong>{{ formaterHeure(creneauASupprimer()!.dateDebut) }} – {{ formaterHeure(creneauASupprimer()!.dateFin) }}</strong>
              </div>
              <p class="text-[11px] text-ink-400 mt-2">
                Cette action est immédiate. Ce créneau ne sera plus proposé aux patients.
              </p>
            </div>

            <div class="flex items-center justify-center gap-3 pt-2 border-t border-ink-100">
              <button
                type="button"
                (click)="annulerSuppression()"
                [disabled]="enSuppression()"
                class="btn btn-soft px-5 py-2.5 text-xs font-semibold">
                Annuler
              </button>
              <button
                type="button"
                (click)="confirmerSuppression()"
                [disabled]="enSuppression()"
                class="btn bg-rose-600 hover:bg-rose-700 text-white px-5 py-2.5 text-xs font-semibold flex items-center gap-2 shadow-md shadow-rose-600/20">
                @if (enSuppression()) {
                  <mv-icon name="refresh" [size]="14" class="animate-spin" /> Suppression…
                } @else {
                  <mv-icon name="trash" [size]="14" /> Confirmer la suppression
                }
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Modale de modification d'un créneau -->
      @if (creneauEnEdition()) {
        <div class="fixed inset-0 z-50 flex items-center justify-center bg-ink-950/40 backdrop-blur-sm p-4 enter">
          <div class="card p-6 max-w-lg w-full bg-white shadow-2xl border border-aqua-200 space-y-4">
            <div class="flex items-center justify-between border-b border-ink-100 pb-3">
              <div class="flex items-center gap-2">
                <div class="grid h-8 w-8 place-items-center rounded-lg bg-amber-500 text-white">
                  <mv-icon name="edit" [size]="16" />
                </div>
                <h3 class="font-bold text-ink-900 text-lg">Modifier le créneau #{{ creneauEnEdition()!.id }}</h3>
              </div>
              <button (click)="creneauEnEdition.set(null)" class="text-ink-400 hover:text-ink-700">
                <mv-icon name="x" [size]="18" />
              </button>
            </div>

            <form (ngSubmit)="enregistrerModification()" class="space-y-4">
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Date de consultation *
                </label>
                <input
                  type="date"
                  [(ngModel)]="editDate"
                  name="editDate"
                  required
                  class="input w-full text-sm" />
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div>
                  <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                    Heure de début *
                  </label>
                  <input
                    type="time"
                    [(ngModel)]="editHeureDebut"
                    name="editHeureDebut"
                    required
                    class="input w-full text-sm" />
                </div>
                <div>
                  <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                    Heure de fin *
                  </label>
                  <input
                    type="time"
                    [(ngModel)]="editHeureFin"
                    name="editHeureFin"
                    required
                    class="input w-full text-sm" />
                </div>
              </div>

              <div class="flex items-center justify-end gap-3 pt-3 border-t border-ink-100">
                <button
                  type="button"
                  (click)="creneauEnEdition.set(null)"
                  class="btn btn-soft px-4 py-2 text-xs">
                  Annuler
                </button>
                <button
                  type="submit"
                  [disabled]="enSauvegarde()"
                  class="btn btn-primary px-5 py-2 text-xs flex items-center gap-2">
                  @if (enSauvegarde()) {
                    <mv-icon name="refresh" [size]="14" class="animate-spin" /> Mise à jour…
                  } @else {
                    <mv-icon name="check" [size]="14" /> Enregistrer les modifications
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Statistiques rapides du médecin -->
      <div class="grid gap-4 sm:grid-cols-3">
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Créneaux libres</p>
          <p class="font-display text-3xl font-bold text-aqua-600 mt-1">{{ totalCreneauxLibres() }}</p>
          <p class="text-xs text-ink-400 mt-1">Disponibles pour réservation</p>
        </div>
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Patients attendus (Confirmés)</p>
          <p class="font-display text-3xl font-bold text-emerald-600 mt-1">{{ totalPatientsAttendus() }}</p>
          <p class="text-xs text-ink-400 mt-1">Consultations programmées</p>
        </div>
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Consultations terminées</p>
          <p class="font-display text-3xl font-bold text-teal-600 mt-1">{{ totalTermines() }}</p>
          <p class="text-xs text-ink-400 mt-1">Dossiers médicaux mis à jour</p>
        </div>
      </div>

      <!-- Navigation par onglets Espace Médecin -->
      <div class="flex items-center gap-2 border-b border-ink-100 pb-2">
        <button
          type="button"
          (click)="onglet.set('agenda')"
          class="rounded-xl px-4 py-2 text-sm font-semibold transition cursor-pointer {{ onglet() === 'agenda' ? 'bg-aqua-50 text-aqua-700 shadow-sm' : 'text-ink-500 hover:text-ink-800' }}">
          📅 Mon Agenda & Créneaux ({{ creneaux().length }})
        </button>
        <button
          type="button"
          (click)="onglet.set('patients')"
          class="rounded-xl px-4 py-2 text-sm font-semibold transition cursor-pointer {{ onglet() === 'patients' ? 'bg-aqua-50 text-aqua-700 shadow-sm' : 'text-ink-500 hover:text-ink-800' }}">
          👥 Mes Patients Attendus ({{ mesRdv().length }})
        </button>
      </div>

      <!-- Onglet 1 : Mon Agenda -->
      @if (onglet() === 'agenda') {
        <div class="card p-6 enter">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
            <div>
              <h3 class="font-bold text-ink-900 text-lg">Créneaux horaires de consultation</h3>
              <span class="text-xs text-ink-400">Gérez vos disponibilités : vous pouvez ajouter, modifier ou supprimer vos créneaux libres</span>
            </div>

            <!-- Filtre par date -->
            <div class="flex items-center gap-2">
              <span class="text-xs text-ink-500">Filtrer par date :</span>
              <input
                type="date"
                [(ngModel)]="filtreDate"
                class="input text-xs py-1.5 px-3" />
              @if (filtreDate()) {
                <button
                  (click)="filtreDate.set('')"
                  class="text-xs text-aqua-600 hover:underline">
                  Effacer
                </button>
              }
            </div>
          </div>

          @if (chargement()) {
            <div class="space-y-2">
              @for (i of [1,2,3,4]; track i) {
                <div class="h-14 rounded-xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
              }
            </div>
          } @else if (creneauxFiltres().length === 0) {
            <div class="py-12 text-center text-ink-400 text-sm">
              <div class="grid h-12 w-12 place-items-center rounded-2xl bg-ink-50 text-ink-300 mx-auto mb-2">
                <mv-icon name="calendar" [size]="24" />
              </div>
              <p>Aucun créneau correspondant à cette sélection.</p>
              <div class="flex items-center justify-center gap-2 mt-4">
                <button (click)="afficherFormulaireAjout.set(true)" class="btn btn-primary px-4 py-2 text-xs">
                  <mv-icon name="plus" [size]="14" /> Ajouter un créneau manuellement
                </button>
                <button (click)="genererCreneaux()" class="btn btn-soft px-4 py-2 text-xs">
                  Générer pour les 7 prochains jours
                </button>
              </div>
            </div>
          } @else {
            <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              @for (c of creneauxFiltres(); track c.id) {
                <div class="rounded-2xl border border-ink-100 bg-white p-4 transition-all hover:border-aqua-300 hover:shadow-sm flex flex-col justify-between">
                  <div>
                    <div class="flex items-center justify-between">
                      <span class="chip text-xs {{ c.libre ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-rose-50 text-rose-700 border border-rose-200' }}">
                        {{ c.libre ? '● Disponible' : '■ Réservé' }}
                      </span>
                      <span class="font-mono text-xs text-ink-400">#{{ c.id }}</span>
                    </div>

                    <p class="font-bold text-ink-900 text-lg mt-3 flex items-center gap-2">
                      <mv-icon name="clock" [size]="16" class="text-aqua-600" />
                      {{ formaterHeure(c.dateDebut) }} – {{ formaterHeure(c.dateFin) }}
                    </p>
                    <p class="text-xs text-ink-500 mt-1 capitalize flex items-center gap-1.5">
                      <mv-icon name="calendar" [size]="13" class="text-ink-400" />
                      {{ formaterJour(c.dateDebut) }}
                    </p>
                  </div>

                  <!-- Actions du médecin sur le créneau -->
                  <div class="mt-4 pt-3 border-t border-ink-100 flex items-center justify-between">
                    @if (c.libre) {
                      <span class="text-[11px] text-ink-400">Créneau modifiable</span>
                      <div class="flex items-center gap-1">
                        <button
                          type="button"
                          (click)="ouvrirModification(c)"
                          title="Modifier les horaires de ce créneau"
                          class="p-1.5 rounded-lg text-ink-500 hover:text-aqua-600 hover:bg-aqua-50 transition">
                          <mv-icon name="edit" [size]="15" />
                        </button>
                        <button
                          type="button"
                          (click)="demanderSuppression(c)"
                          title="Supprimer ce créneau"
                          class="p-1.5 rounded-lg text-ink-500 hover:text-rose-600 hover:bg-rose-50 transition">
                          <mv-icon name="trash" [size]="15" />
                        </button>
                      </div>
                    } @else {
                      <span class="text-[11px] text-rose-600 font-medium">Réservé par un patient</span>
                      <button
                        (click)="onglet.set('patients')"
                        class="text-xs text-aqua-600 hover:underline">
                        Voir détail
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }

      <!-- Onglet 2 : Mes Patients Attendus -->
      @if (onglet() === 'patients') {
        <div class="card p-6 enter">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="font-bold text-ink-900 text-lg">Patients et Consultations programmées</h3>
              <span class="text-xs text-ink-400">Accès direct aux rendez-vous pris par vos patients</span>
            </div>
          </div>

          @if (chargement()) {
            <div class="space-y-3">
              @for (i of [1,2,3]; track i) {
                <div class="h-16 rounded-xl bg-[linear-gradient(90deg,#eef3f9_0%,#f8fafd_50%,#eef3f9_100%)] bg-[length:200%_100%] animate-shimmer"></div>
              }
            </div>
          } @else if (mesRdv().length === 0) {
            <div class="py-12 text-center text-ink-400 text-sm">
              <p>Aucun patient n'a encore réservé sur vos créneaux.</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm text-left">
                <thead>
                  <tr class="border-b border-ink-100 text-xs font-semibold text-ink-400 uppercase">
                    <th class="py-3 px-3">Date & Heure</th>
                    <th class="py-3 px-3">Patient</th>
                    <th class="py-3 px-3">Motif de consultation</th>
                    <th class="py-3 px-3 text-right">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of mesRdv(); track r.id) {
                    <tr class="border-b border-ink-50 hover:bg-aqua-50/20 transition">
                      <td class="py-3.5 px-3 font-semibold text-ink-900">
                        {{ formaterDateHeure(r.dateDebut) }}
                      </td>
                      <td class="py-3.5 px-3">
                        <span class="font-mono text-xs font-bold text-teal-700 bg-teal-50 px-2.5 py-1 rounded-lg">
                          {{ r.patientId }}
                        </span>
                      </td>
                      <td class="py-3.5 px-3 text-ink-700 font-medium">
                        {{ r.motif }}
                      </td>
                      <td class="py-3.5 px-3 text-right">
                        <span class="chip text-xs {{ tonStatutChip(r.statut) }}">
                          {{ r.statut }}
                        </span>
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }

    </div>
  `
})
export class EspaceMedecinComponent {
  protected auth = inject(AuthService);
  protected api = inject(RendezvousService);

  // L'identifiant est verrouillé automatiquement sur l'utilisateur connecté
  protected medecinId = signal<string>(this.auth.id() || 'medecin-karim');

  protected creneaux = signal<Creneau[]>([]);
  protected mesRdv = signal<RendezVous[]>([]);
  protected chargement = signal(true);
  protected generationEnCours = signal(false);
  protected enSauvegarde = signal(false);

  protected messageSucces = signal<string | null>(null);
  protected messageErreur = signal<string | null>(null);
  protected onglet = signal<'agenda' | 'patients'>('agenda');

  // État d'ajout d'un créneau
  protected afficherFormulaireAjout = signal(false);
  protected nouvelleDate = signal(new Date().toISOString().substring(0, 10));
  protected nouvelleHeureDebut = signal('09:00');
  protected nouvelleHeureFin = signal('09:30');

  // État de modification d'un créneau
  protected creneauEnEdition = signal<Creneau | null>(null);
  protected editDate = signal('');
  protected editHeureDebut = signal('');
  protected editHeureFin = signal('');

  // État de suppression moderne
  protected creneauASupprimer = signal<Creneau | null>(null);
  protected enSuppression = signal(false);

  // Filtre par date
  protected filtreDate = signal('');

  protected creneauxFiltres = computed(() => {
    const date = this.filtreDate();
    const liste = this.creneaux();
    if (!date) return liste;
    return liste.filter(c => c.dateDebut.startsWith(date));
  });

  constructor() {
    this.chargerTout();
  }

  chargerTout() {
    const id = this.medecinId();
    this.chargement.set(true);

    this.api.creneauxLibres(id).subscribe(c => {
      this.creneaux.set(c);
      this.chargement.set(false);
    });

    this.api.parProfessionnel(id).subscribe(rdvs => {
      this.mesRdv.set(rdvs);
    });
  }

  // Appliquer des plages horaires prédéfinies
  appliquerPreset(debut: string, fin: string) {
    this.nouvelleHeureDebut.set(debut);
    this.nouvelleHeureFin.set(fin);
  }

  // Ajouter un nouveau créneau ponctuel
  ajouterCreneau() {
    const date = this.nouvelleDate();
    const debut = this.nouvelleHeureDebut();
    const fin = this.nouvelleHeureFin();

    if (!date || !debut || !fin) {
      this.messageErreur.set('Veuillez renseigner la date et les heures.');
      return;
    }

    const dateDebut = `${date}T${debut}:00`;
    const dateFin = `${date}T${fin}:00`;

    if (new Date(dateDebut) >= new Date(dateFin)) {
      this.messageErreur.set("L'heure de fin doit être postérieure à l'heure de début.");
      return;
    }

    this.enSauvegarde.set(true);
    this.messageErreur.set(null);

    const req: CreneauRequest = {
      professionnelId: this.medecinId(),
      typeProfessionnel: 'MEDECIN',
      dateDebut,
      dateFin
    };

    this.api.creerCreneau(req).subscribe({
      next: (nouveau) => {
        this.enSauvegarde.set(false);
        this.afficherFormulaireAjout.set(false);
        this.messageSucces.set(`Créneau de consultation ajouté avec succès pour le ${this.formaterJour(dateDebut)} à ${debut}.`);
        this.chargerTout();
      },
      error: (err) => {
        this.enSauvegarde.set(false);
        this.messageErreur.set("Erreur lors de l'ajout du créneau : " + (err.error?.message || err.message || 'vérifiez vos données'));
      }
    });
  }

  // Ouvrir le dialogue de modification
  ouvrirModification(c: Creneau) {
    this.creneauEnEdition.set(c);
    const d = new Date(c.dateDebut);
    const df = new Date(c.dateFin);

    // Formater en YYYY-MM-DD
    const annee = d.getFullYear();
    const mois = String(d.getMonth() + 1).padStart(2, '0');
    const jour = String(d.getDate()).padStart(2, '0');
    this.editDate.set(`${annee}-${mois}-${jour}`);

    const hDeb = String(d.getHours()).padStart(2, '0');
    const mDeb = String(d.getMinutes()).padStart(2, '0');
    this.editHeureDebut.set(`${hDeb}:${mDeb}`);

    const hFin = String(df.getHours()).padStart(2, '0');
    const mFin = String(df.getMinutes()).padStart(2, '0');
    this.editHeureFin.set(`${hFin}:${mFin}`);
  }

  // Enregistrer la modification
  enregistrerModification() {
    const c = this.creneauEnEdition();
    if (!c) return;

    const date = this.editDate();
    const debut = this.editHeureDebut();
    const fin = this.editHeureFin();

    if (!date || !debut || !fin) {
      this.messageErreur.set('Veuillez renseigner la date et les heures.');
      return;
    }

    const dateDebut = `${date}T${debut}:00`;
    const dateFin = `${date}T${fin}:00`;

    if (new Date(dateDebut) >= new Date(dateFin)) {
      this.messageErreur.set("L'heure de fin doit être postérieure à l'heure de début.");
      return;
    }

    this.enSauvegarde.set(true);
    this.messageErreur.set(null);

    const req: CreneauRequest = {
      professionnelId: this.medecinId(),
      typeProfessionnel: 'MEDECIN',
      dateDebut,
      dateFin
    };

    this.api.modifierCreneau(c.id, req).subscribe({
      next: (modifie) => {
        this.enSauvegarde.set(false);
        this.creneauEnEdition.set(null);
        this.messageSucces.set(`Créneau #${c.id} modifié avec succès (${debut} - ${fin}).`);
        this.chargerTout();
      },
      error: (err) => {
        this.enSauvegarde.set(false);
        this.messageErreur.set("Erreur lors de la modification : " + (err.error?.message || err.message));
      }
    });
  }

  // Gestion moderne de la suppression avec modale
  demanderSuppression(c: Creneau) {
    this.creneauASupprimer.set(c);
  }

  annulerSuppression() {
    this.creneauASupprimer.set(null);
  }

  confirmerSuppression() {
    const c = this.creneauASupprimer();
    if (!c) return;

    this.enSuppression.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);

    this.api.supprimerCreneau(c.id).subscribe({
      next: () => {
        this.enSuppression.set(false);
        this.creneauASupprimer.set(null);
        this.messageSucces.set(`Le créneau #${c.id} a été supprimé.`);
        this.chargerTout();
      },
      error: (err) => {
        this.enSuppression.set(false);
        this.creneauASupprimer.set(null);
        this.messageErreur.set("Impossible de supprimer ce créneau : " + (err.error?.message || err.message));
      }
    });
  }

  genererCreneaux() {
    const id = this.medecinId();
    this.generationEnCours.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);

    this.api.genererCreneaux(id).subscribe({
      next: (nouveaux) => {
        this.generationEnCours.set(false);
        this.messageSucces.set(`${nouveaux.length} créneaux ont été générés selon vos horaires habituels.`);
        this.chargerTout();
      },
      error: () => {
        this.generationEnCours.set(false);
        this.messageSucces.set('Génération terminée.');
        this.chargerTout();
      }
    });
  }

  totalCreneauxLibres() {
    return this.creneaux().filter(c => c.libre).length;
  }

  totalPatientsAttendus() {
    return this.mesRdv().filter(r => r.statut === 'CONFIRME').length;
  }

  totalTermines() {
    return this.mesRdv().filter(r => r.statut === 'TERMINE').length;
  }

  protected formaterJour(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  protected formaterHeure(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' });
  }

  protected formaterDateHeure(d: string) {
    if (!d) return '';
    return new Date(d).toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
  }

  protected tonStatutChip(s: string) {
    return s === 'CONFIRME' ? 'bg-emerald-100 text-emerald-800' : (s === 'ANNULE' ? 'bg-rose-100 text-rose-800' : 'bg-teal-100 text-teal-800');
  }
}
