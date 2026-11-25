import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AcceptationRequest, RapportRequest, RechercheVisites, StatutVisite, Visite, VisiteRequest, WhatsAppMessage, parametres
} from '../models/visite.model';

// Tous les appels HTTP vers /api/visites du soin-domicile-service passent par ce service
@Injectable({ providedIn: 'root' })
export class SoinService {
  private http = inject(HttpClient);
  private readonly url = '/api/visites';

  lister(filtre: { patientId?: string; pharmacieId?: string; statut?: StatutVisite | '' }): Observable<Visite[]> {
    let params = new HttpParams();
    Object.entries(filtre).forEach(([k, v]) => { if (v) params = params.set(k, v); });
    return this.http.get<Visite[]>(this.url, { params });
  }

  /** Recherche multicritère côté serveur (patient, adresse, infirmier, type, statut, dates). */
  rechercher(f: RechercheVisites): Observable<Visite[]> {
    return this.http.get<Visite[]>(`${this.url}/recherche`, { params: parametres(f) });
  }

  demander(r: VisiteRequest): Observable<Visite> {
    return this.http.post<Visite>(this.url, r);
  }

  /** Message WhatsApp (texte + lien wa.me) pour l'infirmier affecté à la visite. */
  whatsapp(id: number): Observable<WhatsAppMessage> {
    return this.http.get<WhatsAppMessage>(`${this.url}/${id}/whatsapp`);
  }

  /** Le pharmacien accepte et affecte un infirmier. */
  accepter(id: number, r: AcceptationRequest): Observable<Visite> {
    return this.http.patch<Visite>(`${this.url}/${id}/accepter`, r);
  }

  saisirRapport(id: number, r: RapportRequest): Observable<Visite> {
    return this.http.patch<Visite>(`${this.url}/${id}/rapport`, r);
  }

  annuler(id: number): Observable<Visite> {
    return this.http.patch<Visite>(`${this.url}/${id}/annuler`, {});
  }

  supprimer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

/** Le backend renvoie ses erreurs au format Problem Details : le texte est dans "detail". */
export function messageErreur(e: HttpErrorResponse): string {
  if (e.status === 0) return 'soin-domicile-service ne répond pas (port 8085).';
  const champs = e.error?.erreurs ? ' : ' + Object.entries(e.error.erreurs).map(([k, v]) => `${k} ${v}`).join(', ') : '';
  return (e.error?.detail ?? 'Erreur ' + e.status) + champs;
}
