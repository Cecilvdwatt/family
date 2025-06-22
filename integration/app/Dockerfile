FROM docker.io/eclipse-temurin:21-jre-alpine
WORKDIR /app/
COPY ./config/application.properties /app/config/application.properties
COPY ./target/app.jar /app/app.jar
COPY start_cmd.sh /app/
ENTRYPOINT [ "sh", "/app/start_cmd.sh"]