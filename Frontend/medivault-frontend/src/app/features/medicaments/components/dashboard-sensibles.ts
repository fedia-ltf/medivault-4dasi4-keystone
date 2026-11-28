import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../../shared/icon';
import { CountUp } from '../../../shared/count-up';
import { CATEGORIES } from '../models/sensible.model';
import { SensibleService } from '../services/sensible.service';

const CERCLE = 2 * Math.PI * 44;
const DEMI = Math.PI * 80;      // longueur de la demi-jauge
const BAGUE = 2 * Math.PI * 16; // longueur d'une petite bague
// Palette du service, la même que le menu : bleu, vert d'eau, ciel
const COULEURS = { STUPEFIANT: '#2957cc', PSYCHOTROPE: '#12a594', ABORTIF: '#38a8e8' } as const;

// ---------------------------------------------------------------------------
// ADMIN : 6 cartes (3 par ligne). Le fil conducteur est la forme « gélule » (bords arrondis),
// mais chaque carte a sa propre visualisation : demi-jauge, bagues, tuiles, gélules verticales,
// barre fractionnée, classement en gélules.
// ---------------------------------------------------------------------------
@Component({
  selector: 'mv-dashboard-sensibles-admin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, CountUp],
  host: { class: 'block' },
  template: `
    <div class="mb-4 flex flex-wrap items-end justify-between gap-2">
      <div>
        <h3 class="font-display text-3xl leading-none text-ink-900">Médicaments sensibles</h3>
        <p class="mt-1.5 font-mono text-[11px] text-aqua-600">medicament-service <span class="text-ink-300">·</span> <span class="text-ink-400">quotas et ventes du mois</span></p>
      </div>
      <div class="flex items-center gap-2">
        <span class="flex items-center gap-1.5 rounded-full bg-white px-3 py-1 font-mono text-[10px] font-semibold tracking-widest text-aqua-700 ring-1 ring-aqua-200"><span class="h-1.5 w-1.5 animate-pulse rounded-full bg-iris-400"></span>{{ s.enLigne() ? 'EN DIRECT' : 'HORS LIGNE' }}</span>
        <a routerLink="/app/medicaments" class="flex items-center gap-1 rounded-full bg-white px-3 py-1 text-xs font-semibold text-ink-600 ring-1 ring-ink-100 transition hover:text-aqua-600">Gérer <mv-icon name="arrow-right" [size]="13" /></a>
      </div>
    </div>

    <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3 [&>*]:min-w-0">

      <!-- 1. Demi-jauge épaisse + trois repères -->
      <article class="card enter flex flex-col p-5 [animation-delay:60ms]">
        <p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Quotas consommés</p>
        <div class="relative mx-auto mt-3 w-full max-w-[15rem]">
          <svg viewBox="0 0 200 112" class="w-full" aria-hidden="true">
            <defs><linearGradient id="demiG" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#2957cc" /><stop offset="1" stop-color="#2cc5b5" /></linearGradient></defs>
            <path d="M20,104 A80,80 0 0 1 180,104" fill="none" stroke="#e8eefb" stroke-width="16" stroke-linecap="round" />
            <path d="M20,104 A80,80 0 0 1 180,104" fill="none" stroke="url(#demiG)" stroke-width="16" stroke-linecap="round"
                  [attr.stroke-dasharray]="demi" [attr.stroke-dashoffset]="pret() ? demi * (1 - taux() / 100) : demi" style="transition: stroke-dashoffset 1.8s cubic-bezier(.16,1,.3,1)" />
          </svg>
          <div class="absolute inset-x-0 bottom-1 text-center"><span class="font-display text-5xl leading-none text-ink-900"><span [mvCountUp]="taux()"></span><span class="text-xl text-ink-300"> %</span></span></div>
        </div>
        <div class="mt-auto grid grid-cols-3 gap-2 pt-4 text-center">
          <div class="rounded-2xl bg-aqua-50/70 py-2"><p class="font-display text-2xl leading-none text-ink-900">{{ s.totalAttribue() }}</p><p class="mt-1 font-mono text-[9px] uppercase tracking-wider text-ink-400">attribuées</p></div>
          <div class="rounded-2xl bg-aqua-50/70 py-2"><p class="font-display text-2xl leading-none text-ink-900">{{ s.totalVendu() }}</p><p class="mt-1 font-mono text-[9px] uppercase tracking-wider text-ink-400">vendues</p></div>
          <div class="rounded-2xl bg-iris-400/10 py-2"><p class="font-display text-2xl leading-none text-iris-500">{{ restant() }}</p><p class="mt-1 font-mono text-[9px] uppercase tracking-wider text-ink-400">restantes</p></div>
        </div>
      </article>

      <!-- 2. Pharmacies à risque : une petite bague par ligne -->
      <article class="card enter flex flex-col p-5 [animation-delay:120ms]">
        <div class="flex items-center justify-between">
          <p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Pharmacies à risque</p>
          <span class="rounded-full bg-aqua-50 px-2 py-0.5 font-mono text-[10px] text-aqua-700">&gt; 90 %</span>
        </div>
        <ul class="mt-3 flex flex-1 flex-col justify-between gap-2">
          @for (r of risque(); track r.id) {
            <li class="flex items-center gap-3 rounded-full bg-aqua-50/60 py-1.5 pl-1.5 pr-4">
              <span class="relative grid h-10 w-10 shrink-0 place-items-center">
                <svg viewBox="0 0 40 40" class="absolute inset-0 -rotate-90" aria-hidden="true">
                  <circle cx="20" cy="20" r="16" fill="none" stroke="#dbe6fb" stroke-width="4" />
                  <circle cx="20" cy="20" r="16" fill="none" [attr.stroke]="r.pct >= 100 ? '#12306b' : '#2957cc'" stroke-width="4" stroke-linecap="round"
                          [attr.stroke-dasharray]="bague" [attr.stroke-dashoffset]="pret() ? bague * (1 - r.pct / 100) : bague" style="transition: stroke-dashoffset 1.4s cubic-bezier(.16,1,.3,1)" />
                </svg>
                <span class="relative font-mono text-[10px] font-bold text-ink-800">{{ r.pct }}</span>
              </span>
              <span class="min-w-0 flex-1"><span class="block truncate text-sm font-semibold text-ink-900">{{ r.pharmacie }}</span><span class="block truncate text-[11px] text-ink-400">{{ r.medicament }}</span></span>
            </li>
          } @empty { <li class="grid flex-1 place-items-center text-xs text-ink-400">Aucune pharmacie au-dessus de 90 %.</li> }
        </ul>
      </article>

      <!-- 3. Sanctions : trois tuiles -->
      <article class="card enter flex flex-col p-5 [animation-delay:180ms]">
        <p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Sanctions du mois</p>
        <div class="mt-3 grid flex-1 grid-cols-3 gap-2">
          @for (x of sanctions(); track x.nom) {
            <div class="flex flex-col items-center justify-center rounded-[1.4rem] px-1 py-5 text-center" [style.background]="x.fond">
              <span class="grid h-9 w-9 place-items-center rounded-full bg-white shadow-sm" [style.color]="x.couleur"><mv-icon [name]="x.icone" [size]="16" /></span>
              <p class="mt-3 font-display text-4xl leading-none" [style.color]="x.couleur" [mvCountUp]="x.valeur"></p>
              <p class="mt-2 px-1 text-[11px] leading-tight text-ink-500">{{ x.nom }}</p>
            </div>
          }
        </div>
        <p class="mt-3 text-[11px] text-ink-400">Au 2e avertissement, le quota passe en SUSPENDUE.</p>
      </article>

      <!-- 4. Ventes par semaine : gélules verticales -->
      <article class="card enter flex flex-col p-5 [animation-delay:240ms]">
        <div class="flex items-center justify-between"><p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Ventes par semaine</p><span class="font-mono text-[10px] text-ink-400">6 sem.</span></div>
        <div class="mt-3 flex flex-1 items-end gap-2.5">
          @for (v of semaines(); track v.s; let i = $index) {
            <div class="flex h-44 flex-1 flex-col items-center justify-end gap-1.5">
              <span class="font-mono text-[10px] {{ i === semaines().length - 1 ? 'font-bold text-iris-500' : 'text-ink-400' }}">{{ v.v }}</span>
              <div class="w-full rounded-full transition-[height] duration-1000 ease-out-expo" [style.height.%]="pret() ? (v.v / maxSemaine()) * 78 : 8" [style.transition-delay.ms]="i * 90"
                   [style.background]="i === semaines().length - 1 ? 'linear-gradient(180deg,#2cc5b5,#2957cc)' : 'linear-gradient(180deg,#cfdcfb,#8fb4ff)'"></div>
              <span class="font-mono text-[9px] text-ink-400">{{ v.s }}</span>
            </div>
          }
        </div>
      </article>

      <!-- 5. Répartition : barre fractionnée en gélule -->
      <article class="card enter flex flex-col p-5 [animation-delay:300ms]">
        <p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Ventes par catégorie</p>
        <p class="mt-3 font-display text-5xl leading-none text-ink-900"><span [mvCountUp]="totalCat()"></span> <span class="font-mono text-[11px] tracking-widest text-ink-400">BOÎTES</span></p>
        <div class="mt-4 flex h-5 gap-1">
          @for (a of repartition(); track a.cle) { <div class="h-full rounded-full transition-[flex-grow] duration-1000 ease-out-expo" [style.background]="a.col" [style.flex-grow]="pret() ? a.vendu : 0.001"></div> }
        </div>
        <ul class="mt-auto space-y-2.5 pt-5">
          @for (a of repartition(); track a.cle) {
            <li class="flex items-center gap-3 text-sm">
              <span class="grid h-7 w-7 shrink-0 place-items-center rounded-full text-white" [style.background]="a.col"><mv-icon [name]="a.icone" [size]="13" /></span>
              <span class="flex-1 text-ink-700">{{ a.pluriel }}</span>
              <span class="font-mono text-xs text-ink-400">{{ a.pct }} %</span>
              <span class="w-10 text-right font-mono text-sm font-semibold text-ink-900">{{ a.vendu }}</span>
            </li>
          }
        </ul>
      </article>

      <!-- 6. Top 5 : classement en gélules horizontales -->
      <article class="card enter flex flex-col p-5 [animation-delay:360ms]">
        <p class="font-mono text-[10px] uppercase tracking-[0.18em] text-ink-400">Top 5 des pharmacies</p>
        <ol class="mt-3 flex flex-1 flex-col justify-between gap-2">
          @for (p of top(); track p.id; let i = $index) {
            <li class="relative h-9 rounded-full bg-aqua-50/70">
              <div class="absolute inset-y-0 left-0 flex items-center rounded-full pl-1.5 transition-[width] duration-1000 ease-out-expo" [style.width.%]="pret() ? 30 + (p.vendu / top()[0].vendu) * 70 : 12" [style.transition-delay.ms]="i * 110"
                   [style.background]="i === 0 ? 'linear-gradient(90deg,#2957cc,#12a594)' : 'linear-gradient(90deg,#4f7be0,#8fb4ff)'">
                <span class="grid h-6 w-6 shrink-0 place-items-center rounded-full bg-white font-mono text-[10px] font-bold text-ink-700">{{ i + 1 }}</span>
                <span class="ml-2 truncate pr-3 text-xs font-semibold text-white">{{ p.nom }}</span>
              </div>
              <span class="absolute right-4 top-1/2 -translate-y-1/2 font-mono text-xs font-semibold text-ink-700">{{ p.vendu }}</span>
            </li>
          }
        </ol>
      </article>
    </div>
  `
})
export class DashboardSensiblesAdmin {
  protected s = inject(SensibleService);
  protected pret = signal(false);
  protected readonly demi = DEMI;
  protected readonly bague = BAGUE;

  protected taux = computed(() => (this.s.totalAttribue() ? Math.round((this.s.totalVendu() / this.s.totalAttribue()) * 100) : 0));
  protected restant = computed(() => Math.max(0, this.s.totalAttribue() - this.s.totalVendu()));
  protected risque = computed(() =>
    this.s.attributionsDuMois()
      .filter(a => a.quantiteAttribuee && a.quantiteVendue / a.quantiteAttribuee > 0.9)
      .sort((a, b) => b.quantiteVendue / b.quantiteAttribuee - a.quantiteVendue / a.quantiteAttribuee)
      .slice(0, 4)
      .map(a => ({ id: a.id, pharmacie: this.s.pharmacie(a.pharmacieId)?.nom ?? '', medicament: this.s.medicament(a.medicamentId)?.nomCommercial ?? '', pct: Math.min(100, Math.round((a.quantiteVendue / a.quantiteAttribuee) * 100)) }))
  );
  protected concernees = computed(() => new Set(this.s.attributionsDuMois().filter(a => a.avertissements > 0).map(a => a.pharmacieId)).size);
  protected sanctions = computed(() => [
    { nom: 'avertissements', valeur: this.s.avertissements(), icone: 'alert', couleur: '#2957cc', fond: '#eef3ff' },
    { nom: 'suspensions', valeur: this.s.pharmaciesSuspendues(), icone: 'octagon', couleur: '#12306b', fond: '#e9edf7' },
    { nom: 'pharmacies concernées', valeur: this.concernees(), icone: 'users', couleur: '#12a594', fond: '#e8f8f5' }
  ]);
  // 6 tranches de 7 jours glissants, la dernière finit aujourd'hui : somme des quantités des vraies ventes (GET /api/ventes)
  protected semaines = computed(() => {
    const ventes = this.s.ventes();
    const auj = new Date(); auj.setHours(0, 0, 0, 0);
    return [5, 4, 3, 2, 1, 0].map(k => {
      const fin = new Date(auj); fin.setDate(auj.getDate() - 7 * k);
      const debut = new Date(fin); debut.setDate(fin.getDate() - 6);
      const v = ventes.filter(x => { const d = new Date(`${x.date.slice(0, 10)}T00:00:00`); return d >= debut && d <= fin; }).reduce((t, x) => t + x.quantite, 0);
      return { s: k === 0 ? 'Auj.' : `S-${k}`, v };
    });
  });
  protected maxSemaine = computed(() => Math.max(1, ...this.semaines().map(x => x.v)));
  protected categories = computed(() =>
    CATEGORIES.map(c => ({ ...c, vendu: this.s.attributionsDuMois().filter(a => this.s.medicament(a.medicamentId)?.categorie === c.cle).reduce((t, a) => t + a.quantiteVendue, 0) }))
  );
  protected totalCat = computed(() => this.categories().reduce((t, c) => t + c.vendu, 0));
  protected repartition = computed(() => {
    const tot = Math.max(1, this.totalCat());
    return this.categories().map(c => ({ ...c, col: COULEURS[c.cle], pct: Math.round((c.vendu / tot) * 100) }));
  });
  protected top = computed(() =>
    this.s.pharmacies
      .map(p => ({ id: p.id, nom: p.nom, vendu: this.s.attributionsDuMois().filter(a => a.pharmacieId === p.id).reduce((t, a) => t + a.quantiteVendue, 0) }))
      .sort((a, b) => b.vendu - a.vendu).slice(0, 5)
  );

  constructor() {
    this.s.charger().subscribe();
    setTimeout(() => this.pret.set(true), 350);
  }
}

// ---------------------------------------------------------------------------
// PHARMACIEN : une seule carte compacte
// ---------------------------------------------------------------------------
@Component({
  selector: 'mv-dashboard-sensibles-pharmacien',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, CountUp],
  host: { class: 'block' },
  template: `
    <article class="card enter flex flex-wrap items-center gap-5 p-5">
      <svg viewBox="0 0 100 100" class="h-24 w-24 shrink-0 -rotate-90" aria-hidden="true">
        <circle cx="50" cy="50" r="44" fill="none" stroke="#e6eefc" stroke-width="9" />
        <circle cx="50" cy="50" r="44" fill="none" [attr.stroke]="taux() > 90 ? '#12306b' : 'url(#anneauP)'" stroke-width="9" stroke-linecap="round"
                [attr.stroke-dasharray]="cercle" [attr.stroke-dashoffset]="pret() ? cercle * (1 - min100() / 100) : cercle"
                style="transition: stroke-dashoffset 1.6s cubic-bezier(0.16, 1, 0.3, 1)" />
        <defs><linearGradient id="anneauP" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#2957cc" /><stop offset="1" stop-color="#2cc5b5" /></linearGradient></defs>
      </svg>
      <div class="min-w-[10rem] flex-1">
        <p class="text-[11px] font-bold uppercase tracking-widest text-ink-400">Médicaments sensibles</p>
        <p class="font-display text-4xl leading-none text-ink-900"><span [mvCountUp]="vendu()"></span><span class="text-2xl text-ink-300"> / {{ attribue() }}</span></p>
        <p class="mt-1 text-sm text-ink-500">boîtes vendues ce mois · <span class="font-bold text-ink-800">{{ attribue() - vendu() }}</span> restantes</p>
      </div>
      <div class="flex flex-wrap items-center gap-2">
        <span class="chip px-3 py-1.5 {{ avert() ? 'bg-sky-50 text-sky-700' : 'bg-ink-50 text-ink-500' }}">{{ avert() }} / 2 avertissement{{ avert() > 1 ? 's' : '' }}</span>
        @if (taux() > 90) { <span class="chip bg-sky-50 px-3 py-1.5 text-sky-700"><mv-icon name="alert" [size]="13" /> Quota presque épuisé</span> }
      </div>
      <a routerLink="/app/medicaments/pharmacien" class="btn btn-ink px-5 py-3 text-sm">Voir ma pharmacie <mv-icon name="arrow-right" [size]="16" /></a>
    </article>
  `
})
export class DashboardSensiblesPharmacien {
  private s = inject(SensibleService);
  protected pret = signal(false);
  protected readonly cercle = CERCLE;
  private mes = computed(() => this.s.attributionsDuMois().filter(a => a.pharmacieId === this.s.pharmacieId()));
  protected attribue = computed(() => this.mes().reduce((t, a) => t + a.quantiteAttribuee, 0));
  protected vendu = computed(() => this.mes().reduce((t, a) => t + a.quantiteVendue, 0));
  protected taux = computed(() => (this.attribue() ? Math.round((this.vendu() / this.attribue()) * 100) : 0));
  protected min100 = computed(() => Math.min(100, this.taux()));
  protected avert = computed(() => Math.max(0, ...this.mes().map(a => a.avertissements)));

  constructor() {
    this.s.charger().subscribe();
    setTimeout(() => this.pret.set(true), 350);
  }
}
