#!/bin/bash

cygwin=false
darwin=false
os400=false
case "`uname`" in
CYGWIN*) cygwin=true;;
Darwin*) darwin=true;;
OS400*) os400=true;;
esac

[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=$HOME/jdk/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/java
[ ! -e "$JAVA_HOME/bin/java" ] && JAVA_HOME=/usr/local/java
[ ! -e "$JAVA_HOME/bin/java" ] && unset JAVA_HOME

if [ -z "$JAVA_HOME" ]; then
  if $darwin; then

    if [ -x '/usr/libexec/java_home' ] ; then
      export JAVA_HOME=`/usr/libexec/java_home`

    elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
      export JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
    fi
  else
    JAVA_PATH=`dirname $(readlink -f $(which javac))`
    if [ "x$JAVA_PATH" != "x" ]; then
      export JAVA_HOME=`dirname $JAVA_PATH 2>/dev/null`
    fi
  fi
  if [ -z "$JAVA_HOME" ]; then
    echo "ERROR: Please set the JAVA_HOME variable in your environment!!!"
    exit 1
  fi
fi

#进入脚本所在目录
cd `dirname $0`

#变量定义
APOLLO_META_SERVER=http://localhost:66
ENV=dev
APP_NAME=fizz-gateway-community-1.3.0.jar
APP_DEP_DIR="` pwd`"
APP_LOG_DIR=${APP_DEP_DIR}'/logs'
JAVA_CMD=${JAVA_HOME}'/bin/java'
PID_FILE="${APP_LOG_DIR}/tpid"
CHECK_COUNT=3

# 远程执行shell脚本初始化环境变量
source '/etc/profile'

SERVER_IP="` ip a |egrep "brd" |grep inet|awk '{print $2}'|sed 's#/24##g'|head -1`"

#创建日志目录
mkdir -p ${APP_LOG_DIR}

#进入应用所在目录（虽然都是绝对路径，但有些应用需要进入应用目录才能启动成功）
cd ${APP_DEP_DIR}

JAVA_OPTS="-Xms256m -Xmx4096m \
-XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m \
-XX:+AggressiveOpts \
-XX:+UseBiasedLocking \
-XX:+UseG1GC \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:-OmitStackTraceInFastThrow \
-verbose:gc \
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-XX:+PrintHeapAtGC \
-Xloggc:${APP_LOG_DIR}/${START_DATE_TIME}.gc \
-XX:+HeapDumpOnOutOfMemoryError \
-XX:HeapDumpPath=${APP_LOG_DIR}/dump.logs \
-Dorg.jboss.netty.epollBugWorkaround=true \
-Dio.netty.leakDetectionLevel=PARANOID -Dio.netty.leakDetection.targetRecords=60 \
-Dio.netty.allocator.type=unpooled \
-Dio.netty.noPreferDirect=true \
-Dio.netty.noUnsafe=true "

#进程状态标识变量，1为存在，0为不存在
PID_FLAG=0

#检查服务进程是否存在
checktpid() {
    TPID=`cat ${PID_FILE} | awk '{print $1}'`
    TPID=`ps -aef | grep ${TPID} | awk '{print $2}' | grep ${TPID}`
    if [[ ${TPID} ]]
    then
	      PID_FLAG=1
    else
	      PID_FLAG=0
    fi
}

#启动服务函数
start() {
    #检查进程状态
    checktpid
    if [[ ${PID_FLAG} -ne 0 ]]
    then
        echo "warn: $APP_NAME already started, ignoring startup request."
    else
        echo "starting $APP_NAME ..."
        rm -f ${PID_FILE}
        #rm -rf ${APP_LOG_DIR}/flumeES/*
        ${JAVA_CMD} -jar ${JAVA_OPTS} -Denv=$ENV -Dapollo.meta=${APOLLO_META_SERVER} ${APP_DEP_DIR}/${APP_NAME} > ${APP_LOG_DIR}/${APP_NAME}.log 2>&1 &
        echo $! > ${PID_FILE}
    fi
}

#关闭服务函数
stop() {
    #检查进程状态
    checktpid

    if [[ ${PID_FLAG} -ne 0 ]]
    then
        echo "stoping $APP_NAME..."

        #循环检查进程3次，每次睡眠2秒
        for((i=1;i<=${CHECK_COUNT};i++))
        do
            kill -9 ${TPID}
            sleep 2

            #检查进程状态
            checktpid

            if [[ ${PID_FLAG} -eq 0 ]]
            then
                break
            fi
        done

        #如果以上正常关闭进程都失败，则强制关闭
        if [[ ${PID_FLAG} -ne 0 ]]
        then
            echo "stoping use kill -9..."
                kill -9 ${TPID}
                sleep 2
        else
            echo "$APP_NAME Stopped!"
        fi
		
    else
        echo "warn:$APP_NAME is not runing"
    fi
}

#检测进程状态函数
status() {
    #检查进程状态
    checktpid
	
    if [[ ${PID_FLAG} -eq 0 ]]
	  then
        echo "$APP_NAME is not runing"
    else
        echo "$APP_NAME is runing"
    fi
}

#####脚本执行入口#####
case "$1" in
    'start')
        start
        ;;
    'stop')
        stop
        ;;
    'restart')
        stop
        start
        ;;
    'status')
        status
        ;;
    *)
    echo "usage: $0 {start|stop|restart|status}"
    exit 1
    ;;
esac

exit 0
