# user-service

Dossier prêt pour le microservice **user-service**. Responsable : **toute l'équipe**.

- Rôle : Comptes et rôles (Keycloak), profils des professionnels, notifications temps réel (SSE)
- Données : Utilisateur, Professionnel, Notification
- Base de données : MongoDB
- Package Java : `tn.esprit.medivault.user`

## Structure à respecter (même que `medicament-service`)

```
src/main/java/tn/esprit/medivault/user/
  entities/
  repositories/
  dto/
  services/
  controllers/
  clients/
  messaging/
  config/
  exceptions/
  UserServiceApplication.java
src/main/resources/application.properties
src/test/java/tn/esprit/medivault/user/
postman/
```

- Service : `I<Entité>Service` (interface) + `<Entité>ServiceImpl`
- Controller : `<Entité>Controller`, URL `/api/<entités>`
- Erreurs : un `GlobalExceptionHandler` au format Problem Details (voir `medicament-service`)
- Créer le projet Spring Boot (Maven) **dans ce dossier** : le `pom.xml` à la racine de `user-service/`.

Les fichiers `.gitkeep` servent seulement à garder les dossiers vides dans Git : les supprimer une fois le dossier rempli.
