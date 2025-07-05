FROM openjdk:26-oraclelinux9
ADD target/DealSpy.jar DealSpy.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/DealSpy.jar"]