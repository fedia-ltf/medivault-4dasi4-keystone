# medicament-service — médicaments sensibles

Microservice de **Fedia Letaief** (MediVault, équipe Keystone). Il contrôle les médicaments **sensibles**
(Subutex, Méthadone, Ritaline…) vendus en pharmacie : quota mensuel par pharmacie, vente justifiée par une
ordonnance, sanction au 2e avertissement. Ce n'est pas un catalogue général (ni prix, ni stock, ni forme).

Port **8084** · base **H2 fichier** `medicament_db` · Swagger : <http://localhost:8084/swagger-ui.html>

## Lancer

```bash
mvn spring-boot:run          # ou ▶ sur MedicamentServiceApplication dans IntelliJ
mvn test                     # 44 tests
```

- La base est le fichier `data/medicament_db.mv.db` (créé au 1er lancement, ignoré par Git).
  Les données restent après l'arrêt. Pour repartir de zéro : arrêter le service et supprimer le dossier `data/`.
- Console H2 : <http://localhost:8084/h2-console> → JDBC URL `jdbc:h2:file:./data/medicament_db`, utilisateur `sa`, mot de passe vide.
- Au premier lancement, des données de démonstration sont créées (7 médicaments, quotas du mois, quelques ventes).
  Désactivation : `medivault.demo-data=false`.

## Modèle (documentation finale v2)

| Entité | Champs |
|---|---|
| `MedicamentSensible` | id, nomCommercial (unique), dci, categorie (`STUPEFIANT` / `PSYCHOTROPE` / `ABORTIF`), description\*, precautions\* |
| `Attribution` (quota) | id, pharmacieId (String), medicament, mois (`YearMonth`), quantiteAttribuee, quantiteVendue, avertissements, statut (`ACTIVE` / `SUSPENDUE`) · unique par (pharmacie, médicament, mois) |
| `VenteSensible` | id, patientId (String), ordonnanceId, ordonnanceCode\*, quantite, date, attribution |

\* attributs ajoutés pour l'affichage. `reste = quantiteAttribuee − quantiteVendue`. Une vente n'est **jamais** modifiée ni supprimée.

## API (toutes sous `/api`)

| Méthode | Route | Codes | Qui |
|---|---|---|---|
| CRUD | `/medicaments-sensibles` (`?categorie=&q=`) | 200 · 201 · 204 · 400 · 404 · 409 | admin |
| CRUD | `/attributions` (`?pharmacieId=&medicamentId=&mois=&statut=`) | 200 · 201 · 204 · 400 · 404 · 409 | admin |
| POST | `/attributions/lot` | 201 · 400 · 404 | admin |
| GET | `/attributions/{id}/bilan` | 200 · 404 | admin |
| POST | `/attributions/renouvellement?mois=AAAA-MM` | 200 | admin (RG4 à la main) |
| POST | `/ventes` | 201 · 400 · 403 · 404 · 409 · 503 | pharmacien, soin-domicile |
| GET | `/ventes` (`?pharmacieId=&medicamentId=&mois=`) · `/ventes/{id}` | 200 · 404 | admin |
| GET | `/disponibilites?medicamentId=` | 200 · 404 | médecin, patient |

Erreurs au format *Problem Details* (`title`, `status`, `detail`). Un refus de vente ajoute `avertissements` et `suspendue`.

## Règles métier

`POST /ventes` contrôle, dans l'ordre :

1. médicament inconnu → **404**
2. pharmacie inactive (user-service) → **403** · user-service ou ordonnance-service en panne → **503** (pas de vérification, pas de vente)
3. aucun quota ce mois-ci, ou quota `SUSPENDUE` → **403**
4. **RG1** ordonnance `VALIDEE` (ou `PARTIELLEMENT_DELIVREE`) qui contient ce médicament, sans dépasser la quantité prescrite → sinon **409** + 1 avertissement
5. **RG2** quota du mois suffisant → sinon **409** + 1 avertissement
6. **RG3** au 2e avertissement, le quota passe `SUSPENDUE` (événement `SanctionPharmacie`)
7. succès → **201**, quota mis à jour (verrou en base : pas de dépassement si deux ventes arrivent ensemble), événement `VenteSensibleEnregistree`

**RG4** : le 1er de chaque mois à 00h05 (`medivault.quota.renouvellement-cron`), `RenouvellementMensuel` garde le bilan
du mois précédent et crée les nouveaux quotas (même quantité, `ACTIVE`, 0 vente, 0 avertissement).
Remettre un quota `ACTIVE` avec `PUT /attributions/{id}` lève la sanction (avertissements remis à 0).

## Communications

| | Détail |
|---|---|
| Synchrone | Feign → `ordonnance-service` `GET /api/ordonnances/{code}` · Feign → `user-service` `GET /api/utilisateurs/{id}` (champ `actif`) |
| Asynchrone | publie `VenteSensibleEnregistree` (`vente.sensible.enregistree`) et `SanctionPharmacie` (`pharmacie.sanction`) sur l'exchange `medivault.events` |
| Panne | Resilience4j + fallback → 503 ; RabbitMQ arrêté → la vente reste valide, l'échec est seulement journalisé |

`medivault.integration.mode` dans `application.properties` :
- `simule` (par défaut) : ordonnances et comptes fictifs, pour travailler seul (`ORD-A7F3`, `ORD-B219` valides ; `ORD-C884` seulement les médicaments 1 et 3 ; `ORD-D102` en attente ; `ORD-E410` délivrée ; pharmacie `off-…` inactive).
- `reel` : vrais appels Feign (Eureka, ordonnance-service et user-service lancés).

À aligner avec les autres membres : le chemin `/api/ordonnances/{code}` et ses champs (`id`, `code`, `patientId`, `statut`, `lignes[medicamentId, quantite]`) avec Amine, le chemin `/api/utilisateurs/{id}` et le champ `actif` avec l'équipe user-service.

## Docker

Le service se lance dans un conteneur : aucun Java ni Maven n'est nécessaire sur le PC.

```bash
# Seul, avec un RabbitMQ (docker-compose.yml de ce dossier)
docker compose up -d --build        # puis http://localhost:8084/swagger-ui.html
docker compose down                 # arrêter (les données restent dans le volume)
docker compose down -v              # tout effacer, données comprises

# Ou l'image seule
docker build -t medicament-service .
docker run -p 8084:8084 -v medicament-data:/app/data medicament-service
```

Variables d'environnement (valeurs par défaut entre parenthèses) :

| Variable | Rôle |
|---|---|
| `DATA_DIR` (`/app/data` dans Docker) | Dossier de la base H2 : à mettre dans un **volume**, sinon les données sont perdues |
| `INTEGRATION_MODE` (`simule`) | `reel` = vrais appels vers ordonnance-service et user-service |
| `DEMO_DATA` (`true`) | Crée des données d'exemple au premier lancement (base vide) |
| `EUREKA_URL` (`http://localhost:8761/eureka/`) | Adresse d'Eureka. `EUREKA_CLIENT_ENABLED=false` pour s'en passer |
| `RABBITMQ_HOST` / `RABBITMQ_PORT` (`localhost` / `5672`) | Dans un conteneur, `localhost` n'est plus le PC : donner le nom du service RabbitMQ |
| `RABBITMQ_USER` / `RABBITMQ_PASSWORD` (`guest`) | Identifiants RabbitMQ |
| `CONFIG_SERVER_URL` | Adresse du Config Server (`SPRING_CLOUD_CONFIG_ENABLED=false` pour s'en passer) |

Dans le `docker-compose` commun de l'équipe, ce service s'inscrit dans Eureka sous le nom `medicament-service` (port 8084).
Si les services tournent sur des PC différents, ajouter `EUREKA_INSTANCE_IPADDRESS=<adresse IP du PC>` : sinon Eureka
publie l'adresse interne du conteneur, que les autres PC ne peuvent pas joindre.

## Où est le code

```
src/main/java/tn/esprit/medivault/medicament/
├─ entities/      MedicamentSensible, Attribution, VenteSensible, Categorie, StatutAttribution, YearMonthConverter
├─ repositories/  accès à la base (Spring Data JPA)
├─ dto/           objets échangés avec le client (Requete / Reponse)
├─ services/      MedicamentSensibleService, AttributionService, VenteService (le cœur : RG1 à RG3)
├─ controllers/   MedicamentSensibleController, AttributionController, VenteController, DisponibiliteController
├─ clients/       OpenFeign vers ordonnance-service et user-service + plans B (fallback)
├─ verification/  VerificationExterne : version réelle (Feign) ou simulée
├─ messaging/     EvenementsPublisher (RabbitMQ)  ·  events/ : les 2 événements
├─ scheduling/    RenouvellementMensuel (RG4)     ·  demo/ : données de démonstration
├─ exceptions/    erreurs → 404, 409, 503 (GlobalExceptionHandler)
└─ config/        Swagger, RabbitMQ, horloge
src/test/java/…   tests de l'API, des règles et des pannes
postman/          collection Postman
```
