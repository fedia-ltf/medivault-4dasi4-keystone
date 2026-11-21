import { ChangeDetectionStrategy, Component, ElementRef, NgZone, OnDestroy, afterNextRender, effect, inject, input, viewChild } from '@angular/core';
import * as THREE from 'three';
import { RoomEnvironment } from 'three/examples/jsm/environments/RoomEnvironment.js';
import { EffectComposer } from 'three/examples/jsm/postprocessing/EffectComposer.js';
import { RenderPass } from 'three/examples/jsm/postprocessing/RenderPass.js';
import { UnrealBloomPass } from 'three/examples/jsm/postprocessing/UnrealBloomPass.js';
import { BokehPass } from 'three/examples/jsm/postprocessing/BokehPass.js';
import { OutputPass } from 'three/examples/jsm/postprocessing/OutputPass.js';

const borne = (v: number, a = 0, b = 1) => Math.min(b, Math.max(a, v));
const douce = (t: number) => { t = borne(t); return t * t * (3 - 2 * t); };
const mix = (a: number, b: number, t: number) => a + (b - a) * t;
// Générateur pseudo-aléatoire fixe : la scène est toujours la même d'un affichage à l'autre.
function graine(n: number) { return () => (n = (n * 16807) % 2147483647) / 2147483647; }

// ---- L'hélice (unités du monde : rayon 1, un tour = 3,4 comme le vrai ADN)
const L = 7.5;                       // demi-longueur
const OMEGA = (2 * Math.PI) / 3.4;
const DECAL = Math.PI * 0.78;        // décalage des 2 brins (grand et petit sillon)
const RT = 0.15;                     // rayon des brins
const RB = 0.055;                    // rayon des barreaux
const ECART = 0.34;                  // distance entre deux barreaux
const NB_BARREAUX = Math.floor((2 * L - 0.4) / ECART) + 1;
const Y_BARREAU = (j: number) => -L + 0.2 + j * ECART;

/** Plans de la caméra : la scène passe de l'un à l'autre pendant le défilement (p de 0 à 1). */
interface Plan { p: number; camZ: number; camY: number; ax: number; az: number; dx: number; cy: number }
const PLANS: Plan[] = [
  { p: 0, camZ: 16, camY: 0, ax: 0.25, az: 0.55, dx: 0.17, cy: 0.5 },       // la gélule éclatée, à droite du titre
  { p: 0.2, camZ: 8, camY: -0.4, ax: 0.35, az: 0.7, dx: -0.1, cy: 0.5 },     // les grains s'assemblent
  { p: 0.36, camZ: 3.9, camY: -1.4, ax: 0.6, az: 0.95, dx: -0.13, cy: 0.5 }, // gros plan : anneaux lumineux
  { p: 0.54, camZ: 4.4, camY: 2, ax: 0.4, az: 1.2, dx: -0.15, cy: 0.5 },     // la caméra glisse le long de l'hélice
  { p: 0.72, camZ: 10, camY: 0.6, ax: 0.22, az: 1.05, dx: -0.14, cy: 0.5 },  // plan large, en diagonale
  { p: 0.87, camZ: 12, camY: 0.5, ax: 0.22, az: 0.75, dx: -0.14, cy: 0.5 },  // l'hélice se défait en grains
  { p: 1, camZ: 14, camY: 0, ax: 0.2, az: 0.6, dx: -0.17, cy: 0.5 }          // les grains rentrent : la gélule éclatée se recompose
];
// Courbe de Catmull-Rom : passe par chaque plan sans à-coup
const cr = (a: number, b: number, c: number, d: number, t: number) =>
  0.5 * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t + (-a + 3 * b - 3 * c + d) * t * t * t);

const CLAIR = new THREE.Color('#eef4ff');
const NUIT = new THREE.Color('#020a1c');

// Points lumineux (grains, poussière) : un disque doux, la lueur vient du « bloom »
const GRAIN_VERTEX = `
  attribute float aAlpha; attribute float aTaille;
  uniform float uEchelle; varying float vAlpha;
  void main() {
    vAlpha = aAlpha;
    vec4 mv = modelViewMatrix * vec4(position, 1.0);
    gl_PointSize = aTaille * uEchelle / max(0.1, -mv.z);
    gl_Position = projectionMatrix * mv;
  }`;
const GRAIN_FRAGMENT = `
  uniform vec3 uCouleur; uniform float uEclat; varying float vAlpha;
  void main() {
    float d = length(gl_PointCoord - 0.5) * 2.0;
    if (d > 1.0) discard;
    float a = pow(1.0 - d, 1.7);
    gl_FragColor = vec4(uCouleur * (1.0 + uEclat * pow(1.0 - d, 6.0)), a * vAlpha);
  }`;

// Éclat de lumière en étoile (4 branches), comme un reflet de studio
const ECLAT_FRAGMENT = `
  varying float vAlpha;
  void main() {
    vec2 c = gl_PointCoord - 0.5;
    float croix = max(exp(-abs(c.x) * 70.0) * exp(-abs(c.y) * 5.0), exp(-abs(c.y) * 70.0) * exp(-abs(c.x) * 5.0));
    float a = (croix + exp(-dot(c, c) * 140.0)) * vAlpha;
    if (a < 0.01) discard;
    gl_FragColor = vec4(1.0, 0.94, 0.82, min(a, 1.0));   // reflet champagne
  }`;

/** Verre « photo » : les bords du verre reflètent le studio sombre (contour foncé), le centre reste clair. */
function bordsSombres(m: THREE.MeshPhysicalMaterial, force: number, finesse = 1.1) {
  m.onBeforeCompile = sh => {
    sh.fragmentShader = sh.fragmentShader.replace('#include <transmission_fragment>', `#include <transmission_fragment>
      float bordVerre = pow(1.0 - abs(dot(normal, normalize(vViewPosition))), ${finesse.toFixed(2)});
      totalDiffuse = mix(totalDiffuse, totalDiffuse * vec3(0.14, 0.19, 0.28), bordVerre * ${force.toFixed(2)});`);
  };
  m.customProgramCacheKey = () => 'bords' + force + '-' + finesse;
  return m;
}

// Bulle d'air minuscule : un anneau fin et un point de lumière, le centre reste transparent
const BULLE_FRAGMENT = `
  varying float vAlpha;
  void main() {
    vec2 c = gl_PointCoord - 0.5;
    float d = length(c) * 2.0;
    if (d > 1.0) discard;
    float anneau = smoothstep(0.55, 0.9, d) * (1.0 - smoothstep(0.9, 1.0, d));
    vec2 h = c - vec2(-0.16, -0.18);
    float reflet = exp(-dot(h, h) * 150.0);
    vec3 couleur = mix(vec3(0.3, 0.45, 0.5), vec3(1.0), reflet);
    float a = (anneau * 0.85 + reflet + 0.04) * vAlpha;
    gl_FragColor = vec4(couleur, a);
  }`;

interface Grain { brin: boolean; y: number; s: number; j: number; t: number; ang: number; depart: THREE.Vector3; tourbillon: number; retard: number }

/**
 * Scène « Qu'y a-t-il dedans ? » en vraie 3D (three.js / WebGL) : une gélule de verre clair (petit ADN nacré, bulles) ; la caméra plonge dedans,
 * ses grains s'assemblent en hélice d'ADN de verre, la caméra plonge entre les anneaux lumineux,
 * puis l'hélice se défait et ses grains rentrent dans la gélule, sur fond pastel de studio. Verre réaliste (transmission), lueur (bloom),
 * flou de profondeur (bokeh). Tout est piloté par le défilement : sans scroll, rien ne bouge.
 */
@Component({
  selector: 'mv-adn-scene',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block' },
  template: `<canvas #cv class="absolute inset-0 h-full w-full"></canvas>`
})
export class AdnScene implements OnDestroy {
  /** Teinte de la lumière (0-360) selon la catégorie du médicament */
  teinte = input(212);

  private cv = viewChild.required<ElementRef<HTMLCanvasElement>>('cv');
  private zone = inject(NgZone);
  private el = inject(ElementRef<HTMLElement>);
  private readonly reduit = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  private cible = 0;
  private courant = 0;
  private sale = true;
  private raf = 0;
  private visible = true;
  private obs?: ResizeObserver;
  private io?: IntersectionObserver;
  private w = 1; private h = 1;

  // ---- three.js
  private renderer?: THREE.WebGLRenderer;
  private composer!: EffectComposer;
  private bloom!: UnrealBloomPass;
  private bokeh!: BokehPass;
  private scene = new THREE.Scene();
  private camera = new THREE.PerspectiveCamera(50, 1, 0.1, 200);
  private pivot = new THREE.Group();       // incline l'hélice (plans de caméra)
  private helice = new THREE.Group();      // tourne sur elle-même et glisse le long de son axe
  private brins: THREE.Mesh[] = [];
  private barreaux!: THREE.InstancedMesh;
  private anneaux!: THREE.InstancedMesh;
  private adnMini = new THREE.Group();     // petit brin d'ADN nacré dans la gélule (il devient la grande hélice)
  private matAdnMini!: THREE.MeshPhysicalMaterial;
  private studio!: THREE.Texture;          // reflets « photo de studio » (boîtes à lumière) pour le verre de la gélule
  private geoBulles = new THREE.BufferGeometry();
  private matBulles!: THREE.ShaderMaterial;
  private geoEclats = new THREE.BufferGeometry();
  private matEclats!: THREE.ShaderMaterial;
  private eclatsPhases: number[] = [];
  private fondStudio!: THREE.Mesh;        // fond pastel de studio + bokeh, collé à la caméra (vu à travers le verre)
  private gelule = new THREE.Group();
  private perles = new THREE.Group();      // vraies billes de verre autour de la gélule (suivent sa place, pas sa rotation)
  private perlesInfos: { m: THREE.Mesh; base: THREE.Vector3 }[] = [];
  private fondus: { m: THREE.Material; max: number }[] = [];   // verre de la gélule et des billes, fondu au noir
  private halo!: THREE.Mesh;
  private matGrains!: THREE.ShaderMaterial;
  private matPoussiere!: THREE.ShaderMaterial;
  private matBokeh!: THREE.ShaderMaterial;
  private grains: Grain[] = [];
  private geoGrains = new THREE.BufferGeometry();
  private geoPoussiere = new THREE.BufferGeometry();
  private poussiere: { base: THREE.Vector3; derive: THREE.Vector3; ph: number }[] = [];
  private lumRimA = new THREE.PointLight('#5b8df5', 3, 0, 0);
  private lumRimB = new THREE.PointLight('#2cc5b5', 2, 0, 0);
  private anneauxInfos: { y: number; pos: THREE.Vector3; quat: THREE.Quaternion }[] = [];
  private barreauxInfos: { y: number; pos: THREE.Vector3; quat: THREE.Quaternion; long: number }[] = [];
  private lueur = new THREE.Color();
  private matBrin!: THREE.MeshPhysicalMaterial;
  private cle = new THREE.DirectionalLight('#ffffff', 2.2);
  private ambiance = new THREE.AmbientLight('#ffffff', 0.35);
  private douceur = new THREE.DirectionalLight('#cfe0ff', 0.6);   // lumière d'appoint froide, à gauche

  constructor() {
    effect(() => { this.teinte(); this.sale = true; });
    afterNextRender(() => this.zone.runOutsideAngular(() => this.demarrer()));
  }

  /** Reçoit l'avancement du défilement (0 → 1). Le mouvement est lissé ici. */
  progression(p: number) {
    this.cible = borne(p);
    if (this.reduit) { this.courant = this.cible; this.dessiner(); }
  }

  ngOnDestroy() {
    cancelAnimationFrame(this.raf);
    this.obs?.disconnect();
    this.io?.disconnect();
    this.scene.traverse(o => {
      const m = o as THREE.Mesh;
      m.geometry?.dispose();
      const mats = Array.isArray(m.material) ? m.material : m.material ? [m.material] : [];
      mats.forEach(x => x.dispose());
    });
    this.composer?.dispose();
    this.renderer?.dispose();
  }

  // ================================================================ mise en place
  private demarrer() {
    const canvas = this.cv().nativeElement;
    let r: THREE.WebGLRenderer;
    try {
      r = new THREE.WebGLRenderer({ canvas, antialias: true, powerPreference: 'high-performance' });
    } catch { return; }                       // pas de WebGL : le texte de la page reste lisible
    this.renderer = r;
    r.toneMapping = THREE.NoToneMapping;                  // le fond garde exactement les couleurs de l'appli
    r.outputColorSpace = THREE.SRGBColorSpace;

    const pmrem = new THREE.PMREMGenerator(r);
    this.scene.environment = pmrem.fromScene(new RoomEnvironment(), 0.04).texture;
    this.studio = this.construireStudio(pmrem);
    this.scene.background = CLAIR.clone();
    this.scene.fog = new THREE.Fog(CLAIR.clone(), 10, 40);

    const cle = this.cle; cle.position.set(3, 6, 8);
    this.lumRimA.position.set(-5, 3, -4);
    this.lumRimB.position.set(5, -3, -3);
    this.douceur.position.set(-6, -2, 4);
    this.scene.add(cle, this.lumRimA, this.lumRimB, this.ambiance, this.douceur);

    this.construireHelice();
    this.construireGrains();      // avant la gélule : ses granules reprennent la place des grains
    this.construireGelule();
    this.construireDecor();
    this.pivot.rotation.order = 'ZYX';
    this.pivot.add(this.helice);
    this.scene.add(this.pivot, this.gelule, this.perles, this.camera);   // la caméra porte le fond de studio

    // contours lisses : rendu multi-échantillonné (MSAA x4), sinon les bords fins du verre crénèlent
    this.composer = new EffectComposer(r, new THREE.WebGLRenderTarget(1, 1, { samples: 4, type: THREE.HalfFloatType }));
    this.composer.addPass(new RenderPass(this.scene, this.camera));
    this.bokeh = new BokehPass(this.scene, this.camera, { focus: 10, aperture: 0, maxblur: 0.008 });
    this.composer.addPass(this.bokeh);
    this.bloom = new UnrealBloomPass(new THREE.Vector2(256, 256), 0.8, 0.85, 0.82);
    this.composer.addPass(this.bloom);
    this.composer.addPass(new OutputPass());

    const mesurer = () => {
      const b = this.el.nativeElement.getBoundingClientRect();
      this.w = Math.max(1, b.width); this.h = Math.max(1, b.height);
      const dpr = Math.min(window.devicePixelRatio || 1, this.w < 700 ? 1.5 : 2);
      r.setPixelRatio(dpr); r.setSize(this.w, this.h, false);
      this.composer.setPixelRatio(dpr); this.composer.setSize(this.w, this.h);
      this.sale = true;
      this.dessiner();
    };
    mesurer();
    this.obs = new ResizeObserver(mesurer);
    this.obs.observe(this.el.nativeElement);
    this.io = new IntersectionObserver(([e]) => { this.visible = e.isIntersecting; });
    this.io.observe(this.el.nativeElement);
    if (this.reduit) return;
    // On ne redessine que si le défilement a bougé : sans scroll, la scène reste immobile.
    const boucle = () => {
      this.raf = requestAnimationFrame(boucle);
      if (!this.visible) return;
      const ecart = this.cible - this.courant;
      if (Math.abs(ecart) < 0.00012 && !this.sale) return;
      this.courant = Math.abs(ecart) < 0.00012 ? this.cible : this.courant + ecart * 0.085;
      this.dessiner();
    };
    this.raf = requestAnimationFrame(boucle);
  }

  private point(y: number, s: number, rayon = 1) {
    const th = y * OMEGA + s * DECAL;
    return new THREE.Vector3(Math.cos(th) * rayon, y, Math.sin(th) * rayon);
  }

  private verre(couleur: string, attenuation: string, distance: number) {
    return new THREE.MeshPhysicalMaterial({
      color: couleur, metalness: 0, roughness: 0.16, transmission: 1, thickness: 0.45, ior: 1.45,
      attenuationColor: new THREE.Color(attenuation), attenuationDistance: distance,
      clearcoat: 1, clearcoatRoughness: 0.12, envMapIntensity: 1.4, specularIntensity: 0.8
    });
  }

  private construireHelice() {
    // 1. Les deux brins : un tube de verre par moitié (du centre vers chaque bout),
    //    pour les faire « pousser » depuis le centre pendant l'assemblage.
    const matBrin = this.matBrin = this.verre('#e4efff', '#4f86f7', 1.6);
    for (let s = 0; s < 2; s++) {
      for (const sens of [1, -1]) {
        const pts: THREE.Vector3[] = [];
        for (let y = 0; y <= L + 1e-6; y += 0.05) pts.push(this.point(y * sens, s));
        const geo = new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 320, RT, 28, false);
        const m = new THREE.Mesh(geo, matBrin);
        this.brins.push(m);
        this.helice.add(m);
      }
    }
    // 2. Les barreaux (paires de bases) : deux moitiés de couleur différente
    const Y = new THREE.Vector3(0, 1, 0), Z = new THREE.Vector3(0, 0, 1);
    const teintes = ['#a9c4f5', '#a8e3dc', '#bcd6f7', '#9fd9d1'];
    this.barreaux = new THREE.InstancedMesh(new THREE.CylinderGeometry(RB, RB, 1, 18, 1), this.verre('#ffffff', '#3b6fe6', 1.4), NB_BARREAUX * 2);
    this.anneaux = new THREE.InstancedMesh(new THREE.TorusGeometry(0.24, 0.022, 16, 72), new THREE.MeshBasicMaterial({ color: '#ffffff', toneMapped: false }), Math.ceil(NB_BARREAUX / 3) * 2);
    let k = 0, a = 0;
    for (let j = 0; j < NB_BARREAUX; j++) {
      const y = Y_BARREAU(j), P0 = this.point(y, 0), P1 = this.point(y, 1), mil = P0.clone().lerp(P1, 0.5);
      for (const [A, B, c] of [[P0, mil, teintes[(j * 7) % 4]], [mil, P1, teintes[(j * 7 + 1) % 4]]] as [THREE.Vector3, THREE.Vector3, string][]) {
        const dir = B.clone().sub(A);
        this.barreauxInfos.push({ y, pos: A.clone().lerp(B, 0.5), quat: new THREE.Quaternion().setFromUnitVectors(Y, dir.clone().normalize()), long: dir.length() });
        this.barreaux.setColorAt(k++, new THREE.Color(c));
      }
      if (j % 3 === 0) {
        const dir = P1.clone().sub(P0).normalize(), q = new THREE.Quaternion().setFromUnitVectors(Z, dir);
        for (const t of [0.2, 0.8]) this.anneauxInfos.push({ y, pos: P0.clone().lerp(P1, t), quat: q });
        a += 2;
      }
    }
    this.anneaux.count = a;
    this.anneaux.setColorAt(0, new THREE.Color());
    this.helice.add(this.barreaux, this.anneaux);
  }

  /** Un studio photo virtuel : fond dégradé bleu-gris + grandes boîtes à lumière. Le verre y prend ses reflets nets et ses bords sombres. */
  private construireStudio(pmrem: THREE.PMREMGenerator) {
    const env = new THREE.Scene();
    env.add(new THREE.Mesh(new THREE.SphereGeometry(20, 48, 24), new THREE.ShaderMaterial({
      side: THREE.BackSide,
      vertexShader: `varying vec3 vP; void main(){ vP = normalize(position); gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`,
      fragmentShader: `varying vec3 vP; void main(){
        float t = smoothstep(-0.6, 0.9, vP.y);
        gl_FragColor = vec4(mix(vec3(0.07, 0.1, 0.17), vec3(0.62, 0.7, 0.84), t), 1.0); }`
    })));
    const boite = (w: number, h: number, x: number, y: number, z: number, force: number) => {
      const m = new THREE.Mesh(new THREE.PlaneGeometry(w, h), new THREE.MeshBasicMaterial({ color: new THREE.Color(force, force, force * 1.03), side: THREE.DoubleSide }));
      m.position.set(x, y, z); m.lookAt(0, 0, 0); env.add(m);
    };
    // très lumineuses (comme de vraies boîtes à lumière) : reflets blancs nets sur le verre
    boite(16, 5, 0, 13, 3, 24);       // grande boîte au-dessus
    boite(3, 16, -13, 2, 5, 18);      // bande verticale à gauche
    boite(2.5, 14, 13, 0, -3, 12);    // bande à droite, en arrière
    boite(7, 3, 3, 4, 14, 6);         // face, douce
    const t = pmrem.fromScene(env, 0.015).texture;
    env.traverse(o => { const m = o as THREE.Mesh; m.geometry?.dispose(); (m.material as THREE.Material | undefined)?.dispose(); });
    return t;
  }

  private construireGelule() {
    const a = graine(29);
    // 1. la gélule : un seul objet en verre clair, poli comme un visuel de studio
    const verre = bordsSombres(new THREE.MeshPhysicalMaterial({
      color: '#ffffff', metalness: 0, roughness: 0, transmission: 1, thickness: 0.6, ior: 1.5, dispersion: 0.3,
      attenuationColor: new THREE.Color('#d6efec'), attenuationDistance: 9, specularIntensity: 1,
      clearcoat: 1, clearcoatRoughness: 0, envMap: this.studio, envMapIntensity: 1.35, depthWrite: false, transparent: true
    }), 0.72, 2.4);
    const coque = new THREE.Mesh(new THREE.CapsuleGeometry(1, 2.3, 48, 128), verre);
    coque.renderOrder = 3;
    // paroi épaisse : liseré clair sur le bord intérieur + jointure fine des deux moitiés
    const lisere = new THREE.Mesh(new THREE.CapsuleGeometry(0.9, 2.3, 32, 96), new THREE.ShaderMaterial({
      transparent: true, depthWrite: false, toneMapped: false, uniforms: { opacity: { value: 1 } },
      vertexShader: `varying vec3 vN; varying vec3 vV; void main(){ vec4 mv = modelViewMatrix * vec4(position, 1.0); vN = normalize(normalMatrix * normal); vV = normalize(-mv.xyz); gl_Position = projectionMatrix * mv; }`,
      fragmentShader: `uniform float opacity; varying vec3 vN; varying vec3 vV; void main(){ float f = pow(1.0 - abs(dot(vN, vV)), 5.0); gl_FragColor = vec4(1.0, 1.0, 1.0, f * 0.8 * opacity); }`
    }));
    lisere.renderOrder = 4;
    const jointure = new THREE.Mesh(new THREE.TorusGeometry(1.003, 0.008, 8, 192), new THREE.MeshBasicMaterial({ color: '#ffffff', transparent: true, opacity: 0.5, depthWrite: false }));
    jointure.rotation.x = Math.PI / 2; jointure.position.y = 0.4; jointure.renderOrder = 4;
    this.gelule.add(coque, lisere, jointure);
    this.fondus.push({ m: verre, max: 1 }, { m: lisere.material as THREE.Material, max: 1 }, { m: jointure.material as THREE.Material, max: 0.5 });

    // 2. à l'intérieur : un petit brin d'ADN nacré
    this.matAdnMini = new THREE.MeshPhysicalMaterial({
      color: '#cfe8e6', roughness: 0.2, metalness: 0, clearcoat: 1, clearcoatRoughness: 0.06,
      sheen: 0.9, sheenColor: new THREE.Color('#7fd0c8'), iridescence: 0.55, iridescenceIOR: 1.3,
      emissive: new THREE.Color('#a9ddd8'), emissiveIntensity: 0.32, envMap: this.studio, envMapIntensity: 0.3
    });
    const or = new THREE.MeshPhysicalMaterial({   // barreaux en or champagne, comme un bijou
      color: '#eac47c', metalness: 1, roughness: 0.16, clearcoat: 1, clearcoatRoughness: 0.04, emissive: new THREE.Color('#a8782a'), emissiveIntensity: 0.3, envMap: this.studio, envMapIntensity: 0.5
    });
    const R = 0.47, H = 1.5, TOURS = 2.4, PHASE = Math.PI * 0.8;
    const pointAdn = (t: number, s: number) => { const th = t * TOURS * 2 * Math.PI + s * PHASE; return new THREE.Vector3(Math.cos(th) * R, mix(-H, H, t), Math.sin(th) * R); };
    for (let s = 0; s < 2; s++) {
      const pts: THREE.Vector3[] = [];
      for (let i = 0; i <= 120; i++) pts.push(pointAdn(i / 120, s));
      this.adnMini.add(new THREE.Mesh(new THREE.TubeGeometry(new THREE.CatmullRomCurve3(pts), 260, 0.058, 20, false), this.matAdnMini));
    }
    const nb = 26, barreaux = new THREE.InstancedMesh(new THREE.CylinderGeometry(0.022, 0.022, 1, 12), or, nb);
    for (let j = 0; j < nb; j++) {
      const t = (j + 0.5) / nb, A = pointAdn(t, 0), B = pointAdn(t, 1), d = B.clone().sub(A);
      barreaux.setMatrixAt(j, new THREE.Matrix4().compose(A.clone().add(B).multiplyScalar(0.5),
        new THREE.Quaternion().setFromUnitVectors(new THREE.Vector3(0, 1, 0), d.clone().normalize()), new THREE.Vector3(1, d.length(), 1)));
    }
    this.adnMini.add(barreaux);
    // petites billes d'or à chaque jonction brin / barreau
    const billesOr = new THREE.InstancedMesh(new THREE.SphereGeometry(0.062, 22, 16), or, nb * 2);
    for (let j = 0; j < nb; j++) for (let k = 0; k < 2; k++) {
      const A = pointAdn((j + 0.5) / nb, k);
      billesOr.setMatrixAt(j * 2 + k, new THREE.Matrix4().makeTranslation(A.x, A.y, A.z));
    }
    this.adnMini.add(billesOr);
    this.gelule.add(this.adnMini);

    // 3. bulles minuscules dans la gélule (plus denses en bas) et quelques grosses autour
    const nI = 420, nE = 0, n = nI + nE, pb = new Float32Array(n * 3), tb = new Float32Array(n);
    for (let i = 0; i < n; i++) {
      const th = a() * 6.2832;
      if (i < nI) {
        const y = -2 + Math.pow(a(), 1.5) * 3.7;
        const lim = Math.abs(y) > 1.15 ? Math.sqrt(Math.max(0, 0.81 - (Math.abs(y) - 1.15) ** 2)) : 0.9;
        const r = Math.min(a() < 0.4 ? 0.87 : Math.sqrt(a()) * 0.85, lim * 0.96);   // une partie collée à la paroi
        pb.set([Math.cos(th) * r, y, Math.sin(th) * r], i * 3);
        tb[i] = 0.026 + Math.pow(a(), 3) * 0.1;
      } else {
        const r = 1.7 + a() * 2.4;
        pb.set([Math.cos(th) * r, (a() * 2 - 1) * 3, 0.6 + a() * 2], i * 3);
        tb[i] = 0.1 + Math.pow(a(), 2) * 0.42;
      }
    }
    this.geoBulles.setAttribute('position', new THREE.BufferAttribute(pb, 3));
    this.geoBulles.setAttribute('aAlpha', new THREE.BufferAttribute(new Float32Array(n), 1));
    this.geoBulles.setAttribute('aTaille', new THREE.BufferAttribute(tb, 1));
    this.matBulles = new THREE.ShaderMaterial({
      vertexShader: GRAIN_VERTEX, fragmentShader: BULLE_FRAGMENT, transparent: true, depthWrite: false, toneMapped: false,
      uniforms: { uEchelle: { value: 400 } }
    });
    const bulles = new THREE.Points(this.geoBulles, this.matBulles);
    bulles.frustumCulled = false; bulles.renderOrder = 5;
    this.gelule.add(bulles);

    // 4. billes de verre plein autour : placées à la main pour une composition de photo produit
    const faireVerre = (att: string, dist: number, disp: number, rugueux = 0) => {
      const v = bordsSombres(new THREE.MeshPhysicalMaterial({
        color: '#ffffff', metalness: 0, roughness: rugueux, transmission: 1, thickness: 2, ior: 1.52, dispersion: disp,
        attenuationColor: new THREE.Color(att), attenuationDistance: dist, specularIntensity: 1,
        clearcoat: 1, clearcoatRoughness: 0, envMap: this.studio, envMapIntensity: 1.2, transparent: true
      }), 0.5, 2.6);
      this.fondus.push({ m: v, max: 1 });
      return v;
    };
    const verres = {
      clair: faireVerre('#e6f6f3', 9, 0.4),
      champagne: faireVerre('#f2d489', 2.4, 0.25),     // verre doré, comme les lumières du fond
      aqua: faireVerre('#86d8d0', 3, 0.25),
      givre: faireVerre('#e9f6f4', 9, 0, 0.3)          // verre dépoli : contraste mat / brillant
    };
    const geoPerle = new THREE.SphereGeometry(1, 64, 40);
    // x, y, z (repère écran, en unités de gélule), rayon, verre
    const places: [number, number, number, number, keyof typeof verres][] = [
      [-1.95, -1.7, -1.2, 0.5, 'clair'], [1.85, 1.75, 0.6, 0.27, 'champagne'], [1.65, -0.7, 1.2, 0.11, 'clair'],
      [-1.6, 1.45, 0.5, 0.14, 'aqua'], [2.3, 0.45, -0.6, 0.08, 'champagne'], [2.9, -2.4, 4.2, 0.46, 'clair'],
      [-2.9, 2.6, -4, 0.8, 'givre'], [0.6, 2.9, 0.3, 0.07, 'clair'], [-2.6, 0.4, 0.3, 0.065, 'clair'],
      [1.1, -2.55, 1.6, 0.16, 'aqua'], [3.2, 2.1, -2.5, 0.34, 'givre']
    ];
    for (const [x, y, z, r, v] of places) {
      const m = new THREE.Mesh(geoPerle, verres[v]);
      m.scale.setScalar(r); m.renderOrder = 2;
      this.perles.add(m);
      this.perlesInfos.push({ m, base: new THREE.Vector3(x, y, z) });
    }

    // 5. quelques reflets champagne autour de la gélule
    const ne = 12, pe = new Float32Array(ne * 3), te = new Float32Array(ne);
    for (let i = 0; i < ne; i++) {
      pe[i * 3] = (a() * 2 - 1) * 2.4; pe[i * 3 + 1] = (a() * 2 - 1) * 2.9; pe[i * 3 + 2] = 1 + a() * 0.6;
      te[i] = 0.35 + Math.pow(a(), 3) * 1.1; this.eclatsPhases.push(a() * 6.28);
    }
    this.geoEclats.setAttribute('position', new THREE.BufferAttribute(pe, 3));
    this.geoEclats.setAttribute('aAlpha', new THREE.BufferAttribute(new Float32Array(ne), 1));
    this.geoEclats.setAttribute('aTaille', new THREE.BufferAttribute(te, 1));
    this.matEclats = new THREE.ShaderMaterial({
      vertexShader: GRAIN_VERTEX, fragmentShader: ECLAT_FRAGMENT, transparent: true, depthWrite: false, toneMapped: false,
      uniforms: { uEchelle: { value: 400 } }
    });
    const eclats = new THREE.Points(this.geoEclats, this.matEclats);
    eclats.frustumCulled = false; eclats.renderOrder = 6;
    this.gelule.add(eclats);
  }

  private matPoints(couleur: string, eclat: number) {
    return new THREE.ShaderMaterial({
      vertexShader: GRAIN_VERTEX, fragmentShader: GRAIN_FRAGMENT, transparent: true, depthWrite: false, toneMapped: false,
      uniforms: { uCouleur: { value: new THREE.Color(couleur) }, uEclat: { value: eclat }, uEchelle: { value: 400 } }
    });
  }

  private construireGrains() {
    const a = graine(11);
    for (let i = 0; i < 900; i++) {
      const brin = a() < 0.72, y = (a() * 2 - 1) * L, j = Math.floor(a() * NB_BARREAUX);
      const yy = brin ? y : Y_BARREAU(j);
      const r = Math.sqrt(a()) * 0.75, th = a() * 6.2832;
      this.grains.push({
        brin, y, s: a() < 0.5 ? 0 : 1, j, t: a(), ang: a() * 6.2832,
        depart: new THREE.Vector3(Math.cos(th) * r, (a() * 2 - 1) * 2.1, Math.sin(th) * r),
        tourbillon: a() * 2 - 1, retard: 0.42 * (Math.abs(yy) / L) * 0.9 + a() * 0.12
      });
    }
    const n = this.grains.length;
    this.geoGrains.setAttribute('position', new THREE.BufferAttribute(new Float32Array(n * 3), 3));
    this.geoGrains.setAttribute('aAlpha', new THREE.BufferAttribute(new Float32Array(n), 1));
    this.geoGrains.setAttribute('aTaille', new THREE.BufferAttribute(new Float32Array(n).fill(0.09), 1));
    this.matGrains = this.matPoints('#9cc4ff', 1.5);
    const pts = new THREE.Points(this.geoGrains, this.matGrains);
    pts.frustumCulled = false; pts.renderOrder = 1;
    this.scene.add(pts);

    // poussière de lumière qui se détache du haut de l'hélice
    for (let i = 0; i < 260; i++) {
      const y = L - 2.6 + a() * 3.2, s = a() < 0.5 ? 0 : 1;
      this.poussiere.push({ base: this.point(y, s, 0.6 + a() * 0.9), derive: new THREE.Vector3(a() * 2 - 1, 0.5 + a() * 1.3, a() * 2 - 1), ph: a() * 6.2832 });
    }
    const m = this.poussiere.length;
    this.geoPoussiere.setAttribute('position', new THREE.BufferAttribute(new Float32Array(m * 3), 3));
    this.geoPoussiere.setAttribute('aAlpha', new THREE.BufferAttribute(new Float32Array(m), 1));
    const tailles = new Float32Array(m); for (let i = 0; i < m; i++) tailles[i] = 0.04 + a() * 0.07;
    this.geoPoussiere.setAttribute('aTaille', new THREE.BufferAttribute(tailles, 1));
    this.matPoussiere = this.matPoints('#9cc4ff', 1.2);
    const pp = new THREE.Points(this.geoPoussiere, this.matPoussiere);
    pp.frustumCulled = false;
    this.helice.add(pp);

    // particules floues au loin (bokeh)
    const nb = 70, pos = new Float32Array(nb * 3), al = new Float32Array(nb), ta = new Float32Array(nb);
    for (let i = 0; i < nb; i++) {
      pos[i * 3] = (a() * 2 - 1) * 22; pos[i * 3 + 1] = (a() * 2 - 1) * 14; pos[i * 3 + 2] = -6 - a() * 22;
      al[i] = 0.15 + a() * 0.35; ta[i] = 0.4 + a() * 1.1;
    }
    const gb = new THREE.BufferGeometry();
    gb.setAttribute('position', new THREE.BufferAttribute(pos, 3));
    gb.setAttribute('aAlpha', new THREE.BufferAttribute(al, 1));
    gb.setAttribute('aTaille', new THREE.BufferAttribute(ta, 1));
    this.matBokeh = this.matPoints('#5b8df5', 0.2);
    const bk = new THREE.Points(gb, this.matBokeh);
    bk.frustumCulled = false;
    this.scene.add(bk);
  }

  private construireDecor() {
    // fond de studio : dégradé doux + tache de lumière derrière la gélule. Opaque et collé à la caméra,
    // il est vu (déformé) à travers chaque bille de verre : c'est ce qui donne l'aspect « photo ».
    // bokeh : lumières floues rondes (blanc, champagne, aqua), peintes dans le fond
    const g = graine(53), disques: THREE.Vector4[] = [], teintes: THREE.Color[] = [];
    const palette = [new THREE.Color('#ffffff'), new THREE.Color('#f1d9a6'), new THREE.Color('#f6e6c4'), new THREE.Color('#e9c77f'), new THREE.Color('#d2efec'), new THREE.Color('#ffffff')];
    for (let i = 0; i < 40; i++) {
      disques.push(new THREE.Vector4(g() * 1.1 - 0.05, g() * 1.1 - 0.05, 0.01 + Math.pow(g(), 2.4) * 0.085, 0.12 + g() * 0.38));
      teintes.push(palette[Math.floor(g() * palette.length)]);
    }
    this.fondStudio = new THREE.Mesh(new THREE.PlaneGeometry(1, 1), new THREE.ShaderMaterial({
      depthWrite: false, toneMapped: false,
      uniforms: {
        uHaut: { value: new THREE.Color('#e4f1f0') }, uBas: { value: new THREE.Color('#86b5ba') },
        uNuit: { value: NUIT.clone() }, uForce: { value: 1 }, uSpot: { value: new THREE.Vector2(0.68, 0.5) }, uAspect: { value: 1.6 },
        uDecal: { value: new THREE.Vector2() }, uDisques: { value: disques }, uTeintes: { value: teintes }
      },
      vertexShader: `varying vec2 vUv; void main(){ vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0); }`,
      fragmentShader: `uniform vec3 uHaut, uBas, uNuit; uniform float uForce, uAspect; uniform vec2 uSpot, uDecal;
        uniform vec4 uDisques[40]; uniform vec3 uTeintes[40]; varying vec2 vUv;
        void main(){
          vec2 s = (vUv - 0.5) * 1.5 + 0.5;
          vec3 c = mix(uBas, uHaut, smoothstep(-0.15, 0.95, s.y));
          vec2 d = (s - uSpot) * vec2(uAspect, 1.0);
          c = mix(c, vec3(0.97, 0.99, 0.985), 0.38 * exp(-dot(d, d) * 3.2));   // contre-jour doux derrière la gélule
          // lumière dorée qui entre par le coin haut-droit : halo + larges rayons
          vec2 r = (s - vec2(1.15, 1.2)) * vec2(uAspect, 1.0);
          float ang = atan(r.y, r.x);
          float rayons = pow(0.5 + 0.5 * sin(ang * 9.0 + 0.8), 2.5) * (0.6 + 0.4 * sin(ang * 23.0 + 2.0));
          c += vec3(1.0, 0.82, 0.54) * (exp(-dot(r, r) * 1.5) * 0.08 + rayons * exp(-length(r) * 1.4) * 0.08);
          for (int i = 0; i < 40; i++) {
            vec4 k = uDisques[i];
            vec2 q = (s - k.xy - uDecal * (0.4 + k.z * 12.0)) * vec2(uAspect, 1.0);   // les gros disques bougent plus (profondeur)
            float e = length(q) / k.z;
            float disque = smoothstep(1.0, 0.8, e) * 0.65 + smoothstep(1.0, 0.92, e) * smoothstep(0.76, 0.92, e) * 0.35;
            c = mix(c, uTeintes[i], disque * k.w);
          }
          // bords un peu plus profonds (vignette) et tramage fin : pas de bandes dans les dégradés
          c *= mix(vec3(1.0), vec3(0.86, 0.94, 0.97), smoothstep(0.35, 1.2, length((s - 0.5) * vec2(uAspect * 0.8, 1.0))) * 0.7);
          c += (fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) * (1.5 / 255.0);
          gl_FragColor = vec4(mix(uNuit, min(c, vec3(1.0)), uForce), 1.0);
        }`
    }));
    this.fondStudio.renderOrder = -10;
    this.fondStudio.frustumCulled = false;
    this.fondStudio.position.z = -80;
    this.camera.add(this.fondStudio);

    // halo de lumière derrière l'hélice (fond nuit)
    this.halo = new THREE.Mesh(new THREE.PlaneGeometry(90, 60), new THREE.ShaderMaterial({
      transparent: true, depthWrite: false, toneMapped: false,
      uniforms: { uCouleur: { value: new THREE.Color('#1d4fbf') }, uForce: { value: 0 } },
      vertexShader: `varying vec2 vUv; void main(){ vUv = uv; gl_Position = projectionMatrix * modelViewMatrix * vec4(position,1.0); }`,
      fragmentShader: `uniform vec3 uCouleur; uniform float uForce; varying vec2 vUv;
        void main(){ vec2 d = (vUv - 0.5) * vec2(1.5, 1.0); float a = exp(-dot(d, d) * 9.0); gl_FragColor = vec4(uCouleur, a * uForce); }`
    }));
    this.halo.position.z = -30;
    this.scene.add(this.halo);

  }

  // ================================================================ une image
  private dessiner() {
    const r = this.renderer;
    if (!r) return;
    this.sale = false;
    const p = this.reduit ? 1 : this.courant;
    const hue = this.teinte();
    const large = window.innerWidth >= 1024;

    // ---- caméra (plans interpolés)
    let i = 0;
    while (i < PLANS.length - 2 && p > PLANS[i + 1].p) i++;
    const A = PLANS[Math.max(0, i - 1)], B = PLANS[i], C = PLANS[i + 1], D = PLANS[Math.min(PLANS.length - 1, i + 2)];
    const t = borne((p - B.p) / (C.p - B.p));
    const v = (k: keyof Plan) => cr(A[k], B[k], C[k], D[k], t);
    const camZ = v('camZ'), f = Math.min(this.h * 0.95, this.w * 1.35);
    this.camera.fov = THREE.MathUtils.radToDeg(2 * Math.atan(this.h / 2 / f));
    this.camera.aspect = this.w / this.h;
    this.camera.position.set(0, 0, camZ);
    this.camera.lookAt(0, 0, 0);
    const dx = large ? v('dx') : 0, cy = v('cy') - (large ? 0 : 0.08);
    this.camera.setViewOffset(this.w, this.h, -dx * this.w, -(cy - 0.5) * this.h, this.w, this.h);
    this.camera.updateProjectionMatrix();
    // le fond de studio couvre toute la vue (marge pour le décalage de cadrage)
    const hFond = 2 * 80 * Math.tan(THREE.MathUtils.degToRad(this.camera.fov) / 2) * 1.5;
    this.fondStudio.scale.set(hFond * this.camera.aspect, hFond, 1);

    this.pivot.rotation.set(v('ax'), 0, v('az'));
    this.helice.position.y = -v('camY');
    this.helice.rotation.y = -p * 7.2;
    this.scene.updateMatrixWorld(true);

    // ---- ambiance : clair (la gélule) puis nuit jusqu'à la fin
    const nuit = douce((p - 0.11) / 0.09) * (1 - douce((p - 0.89) / 0.05)); // passage rapide : pas de gris
    const retour = douce((p - 0.86) / 0.13);                                 // fin : l'hélice rentre dans la gélule
    const defait = douce(retour * 1.25);
    const fond = CLAIR.clone().lerp(NUIT, nuit);
    (this.scene.background as THREE.Color).copy(fond);
    const fog = this.scene.fog as THREE.Fog;
    fog.color.copy(fond); fog.near = camZ + mix(6, 1.5, nuit); fog.far = camZ + mix(40, 16, nuit);
    this.lueur.setHSL(hue / 360, 0.95, 0.6);
    this.lumRimA.color.setHSL(hue / 360, 0.9, 0.6);
    this.lumRimA.intensity = mix(0.5, 2.2, nuit); this.lumRimB.intensity = mix(0.3, 1.2, nuit);
    const halo = this.halo.material as THREE.ShaderMaterial;
    // halo : lumière de studio blanche sur fond clair, lueur bleue profonde la nuit
    halo.uniforms['uCouleur'].value.set('#ffffff').lerp(new THREE.Color().setHSL(hue / 360, 0.8, 0.2), nuit);
    halo.uniforms['uForce'].value = 0.55 * nuit;
    // verre : blanc laiteux sur fond clair, bleu nuit profond sur fond sombre
    this.matBrin.envMapIntensity = mix(1.5, 0.75, nuit);
    this.matBrin.attenuationDistance = mix(2.2, 0.9, nuit);
    this.matBrin.color.set('#eef5ff').lerp(new THREE.Color('#bcd4ff'), nuit);
    this.bloom.strength = mix(0.22, 0.95, nuit);                         // fond clair : presque pas de lueur, rendu net
    this.bloom.threshold = mix(0.97, 0.72, nuit);
    this.matAdnMini.sheenColor.setHSL(hue / 360, 0.55, 0.8, THREE.SRGBColorSpace);
    // flou de profondeur : fort en gros plan, nul en plan large
    const bk = this.bokeh.uniforms as Record<string, THREE.IUniform>;
    bk['focus'].value = camZ;
    bk['aperture'].value = 0.0018 * borne((9 - camZ) / 5) * (this.w / 1000);
    bk['maxblur'].value = 0.0045;
    this.cle.intensity = mix(1.7, 1.1, nuit);
    this.ambiance.intensity = mix(0.06, 0.35, nuit);                    // fond clair : de vraies ombres, du relief
    this.douceur.intensity = mix(0.45, 0, nuit);

    // ---- assemblage
    const forme = borne((p - 0.07) / 0.33);
    const assemble = (y: number) => douce((forme - 0.42 * Math.abs(y) / L - 0.25) / 0.3);
    const haut = (y: number) => 1 - douce((y - (L - 1.6)) / 1.6);       // le haut se dissout en poussière
    for (const m of this.brins) {
      const idx = m.geometry.index!.count, seg = 28 * 6;
      // un brin pousse depuis le centre : part visible = jusqu'où l'assemblage est arrivé
      let k = 0; while (k < 1 && assemble(k * L) > 0.5) k += 0.01;
      const part = Math.min(forme >= 1 ? 1 : borne(k), 1 - defait);
      const n = Math.floor((idx / seg) * part) * seg;
      m.geometry.setDrawRange(0, n);
      m.visible = n > 0;
    }
    const mat = new THREE.Matrix4(), sc = new THREE.Vector3();
    this.barreauxInfos.forEach((b, k) => {
      const s = assemble(b.y) * haut(b.y) * (1 - defait);
      sc.set(s, b.long * (s > 0.001 ? 1 : 0), s);
      this.barreaux.setMatrixAt(k, mat.compose(b.pos, b.quat, sc));
    });
    this.barreaux.instanceMatrix.needsUpdate = true;

    const vague = mix(-L, L, borne((p - 0.28) / 0.6));
    const cAnneau = new THREE.Color(), sombreBleu = new THREE.Color().setHSL(hue / 360, 0.8, 0.42);
    this.anneauxInfos.forEach((a, k) => {
      const s = assemble(a.y) * haut(a.y) * (1 - defait);
      this.anneaux.setMatrixAt(k, mat.compose(a.pos, a.quat, sc.set(s, s, s)));
      const I = 0.3 + 1.7 * Math.exp(-((a.y - vague) ** 2) / (2 * 1.3 * 1.3));
      cAnneau.copy(sombreBleu).lerp(this.lueur.clone().multiplyScalar(I), nuit);
      this.anneaux.setColorAt(k, cAnneau);
    });
    this.anneaux.instanceMatrix.needsUpdate = true;
    if (this.anneaux.instanceColor) this.anneaux.instanceColor.needsUpdate = true;

    // ---- la gélule de verre : la caméra plonge dedans au début, en ressort à la fin
    let plonge: number, adn: number;
    if (p < 0.5) {
      plonge = douce((p - 0.055) / 0.17);
      this.gelule.visible = plonge < 0.999;
      this.gelule.scale.setScalar((large ? 1.8 : 1.5) * (1 + plonge * plonge * 5));
      this.gelule.rotation.set(0.12, -0.25 + p * 2.4, -0.62);
      this.gelule.position.set(0, large ? -0.05 : -1.3, plonge * 6);
      adn = 1 - douce((p - 0.08) / 0.1);                      // le petit ADN se défait en grains
    } else {
      plonge = 1 - retour;
      this.gelule.visible = retour > 0.001;
      this.gelule.scale.setScalar((large ? 1.6 : 1.15) * (1 + plonge * plonge * 5));
      this.gelule.rotation.set(0.12, 0.25 - (1 - p) * 2.4, 0.6);
      this.gelule.position.set(large ? -1.8 : 0, large ? -0.1 : 2.4, plonge * 6);   // grand écran : à gauche du texte
      adn = douce((retour - 0.55) / 0.4);
    }
    const taille = 1 - plonge;
    this.adnMini.visible = adn > 0.01;
    this.adnMini.scale.setScalar(Math.max(0.001, adn));
    this.adnMini.rotation.set(0, p * 4, 0.08);
    const visible = (1 - nuit) * taille;
    const ab = this.geoBulles.getAttribute('aAlpha') as THREE.BufferAttribute;
    for (let i = 0; i < ab.count; i++) ab.setX(i, visible);
    ab.needsUpdate = true;
    const ea = this.geoEclats.getAttribute('aAlpha') as THREE.BufferAttribute;
    this.eclatsPhases.forEach((ph, k) => ea.setX(k, visible * (0.3 + 0.7 * Math.abs(Math.sin(p * 14 + ph)))));
    ea.needsUpdate = true;
    // le verre s'efface quand la nuit tombe (et revient avec le jour)
    const jour = 1 - nuit;
    for (const f of this.fondus) {
      const sm = f.m as THREE.ShaderMaterial;
      if (sm.uniforms?.['opacity']) sm.uniforms['opacity'].value = f.max * jour; else f.m.opacity = f.max * jour;
    }
    if (jour < 0.01) this.gelule.visible = false;
    this.gelule.updateMatrixWorld(true);
    // billes de verre : même place et même échelle que la gélule ; elles s'écartent quand la caméra plonge
    this.perles.visible = this.gelule.visible && visible > 0.01;
    this.perles.position.copy(this.gelule.position);
    this.perles.scale.copy(this.gelule.scale);
    const miroir = p >= 0.5 ? -1 : 1;
    for (const b of this.perlesInfos) {
      const ecart = 1 + plonge * 2.2;
      b.m.position.set(b.base.x * ecart * miroir, b.base.y * ecart, b.base.z + plonge * 2);
    }

    // profondeur de champ « photo » : net sur la gélule, flou devant et derrière
    const dof = (1 - nuit) * Math.min(1, taille * 1.5);
    if (dof > 0.01) {
      bk['focus'].value = camZ - this.gelule.position.z;
      bk['aperture'].value = Math.max(bk['aperture'].value as number, 0.0027 * dof);
      bk['maxblur'].value = mix(0.0045, 0.014, dof);
    }
    const fs = (this.fondStudio.material as THREE.ShaderMaterial).uniforms;
    fs['uForce'].value = 1 - nuit;
    fs['uAspect'].value = this.w / this.h;
    fs['uDecal'].value.set(p * 0.06, p * 0.02);
    if (p < 0.5) fs['uSpot'].value.set(large ? 0.68 : 0.5, large ? 0.5 : 0.42);
    else fs['uSpot'].value.set(large ? 0.32 : 0.5, large ? 0.5 : 0.66);

    // ---- les grains : de la gélule vers leur place dans l'hélice
    const pos = this.geoGrains.getAttribute('position') as THREE.BufferAttribute;
    const al = this.geoGrains.getAttribute('aAlpha') as THREE.BufferAttribute;
    const depart = new THREE.Vector3(), arrivee = new THREE.Vector3(), perp = new THREE.Vector3(), tmp = new THREE.Vector3();
    this.grains.forEach((g, k) => {
      const e = douce((forme - g.retard) / 0.38);
      depart.copy(g.depart).applyMatrix4(this.gelule.matrixWorld);
      if (g.brin) arrivee.copy(this.point(g.y, g.s)).add(tmp.set(Math.cos(g.ang) * RT * 0.5, 0, Math.sin(g.ang) * RT * 0.5));
      else arrivee.copy(this.point(Y_BARREAU(g.j), 0)).lerp(this.point(Y_BARREAU(g.j), 1), g.t);
      arrivee.applyMatrix4(this.helice.matrixWorld);
      if (retour > 0) {
        // fin : le grain quitte l'hélice et rentre dans la gélule
        const r2 = douce((retour - g.retard * 0.55) / 0.45);
        perp.subVectors(depart, arrivee).cross(tmp.set(0, 0, 1)).normalize().multiplyScalar(Math.sin(Math.PI * r2) * g.tourbillon * 1.5);
        pos.setXYZ(k, mix(arrivee.x, depart.x, r2) + perp.x, mix(arrivee.y, depart.y, r2) + perp.y, mix(arrivee.z, depart.z, r2) + perp.z);
        al.setX(k, r2 > 0 ? 1 - douce((r2 - 0.6) / 0.3) : 0);
        return;
      }
      perp.subVectors(arrivee, depart).cross(tmp.set(0, 0, 1)).normalize().multiplyScalar(Math.sin(Math.PI * e) * g.tourbillon * 1.5);
      pos.setXYZ(k, mix(depart.x, arrivee.x, e) + perp.x, mix(depart.y, arrivee.y, e) + perp.y, mix(depart.z, arrivee.z, e) + perp.z);
      al.setX(k, (1 - douce((e - 0.8) / 0.2)) * douce(e / 0.12));
    });
    pos.needsUpdate = true; al.needsUpdate = true;
    const echelle = (this.h * r.getPixelRatio()) / (2 * Math.tan(THREE.MathUtils.degToRad(this.camera.fov) / 2));
    const couleurGrain = new THREE.Color('#d9ad5c').lerp(new THREE.Color().setHSL(hue / 360, 1, 0.78), nuit);
    for (const m of [this.matGrains, this.matPoussiere]) { m.uniforms['uEchelle'].value = echelle; m.uniforms['uCouleur'].value.copy(couleurGrain); }
    this.matBokeh.uniforms['uEchelle'].value = echelle;
    this.matEclats.uniforms['uEchelle'].value = echelle * this.gelule.scale.x;
    this.matBulles.uniforms['uEchelle'].value = echelle * this.gelule.scale.x;
    this.matBokeh.uniforms['uCouleur'].value.setHSL(hue / 360, 0.8, mix(0.75, 0.55, nuit));

    // ---- poussière du haut de l'hélice
    const pp = this.geoPoussiere.getAttribute('position') as THREE.BufferAttribute;
    const pa = this.geoPoussiere.getAttribute('aAlpha') as THREE.BufferAttribute;
    const d = 0.2 + p * 1.6, vis = assemble(L - 1) * (1 - defait);
    this.poussiere.forEach((g, k) => {
      pp.setXYZ(k, g.base.x + g.derive.x * d, g.base.y + g.derive.y * d, g.base.z + g.derive.z * d);
      pa.setX(k, vis * (0.35 + 0.65 * Math.abs(Math.sin(p * 16 + g.ph))));
    });
    pp.needsUpdate = true; pa.needsUpdate = true;

    this.composer.render();
  }
}
