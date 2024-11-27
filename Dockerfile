# Step 1: Build the application
FROM maven:3.9.9-amazoncorretto-21-al2023 AS build
WORKDIR /app

# Copy the pom.xml first to cache dependencies
COPY pom.xml .

# Download dependencies (this will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Now copy the source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Step 2: Run the application
FROM openjdk:24-slim-bullseye
WORKDIR /app

# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application's port
EXPOSE 8085

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]










# # Step 1: Build the application
# FROM maven:3.9.9-amazoncorretto-21-al2023 AS build
# WORKDIR /app
# COPY pom.xml .
# COPY src ./src
# RUN mvn clean package -DskipTests

# # Step 2: Run the application
# FROM openjdk:24-slim-bullseye
# WORKDIR /app
# COPY --from=build /app/target/EmailSender1-0.0.1-SNAPSHOT.jar app.jar
# EXPOSE 8085
# ENTRYPOINT ["java", "-jar", "app.jar"]





# From maven:3.9.9-amazoncorretto-21-al2023 AS build
# COPY . .
# RUN mvn clean package -DskipTests

# FROM openjdk:24-slim-bullseye
# COPY --from=build /target/EmailSender1-0.0.1-SNAPSHOT.jar app.jar
# EXPOSE 8085
# ENTRYPOINT ["java","-jar","app.jar"]
