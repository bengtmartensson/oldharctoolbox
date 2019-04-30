#!/bin/sh

# Wrapper around main harctoolbox class

# Change the following lines to fit your needs
#JAVA_HOME=/opt/jdk1.6.0_25
HARCTOOLBOX_HOME="$(dirname -- "$(readlink -f -- "${0}")" )"
READLINE_LIB=/usr/local/lib

if [ x${JAVA_HOME}y != "xy" ] ; then
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

exec ${JAVA} -Djava.library.path=${READLINE_LIB} -jar ${HARCTOOLBOX_HOME}/OldHarctoolbox-jar-with-dependencies.jar "$@"
