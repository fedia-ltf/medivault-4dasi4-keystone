import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Icon } from '../../../shared/icon';
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
        <p class="mt-4 text-lg text-white/60">Créez une ordonnance et retrouvez celles du patient connecté au contrôleur.</p>
      </div>
    </section>

    <section class="mt-6 grid gap-6 xl:grid-cols-[1fr_1.4fr]">
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
          {{ chargement() ? 'Enregistrement...' : 'Créer l’ordonnance' }}
        </button>
      </form>

      <section class="card p-6">
        <div class="flex items-center justify-between gap-4">
          <div>
            <h3 class="font-display text-2xl text-ink-900">Ordonnances du patient</h3>
            <p class="mt-1 text-sm text-ink-400">Patient ID {{ saisie.patientId }}</p>
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
        <article class="card max-h-[85vh] w-full max-w-2xl overflow-y-auto p-6 shadow-2xl"
                 role="dialog" aria-modal="true" aria-labelledby="ordonnance-details-title"
                 (click)="$event.stopPropagation()">
          <div class="flex items-start justify-between gap-4 border-b border-ink-100 pb-4">
            <div>
              <p class="font-mono text-xs text-aqua-600">Détail de l’ordonnance</p>
              <h3 id="ordonnance-details-title" class="mt-1 font-display text-3xl text-ink-900">{{ ordonnance.code }}</h3>
              <p class="mt-1 text-sm text-ink-400">Émise le {{ ordonnance.dateEmission }} · Expire le {{ ordonnance.dateExpiration }}</p>
            </div>
            <button class="grid h-9 w-9 place-items-center rounded-xl text-xl text-ink-400 hover:bg-ink-50 hover:text-ink-900"
                    type="button" aria-label="Fermer" (click)="fermerDetails()">×</button>
          </div>
          <div class="mt-5 flex items-center justify-between">
            <h4 class="font-semibold text-ink-900">Médicaments prescrits</h4>
            <span class="chip bg-aqua-50 px-3 py-1 text-aqua-700">{{ ordonnance.statut }}</span>
          </div>
          <div class="mt-3 space-y-3">
            @for (ligne of ordonnance.lignes; track $index; let i = $index) {
              <div class="rounded-2xl border border-ink-100 bg-ink-50/50 p-4">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <p class="text-xs font-mono text-ink-400">Médicament {{ i + 1 }}</p>
                    <p class="mt-1 font-semibold text-ink-900">{{ ligne.nomMedicament }}</p>
                  </div>
                  <span class="rounded-lg bg-white px-2.5 py-1 text-xs font-medium text-ink-500">ID {{ ligne.medicamentId }}</span>
                </div>
                <div class="mt-3 grid gap-2 text-sm text-ink-500 sm:grid-cols-3">
                  <span><strong class="text-ink-700">Posologie :</strong> {{ ligne.posologie }}</span>
                  <span><strong class="text-ink-700">Quantité :</strong> {{ ligne.quantite }}</span>
                  <span><strong class="text-ink-700">Durée :</strong> {{ ligne.dureeJours }} jours</span>
                </div>
              </div>
            }
          </div>
          <button class="btn btn-primary mt-6 w-full" type="button" (click)="fermerDetails()">Fermer</button>
        </article>
      </div>
    }
  `
})
export class OrdonnancesPage {
  private readonly api = inject(OrdonnanceService);
  protected readonly saisie: OrdonnanceRequest = { patientId: 1, medecinId: 1, lignes: [] };
  protected readonly lignes: LigneOrdonnance[] = [this.nouvelleLigne()];
  protected readonly ordonnances = signal<Ordonnance[]>([]);
  protected readonly chargementListe = signal(true);
  protected readonly chargement = signal(false);
  protected readonly erreur = signal('');
  protected readonly succes = signal('');
  protected readonly ordonnanceSelectionnee = signal<Ordonnance | null>(null);

  constructor() {
    this.charger();
  }

  protected charger() {
    this.chargementListe.set(true);
    this.api.parPatient(this.saisie.patientId).subscribe({
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

  protected creer() {
    this.erreur.set('');
    this.succes.set('');
    const lignesValides = this.lignes.every(ligne =>
      ligne.medicamentId > 0 &&
      ligne.nomMedicament.trim() &&
      ligne.posologie.trim() &&
      ligne.dureeJours > 0 &&
      ligne.quantite > 0
    );
    if (!this.saisie.patientId || !this.saisie.medecinId || !this.lignes.length || !lignesValides) {
      this.erreur.set('Veuillez remplir tous les champs avec des valeurs valides.');
      return;
    }
    this.chargement.set(true);
    this.api.creer({ ...this.saisie, lignes: this.lignes.map(ligne => ({ ...ligne })) }).subscribe({
      next: ordonnance => {
        this.chargement.set(false);
        this.succes.set(`Ordonnance ${ordonnance.code} créée avec succès.`);
        this.charger();
      },
      error: () => { this.chargement.set(false); this.erreur.set('La création a été refusée par le serveur.'); }
    });
  }

  private nouvelleLigne(): LigneOrdonnance {
    return { medicamentId: 1, nomMedicament: '', posologie: '', dureeJours: 1, quantite: 1 };
  }
}
