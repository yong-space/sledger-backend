FROM amazoncorretto:22
WORKDIR /build
RUN jlink \
  --verbose \
  --add-modules java.base,java.compiler,java.desktop,java.instrument,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,jdk.crypto.ec,jdk.jfr,jdk.management,jdk.naming.dns,jdk.unsupported \
  --strip-java-debug-attributes \
  --no-man-pages \
  --no-header-files \
  --output /jre
COPY ./build/libs/*.jar app.jar
RUN jar -xf app.jar && mkdir /app && cp -r META-INF /app && cp -r BOOT-INF/classes/* /app

FROM gcr.io/distroless/java-base-debian12
COPY --from=0 /jre /jre
COPY --from=0 /build/BOOT-INF/lib /lib
COPY --from=0 /app .
ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080
ENTRYPOINT [ "/jre/bin/java", "-cp", ".:/lib/*", "tech.sledger.Sledger" ]
