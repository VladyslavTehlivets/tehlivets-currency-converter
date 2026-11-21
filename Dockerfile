FROM node:22-slim AS web-export
WORKDIR /build/web
COPY web/package.json web/package-lock.json ./
RUN npm ci
COPY web/ ./
RUN npm run build

FROM eclipse-temurin:21-jdk AS api-jar
WORKDIR /build
COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY conversion conversion
COPY worksheet-api worksheet-api
RUN ./mvnw -B -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=api-jar /build/worksheet-api/target/worksheet-api.jar worksheet-api.jar
COPY --from=web-export /build/web/out web/out
ENV PORT=7070 WEB_EXPORT_DIR=/app/web/out
EXPOSE 7070
USER nobody
CMD ["java", "-jar", "worksheet-api.jar"]
