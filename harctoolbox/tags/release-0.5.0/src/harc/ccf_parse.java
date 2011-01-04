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

/**
 * This is a class for constructing ir_codes from a String array containing a signal in CCF form.
 */
public class ccf_parse extends raw_ir {

    public ccf_parse(String[] ccf_code, String command) {
        super(Integer.parseInt(ccf_code[1], 16), command);

        int i = 0;
        if (Integer.parseInt(ccf_code[i++], 16) != 0) {
            System.err.println("Can only handle ccf codes of type 0");
            System.exit(1);
        }
        int freq_code = Integer.parseInt(ccf_code[i++], 16);
        int intro_length = Integer.parseInt(ccf_code[i++], 16);
        int intro[] = new int[2 * intro_length];
        int repeat_length = Integer.parseInt(ccf_code[i++], 16);
        int repeat[] = new int[2 * repeat_length];

        for (int j = 0; j < 2 * intro_length; j++) {
            intro[j] = Integer.parseInt(ccf_code[i++], 16);
        }

        set_intro_sequence(intro);

        for (int j = 0; j < 2 * repeat_length; j++) {
            repeat[j] = Integer.parseInt(ccf_code[i++], 16);
        }

        set_repeat_sequence(repeat);
    }

    public ccf_parse(String[] ccf_code) {
        this(ccf_code, "unknown");
    }

    public ccf_parse(String code, String command) {
        this(code.split("[ \n\t]+"), command);
    }

    public ccf_parse(String code) {
        this(code.split("[ \n\t]+"), "unknown");
    }

    public static int get_frequency(String ccf) {
        if (ccf == null || ccf.isEmpty())
            return 0;

        String[] arr = ccf.split("[ \n\t]+");
        return ir_code.get_frequency(Integer.parseInt(arr[1], 16));
    }

    public static int get_gap(String ccf) {
        if (ccf == null || ccf.isEmpty())
            return 0;
        
        String[] arr = ccf.split("[ \t\n]+");
        return (int) ((ir_code.get_pulse_time(Integer.parseInt(arr[1], 16)) * Integer.parseInt(arr[arr.length - 1], 16)));
    }
}
