# Pull base image.
FROM ubuntu:latest

RUN \
# Update
apt-get update -y && \
# Install Java
apt-get install default-jre -y

ADD ./out/artifacts/Server_jar/Server.jar Server.jar

EXPOSE 8889

CMD java -jar Server.jar