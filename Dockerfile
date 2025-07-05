FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:21
WORKDIR /app
COPY --from=build /app/target/DealSpy.jar DealSpy.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "DealSpy.jar"]
