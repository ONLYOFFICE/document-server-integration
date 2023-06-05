FROM maven:3.9.2-eclipse-temurin-11-alpine AS example-build
WORKDIR /srv
COPY . .
RUN mvn package

FROM eclipse-temurin:11-jdk-alpine AS example
WORKDIR /srv
COPY --from=example-build /srv/target/*.jar example.jar
CMD ["java", "-jar", "example.jar"]

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY nginx.conf /etc/nginx/nginx.conf
