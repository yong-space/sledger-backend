FROM amazoncorretto:17
WORKDIR /build
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar && jdeps -q \
    --ignore-missing-deps \
    --print-module-deps \
    --recursive \
    --multi-release 17 \
    --class-path="BOOT-INF/lib/*" \
    --module-path="BOOT-INF/lib/*" \
    app.jar > /deps
RUN jlink \
    --verbose \
    --add-modules $(cat /deps),jdk.naming.dns,jdk.crypto.ec,jdk.management \
    --strip-java-debug-attributes \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output /jre
RUN mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM gcr.io/distroless/java-base-debian11
COPY --from=0 /jre /jre
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENTRYPOINT [ \
  "/jre/bin/java", \
  "-cp", ".:/lib/*", \
  "-Djavax.net.ssl.trustStore=/truststore/truststore", \
  "-Djavax.net.ssl.trustStorePassword=changeit", \
  "tech.sledger.Sledger", \
  "--spring.profiles.active=prod" \
]
