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

// ---------- Dessin des médicaments (brillants, avec ombre portée) ----------
function gelule(a: string, b: string): Sprite {
  const [c, g] = toile(260, 150);
  const x = 40, y = 30, L = 180, R = 36;
  const forme = () => { g.beginPath(); g.roundRect(x, y, L, R * 2, R); };

  g.save();
  g.shadowColor = 'rgba(30,64,140,.30)';
  g.shadowBlur = 22;
  g.shadowOffsetY = 16;
  forme();
  g.fillStyle = a;
  g.fill();
  g.restore();

  g.save();
  forme();
  g.clip();
  g.fillStyle = a;
  g.fillRect(x, y, L / 2, R * 2);
  g.fillStyle = b;
  g.fillRect(x + L / 2, y, L / 2, R * 2);
  const ombre = g.createLinearGradient(0, y, 0, y + R * 2);
  ombre.addColorStop(0, 'rgba(255,255,255,.55)');
  ombre.addColorStop(0.38, 'rgba(255,255,255,0)');
  ombre.addColorStop(0.68, 'rgba(11,37,69,0)');
  ombre.addColorStop(1, 'rgba(11,37,69,.30)');
  g.fillStyle = ombre;
  g.fillRect(x, y, L, R * 2);
  g.fillStyle = 'rgba(11,37,69,.14)';
  g.fillRect(x + L / 2 - 1, y, 2, R * 2);
  g.restore();

  g.fillStyle = 'rgba(255,255,255,.8)';
  g.beginPath();
  g.roundRect(x + 24, y + 9, L - 64, 8, 4);
  g.fill();
  return c;
}

function comprime(couleur: string): Sprite {
  const [c, g] = toile(150, 160);
  const cx = 75, cy = 66, r = 46;
  g.save();
  g.shadowColor = 'rgba(30,64,140,.28)';
  g.shadowBlur = 20;
  g.shadowOffsetY = 14;
  g.beginPath();
  g.arc(cx, cy, r, 0, Math.PI * 2);
  g.fillStyle = couleur;
  g.fill();
  g.restore();
  const d = g.createRadialGradient(cx - 16, cy - 18, 4, cx, cy, r);
  d.addColorStop(0, '#ffffff');
  d.addColorStop(0.7, couleur);
  d.addColorStop(1, 'rgba(11,37,69,.16)');
  g.beginPath();
  g.arc(cx, cy, r, 0, Math.PI * 2);
  g.fillStyle = d;
  g.fill();
  g.strokeStyle = 'rgba(11,37,69,.08)';
  g.lineWidth = 2;
  g.beginPath();
  g.arc(cx, cy, r - 9, 0, Math.PI * 2);
  g.stroke();
  g.strokeStyle = 'rgba(11,37,69,.16)';
  g.lineWidth = 3;
  g.lineCap = 'round';
  g.beginPath();
  g.moveTo(cx - r + 14, cy);
  g.lineTo(cx + r - 14, cy);
  g.stroke();
  return c;
}

function capsuleMolle(clair: string, fonce: string): Sprite {
  const [c, g] = toile(210, 140);
  const cx = 105, cy = 58, rx = 68, ry = 38;
  const forme = () => { g.beginPath(); g.ellipse(cx, cy, rx, ry, 0, 0, Math.PI * 2); };
  g.save();
  g.shadowColor = 'rgba(18,120,140,.30)';
  g.shadowBlur = 22;
  g.shadowOffsetY = 14;
  forme();
  g.fillStyle = fonce;
  g.fill();
  g.restore();
  const d = g.createRadialGradient(cx - 22, cy - 16, 4, cx, cy, rx);
  d.addColorStop(0, '#ffffff');
  d.addColorStop(0.35, clair);
  d.addColorStop(1, fonce);
  forme();
  g.fillStyle = d;
  g.fill();
  g.fillStyle = 'rgba(255,255,255,.75)';
  g.beginPath();
  g.ellipse(cx - 20, cy - 18, 26, 7, -0.2, 0, Math.PI * 2);
  g.fill();
  return c;
}

function flou(src: Sprite, px: number): Sprite {
  const [c, g] = toile(src.width + px * 6, src.height + px * 6);
  g.filter = `blur(${px}px)`;
  g.drawImage(src, px * 3, px * 3);
  return c;
}

// Animation du module Médicaments : médicaments en 3D qui flottent lentement, sur trois plans de profondeur :
// lointains (petits et flous), intermédiaires (nets) et premier plan (grands, flous).
// Ils dérivent, tournent, suivent la souris en parallaxe et s'écartent du curseur.
@Component({
  selector: 'mv-medicaments-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block pointer-events-none' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class MedicamentsScene implements AfterViewInit, OnDestroy {
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

    const bases = [
      gelule('#2957cc', '#ffffff'), gelule('#12a594', '#f4f8ff'), gelule('#0b2545', '#8fb4ff'),
      gelule('#ffffff', '#5b8df5'), comprime('#e6eefc'), comprime('#d9f3ef'),
      capsuleMolle('#9af0e4', '#12a594'), capsuleMolle('#bcd3ff', '#3b6fe6')
    ];
    const sprites = bases.map(b => [flou(b, 3), b, flou(b, 7)]);   // lointain, net, premier plan

    const rnd = Math.random;
    const objets = Array.from({ length: this.count() }, (_, i) => {
      const x = (i + rnd()) / this.count();
      let z = rnd();
      if (this.avoidLeft() && x < 0.54) z = rnd() * 0.28;
      return {
        x, y: rnd() * 1.2 - 0.1, z, s: i % bases.length,
        rot: rnd() * Math.PI * 2,
        vr: (rnd() - 0.5) * 0.35, phase: rnd() * 6.28,
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
