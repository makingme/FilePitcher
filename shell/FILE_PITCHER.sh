#!/bin/bash
APP_PNM=UMS_FILE_PITCHER
APP_PID=300_$APP_PNM
PIDS=""
export APP_PNM=$APP_PNM
. ./env.sh
APP_LOGF=$APP_LOG/$APP_PNM.log
runutil $APP_PID $1 "$2"

nohup $APP_JAVA $UMS_JVMOPT -Dpnm=$APP_PID $LOGBACK -server -jar $APP_JAR $APP_CONF_FILE > $APP_LOGF.out 2>&1 &

aftercheck $APP_PID $!
