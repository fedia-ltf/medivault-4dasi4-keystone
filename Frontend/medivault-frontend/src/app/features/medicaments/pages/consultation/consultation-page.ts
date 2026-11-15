import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, NgZone, computed, inject, signal, viewChild } from '@angular/core';
import { Icon } from '../../../../shared/icon';
import { Hero } from '../../components/hero';
import { CategorieBadge } from '../../components/categorie-badge';
import { PillIcon } from '../../components/pill-icon';
import { MoleculesScene } from '../../components/molecules-scene';
import { CATEGORIES, Categorie, Disponibilite, MedicamentSensible, categorieInfo } from '../../models/sensible.model';
import { SensibleService } from '../../services/sensible.service';

const normaliser = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
/** Moments (0 à 1 du défilement) où chaque carte apparaît */
const SEUILS = [0.08, 0.3, 0.52, 0.74];
const VARIANTE: Record<Categorie, number> = { STUPEFIANT: 0, PSYCHOTROPE: 1, ABORTIF: 2 };

// Espace de consultation (médecin, patient) : lecture seule.
// Liste des médicaments sensibles ; un clic ouvre « Qu'y a-t-il dans … ? », une histoire animée
// au défilement : la gélule tourne, se déplace et grossit pendant que les cartes apparaissent.
@Component({
  selector: 'mv-consultation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, Hero, CategorieBadge, PillIcon, MoleculesScene],
  templateUrl: './consultation-page.html'
})
export class ConsultationPage {
  protected s = inject(SensibleService);
  private zone = inject(NgZone);

  protected readonly categories = CATEGORIES;
  protected readonly categorieInfo = categorieInfo;
  protected recherche = signal('');
  protected filtreCat = signal<Categorie | null>(null);
  protected choisi = signal<MedicamentSensible | null>(null);
  protected dispo = signal<Disponibilite[]>([]);
  protected etape = signal(0);
  protected readonly reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private histoire = viewChild<ElementRef<HTMLElement>>('histoire');
  private scene = viewChild<ElementRef<HTMLElement>>('scene');
  private molecules = viewChild(MoleculesScene);
  protected teinte = computed(() => ({ STUPEFIANT: 218, PSYCHOTROPE: 172, ABORTIF: 198 }[this.choisi()?.categorie ?? 'STUPEFIANT']));

  protected liste = computed(() => {
    const q = normaliser(this.recherche()), c = this.filtreCat();
    return this.s.medicaments().filter(m => (!c || m.categorie === c) && (!q || normaliser(`${m.nomCommercial} ${m.dci}`).includes(q)));
  });

  constructor() {
    this.s.charger().subscribe();
    const surDefilement = () => this.majHistoire();
    this.zone.runOutsideAngular(() => window.addEventListener('scroll', surDefilement, { passive: true }));
    inject(DestroyRef).onDestroy(() => window.removeEventListener('scroll', surDefilement));
  }

  protected variante(m: MedicamentSensible) { return VARIANTE[m.categorie]; }
  protected libelleEtat(e: Disponibilite['etat']) {
    return { DISPONIBLE: 'Disponible', BIENTOT_EPUISE: 'Bientôt épuisé', INDISPONIBLE: 'Indisponible' }[e];
  }
  protected tonEtat(e: Disponibilite['etat']) {
    return { DISPONIBLE: 'bg-emerald-50 text-emerald-700', BIENTOT_EPUISE: 'bg-sky-50 text-sky-700', INDISPONIBLE: 'bg-rose-50 text-rose-700' }[e];
  }

  protected ouvrir(m: MedicamentSensible) {
    this.choisi.set(m);
    this.etape.set(this.reduit ? 4 : 0);
    this.dispo.set([]);
    this.s.disponibilites(m.id).subscribe(d => this.dispo.set(d));
    window.scrollTo({ top: 0 });
    setTimeout(() => this.majHistoire(), 80);
  }
  protected fermer() { this.choisi.set(null); window.scrollTo({ top: 0 }); }

  /** Calcule l'avancement du défilement (0 → 1) et déplace la gélule. Hors d'Angular pour rester fluide. */
  private majHistoire() {
    const h = this.histoire()?.nativeElement, st = this.scene()?.nativeElement;
    if (!h || !st) return;
    const parcours = h.offsetHeight - st.offsetHeight;
    const statique = this.reduit || parcours <= 0;
    const p = statique ? 0.4 : Math.min(1, Math.max(0, (112 - h.getBoundingClientRect().top) / parcours));
    this.molecules()?.progression(statique ? 0.6 : p);
    const e = statique ? 4 : SEUILS.filter(t => p >= t).length;
    if (e !== this.etape()) this.zone.run(() => this.etape.set(e));
  }
}
