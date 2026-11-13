# ordonnance-service

Dossier prêt pour le microservice **ordonnance-service**. Responsable : **Amine**.

- Rôle : Rédaction et cycle de vie des ordonnances, contrôle avant validation
- Données : Ordonnance, LigneOrdonnance
- Base de données : MySQL
- Package Java : `tn.esprit.medivault.ordonnance`

## Structure à respecter (même que `medicament-service`)

```
src/main/java/tn/esprit/medivault/ordonnance/
  entities/
  repositories/
  dto/
  services/
  controllers/
  clients/
  messaging/
  config/
  exceptions/
  OrdonnanceServiceApplication.java
src/main/resources/application.properties
src/test/java/tn/esprit/medivault/ordonnance/
postman/
```

- Service : `I<Entité>Service` (interface) + `<Entité>ServiceImpl`
- Controller : `<Entité>Controller`, URL `/api/<entités>`
- Erreurs : un `GlobalExceptionHandler` au format Problem Details (voir `medicament-service`)
- Créer le projet Spring Boot (Maven) **dans ce dossier** : le `pom.xml` à la racine de `ordonnance-service/`.

Les fichiers `.gitkeep` servent seulement à garder les dossiers vides dans Git : les supprimer une fois le dossier rempli.
