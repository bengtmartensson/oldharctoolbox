#!/bin/sh

# Installs Harctoolbox on the local machine, in a normal posix manner in PREFIX.

# This script should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.
# It should be run from the directory oldharctoolbox.

CUSTOM_DIR=../myharc
MYPROG=Harctoolbox
UNZIP="jar xf"

if [ $# = 1 ] ; then
    PREFIX=$1
else
    PREFIX=/usr/local
fi

#mvn install

MYPROG_LOWER=$(echo ${MYPROG} | tr A-Z a-z)
MYPROG_UPPER=$(echo ${MYPROG} | tr a-z A-Z)
MYPROG_HOME=${PREFIX}/share/${MYPROG_LOWER}

if [ ! -d ${MYPROG_HOME} ] ; then
    mkdir -p ${MYPROG_HOME}
else
    rm -rf ${MYPROG_HOME}/*
fi

OLD=`pwd`
SOURCE_DIR=${OLD}/target
cd ${MYPROG_HOME}
${UNZIP} ${SOURCE_DIR}/OldHarctoolbox-bin.zip

ln -sf ../share/${MYPROG_LOWER}/${MYPROG_LOWER}.sh ${PREFIX}/bin/${MYPROG_LOWER}
chmod +x ${MYPROG_HOME}/${MYPROG_LOWER}.sh ${MYPROG_HOME}/${MYPROG_LOWER}d

cd ${OLD}
cp target/OldHarctoolbox-jar-with-dependencies.jar ${MYPROG_HOME}

if [ -f ${CUSTOM_DIR}/config/home.xml ] ; then
    cp ${CUSTOM_DIR}/config/home.xml ${MYPROG_HOME}
fi

if [ -f ${CUSTOM_DIR}/config/tasks.xml ] ; then
    cp ${CUSTOM_DIR}/config/tasks.xml ${MYPROG_HOME}
fi

if [ -f ${CUSTOM_DIR}/python/harcmacros.py ] ; then
    cp ${CUSTOM_DIR}/python/harcmacros.py ${MYPROG_HOME}/python
fi
