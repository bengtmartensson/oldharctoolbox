#!/bin/sh

# Wrapper around main harc class

if [ -n $JAVA_HOME ] ; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=java
fi

export JAVA
HARC_HOME=/home/bengt/harc/harc
export HARC_HOME
READLINE_LIB=/usr/local/lib
if [ -w /var/run ] ; then
    rm -f /var/run/harc.pid
    echo $$ > /var/run/harc.pid
fi

exec ${JAVA} -Djava.library.path=${READLINE_LIB} -jar ${HARC_HOME}/dist/harc.jar -p ${HARC_HOME}/harc.properties.xml "$@"
