FROM eclipse-temurin:17-jre-jammy
VOLUME /tmp
COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting API Gateway...' && java -jar /app.jar --debug"]
