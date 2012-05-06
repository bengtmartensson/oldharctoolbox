/*
Copyright (C) 2009 Bengt Martensson.

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

package org.harctoolbox;

/**
 * Type of command, like infrared or serial.
 */
public enum commandtype_t {

    /** Infrared command */
    ir,

    /** RF command */
    rf,

    /** Invoking the browser */
    www,

    /** WEB API commands */
    web_api,

    /** Commands talking to TCP sockets */
    tcp,

    /** Commands talking to UDP sockets */
    udp,

    serial,

    bluetooth,

    /** Just on or off */
    on_off,

    /** "IP" commands, for now ping and WOL (wakeup-on-lan) */
    ip,

    /** Special commands, requiring some software support */
    special,

    /** Telnet */
    telnet,

    /** Sensor input */
    sensor,

    /** Any of the types, except for invalid */
    any,

    /** Denotes invalid selection */
    invalid;

    /** Determines if a string describes a valid command type */
    public static boolean is_valid(String s) {
        try {
            return commandtype_t.valueOf(s) != invalid;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean is_compatible(commandtype_t required) {
        return required == any || this == required;
    }

    /**
     * Returns nicely formatted string describing supported command types.
     *
     * @param sep Separator character to use.
     * @return Nice string for printing.
     */
    public static String valid_types(char sep) {
        commandtype_t[] vals = commandtype_t.values();
        String res = vals[0].toString();
        for (int i = 1; i < vals.length - 2; i++) {
            res = res + sep + vals[i];
        }
        return res;
    }
    
    /*public static void main(String[] argv) {
        System.out.println(telnet.is_compatible(commandtype_t.ir));
        System.out.println(telnet.is_compatible(commandtype_t.any));
        System.out.println(telnet.is_compatible(commandtype_t.telnet));
    }*/
}
