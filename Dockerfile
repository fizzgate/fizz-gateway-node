FROM java:8

MAINTAINER fizzgate.com

ENV APP_HOME_PATH /opt/fizz-gateway-community

ADD fizz-bootstrap/target/fizz-bootstrap-*.jar ${APP_HOME_PATH}/fizz-gateway-community.jar
ADD fizz-bootstrap/src/main/resources/log4j2-spring.xml ${APP_HOME_PATH}/log4j2-spring.xml

COPY fizz-bootstrap/sh/boot.sh ${APP_HOME_PATH}/boot.sh
COPY fizz-bootstrap/sh/docker-entrypoint.sh ${APP_HOME_PATH}/docker-entrypoint.sh
RUN chmod +x ${APP_HOME_PATH}/boot.sh

WORKDIR ${APP_HOME_PATH}

EXPOSE 8600

ENTRYPOINT ["/bin/bash", "./docker-entrypoint.sh"]