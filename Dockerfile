FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

COPY pom.xml .
COPY lib/pom.xml lib/pom.xml
COPY example/pom.xml example/pom.xml

RUN mvn -B -pl example -am dependency:go-offline

COPY lib/src lib/src
COPY example/src example/src

RUN mvn -B -o -pl example -am package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/example/target/camel-twitch-example-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
