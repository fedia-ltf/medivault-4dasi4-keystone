# rendezvous-service

Microservice **Rendez-vous** de MediVault — responsable : **Yazid**.

| | |
|---|---|
| Port | `8087` |
| Base | H2 (`rendezvous_db`, console sur `/h2-console`) |
| Nom Eureka | `RENDEZVOUS-SERVICE` — route Gateway `/api/creneaux/**` et `/api/rendezvous/**` |
| Swagger | `http://localhost:8087/swagger-ui.html` |

## Modèle
- `Creneau` (id, professionnelId, typeProfessionnel, dateDebut, dateFin) — réservé par 0..1 `RendezVous`
- `RendezVous` (id, patientId, motif, statut)
- Énumérations : `TypeProfessionnel` (MEDECIN, LABORATOIRE), `StatutRdv` (CONFIRME, ANNULE, TERMINE)
- Règle de gestion : **RG8** (créneaux générés selon les jours et heures de travail ; libre tant qu'aucun RDV CONFIRME ne lui est associé)

## Interactions
| Type | Détail |
|---|---|
| Appel synchrone (OpenFeign) | `user-service` : `GET /api/professionnels/{id}/disponibilites` — fallback Resilience4j : créneaux non générés (503) |
| Publie | `RendezVousConfirme` (clé `rendezvous.confirme`) → user (SSE), analyse (affectation labo) |
| Consomme | `ConsultationAjoutee` (file `rendezvous.consultation-ajoutee`) → clôture du RDV (statut TERMINE) |

## API prévue
| Méthode | Endpoint | Codes |
|---|---|---|
| POST | `/api/creneaux/generer?professionnelId=` | 201, 404, 503 |
| GET | `/api/creneaux?professionnelId=&date=` | 200 |
| POST | `/api/rendezvous` | 201, 409 |
| PUT / PATCH | `/api/rendezvous/{id}/annuler` | 200, 404 |

## Lancement
```bash
cd Backend/rendezvous-service
./mvnw spring-boot:run      # Windows : mvnw.cmd spring-boot:run
```
Le service démarre seul ; Eureka (8761), le Config Server (8888) et RabbitMQ (5672) sont utilisés dès qu'ils sont lancés.

> Validation 1 : projet créé, configuré et démarrable. La logique métier (contrôleurs, services, listeners) est développée à partir de la séance 5.

