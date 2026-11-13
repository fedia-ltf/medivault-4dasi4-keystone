import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, input, viewChild
} from '@angular/core';

// Double hélice d'ADN en particules lumineuses, dessinée en 3D sur un canvas.
// La souris incline l'hélice ; l'animation tourne hors d'Angular pour rester fluide.
@Component({
  selector: 'mv-helix',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class Helix implements AfterViewInit, OnDestroy {
  /** Nombre de barreaux de l'hélice */
  rungs = input(34);
  /** Intensité (0 à 1) */
  intensity = input(1);

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

    // Poussière lumineuse autour de l'hélice
    const dust = Array.from({ length: 70 }, () => ({
      x: Math.random(), y: Math.random(), r: Math.random() * 1.4 + 0.3,
      s: Math.random() * 0.00025 + 0.00008, a: Math.random() * 0.5 + 0.15
    }));

    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const t0 = performance.now();

    const draw = (now: number) => {
      const t = reduced ? 0 : (now - t0) / 1000;
      const m = this.mouse;
      m.x += (m.tx - m.x) * 0.04;
      m.y += (m.ty - m.y) * 0.04;
      const k = this.intensity();

      ctx.clearRect(0, 0, w, h);

      for (const p of dust) {
        p.y -= p.s * 16;
        if (p.y < -0.02) p.y = 1.02;
        ctx.beginPath();
        ctx.fillStyle = `rgba(59, 111, 230, ${p.a * 0.35 * k})`;
        ctx.arc(p.x * w, p.y * h, p.r, 0, Math.PI * 2);
        ctx.fill();
      }

      const n = this.rungs();
      const cx = w / 2, cy = h / 2;
      const span = h * 0.92;
      const R = Math.min(w * 0.22, 120);
      const tilt = -0.32 + m.x * 0.5;
      const cosT = Math.cos(tilt), sinT = Math.sin(tilt);
      const pts: { x: number; y: number; z: number; strand: number; i: number }[] = [];

      for (let i = 0; i < n; i++) {
        const u = i / (n - 1);
        const ly = (u - 0.5) * span;
        const ang = t * 0.9 + u * Math.PI * 3.2 + m.y * 1.5;
        for (let s = 0; s < 2; s++) {
          const a = ang + s * Math.PI;
          const lx = Math.cos(a) * R;
          const z = Math.sin(a);
          // Rotation de l'ensemble (inclinaison)
          const x = cx + lx * cosT - ly * sinT;
          const y = cy + lx * sinT + ly * cosT;
          pts.push({ x, y, z, strand: s, i });
        }
      }

      // Barreaux (liaisons entre les deux brins)
      for (let i = 0; i < n; i++) {
        const a = pts[i * 2], b = pts[i * 2 + 1];
        const depth = (a.z + b.z) / 2;
        const g = ctx.createLinearGradient(a.x, a.y, b.x, b.y);
        g.addColorStop(0, `rgba(59, 111, 230, ${(0.22 + 0.14 * (1 - Math.abs(depth))) * k})`);
        g.addColorStop(1, `rgba(18, 165, 148, ${(0.22 + 0.14 * (1 - Math.abs(depth))) * k})`);
        ctx.strokeStyle = g;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(a.x, a.y);
        ctx.lineTo(b.x, b.y);
        ctx.stroke();
      }

      // Particules, les plus proches dessinées en dernier
      pts.sort((p, q) => p.z - q.z);
      for (const p of pts) {
        const near = (p.z + 1) / 2;
        const r = 1.6 + near * 3.6;
        const col = p.strand === 0 ? '41, 87, 204' : '18, 165, 148';
        const halo = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, r * 5);
        halo.addColorStop(0, `rgba(${col}, ${0.16 * near * k})`);
        halo.addColorStop(1, `rgba(${col}, 0)`);
        ctx.fillStyle = halo;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r * 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = `rgba(${col}, ${(0.45 + near * 0.55) * k})`;
        ctx.beginPath();
        ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
        ctx.fill();
      }

      if (!reduced) this.frame = requestAnimationFrame(draw);
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
