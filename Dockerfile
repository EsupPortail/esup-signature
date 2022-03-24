FROM debian:latest
RUN apt update && apt upgrade && apt -y install openjdk-17-jdk-headless && apt install -y ghostscript
COPY target/esup-signature.war esup-signature.war
COPY src/main/resources/application-docker.yml /tmp/application-docker.yml
ENTRYPOINT ["java","-jar","/esup-signature.war","--spring.config.location=file:/tmp/application-docker.yml"]