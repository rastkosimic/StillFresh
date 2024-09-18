# File: vendor-service/Dockerfile
FROM openjdk:17-jdk-slim
VOLUME /tmp
COPY target/vendor-service-0.0.1-SNAPSHOT.jar app.jar

# Add logging to entrypoint
ENTRYPOINT ["sh", "-c", "echo 'Starting Vendor Service...' && java -jar /app.jar --debug"]
