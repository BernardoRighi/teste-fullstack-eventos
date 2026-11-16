# Etapa de build
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# Testes com Testcontainers devem ser executados separadamente,
# pois precisam de Docker disponível.
RUN mvn clean package -DskipTests -B

# Etapa de execução
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

RUN groupadd --system app \
    && useradd --system --gid app app

COPY --from=build --chown=app:app /app/target/*.jar ./app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
