# Use OpenJDK 25 base image
FROM eclipse-temurin:25-jdk-jammy

# Set workdir
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Grant execution rights to the wrapper
RUN chmod +x mvnw

# Download dependencies (this helps caching)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw package -DskipTests

# Expose port 8080
EXPOSE 8080

# Run the jar
CMD ["java", "-jar", "target/api-0.0.1-SNAPSHOT.jar"]