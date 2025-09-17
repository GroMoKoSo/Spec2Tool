# Use OpenJDK 17 for both stages
FROM maven:3.8.5-openjdk-17-slim as maven

# Image layer: with dependencies
ADD pom.xml pom.xml
RUN mvn dependency:go-offline -B

# Image layer: with the application
COPY ./src ./src
RUN mvn package -DskipTests -Djar.name=springApp
#Change if you wanna include tests
#RUN mvn package -Djar.name=springApp

# Run layer: only with the jar file
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY --from=maven target/*.jar ./
EXPOSE 8080
ENTRYPOINT ["java","-jar","./springApp.jar"]

