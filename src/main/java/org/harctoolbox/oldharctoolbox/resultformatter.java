/*
Copyright (C) 2009-2011 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.oldharctoolbox;

import java.util.GregorianCalendar;

/**
 *
 */
public class resultformatter {

    private String format;

    public resultformatter(String format) {
        this.format = format;
    }

    public resultformatter() {
        this(harcprops.get_instance().get_resultformat());
    }

    public String format(String str) {
        GregorianCalendar c = new GregorianCalendar();
        String s = "";
        try {
            s = String.format(format, str, c);
        } catch (/*UnknownFormatConversion*/Exception e) {
            System.err.println("Erroneous format string `" + format + "'.");
        }
        return s;
    }

    // Just for testing...
    public static void main(String[] args) {
        resultformatter formatter = new resultformatter(args[0]);
        System.out.println(formatter.format(args[1]));
    }
}
