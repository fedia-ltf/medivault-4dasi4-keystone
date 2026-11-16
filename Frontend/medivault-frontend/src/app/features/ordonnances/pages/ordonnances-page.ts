import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
import { AuthService } from '../../../core/auth.service';
import { LigneOrdonnance, Ordonnance, OrdonnanceRequest } from '../models/ordonnance.model';
import { OrdonnanceService } from '../services/ordonnance.service';
import { OrdonnancesScene } from '../components/ordonnances-scene';

@Component({
  selector: 'mv-ordonnances-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FormsModule, Icon, OrdonnancesScene],
  template: `
    <section class="enter relative overflow-hidden rounded-[2rem] aurora-dark grain p-8 text-white sm:p-12">
      <div class="absolute inset-0 grid-lines"></div>
      <div class="pointer-events-none absolute inset-y-0 right-0 hidden w-[52%] md:block">
        <mv-ordonnances-scene class="h-full w-full" />
      </div>
      <div class="relative max-w-xl">
        <div class="grid h-14 w-14 place-items-center rounded-2xl bg-aqua-400 text-ink-950">
          <mv-icon name="clipboard" [size]="26" />
        </div>
        <p class="mt-6 font-mono text-xs text-aqua-300">ordonnance-service</p>
        <h2 class="mt-2 font-display text-6xl leading-none">Ordonnances</h2>
        <p class="mt-4 text-lg text-white/60">
          {{ estPatient() ? 'Retrouvez toutes vos ordonnances et le détail de vos traitements.' : 'Créez une ordonnance et retrouvez celles du patient connecté au contrôleur.' }}
        </p>
      </div>
    </section>

    <section class="mt-6 grid gap-6 xl:grid-cols-[1fr_1.4fr]">
      @if (!estPatient()) {
        <form class="card p-6" (ngSubmit)="creer()">
        <h3 class="font-display text-2xl text-ink-900">Nouvelle ordonnance</h3>
        <div class="mt-5 grid gap-4 sm:grid-cols-2">
          <label class="text-sm font-medium text-ink-600">Patient ID
            <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" type="number" min="1" name="patientId" [(ngModel)]="saisie.patientId" required>
          </label>
          <label class="text-sm font-medium text-ink-600">Médecin ID
            <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" type="number" min="1" name="medecinId" [(ngModel)]="saisie.medecinId" required>
          </label>
        </div>
        @for (ligne of lignes; track $index; let i = $index) {
          <div class="mt-5 rounded-2xl border border-ink-100 bg-ink-50/40 p-4">
            <div class="mb-3 flex items-center justify-between">
              <p class="text-sm font-semibold text-ink-700">Médicament {{ i + 1 }}</p>
              @if (lignes.length > 1) {
                <button class="text-xs font-semibold text-rose-600 hover:text-rose-700" type="button" (click)="supprimerLigne(i)">
                  Supprimer
                </button>
              }
            </div>
            <label class="block text-sm font-medium text-ink-600">Médicament ID
              <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" type="number" min="1" [name]="'medicamentId-' + i" [(ngModel)]="ligne.medicamentId" required>
            </label>
            <label class="mt-4 block text-sm font-medium text-ink-600">Nom du médicament
              <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" [name]="'nomMedicament-' + i" [(ngModel)]="ligne.nomMedicament" required>
            </label>
            <div class="mt-4 grid gap-4 sm:grid-cols-3">
              <label class="text-sm font-medium text-ink-600">Posologie
                <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" [name]="'posologie-' + i" [(ngModel)]="ligne.posologie" required>
              </label>
              <label class="text-sm font-medium text-ink-600">Durée (jours)
                <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" type="number" min="1" [name]="'dureeJours-' + i" [(ngModel)]="ligne.dureeJours" required>
              </label>
              <label class="text-sm font-medium text-ink-600">Quantité
                <input class="mt-1 w-full rounded-xl border border-ink-100 bg-white px-3 py-2.5 text-ink-900 outline-none transition focus:border-aqua-400 focus:ring-4 focus:ring-aqua-400/10" type="number" min="1" [name]="'quantite-' + i" [(ngModel)]="ligne.quantite" required>
              </label>
            </div>
          </div>
        }
        <button class="btn btn-soft mt-4 w-full px-4 py-2.5 text-sm" type="button" (click)="ajouterLigne()">
          <span class="text-lg leading-none">+</span> Ajouter un médicament
        </button>
        @if (erreur()) { <p class="mt-4 text-sm text-rose-600">{{ erreur() }}</p> }
        @if (succes()) { <p class="mt-4 text-sm text-emerald-600">{{ succes() }}</p> }
        <button class="btn btn-primary mt-6 w-full" type="submit" [disabled]="chargement()">
          {{ chargement() ? 'Enregistrement...' : (ordonnanceEnEdition() ? 'Modifier l’ordonnance' : 'Créer l’ordonnance') }}
        </button>
        </form>
      }

      <section class="card p-6" [class.xl:col-span-2]="estPatient()">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="font-display text-2xl text-ink-900">{{ estPatient() ? 'Mes ordonnances' : 'Toutes les ordonnances' }}</h3>
            <p class="mt-1 text-sm text-ink-400">{{ estPatient() ? 'Historique de vos prescriptions' : 'Historique complet des ordonnances' }}</p>
          </div>
          <button class="btn btn-soft px-4 py-2 text-sm" type="button" (click)="charger()">Actualiser</button>
        </div>
        @if (chargementListe()) {
          <p class="mt-8 text-sm text-ink-400">Chargement...</p>
        } @else if (!ordonnances().length) {
          <p class="mt-8 text-sm text-ink-400">Aucune ordonnance trouvée.</p>
        } @else {
          <div class="mt-5 space-y-3">
            @for (ordonnance of ordonnances(); track ordonnance.id) {
              <article class="rounded-2xl border border-ink-100 p-4">
                <div class="flex items-center justify-between">
                  <strong class="text-ink-900">{{ ordonnance.code }}</strong>
                  <span class="chip bg-aqua-50 px-3 py-1 text-aqua-700">{{ ordonnance.statut }}</span>
                </div>
                <p class="mt-2 text-sm text-ink-500">{{ ordonnance.dateEmission }} · {{ ordonnance.lignes.length }} médicament(s)</p>
                <button class="btn btn-soft mt-4 w-full px-4 py-2 text-sm" type="button" (click)="ouvrirDetails(ordonnance)">
                  Voir le détail de l’ordonnance
                </button>
              </article>
            }
          </div>
        }
      </section>
    </section>

    @if (ordonnanceSelectionnee(); as ordonnance) {
      <div class="fixed inset-0 z-[100] grid place-items-center bg-ink-950/50 p-4 backdrop-blur-sm"
           role="presentation" (click)="fermerDetails()">
        <article class="card max-h-[92vh] w-full max-w-4xl overflow-y-auto bg-white p-0 shadow-2xl"
                 role="dialog" aria-modal="true" aria-labelledby="ordonnance-details-title"
                 (click)="$event.stopPropagation()">
          <header class="relative overflow-hidden bg-gradient-to-br from-ink-950 via-ink-900 to-iris-600 px-6 py-7 text-white sm:px-10">
            <div class="pointer-events-none absolute -right-20 -top-32 h-72 w-72 rounded-full border border-white/10"></div>
            <div class="pointer-events-none absolute -right-8 -top-20 h-48 w-48 rounded-full border border-white/10"></div>
            <div class="relative flex items-start justify-between gap-6">
              <div>
                <p class="text-xs font-bold uppercase tracking-[0.25em] text-aqua-300">MediVault</p>
                <p class="mt-2 text-sm text-white/60">Dossier médical sécurisé</p>
                <div class="mt-5 flex flex-wrap items-baseline gap-3">
                  <h3 id="ordonnance-details-title" class="font-display text-4xl leading-none">Ordonnance</h3>
                  <span class="font-mono text-sm text-white/60">{{ ordonnance.code }}</span>
                </div>
                <p class="mt-3 text-sm text-white/70">Émise le {{ ordonnance.dateEmission }} · Expire le {{ ordonnance.dateExpiration }}</p>
              </div>
              <button class="relative grid h-9 w-9 place-items-center rounded-xl text-xl text-white/60 hover:bg-white/10 hover:text-white"
                      type="button" aria-label="Fermer" (click)="fermerDetails()">×</button>
            </div>
          </header>

          <div class="px-6 py-6 sm:px-10">
            <section class="grid gap-4 rounded-2xl border border-ink-100 bg-gradient-to-br from-white to-ink-50 p-5 sm:grid-cols-[auto_1fr_1fr]">
              <div class="grid h-14 w-14 place-items-center rounded-full bg-gradient-to-br from-aqua-400 to-iris-500 text-sm font-bold text-white">
                {{ auth.initiales() }}
              </div>
              <div>
                <p class="text-[11px] font-bold uppercase tracking-wider text-ink-400">Patient</p>
                <p class="mt-1 text-lg font-semibold text-ink-900">{{ utilisateur()?.nom ?? 'Patient' }}</p>
                <p class="mt-0.5 text-sm text-ink-500">{{ utilisateur()?.email ?? 'Informations non disponibles' }}</p>
              </div>
              <div class="sm:border-l sm:border-ink-200 sm:pl-5">
                <p class="text-[11px] font-bold uppercase tracking-wider text-ink-400">Informations patient</p>
                <p class="mt-1 text-sm text-ink-600">ID patient : <strong>{{ ordonnance.patientId }}</strong></p>
                <p class="mt-0.5 text-sm text-ink-500">Profil : {{ utilisateur()?.role === 'PATIENT' ? 'Patient' : 'Dossier associé' }}</p>
              </div>
            </section>

            <div class="mt-7 flex items-center justify-between gap-3">
              <div>
                <p class="font-display text-3xl text-iris-600">Rp/</p>
                <p class="text-sm text-ink-400">{{ ordonnance.lignes.length }} prescription{{ ordonnance.lignes.length > 1 ? 's' : '' }}</p>
              </div>
              <span class="chip bg-aqua-50 px-3 py-1 text-aqua-700">{{ ordonnance.statut }}</span>
            </div>
            <div class="mt-3 space-y-3">
              @for (ligne of ordonnance.lignes; track $index; let i = $index) {
                <div class="rounded-2xl border border-ink-100 border-l-4 border-l-aqua-400 bg-white p-5 shadow-sm">
                  <div class="flex items-start justify-between gap-4">
                    <div>
                      <p class="text-xs font-mono text-ink-400">Médicament {{ i + 1 }}</p>
                      <p class="mt-1 text-lg font-semibold text-ink-900">{{ ligne.nomMedicament }}</p>
                    </div>
                    <span class="rounded-lg bg-ink-50 px-2.5 py-1 text-xs font-medium text-ink-500">ID {{ ligne.medicamentId }}</span>
                  </div>
                  <p class="mt-2 text-sm text-ink-500">{{ ligne.posologie }}</p>
                  <div class="mt-3 flex flex-wrap gap-2 text-xs font-semibold">
                    <span class="chip bg-aqua-50 text-aqua-700">Quantité : {{ ligne.quantite }}</span>
                    <span class="chip bg-emerald-50 text-emerald-700">Durée : {{ ligne.dureeJours }} jours</span>
                  </div>
                </div>
              }
            </div>

            <div class="mt-5 rounded-2xl border border-aqua-100 bg-aqua-50/60 p-4">
              <p class="text-sm font-semibold text-ink-800">Prescripteur</p>
              <p class="mt-1 text-sm text-ink-500">Médecin ID : {{ ordonnance.medecinId }}</p>
            </div>
            <div class="mt-6 flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
              @if (!estPatient()) {
                <button class="btn btn-soft px-4 py-2.5 text-sm" type="button" (click)="modifierDepuisDetails(ordonnance)">
                  Modifier
                </button>
                <button class="rounded-xl bg-rose-50 px-4 py-2.5 text-sm font-semibold text-rose-700 hover:bg-rose-100"
                        type="button" (click)="demanderSuppression(ordonnance)">
                  Supprimer
                </button>
              }
              <button class="btn btn-primary px-4 py-2.5 text-sm" type="button" (click)="imprimer(ordonnance)">
                Imprimer / PDF
              </button>
              <button class="btn btn-soft px-4 py-2.5 text-sm" type="button" (click)="fermerDetails()">Fermer</button>
            </div>
          </div>
        </article>
      </div>
    }

    @if (ordonnanceASupprimer(); as ordonnance) {
      <div class="fixed inset-0 z-[110] grid place-items-center bg-ink-950/60 p-4 backdrop-blur-sm"
           role="presentation" (click)="annulerSuppression()">
        <article class="card w-full max-w-md p-6 shadow-2xl"
                 role="alertdialog" aria-modal="true" aria-labelledby="delete-title"
                 (click)="$event.stopPropagation()">
          <div class="grid h-12 w-12 place-items-center rounded-2xl bg-rose-50 text-rose-600">
            <mv-icon name="alert" [size]="24" />
          </div>
          <h3 id="delete-title" class="mt-5 font-display text-2xl text-ink-900">Supprimer l’ordonnance ?</h3>
          <p class="mt-2 text-sm leading-6 text-ink-500">
            L’ordonnance <strong class="text-ink-800">{{ ordonnance.code }}</strong> et ses médicaments seront définitivement supprimés.
          </p>
          <div class="mt-6 grid gap-3 sm:grid-cols-2">
            <button class="btn btn-soft w-full px-4 py-2.5 text-sm" type="button" (click)="annulerSuppression()">
              Annuler
            </button>
            <button class="w-full rounded-xl bg-rose-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-rose-700 disabled:opacity-50"
                    type="button" [disabled]="chargement()" (click)="confirmerSuppression()">
              {{ chargement() ? 'Suppression...' : 'Oui, supprimer' }}
            </button>
          </div>
        </article>
      </div>
    }
  `
})
export class OrdonnancesPage {
  private readonly api = inject(OrdonnanceService);
  protected readonly auth = inject(AuthService);
  protected readonly utilisateur = this.auth.utilisateur;
  protected readonly saisie = { patientId: null as number | null, medecinId: null as number | null };
  protected readonly lignes: LigneSaisie[] = [this.nouvelleLigne()];
  protected readonly ordonnances = signal<Ordonnance[]>([]);
  protected readonly chargementListe = signal(true);
  protected readonly chargement = signal(false);
  protected readonly erreur = signal('');
  protected readonly succes = signal('');
  protected readonly ordonnanceSelectionnee = signal<Ordonnance | null>(null);
  protected readonly ordonnanceEnEdition = signal<number | null>(null);
  protected readonly ordonnanceASupprimer = signal<Ordonnance | null>(null);
  protected readonly estPatient = () => this.auth.utilisateur()?.role === 'PATIENT';

  constructor() {
    this.charger();
  }

  protected charger() {
    this.chargementListe.set(true);
    const utilisateur = this.auth.utilisateur();
    const appel = utilisateur?.role === 'PATIENT'
      ? this.api.parPatient(utilisateur.patientId ?? 1)
      : this.api.toutes();
    appel.subscribe({
      next: ordonnances => { this.ordonnances.set(ordonnances); this.chargementListe.set(false); },
      error: () => { this.erreur.set('Impossible de charger les ordonnances.'); this.chargementListe.set(false); }
    });
  }

  protected ajouterLigne() {
    this.lignes.push(this.nouvelleLigne());
  }

  protected supprimerLigne(index: number) {
    this.lignes.splice(index, 1);
  }

  protected ouvrirDetails(ordonnance: Ordonnance) {
    this.ordonnanceSelectionnee.set(ordonnance);
  }

  protected fermerDetails() {
    this.ordonnanceSelectionnee.set(null);
  }

  protected imprimer(ordonnance: Ordonnance) {
    const utilisateur = this.utilisateur();
    const patient = utilisateur?.role === 'PATIENT'
      ? { name: utilisateur.nom, email: utilisateur.email }
      : { name: `Patient #${ordonnance.patientId}`, email: '' };
    const data = encodeURIComponent(JSON.stringify({
      ...ordonnance,
      patient
    }));
    window.open(`/ordonnance-medivault.html?data=${data}`, '_blank', 'noopener,noreferrer');
  }

  protected modifierDepuisDetails(ordonnance: Ordonnance) {
    this.ordonnanceEnEdition.set(ordonnance.id);
    this.saisie.patientId = ordonnance.patientId;
    this.saisie.medecinId = ordonnance.medecinId;
    this.lignes.splice(0, this.lignes.length, ...ordonnance.lignes.map(ligne => ({
      medicamentId: ligne.medicamentId,
      nomMedicament: ligne.nomMedicament,
      posologie: ligne.posologie,
      dureeJours: ligne.dureeJours,
      quantite: ligne.quantite
    })));
    this.fermerDetails();
    this.succes.set('');
    this.erreur.set('');
  }

  protected demanderSuppression(ordonnance: Ordonnance) {
    this.ordonnanceASupprimer.set(ordonnance);
  }

  protected annulerSuppression() {
    if (!this.chargement()) this.ordonnanceASupprimer.set(null);
  }

  protected confirmerSuppression() {
    const ordonnance = this.ordonnanceASupprimer();
    if (!ordonnance) return;
    this.chargement.set(true);
    this.api.supprimer(ordonnance.id).subscribe({
      next: () => {
        this.chargement.set(false);
        this.ordonnanceASupprimer.set(null);
        this.fermerDetails();
        this.succes.set(`Ordonnance ${ordonnance.code} supprimée.`);
        this.charger();
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set('La suppression a été refusée par le serveur.');
      }
    });
  }

  protected creer() {
    this.erreur.set('');
    this.succes.set('');
    const lignesValides = this.lignes.every(ligne =>
      ligne.medicamentId !== null && ligne.medicamentId > 0 &&
      ligne.nomMedicament.trim() &&
      ligne.posologie.trim() &&
      ligne.dureeJours !== null && ligne.dureeJours > 0 &&
      ligne.quantite !== null && ligne.quantite > 0
    );
    if (this.saisie.patientId === null || this.saisie.medecinId === null || !this.lignes.length || !lignesValides) {
      this.erreur.set('Veuillez remplir tous les champs avec des valeurs valides.');
      return;
    }
    this.chargement.set(true);
    const patientId = this.saisie.patientId;
    const request: OrdonnanceRequest = {
      patientId,
      medecinId: this.saisie.medecinId,
      lignes: this.lignes.map(ligne => ({
        medicamentId: ligne.medicamentId!,
        nomMedicament: ligne.nomMedicament,
        posologie: ligne.posologie,
        dureeJours: ligne.dureeJours!,
        quantite: ligne.quantite!
      }))
    };
    const id = this.ordonnanceEnEdition();
    const appel = id === null ? this.api.creer(request) : this.api.modifier(id, request);
    appel.subscribe({
      next: ordonnance => {
        this.chargement.set(false);
        this.ordonnanceEnEdition.set(null);
        this.succes.set(`Ordonnance ${ordonnance.code} ${id === null ? 'créée' : 'modifiée'} avec succès.`);
        this.reinitialiserFormulaire();
        this.charger();
      },
      error: () => { this.chargement.set(false); this.erreur.set('La création a été refusée par le serveur.'); }
    });
  }

  private nouvelleLigne(): LigneSaisie {
    return { medicamentId: null, nomMedicament: '', posologie: '', dureeJours: null, quantite: null };
  }

  private reinitialiserFormulaire() {
    this.saisie.patientId = null;
    this.saisie.medecinId = null;
    this.lignes.splice(0, this.lignes.length, this.nouvelleLigne());
  }
}

interface LigneSaisie {
  medicamentId: number | null;
  nomMedicament: string;
  posologie: string;
  dureeJours: number | null;
  quantite: number | null;
}
