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
 * This class models an IR sequence, that is, an intro- or a repeat sequence.
 */
public class ir_sequence {

    private pulse_pair[] raw_data;

    public int length() {
        return raw_data.length;
    }

    public boolean nonempty() {
        return raw_data.length > 0;
    }

    public void append(pulse_pair tail[]) {
        pulse_pair new_data[] = new pulse_pair[raw_data.length + tail.length];
        for (int i = 0; i < raw_data.length; i++) {
            new_data[i] = raw_data[i];
        }
        for (int i = 0; i < tail.length; i++) {
            new_data[raw_data.length + i] = tail[i];
        }
        raw_data = new_data;
    }

    public int[] int_array() {
        int result[] = new int[2 * raw_data.length];
        for (int i = 0; i < raw_data.length; i++) {
            result[2 * i] = raw_data[i].x();
            result[2 * i + 1] = raw_data[i].y();
        }
        return result;
    }

    public ir_sequence() {
        raw_data = new pulse_pair[0];
    }

    public ir_sequence(pulse_pair data[]) {
        raw_data = data;
    }

    public void setdata(pulse_pair data[]) {
        System.err.println("ir_sequence::setdata 1");
        raw_data = data;
    }

    public void setdata(int index, pulse_pair x) {
        raw_data[index] = x;
    }

    public void appenddata(pulse_pair data[]) {
        pulse_pair newdata[] = new pulse_pair[raw_data.length + data.length];
        for (int i = 0; i < raw_data.length; i++)
            newdata[i] = raw_data[i];
        for (int i = 0; i < data.length; i++)
            newdata[raw_data.length + i] = data[i];
        raw_data = newdata;
    }
};
