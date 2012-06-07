include paths.mk
VPATH=../config
MAXERRORS=10

all: mymacs.html

clean:
	rm -rf mymacs.html

# Generate documentation for the macro file.
%.html: %.xml macrodescrs.mm
	$(MMX) -M macrodescrs.mm -F xml -A errors $(MAXERRORS) -S $< -F sgml -T $@

%.html-b: %.html
	$(BROWSER) $<
