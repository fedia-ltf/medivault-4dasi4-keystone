import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { RendezvousService } from '../services/rendezvous.service';
import { Creneau, CreneauRequest, RendezVous } from '../models/rendezvous.model';
import { AuthService } from '../../../core/auth.service';

// Espace professionnel dédié au rôle LABORANTIN
// Permet au laboratoire de gérer ses créneaux de prélèvement :
// - Consulter ses créneaux
// - Ajouter un créneau horaire de prélèvement
// - Modifier un créneau existant
// - Supprimer un créneau libre
// - Suivre les rendez-vous d'analyses avec ordonnances
@Component({
  selector: 'mv-espace-laborantin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon],
  template: `
    <div class="space-y-6">

      <!-- En-tête Espace Laboratoire -->
      <div class="card p-6 bg-gradient-to-r from-violet-500/10 via-purple-500/5 to-transparent border-violet-200/60">
        <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <div class="grid h-14 w-14 shrink-0 place-items-center rounded-2xl bg-violet-600 text-white shadow-md">
              <mv-icon name="flask" [size]="28" />
            </div>
            <div>
              <div class="flex items-center gap-2">
                <span class="chip bg-violet-100 text-violet-800 text-xs font-bold uppercase tracking-wider">
                  Espace Laboratoire d'Analyses
                </span>
                <span class="font-mono text-xs text-ink-400">ID : {{ laboId() }}</span>
              </div>
              <h2 class="mt-1 font-display text-3xl text-ink-900">{{ auth.nom() }}</h2>
              <p class="text-sm text-ink-500">Prélèvements sanguins, vérification des prescriptions et suivi des rendez-vous d'analyses.</p>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2">
            <button
              (click)="afficherFormulaireAjout.set(!afficherFormulaireAjout())"
              class="btn bg-violet-600 hover:bg-violet-700 text-white shine px-4 py-2.5 text-xs flex items-center gap-2">
              <mv-icon [name]="afficherFormulaireAjout() ? 'x' : 'plus'" [size]="15" />
              {{ afficherFormulaireAjout() ? 'Fermer' : 'Ajouter un créneau' }}
            </button>
            <button
              (click)="genererCreneaux()"
              [disabled]="generationEnCours()"
              class="btn btn-soft px-4 py-2.5 text-xs flex items-center gap-2">
              @if (generationEnCours()) {
                <mv-icon name="refresh" [size]="15" class="animate-spin" /> Génération…
              } @else {
                <mv-icon name="sparkles" [size]="15" /> Générer créneaux
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

      <!-- Formulaire d'ajout d'un créneau de prélèvement -->
      @if (afficherFormulaireAjout()) {
        <div class="card p-6 border-2 border-violet-300 bg-gradient-to-br from-violet-50/40 via-white to-white enter shadow-lg">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center gap-2">
              <div class="grid h-8 w-8 place-items-center rounded-lg bg-violet-600 text-white">
                <mv-icon name="plus" [size]="18" />
              </div>
              <h3 class="font-bold text-ink-900 text-lg">Ajouter un créneau horaire de prélèvement</h3>
            </div>
            <span class="text-xs text-ink-400">Ce créneau sera immédiatement sélectionnable par les patients</span>
          </div>

          <form (ngSubmit)="ajouterCreneau()" class="space-y-4">
            <div class="grid gap-4 sm:grid-cols-3">
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Date de prélèvement *
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

            <!-- Raccourcis rapides de prélèvement -->
            <div class="flex flex-wrap items-center gap-2 pt-1 text-xs">
              <span class="text-ink-400 font-medium">Plages types :</span>
              <button
                type="button"
                (click)="appliquerPreset('08:00', '08:20')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-violet-500 transition">
                08:00 – 08:20 (À jeun)
              </button>
              <button
                type="button"
                (click)="appliquerPreset('08:30', '08:50')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-violet-500 transition">
                08:30 – 08:50 (Sanguin)
              </button>
              <button
                type="button"
                (click)="appliquerPreset('09:00', '09:20')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-violet-500 transition">
                09:00 – 09:20 (Sérologie)
              </button>
              <button
                type="button"
                (click)="appliquerPreset('10:00', '10:20')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-violet-500 transition">
                10:00 – 10:20 (Bilan)
              </button>
              <button
                type="button"
                (click)="appliquerPreset('14:00', '14:20')"
                class="rounded-lg bg-white border border-ink-200 px-2.5 py-1 text-ink-700 hover:border-violet-500 transition">
                14:00 – 14:20 (Après-midi)
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
                class="btn bg-violet-600 hover:bg-violet-700 text-white px-5 py-2 text-xs flex items-center gap-2">
                @if (enSauvegarde()) {
                  <mv-icon name="refresh" [size]="14" class="animate-spin" /> Enregistrement…
                } @else {
                  <mv-icon name="check" [size]="14" /> Enregistrer le créneau
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
              <h3 class="font-bold text-ink-900 text-xl">Supprimer ce créneau de prélèvement ?</h3>
              <p class="text-sm text-ink-500 mt-1">
                Vous êtes sur le point de retirer le créneau libre <strong class="text-ink-900">#{{ creneauASupprimer()!.id }}</strong>.
              </p>
              <div class="mt-3 p-3 rounded-xl bg-slate-50 border border-slate-200/60 text-xs text-ink-700 flex items-center justify-center gap-2">
                <mv-icon name="clock" [size]="14" class="text-violet-600" />
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
          <div class="card p-6 max-w-lg w-full bg-white shadow-2xl border border-violet-200 space-y-4">
            <div class="flex items-center justify-between border-b border-ink-100 pb-3">
              <div class="flex items-center gap-2">
                <div class="grid h-8 w-8 place-items-center rounded-lg bg-violet-600 text-white">
                  <mv-icon name="edit" [size]="16" />
                </div>
                <h3 class="font-bold text-ink-900 text-lg">Modifier le créneau de prélèvement #{{ creneauEnEdition()!.id }}</h3>
              </div>
              <button (click)="creneauEnEdition.set(null)" class="text-ink-400 hover:text-ink-700">
                <mv-icon name="x" [size]="18" />
              </button>
            </div>

            <form (ngSubmit)="enregistrerModification()" class="space-y-4">
              <div>
                <label class="block text-xs font-semibold text-ink-600 uppercase tracking-wider mb-1">
                  Date de prélèvement *
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
                  class="btn bg-violet-600 hover:bg-violet-700 text-white px-5 py-2 text-xs flex items-center gap-2">
                  @if (enSauvegarde()) {
                    <mv-icon name="refresh" [size]="14" class="animate-spin" /> Enregistrement…
                  } @else {
                    <mv-icon name="check" [size]="14" /> Enregistrer les modifications
                  }
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      <!-- Statistiques Laboratoire -->
      <div class="grid gap-4 sm:grid-cols-3">
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Créneaux de prélèvement</p>
          <p class="font-display text-3xl font-bold text-violet-600 mt-1">{{ totalCreneauxLibres() }}</p>
          <p class="text-xs text-ink-400 mt-1">Disponibles pour rendez-vous</p>
        </div>
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Prélèvements à réaliser</p>
          <p class="font-display text-3xl font-bold text-emerald-600 mt-1">{{ totalAnalysesAttendues() }}</p>
          <p class="text-xs text-ink-400 mt-1">Patients confirmés avec ordonnance</p>
        </div>
        <div class="card p-5">
          <p class="text-xs font-semibold text-ink-400 uppercase tracking-wider">Analyses traitées</p>
          <p class="font-display text-3xl font-bold text-purple-600 mt-1">{{ totalTerminees() }}</p>
          <p class="text-xs text-ink-400 mt-1">Résultats envoyés au dossier</p>
        </div>
      </div>

      <!-- Navigation par onglets Espace Laboratoire -->
      <div class="flex items-center gap-2 border-b border-ink-100 pb-2">
        <button
          type="button"
          (click)="onglet.set('analyses')"
          class="rounded-xl px-4 py-2 text-sm font-semibold transition cursor-pointer {{ onglet() === 'analyses' ? 'bg-violet-50 text-violet-800 shadow-sm' : 'text-ink-500 hover:text-ink-800' }}">
          🔬 Rendez-vous d'Analyses & Patients Attendus ({{ mesRdv().length }})
        </button>
        <button
          type="button"
          (click)="onglet.set('agenda')"
          class="rounded-xl px-4 py-2 text-sm font-semibold transition cursor-pointer {{ onglet() === 'agenda' ? 'bg-violet-50 text-violet-800 shadow-sm' : 'text-ink-500 hover:text-ink-800' }}">
          📅 Mon Agenda de Prélèvement ({{ creneaux().length }})
        </button>
      </div>

      <!-- Onglet 1 : Rendez-vous d'analyses & Patients attendus -->
      @if (onglet() === 'analyses') {
        <div class="card p-6 enter">
          <div class="flex items-center justify-between mb-4">
            <div>
              <h3 class="font-bold text-ink-900 text-lg">Prélèvements programmés et prescriptions associées</h3>
              <span class="text-xs text-ink-400">Le patient doit se présenter avec son ordonnance prescrite</span>
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
              <p>Aucun rendez-vous d'analyse programmé pour le moment.</p>
            </div>
          } @else {
            <div class="overflow-x-auto">
              <table class="w-full text-sm text-left">
                <thead>
                  <tr class="border-b border-ink-100 text-xs font-semibold text-ink-400 uppercase">
                    <th class="py-3 px-3">Date & Heure</th>
                    <th class="py-3 px-3">Patient</th>
                    <th class="py-3 px-3">Type d'analyse demandée</th>
                    <th class="py-3 px-3">Prescription / Ordonnance</th>
                    <th class="py-3 px-3 text-right">Statut</th>
                  </tr>
                </thead>
                <tbody>
                  @for (r of mesRdv(); track r.id) {
                    <tr class="border-b border-ink-50 hover:bg-violet-50/20 transition">
                      <td class="py-3.5 px-3 font-semibold text-ink-900">
                        {{ formaterDateHeure(r.dateDebut) }}
                      </td>
                      <td class="py-3.5 px-3">
                        <span class="font-mono text-xs font-bold text-violet-700 bg-violet-50 px-2.5 py-1 rounded-lg">
                          {{ r.patientId }}
                        </span>
                      </td>
                      <td class="py-3.5 px-3 text-ink-700 font-medium">
                        {{ r.motif }}
                      </td>
                      <td class="py-3.5 px-3">
                        @if (r.analyseId) {
                          <span class="chip bg-sky-50 text-sky-700 text-xs">
                            Ordonnance #{{ r.analyseId }}
                          </span>
                        } @else {
                          <span class="text-xs text-ink-400 italic">Bilan standard</span>
                        }
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

      <!-- Onglet 2 : Mon Agenda de Prélèvement -->
      @if (onglet() === 'agenda') {
        <div class="card p-6 enter">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6">
            <div>
              <h3 class="font-bold text-ink-900 text-lg">Créneaux de prélèvement laboratoire</h3>
              <span class="text-xs text-ink-400">Gérez vos disponibilités de prélèvement : ajoutez, modifiez ou supprimez des créneaux</span>
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
                  class="text-xs text-violet-600 hover:underline">
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
              <p>Aucun créneau de prélèvement disponible pour cette sélection.</p>
              <div class="flex items-center justify-center gap-2 mt-4">
                <button (click)="afficherFormulaireAjout.set(true)" class="btn bg-violet-600 hover:bg-violet-700 text-white px-4 py-2 text-xs">
                  <mv-icon name="plus" [size]="14" /> Ajouter un créneau
                </button>
                <button (click)="genererCreneaux()" class="btn btn-soft px-4 py-2 text-xs">
                  Générer automatiquement
                </button>
              </div>
            </div>
          } @else {
            <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              @for (c of creneauxFiltres(); track c.id) {
                <div class="rounded-2xl border border-ink-100 bg-white p-4 transition-all hover:border-violet-300 hover:shadow-sm flex flex-col justify-between">
                  <div>
                    <div class="flex items-center justify-between">
                      <span class="chip text-xs {{ c.libre ? 'bg-violet-50 text-violet-700 border border-violet-200' : 'bg-rose-50 text-rose-700 border border-rose-200' }}">
                        {{ c.libre ? '● Disponible' : '■ Réservé' }}
                      </span>
                      <span class="font-mono text-xs text-ink-400">#{{ c.id }}</span>
                    </div>

                    <p class="font-bold text-ink-900 text-lg mt-3 flex items-center gap-2">
                      <mv-icon name="clock" [size]="16" class="text-violet-600" />
                      {{ formaterHeure(c.dateDebut) }} – {{ formaterHeure(c.dateFin) }}
                    </p>
                    <p class="text-xs text-ink-500 mt-1 capitalize flex items-center gap-1.5">
                      <mv-icon name="calendar" [size]="13" class="text-ink-400" />
                      {{ formaterJour(c.dateDebut) }}
                    </p>
                  </div>

                  <!-- Actions du laborantin -->
                  <div class="mt-4 pt-3 border-t border-ink-100 flex items-center justify-between">
                    @if (c.libre) {
                      <span class="text-[11px] text-ink-400">Créneau modifiable</span>
                      <div class="flex items-center gap-1">
                        <button
                          type="button"
                          (click)="ouvrirModification(c)"
                          title="Modifier les horaires de ce créneau"
                          class="p-1.5 rounded-lg text-ink-500 hover:text-violet-600 hover:bg-violet-50 transition">
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
                        (click)="onglet.set('analyses')"
                        class="text-xs text-violet-600 hover:underline">
                        Voir analyse
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }

    </div>
  `
})
export class EspaceLaborantinComponent {
  protected auth = inject(AuthService);
  protected api = inject(RendezvousService);

  // L'identifiant est verrouillé automatiquement sur l'utilisateur connecté
  protected laboId = signal<string>(this.auth.id() || 'labo-central');

  protected creneaux = signal<Creneau[]>([]);
  protected mesRdv = signal<RendezVous[]>([]);
  protected chargement = signal(true);
  protected generationEnCours = signal(false);
  protected enSauvegarde = signal(false);

  protected messageSucces = signal<string | null>(null);
  protected messageErreur = signal<string | null>(null);
  protected onglet = signal<'analyses' | 'agenda'>('analyses');

  // Ajout
  protected afficherFormulaireAjout = signal(false);
  protected nouvelleDate = signal(new Date().toISOString().substring(0, 10));
  protected nouvelleHeureDebut = signal('08:00');
  protected nouvelleHeureFin = signal('08:20');

  // Modification
  protected creneauEnEdition = signal<Creneau | null>(null);
  protected editDate = signal('');
  protected editHeureDebut = signal('');
  protected editHeureFin = signal('');

  // Suppression moderne
  protected creneauASupprimer = signal<Creneau | null>(null);
  protected enSuppression = signal(false);

  // Filtre
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
    const id = this.laboId();
    this.chargement.set(true);

    this.api.creneauxLibres(id).subscribe(c => {
      this.creneaux.set(c);
      this.chargement.set(false);
    });

    this.api.parProfessionnel(id).subscribe(rdvs => {
      this.mesRdv.set(rdvs);
    });
  }

  // Appliquer des plages horaires prédéfinies de prélèvement
  appliquerPreset(debut: string, fin: string) {
    this.nouvelleHeureDebut.set(debut);
    this.nouvelleHeureFin.set(fin);
  }

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
      professionnelId: this.laboId(),
      typeProfessionnel: 'LABORANTIN',
      dateDebut,
      dateFin
    };

    this.api.creerCreneau(req).subscribe({
      next: () => {
        this.enSauvegarde.set(false);
        this.afficherFormulaireAjout.set(false);
        this.messageSucces.set(`Créneau de prélèvement ajouté pour le ${this.formaterJour(dateDebut)} à ${debut}.`);
        this.chargerTout();
      },
      error: (err) => {
        this.enSauvegarde.set(false);
        this.messageErreur.set("Erreur lors de l'ajout : " + (err.error?.message || err.message));
      }
    });
  }

  ouvrirModification(c: Creneau) {
    this.creneauEnEdition.set(c);
    const d = new Date(c.dateDebut);
    const df = new Date(c.dateFin);

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
      professionnelId: this.laboId(),
      typeProfessionnel: 'LABORANTIN',
      dateDebut,
      dateFin
    };

    this.api.modifierCreneau(c.id, req).subscribe({
      next: () => {
        this.enSauvegarde.set(false);
        this.creneauEnEdition.set(null);
        this.messageSucces.set(`Créneau de prélèvement #${c.id} modifié (${debut} - ${fin}).`);
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
        this.messageSucces.set(`Le créneau de prélèvement #${c.id} a été supprimé.`);
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
    const id = this.laboId();
    this.generationEnCours.set(true);
    this.messageSucces.set(null);
    this.messageErreur.set(null);

    this.api.genererCreneaux(id).subscribe({
      next: (nouveaux) => {
        this.generationEnCours.set(false);
        this.messageSucces.set(`${nouveaux.length} créneaux de prélèvement générés.`);
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

  totalAnalysesAttendues() {
    return this.mesRdv().filter(r => r.statut === 'CONFIRME').length;
  }

  totalTerminees() {
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
