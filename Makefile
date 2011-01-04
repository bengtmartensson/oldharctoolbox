INSTALLDIR=/usr/local/harc
INSTALLBIN=/usr/local/bin

JAVA=java
MMX=/usr/local/mm/bin/mmx
VPATH=devices

MYLIRCDEVS=\
exports/analog_8ch_switch.lirc    exports/philips_37pfl9603_rc6.lirc \
exports/philips_vr1100.lirc \
exports/canon.lirc \
exports/popcorn_hour.lirc \
exports/dbox2_new.lirc \
exports/dbox2_old.lirc \
exports/denon_avr3808.lirc        exports/samsung_bdp1400.lirc \
exports/denon_avr3808_k.lirc      exports/sanyo_plv_z2000.lirc \
exports/mypowerbox.lirc \
exports/oppo_dv983.lirc           exports/t10dummy.lirc \
exports/toshiba_hd-ep30.lirc      exports/philips_37pfl9603.lirc


%.rmdu: %.xml
	$(JAVA) -classpath dist/harc.jar harc.rmdu_export -j urc_7780.xml -r config/button_rules.xml -o junk.xml $<
	$(MMX) -M misc/x2rmdu.mm -F xml -S junk.xml -F txt -T $@

lirc.conf: $(wildcard exports/*.lirc)
	cat exports/*.lirc > $@

mylirc.conf: $(MYLIRCDEVS)
	cat $(MYLIRCDEVS) > $@


install:
	-mkdir -p $(INSTALLDIR)
	-mkdir $(INSTALLDIR)/devices $(INSTALLDIR)/docs $(INSTALLDIR)/dtds $(INSTALLDIR)/protocols $(INSTALLDIR)/config
	-mkdir $(INSTALLDIR)/exports
	-mkdir -p $(INSTALLDIR)/src/harc
	cp dist/harc.jar $(INSTALLDIR)
	cp -r dist/lib $(INSTALLDIR)
	cp devices/* $(INSTALLDIR)/devices
	cp docs/* $(INSTALLDIR)/docs
	cp dtds/* $(INSTALLDIR)/dtds
	cp protocols/* $(INSTALLDIR)/protocols
	cp config/* $(INSTALLDIR)/config
	cp src/harc/* $(INSTALLDIR)/src/harc
	sed -e s:^HARC_HOME.*$$:HARC_HOME=$(INSTALLDIR): misc/harc.sh > $(INSTALLDIR)/harc.sh
	sed -e s:^HARC_HOME.*$$:HARC_HOME=$(INSTALLDIR): misc/harc-listen.sh > $(INSTALLDIR)/harc-listen.sh
	chmod +x $(INSTALLDIR)/harc.sh $(INSTALLDIR)/harc-listen.sh
	ln -sf $(INSTALLDIR)/harc.sh $(INSTALLBIN)/harc
	ln -sf $(INSTALLDIR)/harc-listen.sh $(INSTALLBIN)/harc-listen

uninstall:
	rm -rf $(INSTALLDIR)
	rm $(INSTALLBIN)/harc

dist: harc.tar.gz

harc.tar.gz: $(INSTALLDIR)
	tar -C $(INSTALLDIR)/.. -c -z -v --exclude=\*~ -f harc.tar.gz harc
