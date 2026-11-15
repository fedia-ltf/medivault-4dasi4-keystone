import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, input, viewChild
} from '@angular/core';

type Ctx = CanvasRenderingContext2D;

// Médicaments affichés tour à tour sur les étiquettes : [nom, substance, classe · dosage]
const FICHES = [
  ['Subutex 8 mg', 'Buprénorphine', 'STUPÉFIANT'],
  ['Méthadone 40 mg', 'Méthadone', 'STUPÉFIANT'],
  ['Ritaline 10 mg', 'Méthylphénidate', 'PSYCHOTROPE'],
  ['Misoprostol 200 µg', 'Misoprostol', 'ABORTIF']
];

function aleatoire(graine: number) {
  let s = graine >>> 0;
  return () => {
    s = (s + 0x6d2b79f5) >>> 0;
    let t = s;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

// Scène du module Médicaments, façon affiche de laboratoire :
// fond bleu profond, grande gélule en verre (moitié blanche nacrée, moitié bleue transparente)
// avec un réseau de molécules lumineuses qui tourne à l'intérieur, ADN et particules en profondeur.
// Les couches fixes (fond, verre de la gélule) sont calculées une seule fois ; seuls les éléments animés sont redessinés.
@Component({
  selector: 'mv-medicaments-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block pointer-events-none' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class MedicamentsScene implements AfterViewInit, OnDestroy {
  /** Affiche les étiquettes (nom, substance, classe) autour de la gélule */
  chips = input(true);
  /** true = ancienne version bleu profond (« hero--dark »), false = version claire */
  sombre = input(false);

  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private frame = 0;
  private resize?: ResizeObserver;
  private souris = { x: 0, y: 0, tx: 0, ty: 0 };
  private onMove = (e: PointerEvent) => {
    this.souris.tx = e.clientX / window.innerWidth - 0.5;
    this.souris.ty = e.clientY / window.innerHeight - 0.5;
  };

  ngAfterViewInit() {
    const cv = this.canvas().nativeElement;
    const ctx = cv.getContext('2d');
    if (!ctx) return;

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    let w = 0, h = 0;
    // Géométrie de la gélule : longueur, rayon, marge des calques, centre
    let Lc = 0, R = 0, pad = 0, cx0 = 0, cy0 = 0, large = true;
    let fondFixe: HTMLCanvasElement | null = null;
    let verreArriere: HTMLCanvasElement | null = null;
    let verreAvant: HTMLCanvasElement | null = null;

    const calque = (lw: number, lh: number): [HTMLCanvasElement, Ctx] => {
      const c = document.createElement('canvas');
      c.width = Math.max(1, Math.round(lw * dpr));
      c.height = Math.max(1, Math.round(lh * dpr));
      const g = c.getContext('2d')!;
      g.scale(dpr, dpr);
      return [c, g];
    };

    // Moitié de gélule, axe horizontal centré en 0 (sens -1 : gauche, +1 : droite)
    const moitie = (g: Ctx, sens: number, k = 1) => {
      const r = R * k, bout = Lc / 2 + (k - 1) * R;
      g.beginPath();
      if (sens < 0) {
        g.moveTo(0, -r);
        g.lineTo(-bout + r, -r);
        g.arc(-bout + r, 0, r, -Math.PI / 2, Math.PI / 2, true);
        g.lineTo(0, r);
      } else {
        g.moveTo(0, -r);
        g.lineTo(bout - r, -r);
        g.arc(bout - r, 0, r, -Math.PI / 2, Math.PI / 2, false);
        g.lineTo(0, r);
      }
      g.closePath();
    };

    // ---------------- Calque : fond bleu profond ----------------
    // ---------------- Fond clair : seulement des halos bleu et teal (pas de grille) ----------------
    const construireFondClair = () => {
      const [c, g] = calque(w, h);
      const l1 = g.createRadialGradient(cx0, cy0, 0, cx0, cy0, Lc * 0.95);
      l1.addColorStop(0, 'rgba(41,87,204,0.34)');
      l1.addColorStop(0.45, 'rgba(91,141,245,0.14)');
      l1.addColorStop(1, 'rgba(91,141,245,0)');
      g.fillStyle = l1;
      g.fillRect(0, 0, w, h);
      const l2 = g.createRadialGradient(cx0 + Lc * 0.3, cy0 + R * 1.8, 0, cx0 + Lc * 0.3, cy0 + R * 1.8, Lc * 0.55);
      l2.addColorStop(0, 'rgba(44,197,181,0.28)');
      l2.addColorStop(1, 'rgba(44,197,181,0)');
      g.fillStyle = l2;
      g.fillRect(0, 0, w, h);
      return c;
    };

    const construireFond = () => {
      if (!this.sombre()) return construireFondClair();
      const [c, g] = calque(w, h);
      const base = g.createLinearGradient(0, 0, w, h);
      base.addColorStop(0, '#12306b');
      base.addColorStop(0.5, '#2957cc');
      base.addColorStop(1, '#4f86f7');
      g.fillStyle = base;
      g.fillRect(0, 0, w, h);

      // grande lumière derrière la gélule
      const l1 = g.createRadialGradient(cx0, cy0, 0, cx0, cy0, Lc * 1.05);
      l1.addColorStop(0, 'rgba(96,170,255,0.62)');
      l1.addColorStop(0.45, 'rgba(50,120,255,0.22)');
      l1.addColorStop(1, 'rgba(50,120,255,0)');
      g.fillStyle = l1;
      g.fillRect(0, 0, w, h);
      const l2 = g.createRadialGradient(cx0 + Lc * 0.25, cy0 + R * 1.9, 0, cx0 + Lc * 0.25, cy0 + R * 1.9, Lc * 0.6);
      l2.addColorStop(0, 'rgba(44,197,181,0.42)');
      l2.addColorStop(1, 'rgba(70,225,255,0)');
      g.fillStyle = l2;
      g.fillRect(0, 0, w, h);

      // faisceau de lumière en diagonale
      g.save();
      g.translate(cx0, cy0);
      g.rotate(-0.5);
      const f = g.createLinearGradient(0, -R * 2.4, 0, R * 2.4);
      f.addColorStop(0, 'rgba(150,200,255,0)');
      f.addColorStop(0.5, 'rgba(150,200,255,0.11)');
      f.addColorStop(1, 'rgba(150,200,255,0)');
      g.fillStyle = f;
      g.fillRect(-w * 2, -R * 2.4, w * 4, R * 4.8);
      g.restore();

      // réseau d'hexagones, visible seulement près de la lumière
      const hr = R * 0.42, pasX = hr * 1.5, pasY = hr * Math.sqrt(3);
      g.lineWidth = 1;
      for (let i = -1; i * pasX < w + pasX; i++) {
        for (let j = -1; j * pasY < h + pasY; j++) {
          const x = i * pasX, y = j * pasY + (i % 2 ? pasY / 2 : 0);
          const d = Math.hypot(x - cx0, y - cy0) / (Lc * 1.15);
          if (d > 1) continue;
          g.strokeStyle = `rgba(170,210,255,${0.09 * (1 - d)})`;
          g.beginPath();
          for (let k = 0; k < 6; k++) {
            const a = (k * Math.PI) / 3;
            g[k ? 'lineTo' : 'moveTo'](x + Math.cos(a) * hr * 0.92, y + Math.sin(a) * hr * 0.92);
          }
          g.closePath();
          g.stroke();
        }
      }

      // mot en filigrane
      g.font = `800 ${Math.round(h * 0.46)}px Manrope, system-ui, sans-serif`;
      g.textAlign = 'right';
      g.textBaseline = 'alphabetic';
      g.fillStyle = 'rgba(255,255,255,0.035)';
      g.fillText('MEDIVAULT', w + h * 0.04, h * 1.06);

      // le côté du texte reste sombre pour la lisibilité, et les bords s'assombrissent
      const gauche = g.createLinearGradient(0, 0, w * 0.6, 0);
      gauche.addColorStop(0, 'rgba(11,37,105,0.5)');
      gauche.addColorStop(1, 'rgba(3,8,32,0)');
      g.fillStyle = gauche;
      g.fillRect(0, 0, w, h);
      const vignette = g.createRadialGradient(w / 2, h / 2, Math.min(w, h) * 0.5, w / 2, h / 2, Math.max(w, h) * 0.75);
      vignette.addColorStop(0, 'rgba(2,6,28,0)');
      vignette.addColorStop(1, 'rgba(11,37,105,0.38)');
      g.fillStyle = vignette;
      g.fillRect(0, 0, w, h);
      return c;
    };

    // ---------------- Calques : le verre de la gélule ----------------
    const construireVerre = () => {
      const LW = Lc + pad * 2, LH = R * 2 + pad * 2;

      // --- arrière : corps des deux moitiés ---
      const [ca, a] = calque(LW, LH);
      a.translate(pad + Lc / 2, pad + R);
      // moitié bleue : verre transparent, plus dense sur les bords
      moitie(a, -1);
      const gb = a.createLinearGradient(0, -R, 0, R);
      if (this.sombre()) {
        gb.addColorStop(0, 'rgba(150,200,255,0.50)');
        gb.addColorStop(0.22, 'rgba(70,130,245,0.26)');
        gb.addColorStop(0.6, 'rgba(24,70,200,0.24)');
        gb.addColorStop(1, 'rgba(6,26,120,0.66)');
      } else {
        gb.addColorStop(0, 'rgba(110,160,250,0.92)');
        gb.addColorStop(0.25, 'rgba(36,84,214,0.9)');
        gb.addColorStop(0.62, 'rgba(18,52,170,0.92)');
        gb.addColorStop(1, 'rgba(8,28,112,0.97)');
      }
      a.fillStyle = gb;
      a.fill();
      // fond du dôme, plus sombre (épaisseur du verre)
      a.save();
      moitie(a, -1);
      a.clip();
      const dome = a.createLinearGradient(-Lc / 2, 0, -Lc / 2 + R * 1.3, 0);
      dome.addColorStop(0, 'rgba(8,30,130,0.55)');
      dome.addColorStop(1, 'rgba(8,30,130,0)');
      a.fillStyle = dome;
      a.fillRect(-Lc / 2, -R, R * 1.3, R * 2);
      a.restore();

      // moitié blanche : nacrée, opaque, légèrement plus large (elle recouvre l'autre)
      a.save();
      a.shadowColor = 'rgba(2,10,50,0.45)';
      a.shadowBlur = R * 0.5;
      a.shadowOffsetY = R * 0.22;
      moitie(a, 1, 1.035);
      const gw = a.createLinearGradient(0, -R, 0, R);
      gw.addColorStop(0, '#ffffff');
      gw.addColorStop(0.34, '#ffffff');
      gw.addColorStop(0.58, '#e6edf9');
      gw.addColorStop(0.82, '#a3b8de');
      gw.addColorStop(0.93, '#8ea6d6');
      gw.addColorStop(1, '#c6dafc');
      a.fillStyle = gw;
      a.fill();
      a.restore();
      a.save();
      moitie(a, 1, 1.035);
      a.clip();
      const occ = a.createLinearGradient(0, 0, Lc * 0.12, 0);
      occ.addColorStop(0, 'rgba(40,62,130,0.34)');
      occ.addColorStop(1, 'rgba(40,62,130,0)');
      a.fillStyle = occ;
      a.fillRect(0, -R * 1.1, Lc * 0.12, R * 2.2);
      const bout = a.createLinearGradient(Lc / 2 - R * 0.9, 0, Lc / 2 + R * 0.05, 0);
      bout.addColorStop(0, 'rgba(50,72,140,0)');
      bout.addColorStop(1, 'rgba(50,72,140,0.34)');
      a.fillStyle = bout;
      a.fillRect(Lc / 2 - R * 0.9, -R * 1.1, R, R * 2.2);
      // reflet bleu du décor sur le bas de la moitié blanche
      const reflet = a.createLinearGradient(0, R * 0.55, 0, R * 1.04);
      reflet.addColorStop(0, 'rgba(90,160,255,0)');
      reflet.addColorStop(1, 'rgba(90,160,255,0.42)');
      a.fillStyle = reflet;
      a.fillRect(0, R * 0.55, Lc / 2 + R, R * 0.5);
      a.restore();

      // --- avant : reflets, bord lumineux, ombre de la jonction ---
      const [cb, b] = calque(LW, LH);
      b.translate(pad + Lc / 2, pad + R);
      // bord lumineux du verre bleu (les bords d'un tube en verre accrochent la lumière)
      b.save();
      moitie(b, -1);
      b.clip();
      b.filter = `blur(${(R * 0.11).toFixed(1)}px)`;
      b.strokeStyle = 'rgba(180,215,255,0.78)';
      b.lineWidth = R * 0.34;
      moitie(b, -1);
      b.stroke();
      // lumière turquoise qui remonte du bas
      b.fillStyle = 'rgba(90,235,255,0.5)';
      b.beginPath();
      b.ellipse(-Lc * 0.24, R * 0.86, Lc * 0.2, R * 0.16, 0, 0, Math.PI * 2);
      b.fill();
      b.filter = 'none';
      // ombre de la moitié blanche sur la moitié bleue
      const ombre = b.createLinearGradient(-R * 0.32, 0, 0, 0);
      ombre.addColorStop(0, 'rgba(3,14,70,0)');
      ombre.addColorStop(1, 'rgba(3,14,70,0.55)');
      b.fillStyle = ombre;
      b.fillRect(-R * 0.32, -R, R * 0.32, R * 2);
      b.restore();

      // long reflet doux sur le dessus, puis un trait net (lumière de studio)
      b.save();
      b.filter = `blur(${(R * 0.07).toFixed(1)}px)`;
      b.fillStyle = 'rgba(255,255,255,0.62)';
      b.beginPath();
      b.roundRect(-Lc / 2 + R * 0.7, -R * 0.76, Lc / 2 - R * 0.95, R * 0.2, R * 0.1);
      b.fill();
      b.fillStyle = 'rgba(255,255,255,0.96)';
      b.beginPath();
      b.roundRect(R * 0.22, -R * 0.82, Lc / 2 - R * 1.15, R * 0.24, R * 0.12);
      b.fill();
      // reflet de fenêtre sur le dôme
      b.fillStyle = 'rgba(255,255,255,0.42)';
      b.beginPath();
      b.ellipse(-Lc / 2 + R * 0.42, -R * 0.22, R * 0.1, R * 0.36, 0.35, 0, Math.PI * 2);
      b.fill();
      // fin reflet bas
      b.fillStyle = 'rgba(255,255,255,0.2)';
      b.beginPath();
      b.roundRect(-Lc / 2 + R * 0.9, R * 0.66, Lc - R * 1.9, R * 0.06, R * 0.03);
      b.fill();
      b.restore();
      b.fillStyle = 'rgba(255,255,255,0.92)';
      b.beginPath();
      b.roundRect(-Lc / 2 + R * 0.95, -R * 0.7, Lc * 0.2, R * 0.045, R * 0.02);
      b.fill();
      b.beginPath();
      b.roundRect(R * 0.4, -R * 0.75, Lc * 0.17, R * 0.05, R * 0.025);
      b.fill();

      // arête de la jonction et contour très fin
      b.strokeStyle = 'rgba(255,255,255,0.7)';
      b.lineWidth = Math.max(1, R * 0.022);
      b.beginPath();
      b.moveTo(0, -R * 1.035);
      b.lineTo(0, R * 1.035);
      b.stroke();
      b.lineWidth = Math.max(1, R * 0.014);
      b.strokeStyle = 'rgba(205,228,255,0.5)';
      moitie(b, -1);
      b.stroke();
      b.strokeStyle = this.sombre() ? 'rgba(255,255,255,0.55)' : 'rgba(120,150,210,0.5)';
      moitie(b, 1, 1.035);
      b.stroke();
      return [ca, cb] as const;
    };

    // ---------------- Molécules à l'intérieur (réseau d'atomes reliés) ----------------
    const rnd = aleatoire(21);
    const atomes = Array.from({ length: 17 }, (_, i) => ({
      u: 0.07 + (i / 16) * 0.86 + (rnd() - 0.5) * 0.06,   // position le long de la moitié bleue (0 = dôme, 1 = jonction)
      phi: i * 2.4 + rnd() * 0.8, rho: 0.3 + rnd() * 0.42, taille: 0.07 + rnd() * 0.06, turquoise: rnd() < 0.35
    }));
    const liaisons: [number, number][] = [];
    atomes.forEach((_, i) => {
      if (i + 1 < atomes.length) liaisons.push([i, i + 1]);
      if (i + 2 < atomes.length && i % 2 === 0) liaisons.push([i, i + 2]);
      if (i + 3 < atomes.length && i % 3 === 0) liaisons.push([i, i + 3]);
    });
    const bulles = Array.from({ length: 22 }, () => ({ u: rnd(), y: (rnd() - 0.5) * 1.3, v: 0.02 + rnd() * 0.05, r: 0.012 + rnd() * 0.02 }));
    const bokeh = Array.from({ length: 26 }, () => ({ x: rnd(), y: rnd(), r: 3 + rnd() * 16, v: 0.006 + rnd() * 0.02, a: 0.1 + rnd() * 0.3, z: rnd() }));

    const fit = () => {
      w = cv.clientWidth;
      h = cv.clientHeight;
      if (!w || !h) return;
      cv.width = w * dpr;
      cv.height = h * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      large = w >= 720;
      Lc = large ? Math.min(w * 0.44, h * 1.18) : Math.min(w * 0.7, h * 0.5);
      R = Lc * 0.192;
      pad = R * 0.9;
      cx0 = large ? w * 0.7 : w * 0.72;
      cy0 = large ? h * 0.5 : h * 0.2;
      fondFixe = construireFond();
      [verreArriere, verreAvant] = construireVerre();
    };
    this.resize = new ResizeObserver(fit);
    this.resize.observe(cv);
    fit();

    const police = (px: number, gras = 700) => `${gras} ${px}px Manrope, system-ui, sans-serif`;
    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const t0 = performance.now();

    const draw = (now: number) => {
      if (!fondFixe || !verreArriere || !verreAvant) {
        this.frame = requestAnimationFrame(draw);
        return;
      }
      const t = reduit ? 2.5 : (now - t0) / 1000;
      const m = this.souris;
      m.x += (m.tx - m.x) * 0.05;
      m.y += (m.ty - m.y) * 0.05;
      const U = Math.min(Math.max(w / 1100, 0.6), 1.15);

      const clair = !this.sombre();
      ctx.globalCompositeOperation = 'source-over';
      ctx.globalAlpha = 1;
      ctx.clearRect(0, 0, w, h);
      ctx.drawImage(fondFixe, 0, 0, w, h);

      // ---------- ADN en profondeur (flou de mise au point) ----------
      ctx.globalCompositeOperation = clair ? 'source-over' : 'lighter';
      const adn = (bx: number, by: number, longueur: number, angle: number, k: number, decalage: number) => {
        const n = 30;
        for (let i = 0; i < n; i++) {
          const u = i / (n - 1), d = (u - 0.5) * longueur, ph = u * 9 + t * 0.7 + decalage;
          const s = Math.sin(ph), c = Math.cos(ph), amp = R * 0.5 * k;
          for (const signe of [1, -1]) {
            const lx = d, ly = s * amp * signe, prof = (c * signe + 1) / 2;
            const x = bx + lx * Math.cos(angle) - ly * Math.sin(angle) - m.x * 30 * k;
            const y = by + lx * Math.sin(angle) + ly * Math.cos(angle) - m.y * 20 * k;
            const r = (2.5 + prof * 5) * k * U;
            const g = ctx.createRadialGradient(x, y, 0, x, y, r * 2.2);
            g.addColorStop(0, clair ? `rgba(44,160,210,${0.1 + prof * 0.2})` : `rgba(150,205,255,${0.2 + prof * 0.3})`);
            g.addColorStop(1, clair ? 'rgba(44,160,210,0)' : 'rgba(150,205,255,0)');
            ctx.fillStyle = g;
            ctx.beginPath();
            ctx.arc(x, y, r * 2.2, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      };
      adn(w * 0.97, h * 0.5, h * 1.25, Math.PI / 2 - 0.12, 1.25, 0);
      if (large) adn(w * 0.44, h * 0.98, w * 0.5, -0.1, 0.8, 2);

      // ---------- Particules de lumière ----------
      for (const p of bokeh) {
        const x = p.x * w - m.x * 50 * p.z, y = (((p.y - t * p.v) % 1) + 1) % 1 * h - m.y * 30 * p.z;
        const scint = 0.7 + 0.3 * Math.sin(t * 1.5 + p.x * 40);
        const g = ctx.createRadialGradient(x, y, 0, x, y, p.r * U);
        g.addColorStop(0, clair ? `rgba(60,150,230,${p.a * scint * 0.38})` : `rgba(190,228,255,${p.a * scint})`);
        g.addColorStop(1, clair ? 'rgba(60,150,230,0)' : 'rgba(190,228,255,0)');
        ctx.fillStyle = g;
        ctx.beginPath();
        ctx.arc(x, y, p.r * U, 0, Math.PI * 2);
        ctx.fill();
      }

      // ---------- La gélule ----------
      const cx = cx0 - m.x * 26, cy = cy0 + Math.sin(t * 0.8) * R * 0.09 - m.y * 16;
      const ang = -0.5 + Math.sin(t * 0.45) * 0.035 + m.x * 0.06;
      const aEcran = (lx: number, ly: number): [number, number] => [
        cx + lx * Math.cos(ang) - ly * Math.sin(ang), cy + lx * Math.sin(ang) + ly * Math.cos(ang)
      ];

      // lueur au sol
      ctx.save();
      ctx.translate(cx + R * 0.3, cy + R * 2.25);
      ctx.scale(1, 0.16);
      const sol = ctx.createRadialGradient(0, 0, 0, 0, 0, Lc * 0.6);
      sol.addColorStop(0, clair ? 'rgba(20,48,120,0.28)' : 'rgba(90,200,255,0.42)');
      sol.addColorStop(1, clair ? 'rgba(20,48,120,0)' : 'rgba(90,200,255,0)');
      ctx.fillStyle = sol;
      ctx.beginPath();
      ctx.arc(0, 0, Lc * 0.6, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();

      ctx.save();
      ctx.translate(cx, cy);
      ctx.rotate(ang);
      ctx.globalCompositeOperation = 'source-over';
      ctx.drawImage(verreArriere, -Lc / 2 - pad, -R - pad, Lc + pad * 2, R * 2 + pad * 2);

      // molécules lumineuses, visibles à travers le verre bleu
      ctx.save();
      moitie(ctx, -1);
      ctx.clip();
      ctx.globalCompositeOperation = 'lighter';
      const coeur = ctx.createRadialGradient(-Lc * 0.24, 0, 0, -Lc * 0.24, 0, R * 1.5);
      coeur.addColorStop(0, 'rgba(120,200,255,0.2)');
      coeur.addColorStop(1, 'rgba(120,200,255,0)');
      ctx.fillStyle = coeur;
      ctx.fillRect(-Lc / 2, -R, Lc / 2, R * 2);

      const rot = t * 0.5;
      const pts = atomes.map(a => {
        const x = -Lc / 2 + R * 0.35 + a.u * (Lc / 2 - R * 0.5);
        const phi = a.phi + rot;
        return { x, y: Math.cos(phi) * a.rho * R, z: Math.sin(phi) * a.rho, a };
      });
      ctx.lineCap = 'round';
      for (const [i, j] of liaisons) {
        const p = pts[i], q = pts[j], prof = ((p.z + q.z) / 2 + 0.7) / 1.4;
        ctx.strokeStyle = `rgba(200,232,255,${0.25 + prof * 0.5})`;
        ctx.lineWidth = R * (0.022 + prof * 0.022);
        ctx.beginPath();
        ctx.moveTo(p.x, p.y);
        ctx.lineTo(q.x, q.y);
        ctx.stroke();
      }
      for (const p of [...pts].sort((a, b) => a.z - b.z)) {
        const prof = (p.z + 0.7) / 1.4, r = R * p.a.taille * (0.7 + prof * 0.6);
        const teinte = p.a.turquoise ? '110,240,235' : '150,200,255';
        const halo = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, r * 3.2);
        halo.addColorStop(0, `rgba(${teinte},${0.26 * (0.4 + prof)})`);
        halo.addColorStop(1, `rgba(${teinte},0)`);
        ctx.fillStyle = halo;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r * 3.2, 0, Math.PI * 2);
        ctx.fill();
        const bille = ctx.createRadialGradient(p.x - r * 0.3, p.y - r * 0.35, r * 0.05, p.x, p.y, r);
        bille.addColorStop(0, `rgba(255,255,255,${0.75 + prof * 0.25})`);
        bille.addColorStop(0.6, `rgba(${teinte},${0.55 + prof * 0.3})`);
        bille.addColorStop(1, `rgba(${teinte},0.12)`);
        ctx.fillStyle = bille;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
        ctx.fill();
      }
      for (const b of bulles) {
        const u = (((b.u + t * b.v) % 1) + 1) % 1;
        ctx.fillStyle = `rgba(220,240,255,${0.5 * Math.sin(u * Math.PI)})`;
        ctx.beginPath();
        ctx.arc(-Lc / 2 + R * 0.3 + u * (Lc / 2 - R * 0.4), b.y * R * 0.6 + Math.sin(t + b.u * 9) * R * 0.04, R * b.r, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.restore();

      ctx.globalCompositeOperation = 'source-over';
      ctx.drawImage(verreAvant, -Lc / 2 - pad, -R - pad, Lc + pad * 2, R * 2 + pad * 2);

      // éclat qui glisse le long du verre
      const glisse = ((t * 0.16) % 1) * 2 - 0.5;
      if (glisse > 0 && glisse < 1) {
        ctx.save();
        moitie(ctx, -1);
        moitie(ctx, 1, 1.035);
        ctx.clip();
        const xg = -Lc / 2 + glisse * Lc;
        const eclat = ctx.createLinearGradient(xg - R * 0.5, 0, xg + R * 0.5, 0);
        eclat.addColorStop(0, 'rgba(255,255,255,0)');
        eclat.addColorStop(0.5, 'rgba(255,255,255,0.28)');
        eclat.addColorStop(1, 'rgba(255,255,255,0)');
        ctx.fillStyle = eclat;
        ctx.transform(1, 0, -0.5, 1, 0, 0);
        ctx.fillRect(xg - R * 0.5, -R * 1.2, R, R * 2.4);
        ctx.restore();
      }
      ctx.restore();

      // ---------- Étiquettes en verre ----------
      if (this.chips() && large) {
        const duree = 4.5, k = Math.floor(t / duree) % FICHES.length, ph = (t % duree) / duree;
        const fondu = Math.min(1, ph * 8, (1 - ph) * 8);
        const [nom, substance, classe] = FICHES[k];
        const liste = [
          { x: cx + Lc * 0.3, y: cy - R * 2.15, e: 'MÉDICAMENT', v: nom, cible: aEcran(Lc * 0.27, -R * 0.3) },
          { x: cx + Lc * 0.47, y: cy + R * 0.9, e: 'SUBSTANCE ACTIVE', v: substance, cible: aEcran(-Lc * 0.1, 0) },
          { x: cx - Lc * 0.02, y: cy + R * 2.45, e: 'CATÉGORIE', v: classe, cible: aEcran(-Lc * 0.3, R * 0.25) }
        ];
        liste.forEach((c, i) => {
          ctx.globalAlpha = fondu;
          ctx.font = police(12 * U, 800);
          const largeur = Math.max(ctx.measureText(c.v).width + 38 * U, 132 * U), hc = 46 * U;
          const x = Math.max(8, Math.min(c.x, w - largeur - 14));
          const y = Math.max(hc / 2 + 10, Math.min(c.y + Math.sin(t * 1.1 + i * 2) * 4 * U, h - hc / 2 - 10));
          const ax = x + (c.cible[0] < x ? 0 : largeur), ay = y;
          ctx.strokeStyle = clair ? 'rgba(18,165,148,0.55)' : 'rgba(190,225,255,0.55)';
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.moveTo(ax, ay);
          ctx.lineTo(c.cible[0], c.cible[1]);
          ctx.stroke();
          const pulse = (t * 0.8 + i * 0.33) % 1;
          ctx.fillStyle = clair ? `rgba(18,165,148,${0.4 * (1 - pulse)})` : `rgba(190,230,255,${0.4 * (1 - pulse)})`;
          ctx.beginPath();
          ctx.arc(c.cible[0], c.cible[1], (4 + pulse * 12) * U, 0, Math.PI * 2);
          ctx.fill();
          ctx.fillStyle = clair ? '#12a594' : '#ffffff';
          ctx.beginPath();
          ctx.arc(c.cible[0], c.cible[1], 3.2 * U, 0, Math.PI * 2);
          ctx.fill();

          ctx.save();
          ctx.shadowColor = clair ? 'rgba(30,64,140,0.26)' : 'rgba(2,8,40,0.45)';
          ctx.shadowBlur = 24 * U;
          ctx.shadowOffsetY = 10 * U;
          const verre = ctx.createLinearGradient(x, y - hc / 2, x, y + hc / 2);
          verre.addColorStop(0, clair ? 'rgba(255,255,255,0.97)' : 'rgba(255,255,255,0.22)');
          verre.addColorStop(1, clair ? 'rgba(244,250,255,0.92)' : 'rgba(255,255,255,0.09)');
          ctx.fillStyle = verre;
          ctx.beginPath();
          ctx.roundRect(x, y - hc / 2, largeur, hc, 14 * U);
          ctx.fill();
          ctx.restore();
          ctx.strokeStyle = clair ? 'rgba(18,165,148,0.5)' : 'rgba(255,255,255,0.38)';
          ctx.lineWidth = 1;
          ctx.beginPath();
          ctx.roundRect(x, y - hc / 2, largeur, hc, 14 * U);
          ctx.stroke();
          ctx.fillStyle = clair ? '#12a594' : '#7fe9e0';
          ctx.beginPath();
          ctx.roundRect(x + 10 * U, y - hc / 2 + 11 * U, 3 * U, hc - 22 * U, 2 * U);
          ctx.fill();
          ctx.textAlign = 'left';
          ctx.textBaseline = 'middle';
          ctx.font = police(8.5 * U, 700);
          ctx.fillStyle = clair ? 'rgba(11,37,69,0.5)' : 'rgba(215,232,255,0.72)';
          ctx.fillText(c.e, x + 21 * U, y - 8.5 * U);
          ctx.font = police(12 * U, 800);
          ctx.fillStyle = clair ? '#0b2545' : '#ffffff';
          ctx.fillText(c.v, x + 21 * U, y + 8 * U);
          ctx.globalAlpha = 1;
        });
      }

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
