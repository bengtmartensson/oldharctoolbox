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
 * This class is useful both for modeling IR signals with no particular structure,
 * as well as being a superclass for more elaborate classes.
 */
public class raw_ir extends ir_code {

    public static pulse_pair[] int_array2pulse_pair_array(int data[]) {
        // Simply assume data.length % 2 == 0
        pulse_pair pairs[] = new pulse_pair[data.length / 2];
        for (int i = 0; i < data.length / 2; i++) {
            pairs[i] = new pulse_pair(data[2 * i], data[2 * i + 1]);
        }
        return pairs;
    }

    public raw_ir(int frequency_code, pulse_pair intro[], pulse_pair repeat[],
            String command_name) {
        super(frequency_code, intro, repeat, command_name);
    }

    public raw_ir(int frequency_code, int intro[], int repeat[],
            String command_name) {
        super(frequency_code, int_array2pulse_pair_array(intro),
                int_array2pulse_pair_array(repeat), command_name);
    }

    public raw_ir(int freq_code, String command_name) {
        super(freq_code, command_name);
    }

    public raw_ir() {
        super();
    }

    public void set_intro_sequence(int data[]) {
        intro_sequence = new ir_sequence(int_array2pulse_pair_array(data));
    }

    public void set_repeat_sequence(int data[]) {
        repeat_sequence = new ir_sequence(int_array2pulse_pair_array(data));
    }

    public void append_repeat_sequence(int data[]) {
        repeat_sequence.append(int_array2pulse_pair_array(data));
    }
}
