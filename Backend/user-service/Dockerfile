# =====================================================================================
# Dockerfile de user-service (comptes, connexion Keycloak) (MediVault)
#   Construire seul :  docker build -t user-service .
#   Tout lancer      :  à la racine du projet, docker compose --profile app up -d --build
# Aucun Java ni Maven n'est nécessaire sur le PC : tout se passe dans Docker.
# =====================================================================================

# ---- ÉTAPE 1 : construire le jar avec Maven (les bibliothèques restent en cache entre deux constructions)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# ---- ÉTAPE 2 : l'image finale, légère (Java seulement, sans Maven ni code source)
FROM eclipse-temurin:17-jre
# Un utilisateur sans droits spéciaux fait tourner le service (plus sûr que root)
RUN useradd --system --create-home --home-dir /app app  && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/*.jar app.jar
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8081
USER app
# "exec" : Java reçoit directement l'ordre d'arrêt de Docker (arrêt propre)
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
