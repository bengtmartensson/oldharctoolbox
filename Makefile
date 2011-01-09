# This Makefile is used just for some private target for building
# distributions and updating www servers. (I simply am faster with
# make than with ant...) It is probably not of interest for anyone
# else.

VERSION=0.6.0
INSTALLDIR=/usr/local/harctoolbox
BINDIR=/usr/local/bin
SVNURL=svn://localhost/harctoolbox/trunk
WWW_ROOT=../www.harctoolbox.org/src/content/xdocs

SRCDISTFILE=harctoolbox-$(VERSION).tar.gz

/tmp/harctoolbox-$(VERSION):
	rm -rf $@
	svn checkout $(SVNURL) $@

$(SRCDISTFILE): /tmp/harctoolbox-$(VERSION)
	tar zcf $@ --exclude=.svn -C /tmp $(notdir $<)

dist: $(SRCDISTFILE)

# harctoolbox runs fine "inplace" (just for example execute
# misc/harctoolbox.sh). This target installs a minimal version in
# INSTALLDIR, with a executable link from BINDIR.

install: dist/harctoolbox.jar
	-mkdir -p $(INSTALLDIR)
	install -m 444 COPYING NEWS $(INSTALLDIR)
	-mkdir -p $(INSTALLDIR)/config
	@if [ -f $(INSTALLDIR)/config/home.xml ] ; then \
		echo "$(INSTALLDIR)/config/home.xml already existing, not overwritten"; \
	else \
		install -m 644 config/home.xml $(INSTALLDIR)/config; \
	fi
	install -m 644 config/button_rules.xml $(INSTALLDIR)/config
	-mkdir -p $(INSTALLDIR)/devices
	install -m 444 devices/* $(INSTALLDIR)/devices
	-mkdir -p $(INSTALLDIR)/dist/lib
	install -m 444 dist/harctoolbox.jar $(INSTALLDIR)/dist
	install -m 444 dist/lib/*.jar $(INSTALLDIR)/dist/lib
	-mkdir $(INSTALLDIR)/dtds
	install -m 444 dtds/* $(INSTALLDIR)/dtds
	sed -e s:^HARCTOOLBOX_HOME.*$$:HARCTOOLBOX_HOME=$(INSTALLDIR): misc/harctoolbox.sh > $(INSTALLDIR)/harctoolbox.sh
	chmod +x $(INSTALLDIR)/harctoolbox.sh
	ln -sf $(INSTALLDIR)/harctoolbox.sh $(BINDIR)/harctoolbox
	-mkdir $(INSTALLDIR)/protocols
	install -m 444 protocols/* $(INSTALLDIR)/protocols
	-mkdir $(INSTALLDIR)/pythonlib
	install -m 444 pythonlib/harcinit.py $(INSTALLDIR)/pythonlib
	@if [ -f $(INSTALLDIR)/pythonlib/harcmacros.py ] ; then \
		echo "$(INSTALLDIR)/pythonlib/harcmacros.py already existing, not overwritten"; \
	else \
		install -m 444 pythonlib/harcmacros.py $(INSTALLDIR)/pythonlib; \
	fi
	-mkdir -p $(INSTALLDIR)/src/org/harctoolbox
	install -m 444 src/org/harctoolbox/commandnames.xml $(INSTALLDIR)/src/org/harctoolbox/commandnames.xml

uninstall:
	rm -rf $(INSTALLDIR)
	rm -f $(BINDIR)/harctoolbox

dist/javadoc:
	ant javadoc

install_www: dist/javadoc $(SRCDISTFILE)
	install -m 444 dtds/*.dtd $(WWW_ROOT)/dtds
	cp -r $< $(WWW_ROOT)
	rm -rf $(WWW_ROOT)/devices
	cp -a devices $(WWW_ROOT)
	rm -rf $(WWW_ROOT)/protocols
	cp -a protocols $(WWW_ROOT)
	install -m 444 dist/harctoolbox.jar $(WWW_ROOT)/downloads
	cp -a dist/lib $(WWW_ROOT)/downloads
	install -m 444 $(SRCDISTFILE) $(WWW_ROOT)/downloads
