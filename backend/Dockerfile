# Build stage
FROM eclipse-temurin:21-jdk AS build
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copy and install parent POM first
COPY pom.xml .
RUN mvn install -N

# Copy backend POM and source
COPY backend/pom.xml ./backend/
COPY backend/src ./backend/src

# Build backend with Spring Boot repackage
WORKDIR /app/backend
RUN mvn clean package spring-boot:repackage -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN apt-get update && apt-get install -y jq && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/backend/target/*.jar app.jar
COPY backend/src/main/resources/downloads ./downloads
COPY scripts ./scripts
RUN chmod +x /app/scripts/*.sh
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
