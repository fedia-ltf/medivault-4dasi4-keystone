import { ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, afterNextRender, inject, input, viewChild } from '@angular/core';

interface Noeud { x: number; y: number; z: number; r: number; perle: boolean; ph: number }
interface Lien { a: number; b: number }
interface Point { n: Noeud; sx: number; sy: number; R: number; z: number; alpha: number }

const borne = (v: number, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const douce = (t: number) => { t = borne(t); return t * t * (3 - 2 * t); };

// Générateur pseudo-aléatoire fixe : la molécule est toujours la même d'un affichage à l'autre.
function graine(n: number) { return () => (n = (n * 16807) % 2147483647) / 2147483647; }

/**
 * Scène de sphères en verre et de bulles qui s'assemblent en molécule pendant le défilement.
 * Dessinée sur un canvas 2D (aucune bibliothèque) : profondeur, perspective, reflets.
 * La page appelle `progression(p)` (0 → 1) ; le mouvement est lissé ici pour rester fluide.
 */
@Component({
  selector: 'mv-molecules-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `<canvas #cv class="absolute inset-0 h-full w-full"></canvas>`
})
export class MoleculesScene implements OnDestroy {
  /** Teinte du verre (0-360) : bleu, violet ou vert d'eau selon la catégorie */
  teinte = input(212);

  private cv = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private el = inject(ElementRef<HTMLElement>);

  private cible = 0;
  private courant = 0;
  private ctx!: CanvasRenderingContext2D;
  private w = 0;
  private h = 0;
  private dpr = 1;
  private raf = 0;
  private visible = true;
  private obs?: ResizeObserver;
  private io?: IntersectionObserver;
  private readonly reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private readonly noeuds: Noeud[] = [];
  private readonly liens: Lien[] = [];
  private nbPrincipaux = 0;

  constructor() {
    this.construire();
    afterNextRender(() => this.demarrer());
  }

  /** Reçoit l'avancement du défilement (0 → 1) */
  progression(p: number) {
    this.cible = borne(p);
    if (this.reduit) this.dessiner(performance.now());
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.raf);
    this.obs?.disconnect();
    this.io?.disconnect();
  }

  // ---------------------------------------------------------------- construction de la molécule
  private construire() {
    const a = graine(7);
    // grosses sphères le long d'une vague, comme une chaîne moléculaire
    const principaux = [
      { x: -1.05, y: 0.22, z: 0.2, r: 0.19, perle: false },
      { x: -0.72, y: 0.34, z: -0.25, r: 0.27, perle: true },
      { x: -0.36, y: 0.1, z: 0.3, r: 0.15, perle: false },
      { x: -0.02, y: -0.08, z: -0.1, r: 0.34, perle: false },
      { x: 0.36, y: 0.08, z: 0.35, r: 0.2, perle: true },
      { x: 0.62, y: -0.2, z: -0.3, r: 0.14, perle: false },
      { x: 0.94, y: -0.05, z: 0.1, r: 0.27, perle: false },
      { x: 0.22, y: -0.42, z: -0.05, r: 0.11, perle: true },
      { x: -0.4, y: -0.34, z: -0.2, r: 0.12, perle: false },
      { x: 0.05, y: 0.36, z: -0.45, r: 0.1, perle: false }
    ];
    for (const p of principaux) this.noeuds.push({ ...p, ph: a() * 6.28 });
    this.nbPrincipaux = principaux.length;
    const lie = [[0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 6], [3, 7], [2, 8], [3, 9], [4, 7], [1, 9]];
    for (const [x, y] of lie) this.liens.push({ a: x, b: y });
    // bulles et gouttelettes autour
    for (let i = 0; i < 70; i++) {
      const t = a() * 6.28, u = a() * 2 - 1, d = 0.5 + a() * 0.75;
      this.noeuds.push({
        x: Math.cos(t) * Math.sqrt(1 - u * u) * d * 1.55,
        y: u * d * 0.62,
        z: Math.sin(t) * Math.sqrt(1 - u * u) * d * 0.9,
        r: 0.014 + Math.pow(a(), 2.4) * 0.05, perle: a() > 0.86, ph: a() * 6.28
      });
    }
  }

  // ---------------------------------------------------------------- cycle de vie du dessin
  private demarrer() {
    const canvas = this.cv().nativeElement;
    this.ctx = canvas.getContext('2d')!;
    const mesurer = () => {
      const r = this.el.nativeElement.getBoundingClientRect();
      this.dpr = Math.min(window.devicePixelRatio || 1, 2);
      this.w = Math.max(1, r.width); this.h = Math.max(1, r.height);
      canvas.width = Math.round(this.w * this.dpr); canvas.height = Math.round(this.h * this.dpr);
      this.dessiner(performance.now());
    };
    mesurer();
    this.zone.runOutsideAngular(() => {
      this.obs = new ResizeObserver(mesurer);
      this.obs.observe(this.el.nativeElement);
      this.io = new IntersectionObserver(([e]) => { this.visible = e.isIntersecting; });
      this.io.observe(this.el.nativeElement);
      if (this.reduit) return;
      const boucle = (t: number) => {
        this.raf = requestAnimationFrame(boucle);
        if (!this.visible) return;
        this.courant += (this.cible - this.courant) * 0.075;
        this.dessiner(t);
      };
      this.raf = requestAnimationFrame(boucle);
    });
  }

  // ---------------------------------------------------------------- dessin
  private dessiner(t: number) {
    const { ctx, w, h, dpr } = this;
    if (!ctx) return;
    const p = this.reduit ? Math.max(this.cible, 0.6) : this.courant;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    const cx = w / 2, cy = h * 0.52, S = Math.min(h * 0.62, w * 0.34);
    const eclate = 0.2 + 0.8 * douce(p / 0.34);          // 0,2 : groupées dans la gélule, 1 : molécule déployée
    const yaw = -0.7 + p * 2.7 + (this.reduit ? 0 : t * 0.00006);
    const cos = Math.cos(yaw), sin = Math.sin(yaw), tilt = 0.18 + Math.sin(p * 3) * 0.08;
    const cosT = Math.cos(tilt), sinT = Math.sin(tilt);
    const temps = t * 0.0007;

    const pts: Point[] = this.noeuds.map(n => {
      const flotte = this.reduit ? 0 : Math.sin(temps + n.ph) * 0.03;
      const dx = n.x * eclate, dy = (n.y + flotte) * eclate, dz = n.z * eclate;
      const xr = dx * cos + dz * sin, zr0 = -dx * sin + dz * cos;
      const yr = dy * cosT - zr0 * sinT, zr = dy * sinT + zr0 * cosT;
      const k = 2.4 / (2.4 + zr);
      const grandit = 0.35 + 0.65 * eclate;
      return { n, sx: cx + xr * k * S * 1.45, sy: cy + yr * k * S, R: n.r * S * k * grandit * (n.r > 0.09 ? 1 : 1.1), z: zr, alpha: borne(0.45 + eclate * 0.8) * (0.55 + 0.45 * k) };
    });

    this.adn(p, t, cx, cy, S);

    // liens (tubes de verre) entre les grosses sphères
    ctx.lineCap = 'round';
    for (const l of this.liens) {
      const A = pts[l.a], B = pts[l.b];
      const ep = Math.max(2, Math.min(A.R, B.R) * 0.32);
      const g = ctx.createLinearGradient(A.sx, A.sy, B.sx, B.sy);
      g.addColorStop(0, `hsla(${this.teinte()},70%,82%,${0.5 * Math.min(A.alpha, B.alpha)})`);
      g.addColorStop(0.5, `hsla(${this.teinte()},40%,96%,${0.85 * Math.min(A.alpha, B.alpha)})`);
      g.addColorStop(1, `hsla(${this.teinte()},70%,82%,${0.5 * Math.min(A.alpha, B.alpha)})`);
      ctx.strokeStyle = g; ctx.lineWidth = ep; ctx.beginPath(); ctx.moveTo(A.sx, A.sy); ctx.lineTo(B.sx, B.sy); ctx.stroke();
      ctx.strokeStyle = `rgba(255,255,255,${0.7 * Math.min(A.alpha, B.alpha)})`; ctx.lineWidth = Math.max(1, ep * 0.22);
      ctx.beginPath(); ctx.moveTo(A.sx - ep * 0.15, A.sy - ep * 0.2); ctx.lineTo(B.sx - ep * 0.15, B.sy - ep * 0.2); ctx.stroke();
    }

    // sphères, de la plus lointaine à la plus proche
    pts.sort((a, b) => b.z - a.z);
    for (const q of pts) this.sphere(q);
    this.gelule(p, t, cx, cy, S);
  }

  // ---------------------------------------------------------------- hélice d'ADN (en arrière-plan)
  private adn(p: number, t: number, cx: number, cy: number, S: number) {
    const vue = douce((p - 0.2) / 0.3);
    if (vue <= 0.01) return;
    const { ctx } = this, hue = this.teinte(), N = 26, H = this.h * 0.9, rad = S * 0.15;
    const phase = p * 5.5 + (this.reduit ? 0 : t * 0.0005);
    const brins: Point[][] = [[], []];
    for (let i = 0; i < N; i++) {
      const tt = i / (N - 1), ph = tt * Math.PI * 4.2 + phase;
      for (let b = 0; b < 2; b++) {
        const sg = b ? -1 : 1, z = Math.sin(ph) * sg, k = 1 / (1.2 - z * 0.25);
        brins[b].push({ n: { perle: i % 4 === 0 } as Noeud, sx: cx + Math.cos(ph) * rad * sg * k, sy: cy + (tt - 0.5) * H, R: S * 0.02 * k, z, alpha: vue * (0.75 + 0.25 * z) });
      }
    }
    ctx.lineCap = 'round';
    for (let i = 0; i < N; i++) {                                   // barreaux
      const A = brins[0][i], B = brins[1][i];
      if (i % 2) continue;
      ctx.strokeStyle = `hsla(${hue},60%,72%,${0.38 * vue})`; ctx.lineWidth = 1.6;
      ctx.beginPath(); ctx.moveTo(A.sx, A.sy); ctx.lineTo(B.sx, B.sy); ctx.stroke();
    }
    for (const brin of brins) {                                     // colonne vertébrale
      ctx.strokeStyle = `hsla(${hue},65%,70%,${0.45 * vue})`; ctx.lineWidth = 2.4; ctx.beginPath();
      brin.forEach((q, i) => (i ? ctx.lineTo(q.sx, q.sy) : ctx.moveTo(q.sx, q.sy))); ctx.stroke();
    }
    [...brins[0], ...brins[1]].sort((a, b) => a.z - b.z).forEach(q => this.sphere(q));
  }

  // ---------------------------------------------------------------- gélule de verre qui s'ouvre
  private gelule(p: number, t: number, cx: number, cy: number, S: number) {
    const fondu = 1 - douce((p - 0.19) / 0.15);
    if (fondu <= 0.01) return;
    const { ctx } = this, ouverture = douce((p - 0.03) / 0.2);
    const L = S * 1.3, r = S * 0.27, ang = -0.5 + p * 1.1 + (this.reduit ? 0 : Math.sin(t * 0.0005) * 0.04);
    ctx.save();
    ctx.globalAlpha = fondu;
    ctx.translate(cx, cy); ctx.rotate(ang); ctx.scale(1 + p * 0.25, 1 + p * 0.25);
    // ombre douce portée sous la gélule
    const om = ctx.createRadialGradient(0, r * 1.5, 0, 0, r * 1.5, L * 0.5);
    om.addColorStop(0, 'rgba(60,90,170,.18)'); om.addColorStop(1, 'rgba(60,90,170,0)');
    ctx.fillStyle = om; ctx.beginPath(); ctx.ellipse(0, r * 1.5, L * 0.5, r * 0.28, 0, 0, 6.2832); ctx.fill();
    this.moitie(-ouverture * S * 0.5, false, L, r);
    this.moitie(ouverture * S * 0.5, true, L, r);
    ctx.restore();
  }

  private moitie(dx: number, clair: boolean, L: number, r: number) {
    const { ctx } = this, hue = this.teinte(), h = L / 2;
    ctx.save();
    ctx.translate(dx, 0);
    if (clair) ctx.scale(-1, 1);
    const corps = () => { ctx.beginPath(); ctx.moveTo(0, -r); ctx.lineTo(-h + r, -r); ctx.arc(-h + r, 0, r, -Math.PI / 2, Math.PI / 2, true); ctx.lineTo(0, r); ctx.closePath(); };
    // face intérieure (visible quand la gélule s'ouvre)
    const ri = ctx.createLinearGradient(0, -r, 0, r);
    ri.addColorStop(0, `hsla(${hue},70%,70%,.5)`); ri.addColorStop(1, `hsla(${hue},60%,92%,.4)`);
    const g = ctx.createLinearGradient(0, -r, 0, r);
    if (clair) {
      g.addColorStop(0, 'rgba(190,210,245,.55)'); g.addColorStop(0.3, 'rgba(255,255,255,.38)'); g.addColorStop(0.62, 'rgba(235,243,255,.22)'); g.addColorStop(1, 'rgba(170,196,240,.6)');
    } else {
      g.addColorStop(0, `hsla(${hue},85%,58%,.78)`); g.addColorStop(0.35, `hsla(${hue},85%,74%,.55)`); g.addColorStop(0.7, `hsla(${hue},80%,70%,.5)`); g.addColorStop(1, `hsla(${hue},85%,48%,.82)`);
    }
    corps(); ctx.fillStyle = g; ctx.fill();
    ctx.strokeStyle = `hsla(${hue},55%,72%,.75)`; ctx.lineWidth = 1.4; ctx.stroke();
    // bord ouvert : anneau de verre
    ctx.beginPath(); ctx.ellipse(0, 0, r * 0.3, r, 0, 0, 6.2832); ctx.fillStyle = ri; ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,.8)'; ctx.lineWidth = 1.6; ctx.stroke();
    // reflets : longue traînée de lumière et fin liseré bas
    ctx.lineCap = 'round';
    ctx.strokeStyle = 'rgba(255,255,255,.85)'; ctx.lineWidth = r * 0.14;
    ctx.beginPath(); ctx.moveTo(-h + r * 0.9, -r * 0.56); ctx.lineTo(-r * 0.4, -r * 0.56); ctx.stroke();
    ctx.strokeStyle = 'rgba(255,255,255,.4)'; ctx.lineWidth = r * 0.05;
    ctx.beginPath(); ctx.moveTo(-h + r * 1.1, r * 0.64); ctx.lineTo(-r * 0.6, r * 0.64); ctx.stroke();
    ctx.restore();
  }

  private sphere(q: Point) {
    const { ctx } = this, { sx: x, sy: y, R, alpha } = q, hue = this.teinte();
    if (R < 0.6 || alpha <= 0.01) return;
    ctx.save();
    ctx.globalAlpha = Math.min(1, alpha);

    // lueur douce sous la sphère
    const halo = ctx.createRadialGradient(x, y + R * 0.15, R * 0.6, x, y + R * 0.15, R * 1.7);
    halo.addColorStop(0, `hsla(${hue},80%,70%,.16)`); halo.addColorStop(1, `hsla(${hue},80%,70%,0)`);
    ctx.fillStyle = halo; ctx.beginPath(); ctx.arc(x, y + R * 0.15, R * 1.7, 0, 6.2832); ctx.fill();

    if (q.n.perle) {
      const g = ctx.createRadialGradient(x - R * 0.35, y - R * 0.4, R * 0.1, x, y, R);
      g.addColorStop(0, '#ffffff'); g.addColorStop(0.55, `hsl(${hue},35%,93%)`); g.addColorStop(1, `hsl(${hue},28%,76%)`);
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, R, 0, 6.2832); ctx.fill();
    } else {
      const g = ctx.createRadialGradient(x - R * 0.3, y - R * 0.35, R * 0.05, x, y, R);
      g.addColorStop(0, 'rgba(255,255,255,.55)');
      g.addColorStop(0.4, `hsla(${hue},85%,93%,.22)`);
      g.addColorStop(0.8, `hsla(${hue},75%,78%,.34)`);
      g.addColorStop(1, `hsla(${hue},70%,58%,.62)`);
      ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, R, 0, 6.2832); ctx.fill();
      // reflet de la lumière qui traverse le verre (bas-droite)
      const c = ctx.createRadialGradient(x + R * 0.38, y + R * 0.45, 0, x + R * 0.38, y + R * 0.45, R * 0.7);
      c.addColorStop(0, 'rgba(255,255,255,.5)'); c.addColorStop(1, 'rgba(255,255,255,0)');
      ctx.save(); ctx.beginPath(); ctx.arc(x, y, R * 0.97, 0, 6.2832); ctx.clip(); ctx.fillStyle = c; ctx.fillRect(x - R, y - R, R * 2, R * 2); ctx.restore();
      ctx.strokeStyle = `hsla(${hue},65%,62%,.55)`; ctx.lineWidth = Math.max(0.8, R * 0.035);
      ctx.beginPath(); ctx.arc(x, y, R - ctx.lineWidth / 2, 0, 6.2832); ctx.stroke();
    }
    // éclat brillant en haut à gauche
    ctx.fillStyle = 'rgba(255,255,255,.92)';
    ctx.beginPath(); ctx.ellipse(x - R * 0.38, y - R * 0.42, R * 0.2, R * 0.12, -0.7, 0, 6.2832); ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,.7)';
    ctx.beginPath(); ctx.arc(x + R * 0.1, y - R * 0.62, Math.max(0.6, R * 0.04), 0, 6.2832); ctx.fill();
    ctx.restore();
  }
}
