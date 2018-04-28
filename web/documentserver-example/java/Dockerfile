FROM ubuntu:16.04
RUN apt-get update && apt-get install -y software-properties-common \
                                         python-software-properties
RUN add-apt-repository -y ppa:webupd8team/java
RUN echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
RUN apt-get update && apt-get install -y oracle-java8-installer \
                                         maven
COPY . /java
CMD mvn -f /java package
