#!/bin/sh

HARC_HOME=/home/bengt/harc/harc
export HARC_HOME
READLINE_LIB=/usr/local/lib

java -Djava.library.path=${READLINE_LIB} -classpath ${HARC_HOME}/harc.jar harc/irtrans -v ${HARC_HOME}/config/listen.xml 
