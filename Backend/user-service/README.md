# user-service

Microservice **commun** de MediVault : comptes, rôles Keycloak, profils et horaires des professionnels, signature du médecin.

- Port : **8081** · Base : **MongoDB** `user_db` (collection `utilisateurs`) · Package : `tn.esprit.medivault.user`
- Swagger : http://localhost:8081/swagger-ui.html

## Modèle

- `Utilisateur` (patient, administrateur) : id, keycloakId, nom, prenom, email (unique), telephone, role, actif, dateCreation
- `Professionnel` hérite de `Utilisateur` (même collection) : type (MEDECIN, PHARMACIEN, LABORANTIN), nomEtablissement, numeroAgrement, adresse, ville, joursTravail, heureDebut, heureFin, **signatureImage** (médecin)
- L'infirmier n'a pas de compte (géré par la pharmacie). Une pharmacie est le compte de son pharmacien titulaire.

## API REST

| Méthode | Endpoint | Codes | Rôle |
|---|---|---|---|
| POST | `/api/utilisateurs` | 201 · 400 · 409 · 503 | créer un compte (Keycloak + MongoDB) et publier `UtilisateurCree` |
| GET | `/api/utilisateurs/{id}` | 200 · 404 | un utilisateur (champ `actif` lu par les autres services) |
| GET | `/api/utilisateurs?role=` | 200 | liste des comptes (administrateur) |
| PATCH | `/api/utilisateurs/{id}/statut` | 200 · 400 · 404 · 503 | activer / désactiver un compte |
| GET | `/api/professionnels?type=&ville=` | 200 · 400 | rechercher des professionnels actifs |
| GET | `/api/professionnels/{id}/disponibilites` | 200 · 404 | horaires (appelé par rendezvous-service) |
| GET | `/api/professionnels/{id}/signature` | 200 · 404 | signature du médecin (appelé par ordonnance-service) |
| PUT | `/api/professionnels/{id}/signature` | 200 · 400 · 404 | envoyer la signature (multipart, champ `fichier`, PNG/JPEG, 200 Ko max) |

Les erreurs suivent le format Problem Details (RFC 9457), comme `medicament-service`.

## Communications

- **Synchrone · appelle** : Keycloak Admin API (création du compte, rôle realm, activation). Désactivé par défaut (`medivault.keycloak.enabled=false`) tant que le realm n'est pas prêt ; si Keycloak est activé et injoignable → 503, aucun compte créé.
- **Synchrone · appelé par** : dossier, rendezvous (`/disponibilites`), soin-domicile, medicament (pharmacie active ?), ordonnance (`/signature`).
- **Asynchrone · publie** : `UtilisateurCree` { utilisateurId, role, nom, prenom } sur `medivault.events`, clé `utilisateur.cree` → dossier.
- **Asynchrone · consomme** : aucun événement (les notifications sont gérées par notification-service).

## Données de démonstration

Créées au démarrage si la collection est vide (`medivault.seed.enabled=true`), avec des id fixes utilisables par les autres services :

| id | Rôle | Détail |
|---|---|---|
| `patient-ahmed` | PATIENT | Ahmed Ben Ali |
| `medecin-karim` | MEDECIN | Dr Karim Trabelsi, Tunis, lun.–ven. 8 h–16 h, avec signature |
| `labo-biolab` | LABORANTIN | Laboratoire BioLab, Ariana, lun.–sam. 7 h–13 h |
| `pharmacie-centrale` | PHARMACIEN | Pharmacie Centrale, Tunis |
| `admin` | ADMIN | administrateur |

## Lancer

1. MongoDB sur `localhost:27017` (ex. `docker run -d -p 27017:27017 mongo:7`)
2. Optionnel : RabbitMQ (`5672`), Eureka (`8761`), Config Server (`8888`) — le service démarre sans eux
3. Lancer `UserServiceApplication` puis ouvrir Swagger
