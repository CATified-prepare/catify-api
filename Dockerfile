##
# Multi-stage build for catify-api (Spring Boot, Java 21)
#
# Build:
#   docker build -t catify-api .
#
# Run (standalone example):
#   docker run --rm -p 8080:8080 catify-api
##

FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

# Copy Maven wrapper + build definition first to maximize layer caching.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Ensure the wrapper is executable in Linux containers.
RUN chmod +x mvnw

# Download dependencies (cached layer).
RUN ./mvnw -q -DskipTests dependency:go-offline

# Now copy sources and build.
COPY src/ src/
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar (typical Spring Boot plugin output).
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]