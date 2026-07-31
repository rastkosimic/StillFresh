FROM eclipse-temurin:17-jre-jammy
VOLUME /tmp
COPY target/eureka-server-0.0.1-SNAPSHOT.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting Eureka Server...' && java -jar /app.jar"]
