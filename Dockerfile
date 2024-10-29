FROM eclipse-temurin:20 as build

WORKDIR /tmp/build
COPY . .
RUN ./gradlew shadowJar

FROM eclipse-temurin:20
WORKDIR /app
COPY --from=build /tmp/build/build/libs/kotlin-htmx-all.jar .
COPY .env.default .

EXPOSE 8080
ENV TZ="Europe/Oslo"
CMD ["java", "-jar", "kotlin-htmx-all.jar"]