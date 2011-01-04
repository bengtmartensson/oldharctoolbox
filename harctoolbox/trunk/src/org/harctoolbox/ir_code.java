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
 * This is the basic abstract class from which all classes describing IR signals
 * are derived.
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
public abstract class ir_code {

    /**
     * Manufacturer of the device to be controlled, for example "Philips".
     */
    public static final String vendor = "no-vendor";
    /**
     * Device to be controlled, for example "TV"
     */
    public static final String device_name = "no-device";

    // Using short for device_no and command_no has nothing to do with
    // efficiency; it is to have the compiler check for types and to
    // prevent confusion of command_no with the numbers in
    // ir_commands.
    /**
     * Device number, in the sense of, e.g., RC5
     */
    protected short device_no;
    /**
     * Device number, in the sense of, e.g., RC5
     */
    protected short subdevice_no;
    /**
     * Command name, for example "Channel Up".
     */
    protected String command_name = "";
    /**
     * Command number, in the sense of, e.g., RC5.
     * Not to be confused with the numbers in ir_commands (short vs. int).
     */
    protected short command_no;
    /**
     * Type in the sense of CCF.
     */
    public final static int ccf_type_learned = 0;
    public final static int ccf_type_rc5 = 0x5000;
    public final static int ccf_type_rc6 = 0x6000;
    public final static int ccf_type_nec1 = 0x900a;
    /**
     * Frequency of the carrier, in the Pronto coding. Every sensible
     * subclass must override.
     */
    private int carrier_frequency_code = 0;
    
    /** Sent as the first sequence, only once. */
    protected ir_sequence intro_sequence;

    /** Sent repetedly if signal is sent repeatedly */
    protected ir_sequence repeat_sequence;
    /**
     * Name of current package.
     */
    public static final String package_name = "harc";

    private final static double pronto_constant = 0.241246;

    //public static int lirc_bits = 0; // to be overridden
    //public static int lirc_eps = 30; // LIRC default
    //public static int lirc_aeps = 100; // LIRC default
    //public static int[] lirc_header = null;
    //public static int[] lirc_one = null;
    //public static int[] lirc_zero = null;
    //public static int lirc_ptrail = 0;
    //public static int lirc_pre_data_bits = 0;
    //public static int lirc_pre_data = -1;
    //public static int lirc_gap = 0;
    //public static int lirc_repeat_bit = -1;
    //public static int lirc_toggle_bit = -1;
    /**
     * Format an integer the Pronto way.
     */
    protected static String ccf_integer(int n) {
        return String.format("%04x", n);
    }

    protected static String ccf_integer(short n, short m) {
        return ccf_integer(256*n  + m);
    }

    /**
     * Returns the carrier frequency in Hz.
     */
    public int get_frequency() {
        return get_frequency(carrier_frequency_code);
    }

    /**
     * Returns frequency code from frequency in Hz.
     */
    public static int get_frequency_code(int f) {
        return f == 0
                ? -1 // Invalid value
                : (int) (1000000.0 / ((double) f * pronto_constant));
    }

    /**
     * Returns the carrier frequency in Hz.
     */
    public static int get_frequency(int code) {
        return code == 0
                ? -1 // Invalid value
                : (int) (1000000.0 / ((double) code * pronto_constant));
    }

    public static double get_pulse_time(int code) { // in microseconds
        return code == 0
                ? -1 // Invalid value
                : code * pronto_constant;
    }

    /**
     *
     * @return Pulse time in microseconds.
     */
    public double get_pulse_time() {
        return get_pulse_time(carrier_frequency_code);
    }

    protected void set_frequency(int f) {
        carrier_frequency_code = get_frequency_code(f);
    }

    public static int usec(int n, int carrier_frequency_code) {
        double T = ir_code.get_pulse_time(carrier_frequency_code);
        return ((int) (n * T + 4) / 8) * 8;
    }

    public static int msec(int n, int carrier_frequency_code) {
        double T = ir_code.get_pulse_time(carrier_frequency_code);
        return (int) (((n * T + 4) + 500) / 1000);
    }
    
    public String get_description() {
        String vendr = "";
        String dev = "";
        try {
            vendr = getClass().getField("vendor").get(this).toString();
            dev = getClass().getField("device_name").get(this).toString();
        } catch (java.lang.NoSuchFieldException e) {
            System.err.println(e.getMessage());
        } catch (java.lang.IllegalAccessException e) {
            System.err.println(e.getMessage());
        }
        return vendr + " " + dev + " " + command_name;
    }

    public void print() {
        System.out.println(get_description());
        System.out.println(ccf_string());
    }

    public boolean is_valid () {
        return true;
    }
    /**
     * Is this code using a toggle bit?
     */
    public static final boolean has_toggle = false;

    /**
     * Returns our command code for the argument.
     */
    //public static int decode_command(String cmd) {
    //    String c = /*(cmd.length() >= 4 && cmd.substring(0, 4).equals("cmd_"))
    //            ? cmd.substring(4) :*/ cmd;
    //    for (int i = 0; i < commandnames.command_name_table.length; i++) {
    //        if (commandnames.command_name_table[i].equals(c)) {
    //            return i;
    //        }
    //    }
    //    return commandnames.cmd_invalid;
    //}
    public static int[] array_msb(int x, int no_bytes) {
        int result[] = new int[no_bytes];
        for (int i = no_bytes - 1; i >= 0; i--) {
            result[i] = x % 2;
            x = x / 2;
        }
        return result;
    }

    public static int[] array_lsb(int x, int no_bytes) {
        int result[] = new int[no_bytes];
        for (int i = 0; i < no_bytes; i++) {
            result[i] = x % 2;
            x = x / 2;
        }
        return result;
    }

    public static int[] invert_bitarray(int[] a) {
        int out[] = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = 1 - a[i];
        }
        return out;
    }

    protected static int flip8(int x) {
        int result = 0;
        int mask = 0x1;
        for (int i = 0; i < 8; i++) {
            int b = ((x & mask) >> i);
            result |= b << 7 - i;
            mask = mask << 1;
        }
        return result;
    }

    public static int[] parse_ccf(String ccf) {
        String[] s = ccf.split("[ \t\n]+");
        int[] out = new int[s.length];
        try {
            for (int i = 0; i < out.length; i++) {
                out[i] = Integer.parseInt(s[i], 16);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return out;
    }

    public static short parse_shortnumber(String s) throws NumberFormatException {
        return s.startsWith("0x") ? Short.parseShort(s.substring(2), 16) : Short.parseShort(s);
    }

    // Override when using different ir_sequences
    protected int[] get_intro_array() {
        return intro_sequence.int_array();
    }

    public int[] get_repeat_array() {
        return repeat_sequence.int_array();
    }

    /** Returns the signal content as an array of integers the CCF way. */
    public int[] raw_ccf_array() {
        int[] intro = get_intro_array();
        int[] repeat = get_repeat_array();

        int[] result = new int[4 + intro.length + repeat.length];
        int indx = 0;
        result[indx++] = ccf_type_learned;
        result[indx++] = carrier_frequency_code;
        result[indx++] = intro.length / 2;
        result[indx++] = repeat.length / 2;
        for (int i = 0; i < intro.length; i++) {
            result[indx++] = intro[i];
        }
        for (int i = 0; i < repeat.length; i++) {
            result[indx++] = repeat[i];
        }

        return result;
    }
    ;

    /** Returns the singal content as an CCF string. */
    public String raw_ccf_string() {
        int[] intro = get_intro_array();
        int[] repeat = get_repeat_array();

        return ccf_integer(ccf_type_learned) + " " + ccf_integer(carrier_frequency_code) + " " + ccf_integer(intro.length / 2) + " " + ccf_integer(repeat.length / 2) +
                ir_code.ccf_joiner(intro) +
                ir_code.ccf_joiner(repeat);
    }

    public int get_gap() {
        int[] s = raw_ccf_array();
        return (int)(get_pulse_time() * s[s.length-1]);
    }

    /**
     * Return a CCF string, cooked if possible (by overriding in rc5 etc).
     */
    public String ccf_string() {
        return raw_ccf_string();
    }

    public String cooked_ccf_string() {
        return null;
    }

    public static String ccf_joiner(int[] array) {
        String result = "";
        for (int i = 0; i < array.length; i++) {
            result = result + " " + ccf_integer(array[i]);
        }
        return result;
    }

    public ir_code() {
        intro_sequence = new ir_sequence();
        repeat_sequence = new ir_sequence();
    }

    public ir_code(int frequency_code,
            pulse_pair intro[], pulse_pair repeat[],
            String command_name) {
        this.command_name = command_name;
        carrier_frequency_code = frequency_code;
        intro_sequence = new ir_sequence(intro);
        repeat_sequence = new ir_sequence(repeat);
    }

    public ir_code(int freq_code, String command_name) {
        this(freq_code, new pulse_pair[0], new pulse_pair[0], command_name);
    }
}
