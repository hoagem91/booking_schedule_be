# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Support build contexts checked out on Windows as well as Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests \
    && mkdir /out \
    && cp target/*.jar /out/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build --chown=app:app /out/app.jar app.jar

USER app:app

# Match application.yaml; override with SERVER_PORT when needed.
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
