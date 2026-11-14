import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap, timeout } from 'rxjs';
import { Forme, Medicament } from '../models/medicament.model';


// ---------- Données de démonstration si l'API ne répond pas ----------
const DEMO: Medicament[] = [
  ['Doliprane', 'Paracétamol', 'Antalgique', 'COMPRIME', '1000 mg'],
  ['Efferalgan', 'Paracétamol', 'Antalgique', 'SIROP', '30 mg/ml'],
  ['Advil', 'Ibuprofène', 'AINS', 'COMPRIME', '400 mg'],
  ['Voltarène', 'Diclofénac', 'AINS', 'POMMADE', '1 %'],
  ['Aspégic', 'Acide acétylsalicylique', 'AINS', 'COMPRIME', '1000 mg'],
  ['Clamoxyl', 'Amoxicilline', 'Pénicilline', 'GELULE', '500 mg'],
  ['Augmentin', 'Amoxicilline / Acide clavulanique', 'Pénicilline', 'COMPRIME', '1 g'],
  ['Zithromax', 'Azithromycine', 'Macrolide', 'COMPRIME', '250 mg'],
  ['Zeclar', 'Clarithromycine', 'Macrolide', 'COMPRIME', '500 mg'],
  ['Ciflox', 'Ciprofloxacine', 'Fluoroquinolone', 'COMPRIME', '500 mg'],
  ['Coumadine', 'Warfarine', 'Anticoagulant', 'COMPRIME', '5 mg'],
  ['Lovenox', 'Énoxaparine', 'Anticoagulant', 'INJECTABLE', '4000 UI'],
  ['Zocor', 'Simvastatine', 'Statine', 'COMPRIME', '20 mg'],
  ['Tahor', 'Atorvastatine', 'Statine', 'COMPRIME', '10 mg'],
  ['Triatec', 'Ramipril', 'IEC', 'COMPRIME', '5 mg'],
  ['Glucophage', 'Metformine', 'Antidiabétique', 'COMPRIME', '850 mg'],
  ['Lantus', 'Insuline glargine', 'Antidiabétique', 'INJECTABLE', '100 UI/ml'],
  ['Xanax', 'Alprazolam', 'Benzodiazépine', 'COMPRIME', '0,25 mg'],
  ['Viagra', 'Sildénafil', 'Inhibiteur PDE5', 'COMPRIME', '50 mg'],
  ['Risordan', 'Dinitrate d\'isosorbide', 'Dérivé nitré', 'COMPRIME', '20 mg']
].map(([n, d, c, f, s], i) => ({
  id: i + 1, nom_commercial: n, dci: d, classe_therapeutique: c, forme: f as Forme, dosage: s, nb_delivrances: (i * 37) % 120
}));

// Tous les appels HTTP vers le medicament-service passent par ce service
@Injectable({ providedIn: 'root' })
export class MedicamentService {
  private http = inject(HttpClient);
  private readonly url = '/api/medicaments';

  /** true si l'API répond, false si on affiche les données de démonstration */
  readonly enLigne = signal<boolean | null>(null);
  private demo = [...DEMO];

  catalogue(): Observable<Medicament[]> {
    return this.http.get<Medicament[]>(this.url).pipe(
      timeout(4000),
      tap(() => this.enLigne.set(true)),
      catchError(() => {
        this.enLigne.set(false);
        return of([...this.demo]);
      })
    );
  }

  ajouter(m: Omit<Medicament, 'id'>): Observable<Medicament> {
    if (this.enLigne() === false) {
      const cree = { ...m, id: Math.max(0, ...this.demo.map(x => x.id)) + 1, nb_delivrances: 0 };
      this.demo.push(cree);
      return of(cree);
    }
    return this.http.post<Medicament>(this.url, m);
  }

  // Le backend doit exposer PUT /api/medicaments/{id} pour que la modification soit enregistrée
  modifier(id: number, m: Omit<Medicament, 'id'>): Observable<Medicament> {
    if (this.enLigne() === false) {
      const i = this.demo.findIndex(x => x.id === id);
      this.demo[i] = { ...this.demo[i], ...m, id };
      return of(this.demo[i]);
    }
    return this.http.put<Medicament>(`${this.url}/${id}`, m);
  }

  retirer(id: number, motif: string): Observable<void> {
    if (this.enLigne() === false) {
      this.demo = this.demo.filter(m => m.id !== id);
      return of(void 0);
    }
    return this.http.delete<void>(`${this.url}/${id}`, { params: { motif } });
  }
}
