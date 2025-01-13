# File: order-service/Dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/order-service-0.0.1-SNAPSHOT.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting Order Service...' && java -jar /app.jar --debug"]
