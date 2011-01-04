VERSION=0.5.0
INSTALLDIR=/usr/local/harc
INSTALLBIN=/usr/local/bin

JAVA=java
MMX=/usr/local/mm/bin/mmx
VPATH=devices exports

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

RMDUS=\
urc-778x_analog_8ch_switch.rmdu \
urc-778x_denon_avr3808_denon_avr3808.rmdu \
urc-778x_ezcontrol_t10.rmdu \
urc-778x_marantz_is201.rmdu \
urc-778x_mypowerbox.rmdu \
urc-778x_nokia_dbox2_dbox2_new.rmdu \
urc-778x_nokia_dbox2_dbox2_old.rmdu \
urc-778x_oppo_dv983.rmdu \
urc-778x_panasonic_dvd_s75.rmdu \
urc-778x_philips_37pfl9603_philips_37pfl9603.rmdu \
urc-778x_philips_37pfl9603_philips_37pfl9603_rc6.rmdu \
urc-778x_philips_vr1100.rmdu \
urc-778x_pioneer_cld925.rmdu \
urc-778x_popcorn_hour_a100.rmdu \
urc-778x_rc5_cd.rmdu \
urc-778x_rc5_tape.rmdu \
urc-778x_samsung_bdp1400.rmdu \
urc-778x_sanyo_plv_z2000.rmdu \
urc-778x_toshiba_hd-ep30.rmdu \
urc-778x_yamaha_rxv1400_yamaha_rxv1400_ext.rmdu \
urc-778x_yamaha_rxv1400_yamaha_rxv1400_std.rmdu \
urc-778x_yamaha_rxv596.rmdu

rmdus: $(RMDUS)

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
	cp COPYING $(INSTALLDIR)
	cp -r dist/lib $(INSTALLDIR)
	cp -r button_remotes $(INSTALLDIR)
	cp devices/* $(INSTALLDIR)/devices
	cp docs/harchelp.html $(INSTALLDIR)/docs
	cp dtds/* $(INSTALLDIR)/dtds
	cp protocols/* $(INSTALLDIR)/protocols
	cp config/macros.xml config/home.xml config/button_rules.xml config/listen.xml $(INSTALLDIR)/config
	cp src/harc/* $(INSTALLDIR)/src/harc
	sed -e s:^HARC_HOME.*$$:HARC_HOME=$(INSTALLDIR): misc/harc.sh > $(INSTALLDIR)/harc.sh
	sed -e s:^HARC_HOME.*$$:HARC_HOME=$(INSTALLDIR): misc/harc-listen.sh > $(INSTALLDIR)/harc-listen.sh
	chmod +x $(INSTALLDIR)/harc.sh $(INSTALLDIR)/harc-listen.sh
	ln -sf $(INSTALLDIR)/harc.sh $(INSTALLBIN)/harc
	ln -sf $(INSTALLDIR)/harc-listen.sh $(INSTALLBIN)/harc-listen

uninstall:
	rm -rf $(INSTALLDIR)
	rm $(INSTALLBIN)/harc

dist: harc-$(VERSION).tar.gz

harc-$(VERSION).tar.gz: $(INSTALLDIR)
	rm -rf $(INSTALLDIR)/exports/*
	tar -C $(INSTALLDIR)/.. -c -z -v --exclude=\*~ -f $@ harc

%.rmdu: %.br transformations/br2rmdu.mm
	$(MMX) -M transformations/br2rmdu.mm -F xml -S $< -F txt -T $@
