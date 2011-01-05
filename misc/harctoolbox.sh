#!/bin/sh

# Wrapper around main harc class

if [ -n $JAVA_HOME ] ; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=java
fi

export JAVA
HARCTOOLBOX_HOME=/home/bengt/harc/harc
export HARCTOOLBOX_HOME
READLINE_LIB=/usr/local/lib
if [ -w /var/run ] ; then
    rm -f /var/run/harctoolbox.pid
    echo $$ > /var/run/harctoolbox.pid
fi

exec ${JAVA} -Djava.library.path=${READLINE_LIB} -jar ${HARCTOOLBOX_HOME}/dist/harctoolbox.jar -p ${HARCTOOLBOX_HOME}/harctoolbox.properties.xml "$@"
