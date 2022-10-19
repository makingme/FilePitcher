#!/bin/bash
################################################################################
#	This script set information such as Os, Environment, Database etc.
################################################################################

. ./shfunc.sh

#===============================================================================
#	OS						(FIX-IT)
#===============================================================================
#----- HPUX / AIX / OSX / LINUX / WIN
export APP_OS=LINUX
export LANG=ko_KR.utf-8

#===============================================================================
#	JAVA					(FIX-IT)
#===============================================================================
export APP_JAVA=java

#===============================================================================
#	APP VARIABLES		(FIX-IT)
#===============================================================================
export APP_HOME=/home/lab/ums-3.0/file-pitcher

export APP_CONF=$APP_HOME/conf
export APP_CONF_FILE=$APP_CONF/config.json
exprot LOGBACK=-Dlogback.configurationFile=$APP_CONF/logback.xml

export APP_JAR=$APP_HOME/target/ums-file-pitcher-1.0.0.jar

export APP_LOG=$APP_HOME/logs
export ENV_NAME=production

#===============================================================================
#	CLASS PATH			(FIX-IT)
#===============================================================================
export APP_EXT_JAR=`findDir $APP_HOME/target/lib/`
export APP_IN_JAR=`findDir $APP_HOME/target/`

export CLASSPATH=$APP_HOME/conf:$APP_IN_JAR:$APP_EXT_JAR:$CLASSPATH
export PATH=.:$PATH

#===============================================================================
#	HOST CONFIG			(FIX-IT)
#===============================================================================
export APP_HOST=`hostname`
echo $APP_HOST

#===============================================================================
#	SHELL CONFIG		(FIX-IT)
#===============================================================================
if [ "$APP_OS" = "HPUX" ]
then
#----- HPUX
#	export SHLIB_PATH=$APP_HOME/lib:/opt/java1.5/jre/lib/PA_RISC2.0/server:$SHLIB_PATH
	export UMS_JVMOPT="-XX:+UseGetTimeOfDay -XX:+UseHighResolutionTimer -Xeprof:off"
	export UMS_PS_OPT="-efx"   
elif [ "$APP_OS" = "AIX" ]
then
#----- AIX

#	export LIBPATH=$APP_HOME/lib:$LIBPATH
	export UMS_JVMOPT=
	export UMS_PS_OPT=-ef
elif [ "$APP_OS" = "LINUX" ]
then
#----- LINUX
#	export LD_LIBRARY_PATH=$APP_HOME/lib:/opt/jdk1.8.0_91/jre/lib/amd64/server:$LD_LIBRARY_PATH:/app/kms
	export UMS_PS_OPT=-ef
	export UMS_JVMOPT="-XX:+UseParallelGC -Xms250m -Xmx250m"
elif [ "$APP_OS" = "OSX" ]
then
#----- OSX
#	export LD_LIBRARY_PATH=$APP_HOME/lib
#	export UMS_PS_OPT=-Djava.library.path=$APP_HOME/lib
	export UMS_PS_OPT=-ex
	export UMS_ECHO_OPT=" -e"
fi
