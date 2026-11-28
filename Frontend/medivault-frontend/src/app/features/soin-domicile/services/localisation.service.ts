import { Injectable } from '@angular/core';

export interface Position {
  latitude: number;
  longitude: number;
  adresse: string;            // adresse lisible, ou les coordonnées si elle est introuvable
}

// Position actuelle du patient : GPS du navigateur, puis adresse via OpenStreetMap (Nominatim)
@Injectable({ providedIn: 'root' })
export class LocalisationService {

  positionActuelle(): Promise<Position> {
    return new Promise((resolve, reject) => {
      if (!('geolocation' in navigator)) {
        reject(new Error('Votre navigateur ne permet pas la localisation.'));
        return;
      }
      navigator.geolocation.getCurrentPosition(
        async p => {
          const latitude = +p.coords.latitude.toFixed(6);
          const longitude = +p.coords.longitude.toFixed(6);
          resolve({ latitude, longitude, adresse: await this.adresse(latitude, longitude) });
        },
        e => reject(new Error(e.code === e.PERMISSION_DENIED
          ? 'Localisation refusée : autorisez-la dans le navigateur ou tapez votre adresse.'
          : 'Position introuvable : tapez votre adresse.')),
        { enableHighAccuracy: true, timeout: 10000 }
      );
    });
  }

  /** Coordonnées vers adresse ("12 Rue de Marseille, Tunis"). */
  private async adresse(lat: number, lon: number): Promise<string> {
    try {
      const r = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${lat}&lon=${lon}&accept-language=fr`);
      const d = await r.json();
      const a = d.address ?? {};
      const rue = [a.house_number, a.road].filter(Boolean).join(' ');
      const ville = a.city ?? a.town ?? a.village ?? a.suburb ?? a.state;
      return [rue, a.suburb !== ville ? a.suburb : null, ville].filter(Boolean).join(', ') || d.display_name;
    } catch {
      return `${lat}, ${lon}`;
    }
  }
}
