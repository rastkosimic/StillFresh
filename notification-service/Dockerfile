# File: notification-service/Dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/notification-service-1.0.0.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting Notification Service...' && java -jar /app.jar --debug"] 