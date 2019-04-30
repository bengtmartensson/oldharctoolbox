#!/bin/sh

# Installs Harctoolbox on the local machine, in a normal posix manner in PREFIX.

# This script should be run with the rights required to write
# at the desired places. I.e. root a priori not necessary.
# It should be run from the directory oldharctoolbox.

MYPROG=Harctoolbox

if [ "$1" = -u ] ; then
    UNCONDITIONAL_INSTALL_LIB=yes
    shift
else
    UNCONDITIONAL_INSTALL_LIB=no
fi

if [ $# = 1 ] ; then
    PREFIX=$1
else
    PREFIX=/usr/local
fi

MYPROG_LOWER=$(echo ${MYPROG} | tr A-Z a-z)
MYPROG_UPPER=$(echo ${MYPROG} | tr a-z A-Z)
MYPROG_HOME=${PREFIX}/share/${MYPROG_LOWER}

if [ ! -d ${MYPROG_HOME} ] ; then
    mkdir -p ${MYPROG_HOME}
else
    rm -rf ${MYPROG_HOME}/*
fi

SOURCE_DIR=`pwd`/target
cd ${MYPROG_HOME}
unzip ${SOURCE_DIR}/OldHarctoolbox-bin.zip

ln -sf ../share/${MYPROG_LOWER}/${MYPROG_LOWER}.sh ${PREFIX}/bin/${MYPROG_LOWER}

exit 0

# Determine the LIB to use
if [ $(uname -m) = 'x86_64' ] ; then
    LIB=${PREFIX}/lib64
    FROMLIB=../native/Linux-amd64
elif [ $(uname -m) = 'x86' ] ; then
    LIB=${PREFIX}/lib
    FROMLIB=../native/Linux-i386
else
    echo "Not programmed for this machine ($(uname -m)) yet, please extend me."
    exit 1
fi

# Check that we are in grouo "dialout", complain otherwise
id -Gn | grep -q dialout  || {
    if [ "$EUID" != "0" ]; then
        echo "Warning: current user $USER not in group dialout"
        echo "run e.g. 'sudo usermod -aG dialout $USER' to fix"
    fi
}

# Check that we are in grouo "lock", complain otherwise
id -Gn | grep -q lock  || {
    if [ "$EUID" != "0" ]; then
        echo "Warning: current user $USER not in group lock"
        echo "run e.g. 'sudo usermod -aG lock $USER' to fix"
    fi
}

# Check that we are in grouo "lirc", complain otherwise
id -Gn | grep -q lirc  || {
    if [ "$EUID" != "0" ]; then
        echo "Warning: current user $USER not in group lirc"
        echo "run e.g. 'sudo usermod -aG lirc $USER' to fix"
    fi
}

# Copy stuff to MYPROG_HOME
install -d ${MYPROG_HOME}
install --mode=444 target/${MYPROG}-jar-with-dependencies.jar ${MYPROG_HOME}
install --mode=444 target/${MYPROG}.png ${MYPROG_HOME}/${MYPROG_LOWER}.png
install --mode=444 target/*.xml target/*.ini ${MYPROG_HOME}
install -d ${MYPROG_HOME}/exportformats.d
install --mode=444 target/exportformats.d/* ${MYPROG_HOME}/exportformats.d
install -d ${MYPROG_HOME}/contributed
install --mode=444 target/contributed/* ${MYPROG_HOME}/contributed
install -d ${MYPROG_HOME}/contributed/import
install --mode=666 target/contributed/import/*.sh ${MYPROG_HOME}/contributed/import
install --mode=444 target/contributed/import/*.xsl ${MYPROG_HOME}/contributed/import
install --mode=444 target/doc/README* target/doc/LICENSE* ${MYPROG_HOME}

# Create trivial wrappers
echo "#!/bin/sh"                                                        >  ${MYPROG_HOME}/${MYPROG_LOWER}.sh
echo 'IRSCRUTINIZERHOME="$(dirname -- "$(readlink -f -- "${0}")" )"'    >> ${MYPROG_HOME}/${MYPROG_LOWER}.sh
echo "java -Djava.library.path=${LIB} -jar \"\${IRSCRUTINIZERHOME}/${MYPROG}-jar-with-dependencies.jar\" --apphome \"\${IRSCRUTINIZERHOME}\" \"\$@\"" >> ${MYPROG_HOME}/${MYPROG_LOWER}.sh
chmod 555 ${MYPROG_HOME}/${MYPROG_LOWER}.sh
mkdir -p ${PREFIX}/bin
ln -sf ../share/${MYPROG_LOWER}/${MYPROG_LOWER}.sh ${PREFIX}/bin/${MYPROG_LOWER}

WRAPPER=${MYPROG_HOME}/irptransmogrifier.sh
echo "#!/bin/sh" > ${WRAPPER}
echo "java -cp \"${MYPROG_HOME}/${MYPROG}-jar-with-dependencies.jar\" org.harctoolbox.irp.IrpTransmogrifier \"\$@\"" >> ${WRAPPER}
chmod 555 ${WRAPPER}
ln -sf ../share/${MYPROG_LOWER}/irptransmogrifier.sh ${PREFIX}/bin/irptransmogrifier

# Install documentation
install -d ${PREFIX}/share/doc/${MYPROG_LOWER}
install --mode=444 target/doc/* ${PREFIX}/share/doc/${MYPROG_LOWER}
ln -sf ../doc/${MYPROG_LOWER} ${MYPROG_HOME}/doc

# Install schemas
install -d ${PREFIX}/share/xml/harctoolbox
install --mode=444 ../schemas/*.xsd ${PREFIX}/share/xml/harctoolbox
ln -sf ../xml/harctoolbox ${MYPROG_HOME}/schemas

# Install devslashlirc
if [ -e ${FROMLIB}/libdevslashlirc.so ] ; then
    install -d ${LIB}
    install --mode=444 ${FROMLIB}/libdevslashlirc.* ${LIB}
fi

# Install RXTX serial
if [ $(uname -m) = 'x86_64' -a -f /usr/lib64/rxtx/librxtxSerial.so -a $UNCONDITIONAL_INSTALL_LIB = 'no' ] ; then
    echo "System librxtxSerial.so found, linking to that instead of installing ours."
    ln -sf /usr/lib64/rxtx/librxtxSerial.so ${LIB}
elif [ $(uname -m) = 'x86' -a -f /usr/lib/rxtx/librxtxSerial.so -a $UNCONDITIONAL_INSTALL_LIB = 'no' ] ; then
    echo "System librxtxSerial.so found, linking to that instead of installing ours."
    ln -sf /usr/lib/rxtx/librxtxSerial.so ${LIB}
else
    echo "System librxtxSerial.so not found or not wanted, installing ours."
    install --mode=444 ${FROMLIB}/librxtxSerial* ${LIB}
fi

# Install desktop file
install -d ${PREFIX}/share/applications
sed -e "s|Exec=.*|Exec=/bin/sh \"${PREFIX}/bin/${MYPROG_LOWER}\"|" \
    -e "s|Icon=.*|Icon=${MYPROG_HOME}/${MYPROG_LOWER}.png|" target/${MYPROG_LOWER}.desktop \
  > ${PREFIX}/share/applications/${MYPROG_LOWER}.desktop

# Install mime type file for girr
install --mode 444 target/maven-shared-archive-resources/girr.xml ${PREFIX}/share/applications

echo "Consider deleting old properties with the command "
echo "irscrutinizer --nuke-properties"
