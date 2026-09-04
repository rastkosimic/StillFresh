FROM eclipse-temurin:17.0.19_10-jre-jammy

RUN groupadd --system stillfresh \
    && useradd --system --gid stillfresh stillfresh

WORKDIR /app
COPY target/user-service-0.0.1-SNAPSHOT.jar app.jar
RUN chown stillfresh:stillfresh app.jar

USER stillfresh
EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8081' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
