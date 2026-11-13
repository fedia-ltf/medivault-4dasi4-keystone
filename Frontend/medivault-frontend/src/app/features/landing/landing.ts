import {
  ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, OnInit, inject, signal
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon';
import { Logo } from '../../shared/logo';
import { Hologram } from '../../shared/hologram';
import { Reveal } from '../../shared/reveal';
import { CountUp } from '../../shared/count-up';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'mv-landing',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, Icon, Logo, Hologram, Reveal, CountUp],
  templateUrl: './landing.html'
})
export class LandingPage implements OnInit, OnDestroy {
  private host = inject(ElementRef<HTMLElement>);
  private zone = inject(NgZone);
  protected auth = inject(AuthService);

  // Écran d'ouverture « coffre-fort »
  protected progress = signal(0);
  protected ready = signal(false);
  protected loaderGone = signal(false);
  protected scrolled = signal(false);

  protected readonly modules = [
    { icon: 'folder', titre: 'Dossier médical', texte: 'Antécédents, allergies et consultations, partagés seulement avec les soignants autorisés.', owner: 'Eya', big: true },
    { icon: 'clipboard', titre: 'Ordonnances', texte: 'Rédaction guidée, alertes en temps réel, code unique par ordonnance.', owner: 'Amine' },
    { icon: 'pill', titre: 'Médicaments', texte: 'Catalogue, interactions et compatibilité vérifiées avant chaque prescription.', owner: 'Fedia', accent: true },
    { icon: 'cross', titre: 'Pharmacie', texte: 'Recherche par code, délivrance totale ou partielle, historique.', owner: 'Aziz' },
    { icon: 'flask', titre: 'Analyses', texte: 'Demandes, saisie des résultats et diffusion au dossier.', owner: 'Taha' },
    { icon: 'calendar', titre: 'Rendez-vous', texte: 'Agenda des médecins, confirmation et rappels.', owner: 'Yazid' },
    { icon: 'users', titre: 'Utilisateurs', texte: 'Cinq rôles, une identité unique, notifications en direct.', owner: 'Équipe' }
  ];

  protected readonly etapes = [
    { n: '01', titre: 'Le médecin rédige', texte: 'Le dossier du patient fournit ses allergies en un instant.', icon: 'stethoscope' },
    { n: '02', titre: 'Vérification immédiate', texte: 'Le medicament-service contrôle allergies et interactions (RG4, RG6).', icon: 'shield-check' },
    { n: '03', titre: 'Validation et notification', texte: 'Un événement part sur RabbitMQ, le patient est prévenu.', icon: 'zap' },
    { n: '04', titre: 'Délivrance en pharmacie', texte: 'Le pharmacien scanne le code, la délivrance est tracée.', icon: 'cross' }
  ];

  // Forme affichée par l'hologramme (cœur, ADN, croix, globe)
  protected forme = signal(0);
  protected readonly formes = [
    { icon: 'heart', titre: 'Dossier médical', texte: 'le suivi du patient' },
    { icon: 'flask', titre: 'Analyses', texte: 'résultats de laboratoire' },
    { icon: 'cross', titre: 'Pharmacie', texte: 'délivrance sécurisée' },
    { icon: 'layers', titre: 'Sept services', texte: 'un seul réseau de soins' }
  ];

  protected readonly atouts = [
    { icon: 'shield-check', titre: 'Keycloak · JWT', texte: 'Une identité, cinq rôles' },
    { icon: 'layers', titre: 'Une base par service', texte: 'Données cloisonnées' },
    { icon: 'zap', titre: 'Temps réel', texte: 'Événements RabbitMQ' },
    { icon: 'activity', titre: 'Résilience', texte: 'Disjoncteur et repli' }
  ];

  protected readonly chiffres = [
    { v: 7, l: 'microservices indépendants' },
    { v: 5, l: 'rôles, du patient à l\'admin' },
    { v: 31, l: 'médicaments au catalogue' },
    { v: 8, l: 'événements métier échangés' }
  ];

  protected readonly motsCles = [
    'Dossier médical', 'Ordonnances', 'Médicaments', 'Pharmacie', 'Analyses', 'Rendez-vous',
    'Keycloak', 'RabbitMQ', 'OpenFeign', 'Eureka', 'Resilience4j', 'Spring Boot', 'Angular'
  ];

  private frame = 0;
  private onMove = (e: PointerEvent) => {
    const el = this.host.nativeElement as HTMLElement;
    el.style.setProperty('--mx', (e.clientX / window.innerWidth - 0.5).toFixed(3));
    el.style.setProperty('--my', (e.clientY / window.innerHeight - 0.5).toFixed(3));
    el.style.setProperty('--px', `${e.clientX}px`);
    el.style.setProperty('--py', `${e.clientY}px`);
  };
  private onScroll = () => {
    const s = window.scrollY > 24;
    if (s !== this.scrolled()) this.zone.run(() => this.scrolled.set(s));
  };

  ngOnInit() {
    const dejaVu = sessionStorage.getItem('mv.intro') === '1';
    const duree = dejaVu ? 500 : 1900;
    sessionStorage.setItem('mv.intro', '1');

    this.zone.runOutsideAngular(() => {
      window.addEventListener('pointermove', this.onMove, { passive: true });
      window.addEventListener('scroll', this.onScroll, { passive: true });

      const start = performance.now();
      const tick = (now: number) => {
        const t = Math.min((now - start) / duree, 1);
        const eased = 1 - Math.pow(1 - t, 3);
        this.zone.run(() => this.progress.set(Math.round(eased * 100)));
        if (t < 1) {
          this.frame = requestAnimationFrame(tick);
        } else {
          setTimeout(() => this.zone.run(() => this.ready.set(true)), 250);
          setTimeout(() => this.zone.run(() => this.loaderGone.set(true)), 1700);
        }
      };
      this.frame = requestAnimationFrame(tick);
    });
  }

  // Bouton « magnétique » qui suit légèrement le curseur
  protected magnet(e: PointerEvent) {
    const b = e.currentTarget as HTMLElement;
    const r = b.getBoundingClientRect();
    const x = (e.clientX - r.left - r.width / 2) * 0.25;
    const y = (e.clientY - r.top - r.height / 2) * 0.35;
    b.style.transform = `translate(${x}px, ${y}px)`;
  }
  protected release(e: PointerEvent) {
    (e.currentTarget as HTMLElement).style.transform = '';
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.frame);
    window.removeEventListener('pointermove', this.onMove);
    window.removeEventListener('scroll', this.onScroll);
  }
}
