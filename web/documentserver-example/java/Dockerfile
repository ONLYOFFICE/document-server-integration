# FROM maven:3.9.3-eclipse-temurin-11-alpine AS example-build
# WORKDIR /srv
# COPY . .
# RUN mvn package

FROM maven:3.6.1-jdk-8-alpine AS example-build
WORKDIR /srv
COPY . .
RUN mvn package

# no main manifest attribute, in example.war
# FROM eclipse-temurin:11-jdk-alpine AS example
# WORKDIR /srv
# COPY --from=example-build /srv/target/*.war example.war
# CMD ["java", "-jar", "example.war"]

FROM tomcat:9.0.20-jre8-alpine AS example
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY --from=example-build /srv/target/*.war $CATALINA_HOME/webapps/ROOT.war

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY proxy/nginx.conf /etc/nginx/nginx.conf
