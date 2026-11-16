# Instalação das dependências
FROM node:22-alpine AS dependencies

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

# Build
FROM node:22-alpine AS build

WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1

ARG JAVA_API_URL=http://backend:8080
ENV JAVA_API_URL=${JAVA_API_URL}

COPY --from=dependencies /app/node_modules ./node_modules
COPY . .

# Garante que o diretório exista mesmo sem arquivos públicos.
RUN mkdir -p public && npm run build

# Execução
FROM node:22-alpine AS runtime

WORKDIR /app

ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup --system --gid 1001 app \
    && adduser --system --uid 1001 --ingroup app app

COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public

USER app

EXPOSE 3000

CMD ["node", "server.js"]
