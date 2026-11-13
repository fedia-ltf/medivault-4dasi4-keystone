# dossier-service

Dossier prêt pour le microservice **dossier-service**. Responsable : **Eya**.

- Rôle : Dossier médical, consultations, accès au dossier
- Données : Dossier, Consultation, AccesDossier
- Base de données : MySQL
- Package Java : `tn.esprit.medivault.dossier`

## Structure à respecter (même que `medicament-service`)

```
src/main/java/tn/esprit/medivault/dossier/
  entities/
  repositories/
  dto/
  services/
  controllers/
  clients/
  messaging/
  config/
  exceptions/
  DossierServiceApplication.java
src/main/resources/application.properties
src/test/java/tn/esprit/medivault/dossier/
postman/
```

- Service : `I<Entité>Service` (interface) + `<Entité>ServiceImpl`
- Controller : `<Entité>Controller`, URL `/api/<entités>`
- Erreurs : un `GlobalExceptionHandler` au format Problem Details (voir `medicament-service`)
- Créer le projet Spring Boot (Maven) **dans ce dossier** : le `pom.xml` à la racine de `dossier-service/`.

Les fichiers `.gitkeep` servent seulement à garder les dossiers vides dans Git : les supprimer une fois le dossier rempli.
