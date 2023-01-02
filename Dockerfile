FROM ghcr.io/graalvm/jdk:21.3
WORKDIR /data/symphony
COPY ./build/libs/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "./app.jar", "--spring.profiles.active=prod" ]
