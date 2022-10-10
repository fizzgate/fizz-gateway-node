# debug bash: DOCKER_BUILDKIT=0 docker build --no-cache -t fizz-gateway-community:v2.7.0 .
# bash: docker buildx build -t fizz-gateway-community:v2.7.0 . --builder "$(docker buildx create --driver-opt env.BUILDKIT_STEP_LOG_MAX_SIZE=10000000 --driver-opt env.BUILDKIT_STEP_LOG_MAX_SPEED=10000000)"
# First stage: complete build environment
FROM maven:3.8.6-openjdk-18 AS builder

WORKDIR /app
# add pom.xml and source code
COPY pom.xml ./pom.xml
#ADD ./src src/
COPY ./repo ./repo

COPY ./fizz-bootstrap ./fizz-bootstrap
COPY ./fizz-common ./fizz-common
COPY ./fizz-core ./fizz-core
COPY ./fizz-plugin ./fizz-plugin
COPY ./fizz-spring-boot-starter ./fizz-spring-boot-starter


# RUN ls /app

# # package jar
#RUN mvn install:install-file -DgroupId=com.networknt -DartifactId=json-schema-validator-i18n-support -Dversion=1.0.39_5 -Dfile=./repo/com/networknt/json-schema-validator-i18n-support/1.0.39_5/json-schema-validator-i18n-support-1.0.39_5.jar -Dpackaging=jar && mvn -B clean package install --file ./pom.xml -Dmaven.test.skip=true && mvn -B clean package install --file ./fizz-bootstrap/pom.xml -Dmaven.test.skip=true
RUN mvn -B clean package install --file ./pom.xml -Dmaven.test.skip=true && mvn -B clean package install --file ./fizz-bootstrap/pom.xml -Dmaven.test.skip=true

FROM openjdk:8u342

MAINTAINER fizzgate.com


ENV APP_HOME_PATH /opt/fizz-gateway-community

COPY --from=builder /app/fizz-bootstrap/target/fizz-bootstrap-*.jar ${APP_HOME_PATH}/fizz-gateway-community.jar
COPY --from=builder /app/fizz-bootstrap/src/main/resources/log4j2-spring.xml ${APP_HOME_PATH}/log4j2-spring.xml

COPY --from=builder /app/fizz-bootstrap/sh/boot.sh ${APP_HOME_PATH}/boot.sh
COPY --from=builder /app/fizz-bootstrap/sh/docker-entrypoint.sh ${APP_HOME_PATH}/docker-entrypoint.sh
RUN chmod +x ${APP_HOME_PATH}/boot.sh

WORKDIR ${APP_HOME_PATH}

EXPOSE 8600

ENTRYPOINT ["/bin/bash", "./docker-entrypoint.sh"]