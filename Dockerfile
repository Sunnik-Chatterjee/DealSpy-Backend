FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .

# Set Java version for compilation
RUN mvn clean package -DskipTests -Dmaven.compiler.source=21 -Dmaven.compiler.target=21

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/DealSpy.jar DealSpy.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "DealSpy.jar"]