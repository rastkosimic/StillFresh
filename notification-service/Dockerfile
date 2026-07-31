# File: notification-service/Dockerfile
FROM eclipse-temurin:17-jre-jammy
VOLUME /tmp
COPY target/notification-service-1.0.0.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting Notification Service...' && java -jar /app.jar --debug"] 
