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

package harc;

import java.util.*;

/**
 *
 */
public class jp1protocoldata {

    /** Functions for turning hex into OBC (in JP1 terminology) */
    public enum tohex_function {
        id,
        reverse,
        complement,
        jp1_00e8,
        reverse_complement
    };

    // assume bits = 8 for now...
    public short obc2hex(short obc) {
        return
               function == tohex_function.reverse_complement ? (short)((255 - (Integer.reverse((int)obc) >> 24)) & 255)
             : function == tohex_function.reverse ? (short) ((Integer.reverse((int)obc) >> 24) & 255)
             : function == tohex_function.jp1_00e8 ? (short)((((255-obc)<<2) & 255) + ((obc>>6)&1))
             : obc;
    }

    public short obc2hex(int obc) {
        return obc2hex((short) obc);
    }

    public short hex2obc(short hex) {
        return function == tohex_function.jp1_00e8 ? (short)(63 - (hex>>2)) : obc2hex(hex);
    }

    private tohex_function function;
    private int bits;
    private int protocol_number;
    private String protocol_name;
    private Hashtable<String,String> cpu_code;

    public jp1protocoldata(String protocol_name, tohex_function function, int bits, int protocol_number) {
        this.protocol_name = protocol_name;
        this.function = function;
        this.bits = bits;
        this.protocol_number = protocol_number;
        cpu_code = new Hashtable<String,String>();
    }

    public void add_code(String cpu, String code) {
        cpu_code.put(cpu, code);
    }

    public String get_cpu_code(String cpu) {
        return cpu_code.get(cpu);
    }

    public String get_protocol_name() {
        return protocol_name;
    }

    public int get_protocol_number() {
        return protocol_number;
    }
}
