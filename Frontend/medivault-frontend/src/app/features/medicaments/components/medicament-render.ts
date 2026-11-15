import { Forme } from '../models/medicament.model';

// ===========================================================================
// Rendu « premium » des médicaments : objets en verre brillant (gélule, comprimé,
// flacon, ampoule, tube), sur un fond bleu profond avec lumière, bokeh et ADN.
// Le même moteur sert aux icônes, aux tuiles des cartes et à la fiche animée.
// ===========================================================================

type Ctx = CanvasRenderingContext2D;

export interface Palette { a: string; b: string; deep: string; glow: string; liquide: string; }

export const PALETTES: Palette[] = [
  { a: '#9dbcff', b: '#2957cc', deep: '#12306b', glow: '#5b8df5', liquide: '#3b6fe6' },
  { a: '#a4f1e6', b: '#12a594', deep: '#0b6b60', glow: '#2cc5b5', liquide: '#14b8a6' },
  { a: '#b9e3ff', b: '#0ea5e9', deep: '#075985', glow: '#38bdf8', liquide: '#0ea5e9' },
  { a: '#9fb4e8', b: '#1e3a8a', deep: '#0f1f4d', glow: '#3b5bdb', liquide: '#2f4fc4' },
  { a: '#c4f1de', b: '#2fb67c', deep: '#0e6b45', glow: '#5fe0a5', liquide: '#34c58a' }
];
export const palette = (variante: number): Palette => PALETTES[Math.abs(Math.trunc(variante)) % PALETTES.length];

const rgba = (hex: string, a: number) => {
  const n = parseInt(hex.slice(1), 16);
  return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})`;
};

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

// ---------- Briques de dessin ----------
function sphere(ctx: Ctx, x: number, y: number, r: number, c0: string, c1: string, c2: string, lueur?: string) {
  ctx.save();
  if (lueur) { ctx.shadowColor = lueur; ctx.shadowBlur = r * 2.4; }
  const g = ctx.createRadialGradient(x - r * 0.35, y - r * 0.4, r * 0.08, x, y, r);
  g.addColorStop(0, c0);
  g.addColorStop(0.55, c1);
  g.addColorStop(1, c2);
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.arc(x, y, r, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
  ctx.fillStyle = 'rgba(255,255,255,0.8)';
  ctx.beginPath();
  ctx.arc(x - r * 0.38, y - r * 0.42, r * 0.18, 0, Math.PI * 2);
  ctx.fill();
}

function ombre(ctx: Ctx, x: number, y: number, rx: number, ry: number, a = 0.35) {
  ctx.save();
  ctx.translate(x, y);
  ctx.scale(1, ry / rx);
  const g = ctx.createRadialGradient(0, 0, 0, 0, 0, rx);
  g.addColorStop(0, `rgba(4,14,50,${a})`);
  g.addColorStop(1, 'rgba(4,14,50,0)');
  ctx.fillStyle = g;
  ctx.beginPath();
  ctx.arc(0, 0, rx, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

// Moitié de gélule : sens -1 = gauche, +1 = droite
function moitie(ctx: Ctx, T: number, r: number, g: number, sens: number, ouvrir = true) {
  if (ouvrir) ctx.beginPath();
  if (sens < 0) {
    ctx.moveTo(-g, -r);
    ctx.lineTo(-T / 2 + r, -r);
    ctx.arc(-T / 2 + r, 0, r, -Math.PI / 2, Math.PI / 2, true);
    ctx.lineTo(-g, r);
  } else {
    ctx.moveTo(g, -r);
    ctx.lineTo(T / 2 - r, -r);
    ctx.arc(T / 2 - r, 0, r, -Math.PI / 2, Math.PI / 2, false);
    ctx.lineTo(g, r);
  }
  ctx.closePath();
}

// ---------- Gélule en verre, avec des billes lumineuses à l'intérieur ----------
function capsule(ctx: Ctx, cx: number, cy: number, S: number, t: number, p: Palette) {
  const T = S * 3.3, r = S * 0.64;
  const g = S * (0.025 + 0.03 * (0.5 + 0.5 * Math.sin(t * 0.9)));
  ombre(ctx, cx + S * 0.2, cy + S * 1.45, S * 1.8, S * 0.3, 0.4);
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(-0.5 + Math.sin(t * 0.5) * 0.04);

  const verreG = ctx.createLinearGradient(0, -r, 0, r);
  verreG.addColorStop(0, p.a);
  verreG.addColorStop(0.5, p.b);
  verreG.addColorStop(1, p.deep);
  const verreD = ctx.createLinearGradient(0, -r, 0, r);
  verreD.addColorStop(0, '#ffffff');
  verreD.addColorStop(0.5, '#e4edfc');
  verreD.addColorStop(1, '#9db4dc');
  for (const [sens, v] of [[-1, verreG], [1, verreD]] as [number, CanvasGradient][]) {
    ctx.save();
    ctx.shadowColor = 'rgba(4,14,50,0.35)';
    ctx.shadowBlur = S * 0.5;
    ctx.shadowOffsetY = S * 0.25;
    ctx.globalAlpha = 0.92;
    moitie(ctx, T, r, g, sens);
    ctx.fillStyle = v;
    ctx.fill();
    ctx.restore();
  }

  // Intérieur : lumière douce et billes qui flottent
  ctx.save();
  moitie(ctx, T, r, g, -1);
  moitie(ctx, T, r, g, 1, false);
  ctx.clip();
  const lueur = ctx.createRadialGradient(0, 0, 0, 0, 0, r * 1.6);
  lueur.addColorStop(0, 'rgba(255,255,255,0.85)');
  lueur.addColorStop(0.5, 'rgba(255,255,255,0.16)');
  lueur.addColorStop(1, 'rgba(255,255,255,0)');
  ctx.fillStyle = lueur;
  ctx.fillRect(-T / 2, -r, T, r * 2);
  for (let i = 0; i < 16; i++) {
    const px = (((i * 0.618) % 1) - 0.5) * T * 0.78;
    const py = (((i * 0.37 + 0.1) % 1) - 0.5) * r * 1.2 + Math.sin(t * 1.3 + i) * S * 0.05;
    sphere(ctx, px, py, S * (0.09 + ((i * 7) % 5) * 0.022), '#ffffff', p.glow, p.b, p.glow);
  }
  ctx.restore();

  // Reflets et bords du verre
  for (const sens of [-1, 1]) {
    const x0 = sens < 0 ? -T / 2 + r * 0.55 : g + S * 0.1;
    const x1 = sens < 0 ? -g - S * 0.1 : T / 2 - r * 0.9;
    ctx.fillStyle = sens < 0 ? 'rgba(255,255,255,0.5)' : 'rgba(255,255,255,0.92)';
    ctx.beginPath();
    ctx.roundRect(x0, -r * 0.82, x1 - x0, r * 0.2, r * 0.1);
    ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.22)';
    ctx.beginPath();
    ctx.roundRect(x0 + (x1 - x0) * 0.1, r * 0.62, (x1 - x0) * 0.8, r * 0.1, r * 0.05);
    ctx.fill();
    moitie(ctx, T, r, g, sens);
    ctx.strokeStyle = 'rgba(255,255,255,0.85)';
    ctx.lineWidth = Math.max(1, S * 0.03);
    ctx.stroke();
  }
  ctx.restore();
}

// ---------- Comprimé : disque en 3D avec biseau et rainure ----------
function comprime(ctx: Ctx, cx: number, cy: number, S: number, t: number, p: Palette) {
  const R = S * 1.25, ry = R * (0.44 + 0.04 * Math.sin(t * 0.8)), ep = R * 0.3;
  ombre(ctx, cx, cy + ep + ry * 1.05, R * 1.3, ry * 0.55, 0.42);
  ctx.save();
  ctx.translate(cx, cy - ep * 0.2);
  ctx.rotate(Math.sin(t * 0.6) * 0.1);
  // tranche
  const tranche = ctx.createLinearGradient(-R, 0, R, 0);
  tranche.addColorStop(0, '#8fa7d2');
  tranche.addColorStop(0.3, '#f6f9ff');
  tranche.addColorStop(0.65, '#c4d3ec');
  tranche.addColorStop(1, '#7790bf');
  ctx.fillStyle = tranche;
  ctx.beginPath();
  ctx.ellipse(0, ep / 2, R, ry, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillRect(-R, -ep / 2, R * 2, ep);
  // dessus
  const dessus = ctx.createRadialGradient(-R * 0.3, -ep / 2 - ry * 0.4, R * 0.05, 0, -ep / 2, R * 1.1);
  dessus.addColorStop(0, '#ffffff');
  dessus.addColorStop(0.6, '#f0f5ff');
  dessus.addColorStop(1, p.a);
  ctx.fillStyle = dessus;
  ctx.beginPath();
  ctx.ellipse(0, -ep / 2, R, ry, 0, 0, Math.PI * 2);
  ctx.fill();
  // biseau
  ctx.strokeStyle = 'rgba(255,255,255,0.95)';
  ctx.lineWidth = Math.max(1, S * 0.05);
  ctx.beginPath();
  ctx.ellipse(0, -ep / 2, R * 0.88, ry * 0.88, 0, 0, Math.PI * 2);
  ctx.stroke();
  ctx.strokeStyle = rgba(p.b, 0.35);
  ctx.lineWidth = Math.max(1, S * 0.03);
  ctx.beginPath();
  ctx.ellipse(0, -ep / 2, R * 0.66, ry * 0.66, 0, 0, Math.PI * 2);
  ctx.stroke();
  // rainure
  ctx.lineCap = 'round';
  ctx.strokeStyle = 'rgba(30,64,140,0.3)';
  ctx.lineWidth = S * 0.09;
  ctx.beginPath();
  ctx.moveTo(-R * 0.8, -ep / 2);
  ctx.lineTo(R * 0.8, -ep / 2);
  ctx.stroke();
  ctx.strokeStyle = 'rgba(255,255,255,0.95)';
  ctx.lineWidth = S * 0.04;
  ctx.beginPath();
  ctx.moveTo(-R * 0.78, -ep / 2 + S * 0.07);
  ctx.lineTo(R * 0.78, -ep / 2 + S * 0.07);
  ctx.stroke();
  // reflet du bord
  ctx.strokeStyle = 'rgba(255,255,255,0.9)';
  ctx.lineWidth = S * 0.07;
  ctx.beginPath();
  ctx.ellipse(0, -ep / 2, R * 0.96, ry * 0.96, 0, Math.PI * 1.08, Math.PI * 1.5);
  ctx.stroke();
  ctx.restore();
}

// ---------- Flacon (sirop) ou ampoule (injectable) : verre, liquide qui ondule, étiquette ----------
function flacon(ctx: Ctx, cx: number, cy: number, S: number, t: number, p: Palette, ampoule: boolean) {
  const bw = (ampoule ? 1.55 : 1.7) * S, bh = (ampoule ? 1.4 : 2.0) * S;
  const nw = (ampoule ? 1.1 : 0.66) * S, nh = (ampoule ? 0.2 : 0.4) * S;
  const sh = (ampoule ? 0.36 : 0.5) * S, caph = (ampoule ? 0.55 : 0.42) * S;
  const H = caph + nh + sh + bh, haut = cy - H / 2;
  const yCou = haut + caph, yEpaule = yCou + nh, yCorps = yEpaule + sh, yBas = yCorps + bh;
  ombre(ctx, cx, yBas + S * 0.06, bw * 0.95, S * 0.24, 0.45);
  ctx.save();
  ctx.translate(cx, 0);

  const verre = () => {
    ctx.beginPath();
    ctx.moveTo(-nw / 2, yCou);
    ctx.lineTo(-nw / 2, yEpaule);
    ctx.bezierCurveTo(-nw / 2, yEpaule + sh * 0.55, -bw / 2, yEpaule + sh * 0.35, -bw / 2, yCorps);
    ctx.lineTo(-bw / 2, yBas - S * 0.24);
    ctx.quadraticCurveTo(-bw / 2, yBas, -bw / 2 + S * 0.24, yBas);
    ctx.lineTo(bw / 2 - S * 0.24, yBas);
    ctx.quadraticCurveTo(bw / 2, yBas, bw / 2, yBas - S * 0.24);
    ctx.lineTo(bw / 2, yCorps);
    ctx.bezierCurveTo(bw / 2, yEpaule + sh * 0.35, nw / 2, yEpaule + sh * 0.55, nw / 2, yEpaule);
    ctx.lineTo(nw / 2, yCou);
    ctx.closePath();
  };
  const gv = ctx.createLinearGradient(-bw / 2, 0, bw / 2, 0);
  gv.addColorStop(0, 'rgba(150,185,255,0.55)');
  gv.addColorStop(0.2, 'rgba(255,255,255,0.8)');
  gv.addColorStop(0.55, 'rgba(255,255,255,0.4)');
  gv.addColorStop(0.88, 'rgba(150,185,255,0.4)');
  gv.addColorStop(1, 'rgba(70,110,210,0.55)');
  ctx.save();
  ctx.shadowColor = 'rgba(4,14,50,0.3)';
  ctx.shadowBlur = S * 0.4;
  ctx.shadowOffsetY = S * 0.2;
  verre();
  ctx.fillStyle = gv;
  ctx.fill();
  ctx.restore();

  // contenu
  ctx.save();
  verre();
  ctx.clip();
  const niveau = yCorps + bh * 0.2;
  const liq = ctx.createLinearGradient(0, niveau, 0, yBas);
  liq.addColorStop(0, rgba(p.liquide, 0.92));
  liq.addColorStop(1, p.b);
  ctx.fillStyle = liq;
  ctx.beginPath();
  ctx.moveTo(-bw / 2, yBas);
  ctx.lineTo(-bw / 2, niveau);
  for (let x = -bw / 2; x <= bw / 2; x += S * 0.08) {
    ctx.lineTo(x, niveau + Math.sin(x / S * 5 + t * 2.2) * S * 0.035);
  }
  ctx.lineTo(bw / 2, yBas);
  ctx.closePath();
  ctx.fill();
  ctx.strokeStyle = 'rgba(255,255,255,0.65)';
  ctx.lineWidth = Math.max(1, S * 0.03);
  ctx.beginPath();
  for (let x = -bw / 2; x <= bw / 2; x += S * 0.08) {
    ctx[x === -bw / 2 ? 'moveTo' : 'lineTo'](x, niveau + Math.sin(x / S * 5 + t * 2.2) * S * 0.035);
  }
  ctx.stroke();
  for (let i = 0; i < 5; i++) {
    const u = (t * 0.1 + i * 0.21) % 1;
    ctx.fillStyle = 'rgba(255,255,255,0.55)';
    ctx.beginPath();
    ctx.arc((-0.3 + i * 0.15) * bw + Math.sin(t + i) * S * 0.05, yBas - u * (yBas - niveau), S * 0.04, 0, Math.PI * 2);
    ctx.fill();
  }
  // étiquette
  const ey = yCorps + bh * 0.3, eh = bh * 0.42;
  ctx.fillStyle = 'rgba(255,255,255,0.94)';
  ctx.beginPath();
  ctx.roundRect(-bw * 0.36, ey, bw * 0.72, eh, S * 0.1);
  ctx.fill();
  ctx.fillStyle = p.b;
  const k = Math.min(eh * 0.5, S * 0.34);
  ctx.fillRect(-bw * 0.3 + k * 0.35, ey + eh / 2 - k * 0.12, k * 0.9, k * 0.24);
  ctx.fillRect(-bw * 0.3 + k * 0.68, ey + eh / 2 - k * 0.45, k * 0.24, k * 0.9);
  ctx.fillStyle = 'rgba(11,37,69,0.35)';
  ctx.fillRect(-bw * 0.3 + k * 1.5, ey + eh * 0.3, bw * 0.3, S * 0.07);
  ctx.fillRect(-bw * 0.3 + k * 1.5, ey + eh * 0.55, bw * 0.2, S * 0.07);
  // reflets
  ctx.fillStyle = 'rgba(255,255,255,0.55)';
  ctx.beginPath();
  ctx.roundRect(-bw / 2 + S * 0.14, yEpaule + sh * 0.3, S * 0.12, bh * 0.95, S * 0.06);
  ctx.fill();
  ctx.fillStyle = 'rgba(255,255,255,0.25)';
  ctx.beginPath();
  ctx.roundRect(bw / 2 - S * 0.24, yCorps + bh * 0.1, S * 0.05, bh * 0.7, S * 0.03);
  ctx.fill();
  ctx.restore();
  verre();
  ctx.strokeStyle = 'rgba(255,255,255,0.85)';
  ctx.lineWidth = Math.max(1, S * 0.03);
  ctx.stroke();

  // bouchon
  if (ampoule) {
    const sertie = ctx.createLinearGradient(-nw * 0.58, 0, nw * 0.58, 0);
    sertie.addColorStop(0, '#9aa8bf');
    sertie.addColorStop(0.35, '#f4f7fc');
    sertie.addColorStop(1, '#8796b0');
    ctx.fillStyle = sertie;
    ctx.beginPath();
    ctx.roundRect(-nw * 0.58, haut + caph * 0.4, nw * 1.16, caph * 0.6 + nh, S * 0.06);
    ctx.fill();
    ctx.fillStyle = '#0b2545';
    ctx.beginPath();
    ctx.roundRect(-nw * 0.36, haut, nw * 0.72, caph * 0.55, S * 0.08);
    ctx.fill();
    ctx.fillStyle = 'rgba(255,255,255,0.3)';
    ctx.fillRect(-nw * 0.28, haut + caph * 0.08, nw * 0.16, caph * 0.34);
  } else {
    const bouchon = ctx.createLinearGradient(-nw * 0.62, 0, nw * 0.62, 0);
    bouchon.addColorStop(0, p.deep);
    bouchon.addColorStop(0.4, p.b);
    bouchon.addColorStop(1, p.deep);
    ctx.fillStyle = bouchon;
    ctx.beginPath();
    ctx.roundRect(-nw * 0.62, haut, nw * 1.24, caph, S * 0.1);
    ctx.fill();
    ctx.strokeStyle = 'rgba(255,255,255,0.18)';
    ctx.lineWidth = 1;
    for (let i = -4; i <= 4; i++) {
      ctx.beginPath();
      ctx.moveTo(i * nw * 0.13, haut + S * 0.06);
      ctx.lineTo(i * nw * 0.13, haut + caph - S * 0.06);
      ctx.stroke();
    }
    ctx.fillStyle = 'rgba(255,255,255,0.35)';
    ctx.beginPath();
    ctx.roundRect(-nw * 0.5, haut + S * 0.04, nw * 0.5, S * 0.07, S * 0.03);
    ctx.fill();
  }
  ctx.restore();
}

// ---------- Tube de pommade, avec une noisette de crème ----------
function tube(ctx: Ctx, cx: number, cy: number, S: number, t: number, p: Palette) {
  const L = S * 3.1, r = S * 0.62;
  ombre(ctx, cx + S * 0.15, cy + S * 1.5, S * 1.8, S * 0.28, 0.42);
  ctx.save();
  ctx.translate(cx, cy);
  ctx.rotate(-0.55 + Math.sin(t * 0.5) * 0.04);
  const corps = () => {
    ctx.beginPath();
    ctx.moveTo(-L / 2, -r * 0.85);
    ctx.bezierCurveTo(-L / 2 + S * 0.5, -r * 0.88, -L / 2 + S * 0.7, -r, -L / 2 + S * 1.0, -r);
    ctx.lineTo(L * 0.2, -r);
    ctx.bezierCurveTo(L * 0.28, -r, L * 0.3, -r * 0.5, L * 0.34, -r * 0.4);
    ctx.lineTo(L * 0.34, r * 0.4);
    ctx.bezierCurveTo(L * 0.3, r * 0.5, L * 0.28, r, L * 0.2, r);
    ctx.lineTo(-L / 2 + S * 1.0, r);
    ctx.bezierCurveTo(-L / 2 + S * 0.7, r, -L / 2 + S * 0.5, r * 0.88, -L / 2, r * 0.85);
    ctx.closePath();
  };
  const gc = ctx.createLinearGradient(0, -r, 0, r);
  gc.addColorStop(0, '#ffffff');
  gc.addColorStop(0.4, '#eaf1ff');
  gc.addColorStop(0.75, '#c9d8f2');
  gc.addColorStop(1, '#8ea6d2');
  ctx.save();
  ctx.shadowColor = 'rgba(4,14,50,0.3)';
  ctx.shadowBlur = S * 0.4;
  ctx.shadowOffsetY = S * 0.2;
  corps();
  ctx.fillStyle = gc;
  ctx.fill();
  ctx.restore();
  ctx.save();
  corps();
  ctx.clip();
  const bande = ctx.createLinearGradient(0, -r, 0, r);
  bande.addColorStop(0, p.a);
  bande.addColorStop(0.5, p.b);
  bande.addColorStop(1, p.deep);
  ctx.fillStyle = bande;
  ctx.fillRect(-L * 0.12, -r, L * 0.3, r * 2);
  ctx.fillStyle = 'rgba(255,255,255,0.8)';
  ctx.fillRect(L * 0.02, -r, S * 0.08, r * 2);
  ctx.strokeStyle = 'rgba(11,37,69,0.18)';
  ctx.lineWidth = 1;
  for (let i = 0; i < 6; i++) {
    ctx.beginPath();
    ctx.moveTo(-L / 2 + S * 0.08 + i * S * 0.08, -r);
    ctx.lineTo(-L / 2 + S * 0.08 + i * S * 0.08, r);
    ctx.stroke();
  }
  ctx.fillStyle = 'rgba(255,255,255,0.7)';
  ctx.beginPath();
  ctx.roundRect(-L / 2 + S * 1.1, -r * 0.84, L * 0.5, r * 0.16, r * 0.08);
  ctx.fill();
  ctx.restore();
  corps();
  ctx.strokeStyle = 'rgba(255,255,255,0.75)';
  ctx.lineWidth = Math.max(1, S * 0.03);
  ctx.stroke();
  // capuchon
  const gk = ctx.createLinearGradient(0, -r * 0.42, 0, r * 0.42);
  gk.addColorStop(0, p.a);
  gk.addColorStop(0.5, p.b);
  gk.addColorStop(1, p.deep);
  ctx.fillStyle = gk;
  ctx.beginPath();
  ctx.roundRect(L * 0.34 - 1, -r * 0.42, L * 0.16, r * 0.84, S * 0.09);
  ctx.fill();
  ctx.fillStyle = 'rgba(255,255,255,0.4)';
  ctx.beginPath();
  ctx.roundRect(L * 0.36, -r * 0.34, L * 0.11, r * 0.12, r * 0.06);
  ctx.fill();
  // crème
  const bx = L / 2 + S * 0.55, by = Math.sin(t * 1.2) * S * 0.03;
  for (let i = 0; i < 3; i++) {
    const w = S * (0.5 - i * 0.14), hh = S * 0.2;
    const gcr = ctx.createLinearGradient(0, by - i * hh - hh, 0, by - i * hh + hh);
    gcr.addColorStop(0, '#ffffff');
    gcr.addColorStop(1, p.a);
    ctx.fillStyle = gcr;
    ctx.beginPath();
    ctx.ellipse(bx, by + S * 0.24 - i * S * 0.22, w, hh, 0, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.fillStyle = 'rgba(255,255,255,0.9)';
  ctx.beginPath();
  ctx.ellipse(bx - S * 0.14, by - S * 0.2, S * 0.07, S * 0.04, -0.4, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

/** Dessine un médicament centré en (cx, cy). S = unité de taille. */
export function dessiner(ctx: Ctx, forme: Forme, cx: number, cy: number, S: number, t: number, p: Palette) {
  switch (forme) {
    case 'COMPRIME': return comprime(ctx, cx, cy, S, t, p);
    case 'SIROP': return flacon(ctx, cx, cy, S, t, p, false);
    case 'INJECTABLE': return flacon(ctx, cx, cy, S, t, p, true);
    case 'POMMADE': return tube(ctx, cx, cy, S, t, p);
    default: return capsule(ctx, cx, cy, S, t, p);
  }
}

/** Fond clair bleuté (tuiles) : même ambiance que le menu latéral, avec le halo bleu du bandeau. */
export function fondClair(ctx: Ctx, w: number, h: number, p: Palette) {
  const g = ctx.createLinearGradient(0, 0, w, h);
  g.addColorStop(0, '#f7faff');
  g.addColorStop(0.55, '#dce8ff');
  g.addColorStop(1, '#bdd2fb');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, w, h);
  const l = ctx.createRadialGradient(w * 0.5, h * 0.45, 0, w * 0.5, h * 0.45, Math.max(w, h) * 0.6);
  l.addColorStop(0, rgba(p.glow, 0.5));
  l.addColorStop(1, rgba(p.glow, 0));
  ctx.fillStyle = l;
  ctx.fillRect(0, 0, w, h);
  const hr = h * 0.16, px = hr * 1.5, py = hr * Math.sqrt(3);
  ctx.lineWidth = 1;
  for (let i = -1; i * px < w + px; i++) {
    for (let j = -1; j * py < h + py; j++) {
      const x = i * px, y = j * py + (i % 2 ? py / 2 : 0);
      const d = Math.hypot(x - w / 2, y - h * 0.45) / (w * 0.55);
      if (d > 1) continue;
      ctx.strokeStyle = `rgba(41,87,204,${0.14 * (1 - d)})`;
      ctx.beginPath();
      for (let k = 0; k < 6; k++) {
        const a = (k * Math.PI) / 3;
        ctx[k ? 'lineTo' : 'moveTo'](x + Math.cos(a) * hr * 0.9, y + Math.sin(a) * hr * 0.9);
      }
      ctx.closePath();
      ctx.stroke();
    }
  }
}

/** Fond bleu profond : lumière, bokeh qui monte et deux hélices d'ADN. */
export function fond(ctx: Ctx, w: number, h: number, t: number, p: Palette) {
  const g = ctx.createLinearGradient(0, 0, w, h);
  g.addColorStop(0, '#12306b');
  g.addColorStop(0.55, '#2957cc');
  g.addColorStop(1, '#4f86f7');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, w, h);
  const lueur = ctx.createRadialGradient(w * 0.5, h * 0.52, 0, w * 0.5, h * 0.52, Math.max(w, h) * 0.62);
  lueur.addColorStop(0, rgba(p.glow, 0.55));
  lueur.addColorStop(0.5, rgba(p.glow, 0.14));
  lueur.addColorStop(1, rgba(p.glow, 0));
  ctx.fillStyle = lueur;
  ctx.fillRect(0, 0, w, h);

  // ADN en filigrane
  for (const [bx, off, k] of [[w * 0.87, 0, 1], [w * 0.1, 2.4, 0.7]] as [number, number, number][]) {
    const amp = w * 0.04 * k, n = 34;
    for (let i = 0; i < n; i++) {
      const u = i / (n - 1), y = u * h, ph = u * 8 + t * 0.8 + off, s = Math.sin(ph), c = Math.cos(ph);
      const x1 = bx + s * amp, x2 = bx - s * amp;
      if (i % 2 === 0) {
        ctx.strokeStyle = 'rgba(160,208,255,0.13)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x1, y);
        ctx.lineTo(x2, y);
        ctx.stroke();
      }
      ctx.fillStyle = `rgba(160,208,255,${0.2 + 0.22 * (c + 1) / 2})`;
      ctx.beginPath();
      ctx.arc(x1, y, (1.3 + (c + 1) * 1.1) * k, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = `rgba(160,208,255,${0.2 + 0.22 * (1 - c) / 2})`;
      ctx.beginPath();
      ctx.arc(x2, y, (1.3 + (1 - c) * 1.1) * k, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  // Bokeh
  const rnd = aleatoire(7);
  for (let i = 0; i < 22; i++) {
    const x = rnd() * w, y0 = rnd(), r = 3 + rnd() * 12, v = 0.01 + rnd() * 0.03, a = 0.12 + rnd() * 0.28;
    const y = (((y0 - t * v) % 1) + 1) % 1 * h;
    const b = ctx.createRadialGradient(x, y, 0, x, y, r);
    b.addColorStop(0, `rgba(190,225,255,${a})`);
    b.addColorStop(1, 'rgba(190,225,255,0)');
    ctx.fillStyle = b;
    ctx.beginPath();
    ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();
  }
}

// ---------- Images prêtes à afficher (mises en cache) ----------
const cache = new Map<string, string>();

/** Icône (fond transparent) ou tuile de carte (fond bleu profond) d'un médicament. */
export function sprite(forme: Forme, variante: number, mode: 'icone' | 'tuile'): string {
  const cle = `${mode}|${forme}|${Math.abs(variante) % PALETTES.length}`;
  const connu = cache.get(cle);
  if (connu) return connu;
  const tuile = mode === 'tuile';
  const W = tuile ? 360 : 128, H = tuile ? 185 : 128, k = 2;
  const c = document.createElement('canvas');
  c.width = W * k;
  c.height = H * k;
  const ctx = c.getContext('2d')!;
  ctx.scale(k, k);
  const p = palette(variante);
  if (tuile) {
    fondClair(ctx, W, H, p);
    dessiner(ctx, forme, W / 2, H * 0.42, 34, 1.2, p);
  } else {
    dessiner(ctx, forme, W / 2, H / 2, 21, 1.2, p);
  }
  const url = c.toDataURL(tuile ? 'image/jpeg' : 'image/png', 0.92);
  cache.set(cle, url);
  return url;
}
