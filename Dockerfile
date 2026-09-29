FROM gradle:jdk25-ubi as build

WORKDIR /usr/app/
COPY . .
RUN ./gradlew jar --no-daemon

FROM mcr.microsoft.com/openjdk/jdk:25-ubuntu

ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME

COPY --from=build /usr/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
