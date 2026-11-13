# pharmacie-service

Dossier prêt pour le microservice **pharmacie-service**. Responsable : **Aziz**.

- Rôle : Délivrance unique (totale ou partielle), blocage des médicaments retirés
- Données : Pharmacie, Delivrance, MedicamentBloque
- Base de données : H2
- Package Java : `tn.esprit.medivault.pharmacie`

## Structure à respecter (même que `medicament-service`)

```
src/main/java/tn/esprit/medivault/pharmacie/
  entities/
  repositories/
  dto/
  services/
  controllers/
  clients/
  messaging/
  config/
  exceptions/
  PharmacieServiceApplication.java
src/main/resources/application.properties
src/test/java/tn/esprit/medivault/pharmacie/
postman/
```

- Service : `I<Entité>Service` (interface) + `<Entité>ServiceImpl`
- Controller : `<Entité>Controller`, URL `/api/<entités>`
- Erreurs : un `GlobalExceptionHandler` au format Problem Details (voir `medicament-service`)
- Créer le projet Spring Boot (Maven) **dans ce dossier** : le `pom.xml` à la racine de `pharmacie-service/`.

Les fichiers `.gitkeep` servent seulement à garder les dossiers vides dans Git : les supprimer une fois le dossier rempli.
