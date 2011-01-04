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

public class pulse_pair {

    private int first;
    private int second;

    private boolean inverted; // If true, first is a zero, second a one.

    public pulse_pair(int x, int y, boolean inverted) {
        first = x;
        second = y;
        this.inverted = inverted;
    }

    public pulse_pair(int x, int y) {
        this(x, y, false);
    }

    public pulse_pair() {
        first = 0;
        second = 0;
    }

    public boolean is_inverted() {
        return inverted;
    }

    public int first_value() {
        return inverted ? 0 : 1;
    }

    public int second_value() {
        return inverted ? 1 : 0;
    }

    public boolean is_zero() {
        return (first == 0) && (second == 0);
    }

    public int x() {
        return first;
    }

    public int y() {
        return second;
    }

    @Override
    public String toString() {
        return Integer.toString(first) + " " + Integer.toString(second)
                + (inverted ? "(inverted)" : "");
    }

    public String ccf_string(double unit) {
        return ir_code.ccf_integer((int) ((double) first * unit)) + " " + ir_code.ccf_integer((int) ((double) second * unit));
    }
}
