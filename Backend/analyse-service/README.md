# analyse-service

Microservice **Analyses** de MediVault — responsable : **Taha**.

| | |
|---|---|
| Port | `8086` |
| Base | H2 (`analyse_db`, console sur `/h2-console`) |
| Nom Eureka | `ANALYSE-SERVICE` — route Gateway `/api/analyses/**` |
| Swagger | `http://localhost:8086/swagger-ui.html` |

## Modèle
- `Analyse` (id, consultationId, patientId, medecinId, laboratoireId, typeAnalyse, statut, dateDemande, dateResultat) — compose 0..* `ResultatAnalyse` (aucun résultat tant qu'elle n'est pas TERMINEE)
- `ResultatAnalyse` (id, parametre, valeur, unite, valeurReference, interpretation)
- Énumérations : `StatutAnalyse` (EN_ATTENTE, EN_COURS, TERMINEE), `Interpretation` (NORMAL, BAS, ELEVE)
- patientId, medecinId, laboratoireId : id **String** de user-service

## Cycle de vie
1. `ConsultationAjoutee` → une analyse **EN_ATTENTE** par analyse demandée
2. `RendezVousConfirme` (typeProfessionnel = LABORANTIN, analyseId) → **EN_COURS** + laboratoireId
3. `RendezVousAnnule` → repasse **EN_ATTENTE**, laboratoireId vidé
4. Saisie du résultat → **TERMINEE**, interprétation calculée (1,30 g/L pour 0,70–1,10 → ELEVE), publication de `ResultatDisponible`

## Interactions
| Type | Détail |
|---|---|
| Synchrone · appelle | `dossier-service` : `GET /api/dossiers/patient/{patientId}/acces?professionnelId=` → justification d'accès du laboratoire toujours ACTIVE ? Fallback Resilience4j : saisie refusée (503) |
| Synchrone · appelé par | `rendezvous-service` : `GET /api/analyses/{id}` (prescription valide avant réservation au laboratoire) |
| Publie | `ResultatDisponible` { analyseId, patientId, medecinId, laboratoireId, typeAnalyse, date } (clé `resultat.disponible`) → dossier, notification |
| Consomme | `ConsultationAjoutee` (file `analyse.consultation-ajoutee`) · `RendezVousConfirme` (file `analyse.rendezvous-confirme`) · `RendezVousAnnule` (file `analyse.rendezvous-annule`) |

## API
| Méthode | Endpoint | Codes |
|---|---|---|
| GET | `/api/analyses?statut=EN_COURS&laboratoireId=` | 200 · 400 |
| POST | `/api/analyses/{id}/resultats` | 201 · 400 · 403 · 404 · 409 · 503 |
| GET | `/api/analyses/patient/{patientId}` | 200 |
| GET | `/api/analyses/{id}` | 200 · 404 |

Règles de la saisie, dans l'ordre : **RG7** analyse EN_COURS (sinon 409) → **RG10** laboratoire affecté (sinon 403) → justification d'accès ACTIVE auprès de dossier-service (retirée ou expirée : 403 ; dossier indisponible : 503, fail-closed).

Exemple de saisie :
```json
{"laboratoireId":"labo-biolab","resultats":[{"parametre":"Glycémie","valeur":1.30,"unite":"g/L","referenceMin":0.70,"referenceMax":1.10}]}
```

## Données de démonstration
Avec les id de user-service (`medivault.seed.enabled=true`) : analyse 1 « Bilan glycémique » EN_COURS chez `labo-biolab`, analyse 2 « NFS » EN_ATTENTE, analyse 3 « Bilan lipidique » TERMINEE — patient `patient-ahmed`, médecin `medecin-karim`.

## Lancement
```bash
cd Backend/analyse-service
./mvnw spring-boot:run      # Windows : mvnw.cmd spring-boot:run
```
Le service démarre seul ; Eureka (8761), le Config Server (8888), RabbitMQ (5672) et dossier-service sont utilisés dès qu'ils sont lancés. Sans dossier-service, la saisie d'un résultat répond 503 (comportement attendu).
