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

public enum connectiontype {

    analog,
    s_video,
    yuv,
    spdif, // No not distinguish between optical and coax here
    hdmi,
    cvbs,
    rgb,
    denon_link,
    external_analog,// 6 or 8 analog RCA plugs
    other,
    invalid,
    any;

    public static connectiontype parse(String s) {
        connectiontype ct = invalid;
        try {
            ct = valueOf(s);
        } catch (IllegalArgumentException e) {
        }
        return ct;
    }

    boolean is_ok(connectiontype requested) {
        return requested == null || requested == any || equals(requested);
    }
}
