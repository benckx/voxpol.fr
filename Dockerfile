FROM gradle:9.9.1-jdk25 AS build
COPY --chown=gradle:gradle . /home/gradle/src

WORKDIR /home/gradle/src
RUN gradle :app:shadowJar -x test --no-daemon

FROM eclipse-temurin:25-jre

EXPOSE 8080 25060

RUN mkdir /app
COPY --from=build /home/gradle/src/app/build/libs/app.jar /app/app.jar

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=25", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]
