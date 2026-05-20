##
# Multi-stage build for catify-api (Spring Boot, Java 21)
##

FROM eclipse-temurin:21-jdk AS build

# Install Maven via apt-get (avoids mvnw needing to download Maven at build time)
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

# Copy build definition first to maximize layer caching.
COPY pom.xml ./

# Download dependencies (cached layer).
# SSL flags needed for corporate MITM proxy environments
RUN mvn -q -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    dependency:go-offline

# Now copy sources and build.
COPY src/ src/
RUN mvn -q -DskipTests \
    -Dmaven.wagon.http.ssl.insecure=true \
    -Dmaven.wagon.http.ssl.allowall=true \
    -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
    package

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built jar (typical Spring Boot plugin output).
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]