# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy dependency files first (better caching)
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Stage 2: Run the application (lightweight)
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install runtimes/compilers needed to execute student-submitted code
# for the automated programming-assignment grading feature.
# (python3 for Python submissions, gcc/g++ for C/C++ submissions —
# the JDK above already provides javac/java for Java submissions.)
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 gcc g++ bash \
    && rm -rf /var/lib/apt/lists/*

# Copy generated jar
COPY --from=build /app/target/*.jar app.jar

# Match Spring Boot port
EXPOSE 8000

# Run app
ENTRYPOINT ["java", "-jar", "app.jar"]