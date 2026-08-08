FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S bakeflow && adduser -S bakeflow -G bakeflow
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER bakeflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
