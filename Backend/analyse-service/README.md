# analyse-service

Microservice **Analyses** de MediVault — responsable : **Taha**.

| | |
|---|---|
| Port | `8086` |
| Base | H2 (`analyse_db`, console sur `/h2-console`) |
| Nom Eureka | `ANALYSE-SERVICE` — route Gateway `/api/analyses/**` |
| Swagger | `http://localhost:8086/swagger-ui.html` |

## Modèle
- `Analyse` (id, patientId, medecinId, laboratoireId, typeAnalyse, statut) — compose 1..* `ResultatAnalyse`
- `ResultatAnalyse` (id, parametre, valeur, unite, valeurReference, interpretation)
- Énumérations : `StatutAnalyse` (EN_ATTENTE, EN_COURS, TERMINEE), `Interpretation` (NORMAL, BAS, ELEVE)

## Interactions
| Type | Détail |
|---|---|
| Appel synchrone (OpenFeign) | `dossier-service` : `GET /api/dossiers/patient/{patientId}/acces?professionnelId=` — fallback Resilience4j : saisie refusée (503) |
| Publie | `ResultatDisponible` (clé `resultat.disponible`) → dossier, user |
| Consomme | `ConsultationAjoutee` (file `analyse.consultation-ajoutee`) → analyse EN_ATTENTE · `RendezVousConfirme` (file `analyse.rendezvous-confirme`) → EN_COURS + laboratoireId |

## API prévue
| Méthode | Endpoint | Codes |
|---|---|---|
| GET | `/api/analyses?statut=EN_COURS&laboratoireId=` | 200 |
| POST | `/api/analyses/{id}/resultats` | 201, 400, 403, 503 |
| GET | `/api/analyses/patient/{patientId}` | 200 |
| GET | `/api/analyses/{id}` | 200, 404 |

## Lancement
```bash
cd Backend/analyse-service
./mvnw spring-boot:run      # Windows : mvnw.cmd spring-boot:run
```
Le service démarre seul ; Eureka (8761), le Config Server (8888) et RabbitMQ (5672) sont utilisés dès qu'ils sont lancés.

> Validation 1 : projet créé, configuré et démarrable. La logique métier (contrôleurs, services, listeners) est développée à partir de la séance 5.
