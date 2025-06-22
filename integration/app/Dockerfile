FROM docker.io/eclipse-temurin:21-jre-alpine
WORKDIR /app/
COPY ./config/* /app/config/
COPY ./target/* /app/
COPY ./target/verto-cms-gateway-test-plugin.jar /tmp/
RUN unzip /tmp/verto-cms-gateway-test-plugin.jar -d /app/plugin
COPY ./entry.sh /app/
COPY ./keys /app/keys
RUN touch key
HEALTHCHECK --interval=5s --timeout=10s --start-period=5s --retries=15 CMD netstat -tln | grep -q 9000
ENTRYPOINT [ "sh", "/app/entry.sh"]