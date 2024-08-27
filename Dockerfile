FROM         maven:3.8.6-openjdk-18 AS build
WORKDIR     /app
COPY         pom.xml .
RUN         mvn dependency:go-offline
COPY         src ./src
RUN         mvn package -DskipTests

FROM        openjdk:18-jdk-alpine
WORKDIR     /deployments
COPY        --from=build /app/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]