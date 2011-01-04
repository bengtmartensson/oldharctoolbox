/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

/*
 * This implements the RC5 protocol for commands up to 63.
 */
public class rc5 extends biphase_ir implements commandnames, toggle_code {

    /**
     * Number of arguments considering the code as a remote contoller.
     */
    public static int no_args = 2;
    public static String remote_name = "rc5";
    public static int carrier_frequency_code = 0x73;

    // Common device numbers
    // (see also http://www.sbprojects.com/knowledge/ir/rc5.htm)
    public final static short rc5_device_tv = 0;
    public final static short rc5_device_tv2 = 1;
    public final static short rc5_device_vcr = 5;
    public final static short rc5_device_vcr2 = 6;
    public final static short rc5_device_sat = 8;
    public final static short rc5_device_ld = 12;
    public final static short rc5_device_tuner = 17;
    public final static short rc5_device_tape = 18;
    public final static short rc5_device_cd = 20;
    public final static short rc5_device_phono = 21;

    // Note: RC5 devices (Philips, Marantz) often selects inputs
    // by (devicenumber_to_be_selected, cmd_tv) 

    // The following commands appears in all devices
    public final static int[][] commands = {
        {cmd_0, 0},
        {cmd_1, 1},
        {cmd_2, 2},
        {cmd_3, 3},
        {cmd_4, 4},
        {cmd_5, 5},
        {cmd_6, 6},
        {cmd_7, 7},
        {cmd_8, 8},
        {cmd_9, 9},
        {cmd_power_toggle, 12},
        {cmd_in_tv, 63},};
    public final static int no_bytes_device = 5;
    public final static int no_bytes_command = 6; // despite the RC5x trick
    public final static short min_device = 0;
    public final static short max_device = 31;
    public final static short min_command = 0;
    public final static short max_command = 127; // 63 for pure RC5
    private int toggle;

    public static String get_rem_timings() {
        return "[0][N]0[RC]2[RP]87[FREQ]" //+ (carrier_frequency(carrier_frequency_code)+500)/1000
                + "36" + "[RC5]";
    }

    public String rem_code_string() {
        String result = "[T]0[D]" + (repeat_sequence.has_leadin() ? "S" : "");
        result = result + "1";
        for (int i = 0; i < repeat_sequence.getdatalength(); i++) {
            result = result + repeat_sequence.getdata(i);
        }

        return result;
    }
    public static int lirc_bits = no_bytes_device + no_bytes_command + 2;
    public static String lirc_flags = "RC5";
    public static int[] lirc_one = {(int) (0x20 * pulse_time(carrier_frequency_code)), (int) (0x20 * pulse_time(carrier_frequency_code))};
    public static int[] lirc_zero = {(int) (0x20 * pulse_time(carrier_frequency_code)), (int) (0x20 * pulse_time(carrier_frequency_code))};
    public static int lirc_plead = (int) (0x20 * pulse_time(carrier_frequency_code));
    public static int lirc_gap = (int) (0xcc0 * pulse_time(carrier_frequency_code)); //112320;
    public static int lirc_toggle_bit = 2;

    public String lirc_code_string() {
        return "0x" + Integer.toHexString(
                ((0x40 - (command_no & 0x40)) << 6) | (toggle == 1 ? 1 : 0) << 11 | device_no << 6 | command_no & 0x3f);
    }
    private static int class_toggle = 0;

    private void do_toggle() {
        class_toggle = 1 - class_toggle;
    }

    public rc5(short device, short command, int toggle, String name) {
        super(0x73, 32, 0, 0xcc0, name);
        toggle = toggle == 1 ? 1 : 0;
        device_no = device;
        command_no = command;
        this.toggle = toggle;
        int second_startbit[] = {1 - command / 64}; // bit 7 inverted (use 1 for pure RC5)
        repeat_sequence.setdata(second_startbit);
        repeat_sequence.appenddata(array_msb(toggle, 1));
        repeat_sequence.appenddata(array_msb(device, no_bytes_device));
        repeat_sequence.appenddata(array_msb(command, no_bytes_command));
        do_toggle();
    }

    public rc5(short device, short command, int toggle) {
        this(device, command, toggle, (new Integer(command)).toString());
    }

    public rc5(short device, short command) {
        this(device, command, class_toggle);
    }

    @Override
    public String description() {
        return "RC5 deviceno = " + device_no + ", command = " + command_no + ", toggle = " + toggle;
    }

    @Override
    public String ccf_string() {
        return cooked_ccf_string();
    }

    public String cooked_ccf_string() {
        return ccf_integer(ccf_type_rc5) + " 0073 0000 0001 " + ccf_integer(device_no) + " " + ccf_integer(command_no);
    }

    @Override
    public void print() {
        System.out.println(description());
        System.out.println(ccf_string());
        System.out.println(raw_ccf_string());
    }

    public static void print_table(short min_device, short max_device,
            short min_command, short max_command,
            int toggle) {
        for (short device = min_device; device <= max_device; device++) {
            for (short command = min_command; command <= max_command; command++) {
                if (toggle != 1) {
                    (new rc5(device, command, 0)).print();
                    System.out.println();
                }
                if (toggle != 0) {
                    (new rc5(device, command, 1)).print();
                    System.out.println();
                }
            }
        }
    }

    private static void usage() {
        System.err.println("usage:");
        System.err.println("rc5: [deviceno commandno [toggle]]");
        System.exit(1);
    }

    public static void main(String args[]) {
        if (args.length == 0)
            // Print total table (???)
            print_table(min_device, max_device, min_command, max_command, 2);
        else if (args.length == 2) // Just one code, specified toggle
            (new rc5(Short.parseShort(args[0]), Short.parseShort(args[1]))).print();
        else if (args.length == 3)
            (new rc5(Short.parseShort(args[0]), Short.parseShort(args[1]), Integer.parseInt(args[2]))).print();
        else
            usage();


// 	else if (args[0].length() >= 2 && args[0].substring(0,2).equals("gc")) {
// 	    // Send a GlobalCache command
// 	    (new rc5(Short.parseShort(args[1]), Short.parseShort(args[2]), args.length >= 4 ? Integer.parseInt(args[3]) : 0)).gc(args[0].equals("gcv"));
// 	} else if (args.length == 1) {
// 	    // Table for a device
// 	    print_table(Short.parseShort(args[0]), Short.parseShort(args[0]), 
// 			min_command, max_command, 2);
// 	} else if (args.length == 2) {
// 	    // Just one code, any toggle
// 	    (new rc5(Short.parseShort(args[0]), Short.parseShort(args[1]))).print();
// 	} else if (args.length == 3) {
// 	    // Just one code, specified toggle
// 	    (new rc5(Short.parseShort(args[0]), Short.parseShort(args[1]), Integer.parseInt(args[2]))).print();
// 	} else if (args.length == 5) {
// 	    // Print partial table
// 	    print_table(Short.parseShort(args[0]), Short.parseShort(args[1]),
// 			Short.parseShort(args[2]), Short.parseShort(args[3]),
// 			Short.parseShort(args[4]));
// 	} else
// 	    System.err.println("Nothing to do...??!!");
    }
}
