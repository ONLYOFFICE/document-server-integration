FROM maven:3.6.1-jdk-8-alpine AS example-build
WORKDIR /srv
COPY . /srv
RUN mvn package

FROM tomcat:9.0.20-jre8-alpine AS example
RUN rm -fr /usr/local/tomcat/webapps/ROOT
COPY --from=example-build /srv/target/*.war $CATALINA_HOME/webapps/ROOT.war

FROM nginx:1.23.4-alpine3.17 AS proxy
COPY nginx.conf /etc/nginx/nginx.conf
