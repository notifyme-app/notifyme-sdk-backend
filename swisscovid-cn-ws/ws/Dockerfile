# Dockerfile for SwissCovid-CN-WS
# http://www.ubique.ch/

FROM openjdk:11.0.7-jre-buster

LABEL maintainer="Martin Alig <alig@ubique.ch>"
LABEL maintainer="Felix Haller <haller@ubique.ch>"
LABEL maintainer="Fabian Aggeler <aggeler@ubique.ch>"

# Install ws
RUN useradd ws

WORKDIR /home/ws/

# Copy configs
COPY . /home/ws

# Create skeleton
RUN mkdir -p /home/ws/bin && \
    mkdir -p /home/ws/archive && \
    mkdir -p /home/ws/log && \
    mkdir -p /home/ws/tmp

# Copy binary
COPY ./bin/swisscovid-cn-sdk-backend-ws-2.0.0.jar /home/ws/bin/swisscovid-cn-sdk-backend-ws.jar

RUN chown -R ws:ws /home/ws

USER ws
# Access to webinterface
EXPOSE 8080

CMD java -jar $JAVA_OPTS -Dlogging.config=/home/ws/conf/swisscovid-cn-sdk-backend-ws-logback.xml -Dspring.config.location=/home/ws/conf/swisscovid-cn-sdk-backend-ws.properties /home/ws/bin/swisscovid-cn-sdk-backend-ws.jar
