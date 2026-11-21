import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, NgZone, computed, inject, signal, viewChild } from '@angular/core';
import { Icon } from '../../../../shared/icon';
import { Hero } from '../../components/hero';
import { CategorieBadge } from '../../components/categorie-badge';
import { PillIcon } from '../../components/pill-icon';
import { AdnScene } from '../../components/adn-scene';
import { CATEGORIES, Categorie, Disponibilite, MedicamentSensible, categorieInfo } from '../../models/sensible.model';
import { SensibleService } from '../../services/sensible.service';

const normaliser = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
/** Moments (0 à 1 du défilement) où chaque chapitre commence */
const SEUILS = [0.17, 0.38, 0.56, 0.74];
const VARIANTE: Record<Categorie, number> = { STUPEFIANT: 0, PSYCHOTROPE: 1, ABORTIF: 2 };

// Espace de consultation (médecin, patient) : lecture seule.
// Liste des médicaments sensibles ; un clic ouvre « Qu'y a-t-il dans … ? », une histoire animée
// au défilement : la gélule s'ouvre, ses grains forment une hélice d'ADN, la caméra plonge dedans
// pendant que les 4 chapitres défilent. Rien ne bouge sans le défilement.
@Component({
  selector: 'mv-consultation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Icon, Hero, CategorieBadge, PillIcon, AdnScene],
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
  /** Vrai quand la scène est sur son fond nuit : le texte passe en blanc */
  protected sombre = signal(false);
  /** Le grand titre d'ouverture : il s'efface dès que la gélule commence à s'ouvrir */
  protected debut = signal(true);
  protected readonly reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private histoire = viewChild<ElementRef<HTMLElement>>('histoire');
  private scene = viewChild<ElementRef<HTMLElement>>('scene');
  private adn = viewChild(AdnScene);
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
    this.sombre.set(false);
    this.debut.set(!this.reduit);
    this.dispo.set([]);
    this.s.disponibilites(m.id).subscribe(d => this.dispo.set(d));
    window.scrollTo({ top: 0 });
    setTimeout(() => this.majHistoire(), 80);
  }
  protected fermer() { this.choisi.set(null); window.scrollTo({ top: 0 }); }

  /** Calcule l'avancement du défilement (0 → 1) et le donne à la scène. Hors d'Angular pour rester fluide. */
  private majHistoire() {
    const h = this.histoire()?.nativeElement, st = this.scene()?.nativeElement;
    if (!h || !st) return;
    const haut = parseFloat(getComputedStyle(st).top) || 0;
    const parcours = h.offsetHeight - st.offsetHeight;
    const statique = this.reduit || parcours <= 0;
    const p = statique ? 1 : Math.min(1, Math.max(0, (haut - h.getBoundingClientRect().top) / parcours));
    this.adn()?.progression(p);
    const e = statique ? 4 : SEUILS.filter(t => p >= t).length;
    const sombre = !statique && p > 0.155 && p < 0.905;
    const debut = !statique && p < 0.055;
    if (e !== this.etape() || sombre !== this.sombre() || debut !== this.debut())
      this.zone.run(() => { this.etape.set(e); this.sombre.set(sombre); this.debut.set(debut); });
  }
}
