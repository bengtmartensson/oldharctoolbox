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

/**
 * Type of toggle in an IR signal.
 */

public enum toggletype {

    /**
     * Generate the toggle code with toggle = 0.
     */
    toggle_0,
    /**
     * Generate the toggle code with toggle = 1.
     */
    toggle_1,

    /**
     * Don't care
     */
    dont_care;

    /**
     * Do not generate toggle codes
     */
    //no_toggle,
    /**
     * Generate toggle codes
     */
    //do_toggle;

    public static toggletype flip(toggletype t) {
        return t == toggle_0 ? toggle_1 : toggle_0;
    }

    public static int toInt(toggletype t) {
        return t == dont_care ? -1 : t.ordinal();// == toggle_1 ? 1 : 0;
    }

    public static toggletype decode_toggle(String t) {
        return t.equals("0") ? toggletype.toggle_0 : t.equals("1") ? toggletype.toggle_1 : toggletype.dont_care;
    }

    public static String format_toggle(toggletype toggle) {
        return toggle == toggletype.toggle_0 ? "0"
                : toggle == toggletype.toggle_1 ? "1" : "-";
    }
}
