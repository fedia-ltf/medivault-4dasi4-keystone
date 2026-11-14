import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { CountUp } from '../../shared/count-up';
import { AuthService } from '../../core/auth.service';
import { MedicamentService } from '../medicaments/services/medicament.service';

interface Serie { label: string; mois: string[]; prescrites: number[]; delivrees: number[]; }

@Component({
  selector: 'mv-dashboard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, RouterLink, Icon, CountUp],
  templateUrl: './dashboard.html'
})
export class DashboardPage {
  protected auth = inject(AuthService);
  private medicaments = inject(MedicamentService);

  protected aujourdhui = new Date();
  protected nbMedicaments = signal(31);
  protected apiEnLigne = signal<boolean | null>(null);

  protected salutation = computed(() => {
    const h = new Date().getHours();
    const debut = h < 18 ? 'Bonjour' : 'Bonsoir';
    const u = this.auth.utilisateur();
    const prefixe = u?.role === 'MEDECIN' ? 'Dr ' : '';
    return `${debut}, ${prefixe}${u?.nom.split(' ')[0] ?? ''}`;
  });

  protected kpis = computed(() => [
    { label: 'Patients suivis', valeur: 1284, delta: '+4,2 %', icon: 'users', ton: 'from-aqua-300 to-aqua-500', points: [8, 10, 9, 12, 11, 14, 16] },
    { label: 'Ordonnances ce mois', valeur: 342, delta: '+12 %', icon: 'clipboard', ton: 'from-sky-300 to-sky-500', points: [5, 7, 6, 9, 12, 11, 15] },
    { label: 'Médicaments au catalogue', valeur: this.nbMedicaments(), delta: 'API', icon: 'pill', ton: 'from-iris-400 to-iris-500', points: [9, 9, 10, 10, 11, 11, 12] },
    { label: 'Alertes d\'interaction', valeur: 12, delta: '−3', icon: 'alert', ton: 'from-amber-300 to-orange-400', points: [14, 12, 13, 10, 11, 9, 8] }
  ]);

  // ---------- Graphique ----------
  protected readonly series: Serie[] = [
    {
      label: '6 mois', mois: ['Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.'],
      prescrites: [210, 245, 228, 290, 318, 342], delivrees: [180, 214, 205, 251, 286, 301]
    },
    {
      label: '12 mois', mois: ['Déc.', 'Janv.', 'Févr.', 'Mars', 'Avr.', 'Mai', 'Juin', 'Juil.', 'Août', 'Sept.', 'Oct.', 'Nov.'],
      prescrites: [160, 172, 190, 185, 204, 199, 210, 245, 228, 290, 318, 342],
      delivrees: [140, 151, 166, 170, 182, 178, 180, 214, 205, 251, 286, 301]
    }
  ];
  protected serieIndex = signal(0);
  protected serie = computed(() => this.series[this.serieIndex()]);
  protected survol = signal<number | null>(null);

  private readonly W = 640;
  private readonly H = 220;
  protected graph = computed(() => {
    const s = this.serie();
    const max = Math.max(...s.prescrites) * 1.15;
    const pas = this.W / (s.mois.length - 1);
    const pt = (v: number, i: number) => [i * pas, this.H - (v / max) * this.H] as const;
    const courbe = (vals: number[]) => {
      const p = vals.map(pt);
      let d = `M${p[0][0]},${p[0][1]}`;
      for (let i = 1; i < p.length; i++) {
        const [x0, y0] = p[i - 1], [x1, y1] = p[i];
        const cx = (x0 + x1) / 2;
        d += ` C${cx},${y0} ${cx},${y1} ${x1},${y1}`;
      }
      return d;
    };
    const ligneA = courbe(s.prescrites);
    const ligneB = courbe(s.delivrees);
    return {
      ligneA, ligneB,
      aireA: `${ligneA} L${this.W},${this.H} L0,${this.H} Z`,
      aireB: `${ligneB} L${this.W},${this.H} L0,${this.H} Z`,
      points: s.prescrites.map((v, i) => ({ x: pt(v, i)[0], y: pt(v, i)[1], yb: pt(s.delivrees[i], i)[1] })),
      pas
    };
  });

  protected sparkline(points: number[]) {
    const max = Math.max(...points), min = Math.min(...points);
    return points
      .map((p, i) => `${i === 0 ? 'M' : 'L'}${(i / (points.length - 1)) * 100},${28 - ((p - min) / (max - min || 1)) * 24}`)
      .join(' ');
  }

  // ---------- Données de démonstration ----------
  protected readonly agenda = [
    { heure: '09:00', patient: 'Ahmed Trabelsi', motif: 'Suivi cardiologique', statut: 'Terminé', ton: 'bg-ink-100 text-ink-500' },
    { heure: '10:30', patient: 'Sarra Mejri', motif: 'Résultats d\'analyses', statut: 'En cours', ton: 'bg-aqua-50 text-aqua-700' },
    { heure: '11:15', patient: 'Youssef Ben Ali', motif: 'Renouvellement d\'ordonnance', statut: 'Confirmé', ton: 'bg-sky-50 text-sky-700' },
    { heure: '14:00', patient: 'Leila Haddad', motif: 'Première consultation', statut: 'Confirmé', ton: 'bg-sky-50 text-sky-700' }
  ];

  // Alertes triées par gravité (la plus grave en premier), avec couleur, icône et mot-signal
  protected readonly alertes = [
    { niveau: 'CONTRE-INDIQUÉE', icon: 'octagon', ton: 'border-rose-200 bg-rose-50/80 text-rose-700', texte: 'Viagra + Risordan', detail: 'Hypotension sévère · prescription bloquée' },
    { niveau: 'ALLERGIE', icon: 'shield', ton: 'border-rose-200 bg-rose-50/80 text-rose-700', texte: 'Augmentin · classe Pénicilline', detail: 'Allergie déclarée au dossier' },
    { niveau: 'GRAVE', icon: 'alert', ton: 'border-amber-200 bg-amber-50/80 text-amber-700', texte: 'Coumadine + Advil', detail: 'Risque hémorragique · à surveiller' },
    { niveau: 'MODÉRÉE', icon: 'info', ton: 'border-sky-200 bg-sky-50/80 text-sky-700', texte: 'Advil + Aspégic', detail: 'Toxicité digestive accrue' }
  ];

  protected readonly ordonnances = [
    { code: 'ORD-A7F3', patient: 'Ahmed Trabelsi', date: '12 nov.', lignes: 3, statut: 'VALIDÉE', ton: 'bg-emerald-50 text-emerald-700' },
    { code: 'ORD-B219', patient: 'Sarra Mejri', date: '12 nov.', lignes: 2, statut: 'DÉLIVRÉE', ton: 'bg-ink-100 text-ink-600' },
    { code: 'ORD-C884', patient: 'Youssef Ben Ali', date: '11 nov.', lignes: 4, statut: 'EN ATTENTE', ton: 'bg-amber-50 text-amber-700' },
    { code: 'ORD-D102', patient: 'Leila Haddad', date: '10 nov.', lignes: 1, statut: 'PARTIELLE', ton: 'bg-sky-50 text-sky-700' }
  ];

  protected services = computed(() => [
    { nom: 'medicament', etat: this.apiEnLigne() },
    { nom: 'ordonnance', etat: null },
    { nom: 'dossier', etat: null },
    { nom: 'pharmacie', etat: null },
    { nom: 'analyse', etat: null },
    { nom: 'rendezvous', etat: null },
    { nom: 'user', etat: null }
  ]);

  constructor() {
    this.medicaments.catalogue().subscribe(liste => {
      this.nbMedicaments.set(liste.length);
      this.apiEnLigne.set(this.medicaments.enLigne());
    });
  }
}
