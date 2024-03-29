FROM amazoncorretto:21
WORKDIR /build
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar && mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM ghcr.io/yong-space/jre-tiny
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT [ "/jre/bin/java", "-cp", ".:/lib/*", "tech.sledger.Sledger" ]
