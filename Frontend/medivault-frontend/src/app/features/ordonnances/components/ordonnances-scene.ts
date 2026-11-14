import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, input, viewChild
} from '@angular/core';

type Sprite = HTMLCanvasElement;

function toile(w: number, h: number): [Sprite, CanvasRenderingContext2D] {
  const c = document.createElement('canvas');
  c.width = w;
  c.height = h;
  return [c, c.getContext('2d')!];
}

// Feuille d'ordonnance : en-tête, lignes de texte et tampon de validation
function feuille(accent: string): Sprite {
  const [c, g] = toile(190, 240);
  const x = 26, y = 18, L = 138, H = 180;
  g.save();
  g.shadowColor = 'rgba(30,64,140,.28)';
  g.shadowBlur = 24;
  g.shadowOffsetY = 16;
  g.fillStyle = '#ffffff';
  g.beginPath();
  g.roundRect(x, y, L, H, 14);
  g.fill();
  g.restore();
  const voile = g.createLinearGradient(x, y, x + L, y + H);
  voile.addColorStop(0, 'rgba(255,255,255,0)');
  voile.addColorStop(1, 'rgba(41,87,204,.08)');
  g.fillStyle = voile;
  g.beginPath();
  g.roundRect(x, y, L, H, 14);
  g.fill();
  // croix médicale + titre
  g.fillStyle = accent;
  g.beginPath();
  g.roundRect(x + 16, y + 16, 26, 26, 8);
  g.fill();
  g.fillStyle = '#ffffff';
  g.fillRect(x + 26, y + 21, 6, 16);
  g.fillRect(x + 21, y + 26, 16, 6);
  g.fillStyle = 'rgba(11,37,69,.75)';
  g.beginPath();
  g.roundRect(x + 52, y + 20, 56, 7, 3.5);
  g.fill();
  g.fillStyle = 'rgba(11,37,69,.25)';
  g.beginPath();
  g.roundRect(x + 52, y + 33, 36, 5, 2.5);
  g.fill();
  // lignes
  [104, 86, 96, 62].forEach((l, i) => {
    g.fillStyle = 'rgba(11,37,69,.16)';
    g.beginPath();
    g.roundRect(x + 16, y + 62 + i * 18, l, 6, 3);
    g.fill();
  });
  // tampon validé
  g.fillStyle = accent;
  g.beginPath();
  g.arc(x + L - 30, y + H - 30, 15, 0, Math.PI * 2);
  g.fill();
  g.strokeStyle = '#ffffff';
  g.lineWidth = 3;
  g.lineCap = 'round';
  g.lineJoin = 'round';
  g.beginPath();
  g.moveTo(x + L - 37, y + H - 30);
  g.lineTo(x + L - 32, y + H - 25);
  g.lineTo(x + L - 23, y + H - 35);
  g.stroke();
  return c;
}

function flou(src: Sprite, px: number): Sprite {
  const [c, g] = toile(src.width + px * 6, src.height + px * 6);
  g.filter = `blur(${px}px)`;
  g.drawImage(src, px * 3, px * 3);
  return c;
}

// Animation du module Ordonnances : feuilles d'ordonnance qui flottent lentement, sur trois plans de profondeur :
// lointains (petits et flous), intermédiaires (nets) et premier plan (grands, flous).
// Ils dérivent, tournent, suivent la souris en parallaxe et s'écartent du curseur.
@Component({
  selector: 'mv-ordonnances-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block pointer-events-none' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class OrdonnancesScene implements AfterViewInit, OnDestroy {
  count = input(22);
  /** Garde la moitié gauche discrète (zone du texte) */
  avoidLeft = input(false);

  private canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private frame = 0;
  private resize?: ResizeObserver;
  private souris = { x: 0, y: 0, tx: 0, ty: 0, px: -999, py: -999 };
  private onMove = (e: PointerEvent) => {
    const r = this.canvas().nativeElement.getBoundingClientRect();
    this.souris.tx = e.clientX / window.innerWidth - 0.5;
    this.souris.ty = e.clientY / window.innerHeight - 0.5;
    this.souris.px = e.clientX - r.left;
    this.souris.py = e.clientY - r.top;
  };

  ngAfterViewInit() {
    const cv = this.canvas().nativeElement;
    const ctx = cv.getContext('2d');
    if (!ctx) return;

    const bases = [feuille('#2957cc'), feuille('#12a594'), feuille('#5b8df5')];
    const sprites = bases.map(b => [flou(b, 3), b, flou(b, 7)]);   // lointain, net, premier plan

    const rnd = Math.random;
    const objets = Array.from({ length: this.count() }, (_, i) => {
      const x = (i + rnd()) / this.count();
      let z = rnd();
      if (this.avoidLeft() && x < 0.54) z = rnd() * 0.28;
      return {
        x, y: rnd() * 1.2 - 0.1, z, s: i % bases.length,
        rot: (rnd() - 0.5) * 0.9,
        vr: (rnd() - 0.5) * 0.08, phase: rnd() * 6.28,
        ox: 0, oy: 0, vx: 0, vy: 0
      };
    }).sort((a, b) => a.z - b.z);

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
    let avant = performance.now();

    const draw = (now: number) => {
      const dt = Math.min((now - avant) / 1000, 0.05);
      avant = now;
      const t = now / 1000;
      const m = this.souris;
      m.x += (m.tx - m.x) * 0.05;
      m.y += (m.ty - m.y) * 0.05;
      const unite = Math.max(0.55, Math.min(w / 1440, 1.15));

      ctx.clearRect(0, 0, w, h);
      for (const o of objets) {
        if (!reduit) {
          o.y -= dt * 0.014 * (0.35 + o.z);
          if (o.y < -0.15) o.y = 1.15;
          o.rot += o.vr * dt;
        }
        const plan = o.z < 0.3 ? 0 : o.z > 0.9 ? 2 : 1;
        const img = sprites[o.s][plan];
        const echelle = (0.2 + o.z * 0.62) * unite;
        const bx = o.x * w + Math.sin(t * 0.35 + o.phase) * 22 * o.z - m.x * 70 * o.z;
        const by = o.y * h + Math.cos(t * 0.28 + o.phase) * 14 * o.z - m.y * 46 * o.z;

        // Le curseur repousse doucement les médicaments proches
        const dx = bx + o.ox - m.px, dy = by + o.oy - m.py;
        const dist = Math.hypot(dx, dy) || 1;
        if (dist < 170 && plan > 0) {
          const f = (1 - dist / 170) * 1.6;
          o.vx += (dx / dist) * f;
          o.vy += (dy / dist) * f;
          o.vr += (dx > 0 ? 1 : -1) * f * 0.004;
        }
        o.vx = (o.vx - o.ox * 0.012) * 0.92;
        o.vy = (o.vy - o.oy * 0.012) * 0.92;
        o.ox += o.vx;
        o.oy += o.vy;

        ctx.save();
        ctx.translate(bx + o.ox, by + o.oy);
        ctx.rotate(o.rot);
        ctx.globalAlpha = plan === 0 ? 0.5 : plan === 2 ? 0.85 : 1;
        ctx.drawImage(img, (-img.width / 2) * echelle, (-img.height / 2) * echelle, img.width * echelle, img.height * echelle);
        ctx.restore();
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
