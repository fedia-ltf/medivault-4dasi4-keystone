# =====================================================================================
# Dockerfile de medicament-service (médicaments sensibles)
#
# Un Dockerfile = la recette pour fabriquer une "image" : une boîte fermée qui contient
# le service avec tout ce qu'il lui faut (Java + le programme). Cette boîte tourne de la
# même façon sur n'importe quel PC.
#
#   Construire :  docker build -t medicament-service .
#   Lancer     :  docker run -p 8084:8084 -v medicament-data:/app/data medicament-service
#
# Aucun Java ni Maven n'est nécessaire sur le PC : tout se passe dans Docker.
# =====================================================================================

# ---- ÉTAPE 1 : construire le programme (le "jar") avec Maven -------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# On copie d'abord pom.xml seul puis le code : si seul le code change, Docker garde en cache
# les bibliothèques déjà téléchargées (le cache Maven est conservé entre deux constructions).
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# ---- ÉTAPE 2 : l'image finale, légère (Java seulement, sans Maven ni code source) -----
FROM eclipse-temurin:17-jre

# Un utilisateur sans droits spéciaux fait tourner le service (plus sûr que "root").
RUN useradd --system --create-home --home-dir /app app \
 && mkdir -p /app/data \
 && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/medicament-service-*.jar app.jar

# Variables d'environnement (valeurs par défaut) : voir aussi application.properties.
#   DATA_DIR          dossier de la base H2 (fichier) : à garder dans un volume pour ne pas perdre les données
#   INTEGRATION_MODE  simule (fausses ordonnances et comptes) ou reel (vrais appels aux autres services)
#   DEMO_DATA         true : crée des données d'exemple au premier lancement (base vide)
#   EUREKA_URL, RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASSWORD, CONFIG_SERVER_URL
ENV DATA_DIR=/app/data \
    INTEGRATION_MODE=simule \
    DEMO_DATA=true \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Le dossier de la base H2 : un "volume" garde les données quand le conteneur est recréé.
VOLUME /app/data
EXPOSE 8084
USER app

# "exec" : Java reçoit directement l'ordre d'arrêt de Docker (arrêt propre).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
