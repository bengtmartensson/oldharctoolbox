#!/bin/sh

# Wrapper around main harctoolbox class

# Change the following lines to fit your needs
HARCTOOLBOX_HOME=/home/bengt/harc/harctoolbox
READLINE_LIB=/usr/local/lib

if [ -n $JAVA_HOME ] ; then
    JAVA=$JAVA_HOME/bin/java
else
    JAVA=java
fi

export JAVA
export HARCTOOLBOX_HOME
if [ -w /var/run ] ; then
    rm -f /var/run/harctoolbox.pid
    echo $$ > /var/run/harctoolbox.pid
fi

exec ${JAVA} -Djava.library.path=${READLINE_LIB} -jar ${HARCTOOLBOX_HOME}/dist/harctoolbox.jar -p ${HARCTOOLBOX_HOME}/harctoolbox.properties.xml "$@"
