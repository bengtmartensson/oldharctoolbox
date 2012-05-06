# ifeq ($(HOST),godzilla)
# 	SYSTEMDIR=/usr/local/share/harc
# 	SYSTEM_REMS=/usr/local/irtrans/remotes
# 	WWWDIR=/u/www
# 	BINDIR=/usr/local/bin
# 	BROWSER=firefox
# 	MMX=/usr/local/mm/bin/mmx
# 	JAVASDK=/usr/local/jdk1.6.0
# 	JAVASDKBIN=$(JAVASDK)/bin
# 	JAVA=$(JAVASDKBIN)/java
# 	JAVAC=$(JAVASDKBIN)/javac
# 	CLASSPATH=../../..:$(TONTOJAR):$(WOLJAR)
ifeq ($(HOST),delta)
	SYSTEMDIR=/usr/local/share/harc
	SYSTEM_REMS=/usr/local/irtrans/remotes
	WWWDIR=/home/www
	BINDIR=/usr/local/bin
	BROWSER=firefox
	MMX=/usr/local/mm/bin/mmx
	JAVASDK=/usr/local/jdk1.6.0_07
	JAVASDKBIN=$(JAVASDK)/bin
	JAVA=$(JAVASDKBIN)/java
	JAVAC=$(JAVASDKBIN)/javac
	CLASSPATH=../../..:$(TONTOJAR):$(WOLJAR)
else
	BROWSER=/cygdrive/C/Program\ Files/mozilla/firefox/firefox
	MMX=/cygdrive/C/Program\ Files/Ovidius/mm4.0/bin/MMx
	SYSTEM_REMS_DIR=/cygdrive/C/Program\ Files/IRtrans/remotes
	JAVASDK=/cygdrive/C/Program\ Files/Java/jdk1.6.0_04
	JAVASDKBIN=$(JAVASDK)/bin
	JAVA=$(JAVASDKBIN)/java
	JAVAC=$(JAVASDKBIN)/javac
	JAVADOC=$(JAVASDKBIN)/javadoc
	CLASSPATH=../../..\;$(TONTOJAR)\;$(WOLJAR)
	PROJECTDIR=.
endif

TONTOJAR=../../../../tonto/bin/tonto.jar
WOLJAR=../../../wakeonlan-1.0.0/wakeonlan.jar

# My directories
DTDDIR=$(PROJECTDIR)/dtds
RTFSDIR=$(PROJECTDIR)/rtfs
DEVICESDIR=$(PROJECTDIR)/devices
PROTOCOLSDIR=$(PROJECTDIR)/protocols
TRANSFORMATIONSDIR=$(PROJECTDIR)/transformations
OUTPUTDIR=$(PROJECTDIR)/output
IRTRANS_REMS=$(OUTPUTDIR)/irtrans_rems
TESTDIR=$(PROJECTDIR)/test
IRTRANS_REMS_TEST=$(TESTDIR)/irtrans_rems
INFODIR=$(OUTPUTDIR)/infos
OUTPUTXMLDIR=$(OUTPUTDIR)/xmls
LIRC_CONFDIR=$(OUTPUTDIR)/lirc_confs
RMDUDIR=$(OUTPUTDIR)/rmdus
