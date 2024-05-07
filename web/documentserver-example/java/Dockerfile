FROM maven:3.6.1-jdk-8-alpine AS MVN_BLDR
COPY ./ /tmp/
WORKDIR /tmp/
RUN mvn package

FROM tomcat:alpine
RUN rm -fr /usr/local/tomcat/webapps/ROOT
COPY --from=MVN_BLDR /tmp/target/*.war $CATALINA_HOME/webapps/ROOT.war
