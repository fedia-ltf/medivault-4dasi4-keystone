import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, input, output, viewChild
} from '@angular/core';

type P = [number, number, number];
const rnd = Math.random;

// ---------- Formes 3D en nuage de points (coordonnées entre -1 et 1) ----------
function coeur(n: number): P[] {
  return Array.from({ length: n }, () => {
    const t = rnd() * Math.PI * 2;
    const v = rnd() * 2 - 1;
    const s = Math.sqrt(1 - v * v);
    const x = (16 * Math.sin(t) ** 3) / 17;
    const y = (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 17;
    return [x * s, y * s + 0.12 * s, v * 0.5] as P;
  });
}

function adn(n: number): P[] {
  const R = 0.42, tours = Math.PI * 5, barreaux = 24;
  return Array.from({ length: n }, (_, i) => {
    const u = rnd();
    if (i % 3 < 2) {
      const a = u * tours + (i % 3) * Math.PI;
      return [Math.cos(a) * R, (u - 0.5) * 2.1, Math.sin(a) * R] as P;
    }
    const uq = Math.round(u * barreaux) / barreaux;
    const a = uq * tours, f = rnd() * 2 - 1;
    return [Math.cos(a) * R * f, (uq - 0.5) * 2.1, Math.sin(a) * R * f] as P;
  });
}

function croix(n: number): P[] {
  const a = 0.3, L = 0.92, d = 0.2;
  const long = () => (a + rnd() * (L - a)) * (rnd() < 0.5 ? -1 : 1);
  return Array.from({ length: n }, () => {
    const r = rnd(), z = rnd() * 2 * d - d, side = rnd() < 0.5 ? -1 : 1;
    if (r < 0.28) return [rnd() * 2 * a - a, rnd() * 2 * L - L, d * side] as P;   // faces, barre verticale
    if (r < 0.56) return [rnd() * 2 * L - L, rnd() * 2 * a - a, d * side] as P;   // faces, barre horizontale
    if (r < 0.72) return [a * side, long(), z] as P;                               // tranches verticales
    if (r < 0.88) return [long(), a * side, z] as P;                               // tranches horizontales
    return rnd() < 0.5
      ? ([rnd() * 2 * a - a, L * side, z] as P)
      : ([L * side, rnd() * 2 * a - a, z] as P);                                   // extrémités
  });
}

function globe(n: number): P[] {
  const or = Math.PI * (3 - Math.sqrt(5));
  return Array.from({ length: n }, (_, i) => {
    const y = 1 - (i / (n - 1)) * 2, r = Math.sqrt(1 - y * y), t = i * or;
    return [Math.cos(t) * r * 0.86, y * 0.86, Math.sin(t) * r * 0.86] as P;
  });
}

// Hologramme médical : une gélule s'ouvre et libère un nuage de particules
// qui se transforme en cœur, ADN, croix puis globe.
// Rotation 3D continue, ligne de balayage et socle de projection ; la souris oriente l'objet.
@Component({
  selector: 'mv-hologram',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class Hologram implements AfterViewInit, OnDestroy {
  count = input(1700);
  /** Forme fixe (0 cœur, 1 ADN, 2 croix, 3 globe) ; null = enchaîne toutes les formes */
  fixe = input<number | null>(null);
  /** Affiche la gélule qui s'ouvre autour de l'hologramme */
  capsule = input(true);
  /** Émet l'index de la forme affichée (0 cœur, 1 ADN, 2 croix, 3 globe) */
  shape = output<number>();

  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private frame = 0;
  private resize?: ResizeObserver;
  private mouse = { x: 0, y: 0, tx: 0, ty: 0 };
  private onMove = (e: PointerEvent) => {
    this.mouse.tx = e.clientX / window.innerWidth - 0.5;
    this.mouse.ty = e.clientY / window.innerHeight - 0.5;
  };

  ngAfterViewInit() {
    const cv = this.canvas().nativeElement;
    const ctx = cv.getContext('2d');
    if (!ctx) return;

    const n = this.count();
    const fx = this.fixe();
    const avecGelule = this.capsule();
    const formes = [coeur(n), adn(n), croix(n), globe(n)];
    const nuage: P[] = Array.from({ length: n }, () => {
      const a = rnd() * Math.PI * 2, b = Math.acos(rnd() * 2 - 1), r = rnd() * 0.2;
      return [Math.sin(b) * Math.cos(a) * r, Math.cos(b) * r, Math.sin(b) * Math.sin(a) * r] as P;
    });
    const retard = Float32Array.from({ length: n }, () => rnd() * 0.4);
    const souffle: P[] = Array.from({ length: n }, () => [rnd() - 0.5, rnd() - 0.5, rnd() - 0.5] as P);
    const teinte = Uint8Array.from({ length: n }, () => (rnd() < 0.22 ? 1 : 0));
    const COULEURS = ['#2957cc', '#12a594', '#38bdf8'];

    let w = 0, h = 0;
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    const fit = () => {
      w = cv.clientWidth;
      h = cv.clientHeight;
      cv.width = w * dpr;
      cv.height = h * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    };
    this.resize = new ResizeObserver(fit);
    this.resize.observe(cv);
    fit();

    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const PAUSE = 3.4, MORPH = 1.8, DEPART = 1.5;
    const t0 = performance.now();
    let courant = -1;

    const ellipse = (x: number, y: number, rx: number, alpha: number, lw = 1) => {
      ctx.beginPath();
      ctx.ellipse(x, y, rx, rx * 0.2, 0, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(59,111,230,${alpha})`;
      ctx.lineWidth = lw;
      ctx.stroke();
    };

    const draw = (now: number) => {
      const t = reduit ? 99 : (now - t0) / 1000;
      const m = this.mouse;
      m.x += (m.tx - m.x) * 0.05;
      m.y += (m.ty - m.y) * 0.05;

      // Étape de l'animation : de quelle forme vers quelle forme, et à quel avancement
      const e = Math.max(t - DEPART, 0);
      const k = reduit ? 0 : Math.floor(e / (PAUSE + MORPH));
      const kf = fx === null ? k : 0;
      const avance = reduit ? 1 : Math.min((e - kf * (PAUSE + MORPH)) / MORPH, 1);
      const de = kf === 0 ? nuage : formes[(kf - 1) % formes.length];
      const vers = formes[fx ?? kf % formes.length];
      if (fx === null && k % formes.length !== courant && t >= DEPART) {
        courant = k % formes.length;
        this.zone.run(() => this.shape.emit(courant));
      }

      const cx = w / 2, cy = h * 0.5;
      ctx.clearRect(0, 0, w, h);

      // Ouverture de la gélule (0 = fermée, 1 = ouverte)
      const o1 = reduit ? 1 : Math.min(Math.max((t - 0.5) / 1.6, 0), 1);
      const ouv = 1 - Math.pow(1 - o1, 3);
      const S = Math.min(w, h) * (avecGelule ? 0.22 : 0.33);
      // Un cœur seul bat
      const battement = fx === 0 ? 1 + 0.08 * Math.pow(Math.max(0, Math.sin(t * 4.6)), 3) : 1;
      const Se = S * (0.12 + 0.88 * ouv) * battement;
      const W = S * 1.55, rimRy = W * 0.13, domeRy = W * 0.36, Hc = S * 0.2;
      const flotte = Math.sin(t * 1.1) * 4 * ouv;
      const yHaut = cy - S * 1.12 * ouv - flotte;
      const yBas = cy + S * 1.12 * ouv + flotte;

      const halo = ctx.createRadialGradient(cx, cy, 0, cx, cy, S * 2.4);
      halo.addColorStop(0, 'rgba(91,141,245,0.22)');
      halo.addColorStop(1, 'rgba(91,141,245,0)');
      ctx.fillStyle = halo;
      ctx.fillRect(0, 0, w, h);

      // Une moitié de gélule : bleue en haut, nacrée en bas (on voit l'intérieur lumineux)
      const demi = (yr: number, haut: boolean) => {
        const dir = haut ? -1 : 1;
        const g = ctx.createLinearGradient(cx - W / 2, 0, cx + W / 2, 0);
        const tons = haut
          ? ['#16327a', '#2957cc', '#7ea6ff', '#3b6fe6', '#122a66']
          : ['#c5d3ea', '#f2f6fd', '#ffffff', '#e3ebf8', '#b9c8e2'];
        [0, 0.22, 0.4, 0.62, 1].forEach((p, i) => g.addColorStop(p, tons[i]));
        ctx.save();
        if (!haut) {
          ctx.shadowColor = 'rgba(30,64,140,0.32)';
          ctx.shadowBlur = 34;
          ctx.shadowOffsetY = 20;
        }
        ctx.beginPath();
        ctx.moveTo(cx - W / 2, yr);
        ctx.lineTo(cx - W / 2, yr + dir * Hc);
        ctx.ellipse(cx, yr + dir * Hc, W / 2, domeRy, 0, Math.PI, 0, !haut);
        ctx.lineTo(cx + W / 2, yr);
        ctx.ellipse(cx, yr, W / 2, rimRy, 0, 0, Math.PI, false);
        ctx.fillStyle = g;
        ctx.fill();
        ctx.restore();
        // reflet
        ctx.fillStyle = haut ? 'rgba(255,255,255,0.30)' : 'rgba(255,255,255,0.85)';
        ctx.beginPath();
        ctx.ellipse(cx - W * 0.25, yr + dir * (Hc * 0.5 + domeRy * 0.28), W * 0.045, Hc * 0.5 + domeRy * 0.42, 0, 0, Math.PI * 2);
        ctx.fill();
        if (haut) {
          ctx.strokeStyle = 'rgba(255,255,255,0.45)';
          ctx.lineWidth = 1.5;
          ctx.beginPath();
          ctx.ellipse(cx, yr, W / 2, rimRy, 0, 0.15, Math.PI - 0.15, false);
          ctx.stroke();
        } else {
          const dedans = ctx.createRadialGradient(cx, yr, 0, cx, yr, W / 2);
          dedans.addColorStop(0, 'rgba(214,229,255,1)');
          dedans.addColorStop(0.55, 'rgba(91,141,245,0.95)');
          dedans.addColorStop(1, '#1d3f9e');
          ctx.beginPath();
          ctx.ellipse(cx, yr, W / 2 - 1, rimRy, 0, 0, Math.PI * 2);
          ctx.fillStyle = dedans;
          ctx.fill();
          ctx.strokeStyle = 'rgba(255,255,255,0.95)';
          ctx.lineWidth = 1.6;
          ctx.stroke();
        }
      };

      // Faisceau de lumière qui monte de la moitié basse
      if (avecGelule) {
      const cone = ctx.createLinearGradient(0, yBas, 0, yHaut);
      cone.addColorStop(0, `rgba(91,141,245,${0.22 * ouv})`);
      cone.addColorStop(1, 'rgba(91,141,245,0)');
      ctx.fillStyle = cone;
      ctx.beginPath();
      ctx.moveTo(cx - W * 0.46, yBas);
      ctx.lineTo(cx - W * 0.7, yHaut);
      ctx.lineTo(cx + W * 0.7, yHaut);
      ctx.lineTo(cx + W * 0.46, yBas);
      ctx.fill();
      demi(yBas, false);
      }
      const pouls = (t * 0.45) % 1;
      if (avecGelule) {
        ellipse(cx, yBas, W * (0.5 + pouls * 0.45), 0.4 * (1 - pouls) * ouv);
      } else {
        const sol = cy + S * 1.3;
        const cone = ctx.createLinearGradient(0, sol, 0, cy - S * 0.6);
        cone.addColorStop(0, `rgba(59,111,230,${0.14 * ouv})`);
        cone.addColorStop(1, 'rgba(59,111,230,0)');
        ctx.fillStyle = cone;
        ctx.beginPath();
        ctx.moveTo(cx - S * 0.42, sol);
        ctx.lineTo(cx - S * 1.05, cy - S * 0.6);
        ctx.lineTo(cx + S * 1.05, cy - S * 0.6);
        ctx.lineTo(cx + S * 0.42, sol);
        ctx.fill();
        ellipse(cx, sol, S * 0.42, 0.55, 1.4);
        ellipse(cx, sol, S * 0.66, 0.25);
        ellipse(cx, sol, S * (0.42 + pouls * 0.6), 0.35 * (1 - pouls));
      }

      // Ligne de balayage
      const scan = cy + Math.sin(t * 0.85) * S * 0.95 * ouv;
      ctx.beginPath();
      ctx.ellipse(cx, scan, S * 0.95, S * 0.1, 0, 0, Math.PI * 2);
      ctx.strokeStyle = `rgba(56,189,248,${0.45 * ouv})`;
      ctx.lineWidth = 1;
      ctx.stroke();

      const ry = t * 0.5 + m.x * 1.1;
      const rx = -0.1 + m.y * 0.5;
      const cA = Math.cos(ry), sA = Math.sin(ry), cB = Math.cos(rx), sB = Math.sin(rx);
      let style = -1;

      for (let i = 0; i < n; i++) {
        let p = (avance - retard[i]) / 0.6;
        p = p < 0 ? 0 : p > 1 ? 1 : p;
        const q = p < 0.5 ? 4 * p * p * p : 1 - Math.pow(-2 * p + 2, 3) / 2;
        const jet = Math.sin(Math.PI * p) * 0.55;
        const a = de[i], b = vers[i], d = souffle[i];
        const flot = 0.012 * Math.sin(t * 1.4 + i);
        const x = a[0] + (b[0] - a[0]) * q + d[0] * jet + flot;
        const y = a[1] + (b[1] - a[1]) * q + d[1] * jet;
        const z = a[2] + (b[2] - a[2]) * q + d[2] * jet;

        const x1 = x * cA + z * sA, z1 = -x * sA + z * cA;
        const y1 = y * cB - z1 * sB, z2 = y * sB + z1 * cB;
        const persp = 2.8 / (2.8 - z2);
        const sx = cx + x1 * Se * persp, sy = cy - y1 * Se * persp;
        let pres = (z2 + 1) / 2;
        pres = pres < 0 ? 0 : pres > 1 ? 1 : pres;

        let r = 0.55 + pres * 1.75;
        let alpha = 0.16 + pres * 0.78;
        let c = teinte[i];
        const ecart = Math.abs(sy - scan);
        if (ecart < 12) {           // particule traversée par le balayage
          const f = 1 - ecart / 12;
          r += f * 1.5;
          alpha = Math.min(1, alpha + f * 0.6);
          c = 2;
        }
        if (c !== style) {
          ctx.fillStyle = COULEURS[c];
          style = c;
        }
        ctx.globalAlpha = alpha * ouv;
        ctx.beginPath();
        ctx.arc(sx, sy, r, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.globalAlpha = 1;
      if (avecGelule) demi(yHaut, true);
      if (!reduit) this.frame = requestAnimationFrame(draw);
    };

    this.zone.runOutsideAngular(() => {
      window.addEventListener('pointermove', this.onMove, { passive: true });
      this.frame = requestAnimationFrame(draw);
    });
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.frame);
    this.resize?.disconnect();
    window.removeEventListener('pointermove', this.onMove);
  }
}
