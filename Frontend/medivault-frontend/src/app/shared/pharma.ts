import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, viewChild
} from '@angular/core';

type V3 = [number, number, number];

// Enseigne de pharmacie : une croix en 3D qui tourne, avec ses faces éclairées,
// des ondes lumineuses, des gélules en orbite et de petits « + » qui s'élèvent.
@Component({
  selector: 'mv-pharma',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block pointer-events-none' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class Pharma implements AfterViewInit, OnDestroy {
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

    // Contour de la croix (12 sommets), puis épaisseur d
    const a = 0.34, L = 1, d = 0.2;
    const contour: [number, number][] = [
      [-a, -L], [a, -L], [a, -a], [L, -a], [L, a], [a, a],
      [a, L], [-a, L], [-a, a], [-L, a], [-L, -a], [-a, -a]
    ];
    const rnd = Math.random;
    const plus = Array.from({ length: 16 }, () => ({
      x: rnd(), y: rnd(), s: 4 + rnd() * 7, v: 0.03 + rnd() * 0.05, p: rnd() * 6.28
    }));
    const gelules = [
      { k: 1.75, a: 0.2, v: 0.5, c: ['#2957cc', '#ffffff'] },
      { k: 1.75, a: 2.3, v: 0.5, c: ['#12a594', '#ffffff'] },
      { k: 1.75, a: 4.4, v: 0.5, c: ['#0b2545', '#8fb4ff'] },
      { k: 2.3, a: 1.1, v: -0.32, c: ['#ffffff', '#5b8df5'] },
      { k: 2.3, a: 3.9, v: -0.32, c: ['#12a594', '#e6f7f4'] }
    ];
    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const t0 = performance.now();
    const melange = (c1: number[], c2: number[], f: number) =>
      `rgb(${c1.map((v, i) => Math.round(v + (c2[i] - v) * f)).join(',')})`;

    const draw = (now: number) => {
      const t = reduit ? 1.2 : (now - t0) / 1000;
      const m = this.souris;
      m.x += (m.tx - m.x) * 0.05;
      m.y += (m.ty - m.y) * 0.05;
      const cx = w / 2, cy = h * 0.48;
      const R = Math.min(w, h) * 0.27;
      ctx.clearRect(0, 0, w, h);

      // Halo et ondes de l'enseigne
      const halo = ctx.createRadialGradient(cx, cy, 0, cx, cy, R * 2.6);
      halo.addColorStop(0, 'rgba(44,197,181,0.26)');
      halo.addColorStop(0.5, 'rgba(91,141,245,0.10)');
      halo.addColorStop(1, 'rgba(91,141,245,0)');
      ctx.fillStyle = halo;
      ctx.fillRect(0, 0, w, h);
      for (let i = 0; i < 3; i++) {
        const p = (t * 0.32 + i / 3) % 1;
        ctx.beginPath();
        ctx.arc(cx, cy, R * (1.1 + p * 1.7), 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(18,165,148,${0.4 * (1 - p)})`;
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }

      // Petits « + » qui montent
      for (const s of plus) {
        const y = ((s.y - t * s.v) % 1 + 1) % 1;
        const x = s.x * w + Math.sin(t * 0.6 + s.p) * 10;
        const alpha = Math.sin(y * Math.PI) * 0.45;
        ctx.strokeStyle = `rgba(41,87,204,${alpha})`;
        ctx.lineWidth = 1.6;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(x - s.s / 2, y * h);
        ctx.lineTo(x + s.s / 2, y * h);
        ctx.moveTo(x, y * h - s.s / 2);
        ctx.lineTo(x, y * h + s.s / 2);
        ctx.stroke();
      }

      const aplati = 0.3, incl = -0.22 + m.x * 0.2;
      const gelule = (g: (typeof gelules)[number], devant: boolean) => {
        const ang = g.a + t * g.v;
        if ((Math.sin(ang) > 0) !== devant) return;
        const x = Math.cos(ang) * R * g.k, y = Math.sin(ang) * R * g.k * aplati;
        const px = cx + x * Math.cos(incl) - y * Math.sin(incl);
        const py = cy + x * Math.sin(incl) + y * Math.cos(incl);
        const e = (devant ? 1 : 0.7) * R * 0.2;
        ctx.save();
        ctx.translate(px, py);
        ctx.rotate(ang * 1.3 + t * 0.4);
        ctx.shadowColor = 'rgba(30,64,140,0.28)';
        ctx.shadowBlur = 10;
        ctx.shadowOffsetY = 5;
        ctx.beginPath();
        ctx.roundRect(-e, -e * 0.4, e * 2, e * 0.8, e * 0.4);
        ctx.fillStyle = g.c[0];
        ctx.fill();
        ctx.shadowColor = 'transparent';
        ctx.clip();
        ctx.fillStyle = g.c[1];
        ctx.fillRect(0, -e, e, e * 2);
        ctx.fillStyle = 'rgba(255,255,255,0.55)';
        ctx.fillRect(-e, -e * 0.4, e * 2, e * 0.22);
        ctx.restore();
      };
      const orbite = (k: number, debut: number, fin: number) => {
        ctx.beginPath();
        ctx.ellipse(cx, cy, R * k, R * k * aplati, incl, debut, fin);
        ctx.strokeStyle = 'rgba(59,111,230,0.22)';
        ctx.lineWidth = 1;
        ctx.stroke();
      };

      orbite(1.75, Math.PI, Math.PI * 2);
      orbite(2.3, Math.PI, Math.PI * 2);
      gelules.forEach(g => gelule(g, false));

      // Ombre au sol
      ctx.fillStyle = 'rgba(30,64,140,0.12)';
      ctx.beginPath();
      ctx.ellipse(cx, cy + R * 1.5, R * 0.9, R * 0.12, 0, 0, Math.PI * 2);
      ctx.fill();

      // ---------- Croix en 3D ----------
      const ry = t * 0.7 + m.x * 1.2, rx = 0.22 + m.y * 0.4;
      const cA = Math.cos(ry), sA = Math.sin(ry), cB = Math.cos(rx), sB = Math.sin(rx);
      const flotte = Math.sin(t * 1.3) * R * 0.05;
      const tourne = ([x, y, z]: V3): V3 => {
        const x1 = x * cA + z * sA, z1 = -x * sA + z * cA;
        return [x1, y * cB - z1 * sB, y * sB + z1 * cB];
      };
      const ecran = ([x, y, z]: V3): [number, number] => {
        const p = 3.4 / (3.4 - z);
        return [cx + x * R * p, cy + flotte + y * R * p];
      };
      const lumiere: V3 = [-0.42, -0.62, 0.66];
      const faces: { pts: V3[]; n: V3; avant?: boolean }[] = [
        { pts: contour.map(([x, y]) => [x, y, d] as V3), n: [0, 0, 1], avant: true },
        { pts: contour.map(([x, y]) => [x, y, -d] as V3).reverse(), n: [0, 0, -1], avant: true }
      ];
      contour.forEach(([x, y], i) => {
        const [x2, y2] = contour[(i + 1) % contour.length];
        const dx = x2 - x, dy = y2 - y, l = Math.hypot(dx, dy);
        faces.push({ pts: [[x, y, d], [x2, y2, d], [x2, y2, -d], [x, y, -d]], n: [dy / l, -dx / l, 0] });
      });

      const visibles = faces
        .map(f => ({ f, n: tourne(f.n), pts: f.pts.map(tourne) }))
        .filter(o => o.n[2] > 0.02)
        .sort((p, q) => p.pts.reduce((s, v) => s + v[2], 0) / p.pts.length - q.pts.reduce((s, v) => s + v[2], 0) / q.pts.length);

      for (const o of visibles) {
        const eclat = Math.max(0, o.n[0] * lumiere[0] + o.n[1] * lumiere[1] + o.n[2] * lumiere[2]);
        const pts = o.pts.map(ecran);
        ctx.beginPath();
        pts.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
        ctx.closePath();
        ctx.fillStyle = melange([10, 110, 100], [126, 232, 218], 0.15 + eclat * 0.85);
        ctx.fill();
        ctx.strokeStyle = 'rgba(255,255,255,0.65)';
        ctx.lineWidth = 1.2;
        ctx.lineJoin = 'round';
        ctx.stroke();
        if (o.f.avant) {
          // croix blanche intérieure, comme une enseigne
          const z = o.f.pts[0][2] > 0 ? d + 0.001 : -d - 0.001;
          const dedans = contour.map(([x, y]) => ecran(tourne([x * 0.6, y * 0.6, z])));
          ctx.beginPath();
          dedans.forEach(([x, y], i) => (i ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
          ctx.closePath();
          ctx.fillStyle = `rgba(255,255,255,${0.75 + 0.2 * Math.sin(t * 2.2)})`;
          ctx.fill();
        }
      }

      orbite(1.75, 0, Math.PI);
      orbite(2.3, 0, Math.PI);
      gelules.forEach(g => gelule(g, true));

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
