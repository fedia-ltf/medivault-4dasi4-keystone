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

### Authentification (ajouté : connexion, inscription, mot de passe oublié)

| Méthode | Endpoint | Codes | Rôle |
|---|---|---|---|
| POST | `/api/auth/login` | 200 · 400 · 401 · 403 · 503 | connexion e-mail + mot de passe → jetons JWT Keycloak + profil (le **rôle vient du compte**) |
| POST | `/api/auth/refresh` | 200 · 401 | renouveler le jeton |
| POST | `/api/auth/logout` | 204 | fermer la session Keycloak |
| POST | `/api/auth/inscription` | 201 · 400 · 409 | s'inscrire : **toujours PATIENT** (le rôle demandé est ignoré) |
| POST | `/api/auth/mot-de-passe-oublie` | 202 | envoie l'e-mail de réinitialisation (même réponse si le compte n'existe pas) |
| POST | `/api/auth/reinitialiser-mot-de-passe` | 200 · 400 · 403 | choisit le nouveau mot de passe avec le lien de l'e-mail (`token`, usage unique, 30 min) |
| GET | `/api/auth/moi` | 200 · 401 | profil du compte connecté |
| POST | `/api/auth/changer-mot-de-passe` | 200 · 401 | changer son mot de passe (ancien requis) |

Sécurité (profil Spring `keycloak`) : jeton JWT vérifié sur toutes les routes sauf login, inscription, mot de passe oublié, Swagger, santé, `GET /api/utilisateurs/{id}` et `GET /api/professionnels/**` (lus par les autres services). Créer un compte / lister / activer-désactiver = **ADMIN** ; envoyer une signature = **MEDECIN** ou **ADMIN**.

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
| `pharmacie-marsa`, `-manar`, `-ibnsina`, `-jasmins`, `-carthage`, `-menzah`, `-bizerte` | PHARMACIEN | 7 autres pharmacies (ajouté : le medicament-service leur attribue des quotas) |
| `admin` | ADMIN | administrateur |

Comptes Keycloak correspondants (mot de passe de **tous** : `Medivault2026!`, développement seulement) :
`ahmed.benali@medivault.tn`, `karim.trabelsi@medivault.tn`, `pharmacie.centrale@medivault.tn`, `contact@biolab.tn`, `admin@medivault.tn`, + `marsa@`, `manar@`, `ibnsina@`, `jasmins@`, `carthage@`, `menzah@`, `bizerte@medivault.tn`.

## Lancer

**Version simple (comme avant, sans connexion réelle)** :
1. MongoDB sur `localhost:27017` (ex. `docker run -d -p 27017:27017 mongo:7`)
2. Optionnel : RabbitMQ (`5672`), Eureka (`8761`), Config Server (`8888`) — le service démarre sans eux
3. Lancer `UserServiceApplication` puis ouvrir Swagger

**Version complète (ajouté : Keycloak + e-mails + MongoDB visible)** :
1. `docker compose up -d` dans ce dossier. Cela lance :

   | Outil | Adresse | Rôle |
   |---|---|---|
   | MongoDB | `localhost:27017` | base `user_db` (volume : les données restent) |
   | mongo-express | http://localhost:8082 | **voir les utilisateurs** dans MongoDB |
   | Keycloak | http://localhost:8180 (admin / admin) | connexion, rôles, JWT — realm `medivault` importé depuis `keycloak/medivault-realm.json` |
   | Mailpit | http://localhost:8025 | boîte e-mail de test : reçoit les e-mails « mot de passe oublié » |

2. Lancer le service avec le profil `keycloak` : `SPRING_PROFILES_ACTIVE=keycloak` (PowerShell : `$env:SPRING_PROFILES_ACTIVE='keycloak'`)
3. Tout effacer et repartir de zéro : `docker compose down -v`

### Mot de passe oublié : deux modes

1. **Sans rien configurer** : Keycloak envoie l'e-mail, visible dans **Mailpit** (http://localhost:8025) → lien → page Keycloak en français → nouveau mot de passe.
2. **Vraie boîte mail, par l'API d'un service d'e-mail (Brevo, offre gratuite)** : le user-service envoie lui-même le mail, avec un lien à usage unique (30 min) vers la page `/reinitialiser-mot-de-passe` du front. Seule l'empreinte SHA-256 du lien est gardée en base (`jetons_reinitialisation`, supprimée automatiquement à l'expiration).

   Mise en route (la clé reste dans une variable d'environnement, jamais dans un fichier) :
   1. Créer un compte sur https://www.brevo.com, puis *SMTP & API → API Keys* : créer une clé d'API (`xkeysib-...`).
   2. *Senders, Domains* : ajouter et **vérifier** l'adresse qui enverra les e-mails (ex. ton Gmail : Brevo envoie un code de confirmation).
   3. Lancer le service avec ces variables (PowerShell) :

      ```powershell
      $env:BREVO_API_KEY='xkeysib-...'; $env:MAIL_EXPEDITEUR='ton.adresse@gmail.com'
      $env:SPRING_PROFILES_ACTIVE='keycloak'; mvn spring-boot:run
      ```

   Ou, plus simple, dans le fichier local `secrets.properties` (copie de `secrets.properties.example`, ignoré par Git).

   **Resend** (https://resend.com) marche aussi (`medivault.mail.provider=resend`, clé `re_...`) et ne demande pas d'activation manuelle du compte : sans domaine à soi, le mail part de `onboarding@resend.dev` et n'est livré qu'à l'adresse du compte Resend.

   Sans clé, le mode 1 (Mailpit) reste actif. Si le service d'e-mail refuse l'envoi, l'erreur est écrite dans les logs du user-service ; l'appelant reçoit toujours la même réponse 202.
