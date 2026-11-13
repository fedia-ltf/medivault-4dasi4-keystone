import {
  AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, inject, viewChild
} from '@angular/core';

// Scène des rendez-vous : un calendrier en verre incliné dont les jours s'allument,
// une horloge dont les aiguilles tournent, des rendez-vous en orbite autour d'elle
// et une bulle « confirmé » qui flotte.
@Component({
  selector: 'mv-orbit',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block pointer-events-none' },
  template: `<canvas #cv class="h-full w-full"></canvas>`
})
export class Orbit implements AfterViewInit, OnDestroy {
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

    const reserves: Record<number, string> = { 9: '#2957cc', 17: '#12a594', 26: '#5b8df5' };
    const AUJOURDHUI = 12;
    const orbites = [
      { k: 1.45, pts: [{ a: 0.4, v: 0.6, c: '41,87,204' }, { a: 3.5, v: 0.6, c: '18,165,148' }] },
      { k: 1.95, pts: [{ a: 1.4, v: -0.38, c: '18,165,148' }, { a: 3.6, v: -0.38, c: '41,87,204' }, { a: 5.4, v: -0.38, c: '91,141,245' }] }
    ];
    const reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const t0 = performance.now();
    const police = (px: number, gras = 700) => `${gras} ${px}px Manrope, system-ui, sans-serif`;

    const draw = (now: number) => {
      const t = reduit ? 2 : (now - t0) / 1000;
      const m = this.souris;
      m.x += (m.tx - m.x) * 0.05;
      m.y += (m.ty - m.y) * 0.05;
      ctx.clearRect(0, 0, w, h);
      const U = Math.min(w / 560, h / 300);            // unité : la scène est pensée pour 560 x 300

      // ---------- Calendrier en verre, incliné ----------
      const Wc = 250 * U, Hc = 200 * U;
      ctx.save();
      ctx.translate(w * 0.36 - m.x * 14, h * 0.5 + Math.sin(t * 0.9) * 6 * U - m.y * 10);
      ctx.transform(1, -0.1, -0.22, 0.96, 0, 0);
      ctx.save();
      ctx.shadowColor = 'rgba(30,64,140,0.28)';
      ctx.shadowBlur = 40 * U;
      ctx.shadowOffsetY = 22 * U;
      ctx.fillStyle = 'rgba(255,255,255,0.92)';
      ctx.beginPath();
      ctx.roundRect(-Wc / 2, -Hc / 2, Wc, Hc, 18 * U);
      ctx.fill();
      ctx.restore();
      // en-tête
      const entete = ctx.createLinearGradient(-Wc / 2, 0, Wc / 2, 0);
      entete.addColorStop(0, '#12306b');
      entete.addColorStop(0.6, '#2957cc');
      entete.addColorStop(1, '#4f86f7');
      ctx.fillStyle = entete;
      ctx.beginPath();
      ctx.roundRect(-Wc / 2, -Hc / 2, Wc, 40 * U, [18 * U, 18 * U, 0, 0]);
      ctx.fill();
      ctx.fillStyle = '#ffffff';
      ctx.font = police(13 * U);
      ctx.textBaseline = 'middle';
      ctx.textAlign = 'left';
      ctx.fillText('Novembre 2026', -Wc / 2 + 16 * U, -Hc / 2 + 21 * U);
      ctx.fillStyle = 'rgba(255,255,255,0.55)';
      [0, 1].forEach(i => {
        ctx.beginPath();
        ctx.arc(Wc / 2 - (18 + i * 16) * U, -Hc / 2 + 20 * U, 4 * U, 0, Math.PI * 2);
        ctx.fill();
      });
      // grille des jours
      const cols = 7, rangs = 5, marge = 14 * U, haut = -Hc / 2 + 50 * U;
      const cw = (Wc - marge * 2) / cols, ch = (Hc - 50 * U - marge) / rangs;
      ctx.textAlign = 'center';
      for (let i = 0; i < cols * rangs; i++) {
        const c = i % cols, r = Math.floor(i / cols);
        const x = -Wc / 2 + marge + c * cw + cw / 2, y = haut + r * ch + ch / 2;
        const vague = Math.pow(Math.max(0, Math.sin(t * 1.6 - (c + r) * 0.55)), 8);
        const couleur = reserves[i];
        const pop = couleur ? 1 + 0.08 * Math.sin(t * 3 + i) : 1;
        const cote = Math.min(cw, ch) * 0.82 * pop;
        ctx.beginPath();
        ctx.roundRect(x - cote / 2, y - cote / 2, cote, cote, 7 * U);
        ctx.fillStyle = couleur ?? `rgba(41,87,204,${0.06 + vague * 0.22})`;
        ctx.fill();
        if (i === AUJOURDHUI) {
          const p = (t * 0.9) % 1;
          ctx.strokeStyle = '#2957cc';
          ctx.lineWidth = 2 * U;
          ctx.stroke();
          ctx.beginPath();
          ctx.roundRect(x - cote / 2 - p * 7 * U, y - cote / 2 - p * 7 * U, cote + p * 14 * U, cote + p * 14 * U, 9 * U);
          ctx.strokeStyle = `rgba(41,87,204,${0.5 * (1 - p)})`;
          ctx.lineWidth = 1.5 * U;
          ctx.stroke();
        }
        ctx.fillStyle = couleur ? '#ffffff' : 'rgba(11,37,69,0.55)';
        ctx.font = police(9.5 * U, couleur || i === AUJOURDHUI ? 800 : 600);
        if (i < 30) ctx.fillText(String(i + 1), x, y + 0.5 * U);   // novembre : 30 jours
      }
      ctx.restore();

      // ---------- Horloge et rendez-vous en orbite ----------
      const cx = w * 0.74 - m.x * 26, cy = h * 0.56 - m.y * 16;
      const r = 46 * U;
      const aplati = 0.34, incl = -0.28 + m.x * 0.2;
      const position = (k: number, a: number) => {
        const x = Math.cos(a) * r * k, y = Math.sin(a) * r * k * aplati;
        return { x: cx + x * Math.cos(incl) - y * Math.sin(incl), y: cy + x * Math.sin(incl) + y * Math.cos(incl), devant: Math.sin(a) > 0 };
      };
      const orbite = (k: number, debut: number, fin: number) => {
        ctx.beginPath();
        ctx.ellipse(cx, cy, r * k, r * k * aplati, incl, debut, fin);
        ctx.strokeStyle = 'rgba(59,111,230,0.30)';
        ctx.lineWidth = 1.2;
        ctx.stroke();
      };
      const points = (devant: boolean) => {
        for (const o of orbites) {
          for (const p of o.pts) {
            const a = p.a + t * p.v;
            const pos = position(o.k, a);
            if (pos.devant !== devant) continue;
            for (let i = 1; i <= 14; i++) {
              const q = position(o.k, a - Math.sign(p.v) * i * 0.04);
              ctx.fillStyle = `rgba(${p.c},${0.24 * (1 - i / 14)})`;
              ctx.beginPath();
              ctx.arc(q.x, q.y, 4.5 * U * (1 - i / 16), 0, Math.PI * 2);
              ctx.fill();
            }
            const taille = (devant ? 7 : 5) * U;
            const g = ctx.createRadialGradient(pos.x, pos.y, 0, pos.x, pos.y, taille * 3.2);
            g.addColorStop(0, `rgba(${p.c},0.38)`);
            g.addColorStop(1, `rgba(${p.c},0)`);
            ctx.fillStyle = g;
            ctx.beginPath();
            ctx.arc(pos.x, pos.y, taille * 3.2, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = `rgb(${p.c})`;
            ctx.beginPath();
            ctx.arc(pos.x, pos.y, taille, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = 'rgba(255,255,255,0.9)';
            ctx.beginPath();
            ctx.arc(pos.x - taille * 0.3, pos.y - taille * 0.3, taille * 0.3, 0, Math.PI * 2);
            ctx.fill();
          }
        }
      };

      const halo = ctx.createRadialGradient(cx, cy, 0, cx, cy, r * 2.6);
      halo.addColorStop(0, 'rgba(91,141,245,0.22)');
      halo.addColorStop(1, 'rgba(91,141,245,0)');
      ctx.fillStyle = halo;
      ctx.fillRect(0, 0, w, h);

      orbites.forEach(o => orbite(o.k, Math.PI, Math.PI * 2));
      points(false);

      ctx.save();
      ctx.shadowColor = 'rgba(30,64,140,0.32)';
      ctx.shadowBlur = 30 * U;
      ctx.shadowOffsetY = 16 * U;
      const cadran = ctx.createRadialGradient(cx - r * 0.3, cy - r * 0.35, r * 0.1, cx, cy, r);
      cadran.addColorStop(0, '#ffffff');
      cadran.addColorStop(1, '#e1ebfb');
      ctx.fillStyle = cadran;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
      // anneau de progression de la journée
      ctx.strokeStyle = 'rgba(41,87,204,0.14)';
      ctx.lineWidth = 4 * U;
      ctx.beginPath();
      ctx.arc(cx, cy, r - 3 * U, 0, Math.PI * 2);
      ctx.stroke();
      const jour = ctx.createLinearGradient(cx - r, cy, cx + r, cy);
      jour.addColorStop(0, '#2957cc');
      jour.addColorStop(1, '#2cc5b5');
      ctx.strokeStyle = jour;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.arc(cx, cy, r - 3 * U, -Math.PI / 2, -Math.PI / 2 + ((t * 0.25) % 1) * Math.PI * 2);
      ctx.stroke();
      for (let i = 0; i < 12; i++) {
        const a = (i / 12) * Math.PI * 2, long = i % 3 === 0 ? 0.14 : 0.07;
        ctx.strokeStyle = i % 3 === 0 ? 'rgba(11,37,69,0.55)' : 'rgba(11,37,69,0.22)';
        ctx.lineWidth = (i % 3 === 0 ? 2 : 1.3) * U;
        ctx.beginPath();
        ctx.moveTo(cx + Math.cos(a) * r * (0.8 - long), cy + Math.sin(a) * r * (0.8 - long));
        ctx.lineTo(cx + Math.cos(a) * r * 0.8, cy + Math.sin(a) * r * 0.8);
        ctx.stroke();
      }
      const aiguille = (angle: number, longueur: number, epaisseur: number, couleur: string) => {
        ctx.strokeStyle = couleur;
        ctx.lineWidth = epaisseur * U;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.lineTo(cx + Math.cos(angle) * r * longueur, cy + Math.sin(angle) * r * longueur);
        ctx.stroke();
      };
      aiguille(t * 0.05 - 1.0, 0.4, 4, '#0b2545');
      aiguille(t * 0.6 - Math.PI / 2, 0.6, 3, '#2957cc');
      aiguille(t * 2.4, 0.68, 1.4, '#12a594');
      ctx.fillStyle = '#0b2545';
      ctx.beginPath();
      ctx.arc(cx, cy, 4 * U, 0, Math.PI * 2);
      ctx.fill();

      orbites.forEach(o => orbite(o.k, 0, Math.PI));
      points(true);

      // ---------- Bulle « rendez-vous confirmé » ----------
      const bx = w * 0.6 - m.x * 34, by = h * 0.17 + Math.sin(t * 1.2 + 1) * 5 * U - m.y * 20;
      const bw = 148 * U, bh = 38 * U;
      ctx.save();
      ctx.shadowColor = 'rgba(30,64,140,0.26)';
      ctx.shadowBlur = 22 * U;
      ctx.shadowOffsetY = 10 * U;
      ctx.fillStyle = 'rgba(255,255,255,0.96)';
      ctx.beginPath();
      ctx.roundRect(bx, by, bw, bh, bh / 2);
      ctx.fill();
      ctx.restore();
      ctx.fillStyle = '#12a594';
      ctx.beginPath();
      ctx.arc(bx + bh / 2, by + bh / 2, 11 * U, 0, Math.PI * 2);
      ctx.fill();
      ctx.strokeStyle = '#ffffff';
      ctx.lineWidth = 2.2 * U;
      ctx.lineJoin = 'round';
      ctx.beginPath();
      ctx.moveTo(bx + bh / 2 - 5 * U, by + bh / 2);
      ctx.lineTo(bx + bh / 2 - 1.5 * U, by + bh / 2 + 3.5 * U);
      ctx.lineTo(bx + bh / 2 + 5 * U, by + bh / 2 - 4 * U);
      ctx.stroke();
      ctx.textAlign = 'left';
      ctx.fillStyle = '#0b2545';
      ctx.font = police(11.5 * U, 800);
      ctx.fillText('10:30 · Confirmé', bx + bh + 2 * U, by + bh / 2 - 6 * U);
      ctx.fillStyle = 'rgba(11,37,69,0.5)';
      ctx.font = police(9.5 * U, 600);
      ctx.fillText('Dr Karim · Cardiologie', bx + bh + 2 * U, by + bh / 2 + 7 * U);

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
