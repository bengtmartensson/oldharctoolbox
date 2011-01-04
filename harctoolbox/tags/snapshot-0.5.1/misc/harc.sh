#!/bin/sh

# Wrapper around main harc class

HARC_HOME=/home/bengt/harc/harc
export HARC_HOME
READLINE_LIB=/usr/local/lib

java -Djava.library.path=${READLINE_LIB} -jar ${HARC_HOME}/harc.jar -p ${HARC_HOME}/harcprops.xml "$@"
