# =====================================================================================
# Dockerfile du front Angular de MediVault
#   Tout lancer :  à la racine du projet, docker compose --profile app up -d --build
#   Puis ouvrir :  http://localhost:4200
# Le site est servi par nginx, qui envoie /api/** à la Gateway (point d'entrée unique, port 9000).
# =====================================================================================

# ---- ÉTAPE 1 : construire le site (ng build)
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# ---- ÉTAPE 2 : nginx sert les fichiers et relaie /api vers la Gateway
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/medivault-frontend/browser /usr/share/nginx/html
EXPOSE 80
