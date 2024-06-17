FROM amazoncorretto:21
WORKDIR /build
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar && mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM gcr.io/distroless/java21-debian12
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT [ "java", "-cp", ".:/lib/*", "tech.sledger.Sledger" ]
