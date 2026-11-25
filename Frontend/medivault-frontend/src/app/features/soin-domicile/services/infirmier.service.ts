import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Infirmier, InfirmierRequest } from '../models/visite.model';

// CRUD des infirmiers de la pharmacie : /api/infirmiers du soin-domicile-service
@Injectable({ providedIn: 'root' })
export class InfirmierService {
  private http = inject(HttpClient);
  private readonly url = '/api/infirmiers';

  lister(pharmacieId: string): Observable<Infirmier[]> {
    return this.http.get<Infirmier[]>(this.url, { params: new HttpParams().set('pharmacieId', pharmacieId) });
  }

  /** Recherche par prénom, nom ou téléphone. */
  rechercher(pharmacieId: string, q: string): Observable<Infirmier[]> {
    return this.http.get<Infirmier[]>(`${this.url}/recherche`, { params: { pharmacieId, q } });
  }

  ajouter(r: InfirmierRequest): Observable<Infirmier> {
    return this.http.post<Infirmier>(this.url, r);
  }

  modifier(id: number, r: InfirmierRequest): Observable<Infirmier> {
    return this.http.put<Infirmier>(`${this.url}/${id}`, r);
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
