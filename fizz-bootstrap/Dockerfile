FROM java:8

MAINTAINER fizzgate.com

ENV APP_HOME_PATH /opt/fizz-gateway-community

ADD target/fizz-bootstrap-*.jar ${APP_HOME_PATH}/fizz-gateway-community.jar

COPY sh/boot.sh ${APP_HOME_PATH}/boot.sh
COPY sh/docker-entrypoint.sh ${APP_HOME_PATH}/docker-entrypoint.sh
RUN chmod +x ${APP_HOME_PATH}/boot.sh

WORKDIR ${APP_HOME_PATH}

EXPOSE 8600

ENTRYPOINT ["/bin/bash", "./docker-entrypoint.sh"]