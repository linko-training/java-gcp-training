# Etapa 1: Compilación de la App usando una imagen ligera de Maven con JDK 21
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copiar pom y descargar dependencias para aprovechar caché de capas Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el jar omitiendo los tests para el build rápido
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución (Runtime) optimizada
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Crear un usuario no-root por seguridad (Buenas prácticas de Gobernanza)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar el Jar compilado desde la etapa anterior
COPY --from=build /app/target/cloud-run-api-0.0.1-SNAPSHOT.jar app.jar

# Cloud Run inyecta automáticamente la variable de entorno $PORT (suele ser 8080)
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]