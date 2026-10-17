# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Primero solo el pom, para aprovechar la caché de dependencias entre builds
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 1001 stockiq
COPY --from=build /build/target/*.jar app.jar
USER stockiq

EXPOSE 8080

# D-16: la JVM corre en UTC
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "app.jar"]
