# medivault-frontend

Front Angular 20 de MediVault (équipe Keystone), avec Tailwind CSS 4.

```bash
npm install
npm start        # http://localhost:4200
```

Pour entrer dans l'espace connecté : **Se connecter** → **Continuer avec Keycloak (SSO)** (compte démo, la vraie connexion Keycloak viendra plus tard).

## Organisation : un dossier par microservice

Chaque membre travaille **uniquement dans le dossier de son module**, avec les mêmes sous-dossiers et les mêmes noms. Ainsi, au moment de réunir le travail, chaque fichier arrive à sa place et il n'y a pas de conflit Git.

```
src/app/
├── core/          COMMUN  auth.service, auth.guard, modules.ts (liste des modules)
├── shared/        COMMUN  icônes, logo, animations, module-banner (bandeau)
├── layout/        COMMUN  shell + sidebar/ (menu latéral) + topbar/ (en-tête)
├── app.routes.ts  routes principales (ne pas modifier)
└── features/
    ├── landing/ auth/ dashboard/      pages communes
    ├── medicaments/   Fedia   ← module complet, sert d'exemple
    ├── dossiers/      Eya
    ├── ordonnances/   Amine
    ├── pharmacie/     Aziz
    ├── analyses/      Taha
    └── rendez-vous/   Yazid
```

### Dans le dossier d'un module

```
features/<module>/
├── pages/                  les écrans (un composant par page)
├── services/               les appels HTTP vers le microservice
├── models/                 les types des données (interfaces)
└── <module>.routes.ts      les routes du module
```

Les modules `dossiers`, `ordonnances`, `pharmacie`, `analyses` et `rendez-vous` contiennent pour l'instant une page d'attente (le bandeau animé). Chacun la remplace par ses vrais écrans.

## Règles de nommage

| Quoi | Convention | Exemple |
|---|---|---|
| Dossier du module | nom au pluriel, en minuscules | `ordonnances` |
| Page | `<module>-page.ts` / `.html` | `ordonnances-page.ts` |
| Service | `<entité>.service.ts` | `ordonnance.service.ts` |
| Modèle | `<entité>.model.ts` | `ordonnance.model.ts` |
| Routes | `<module>.routes.ts`, constante `<MODULE>_ROUTES` | `ORDONNANCES_ROUTES` |
| URL de l'API | `/api/<module>` | `/api/ordonnances` |

## Ajouter une page à son module

1. Créer le composant dans `features/<module>/pages/` (voir `medicaments-page.ts` comme modèle).
2. L'ajouter dans `<module>.routes.ts` :

```ts
export const ORDONNANCES_ROUTES: Routes = [
  { path: '', data: { title: 'Ordonnances' }, loadComponent: () => import('./pages/ordonnances-page').then(m => m.OrdonnancesPage) },
  { path: 'nouvelle', data: { title: 'Nouvelle ordonnance' }, loadComponent: () => import('./pages/ordonnance-form-page').then(m => m.OrdonnanceFormPage) }
];
```

Le titre affiché dans l'en-tête vient de `data.title`. Le menu latéral et `app.routes.ts` n'ont pas à être modifiés.

## Appeler son microservice

`proxy.conf.json` redirige `/api/**` vers `http://localhost:8084` (medicament-service). Quand l'API Gateway sera prête, remplacer la cible par `http://localhost:8080`.

Dans `services/`, utiliser `HttpClient` avec une URL relative :

```ts
private http = inject(HttpClient);
liste() { return this.http.get<Ordonnance[]>('/api/ordonnances'); }
```

## Design commun

- Les couleurs, les boutons (`btn`), les cartes (`card`), les champs (`field`) et les animations sont dans `src/styles.css` : les utiliser tels quels pour garder le même style partout.
- Le bandeau d'un module : `<mv-module-banner cle="pharmacie" />` (titre, icône et description dans `core/modules.ts`).
- Les icônes : `<mv-icon name="pill" />` (liste dans `shared/icon.ts`).

## Pages

| Route | Page |
|---|---|
| `/` | Accueil animé |
| `/login` | Connexion avec choix du rôle |
| `/app/dashboard` | Tableau de bord |
| `/app/medicaments` | Catalogue : recherche, ajout, modification, suppression |
| `/app/dossiers`, `/app/ordonnances`, `/app/pharmacie`, `/app/analyses`, `/app/rendez-vous` | Modules en cours de développement |
