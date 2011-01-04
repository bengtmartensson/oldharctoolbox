package harc;

/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

/**
 * Three types of remotes: 1. one parameter. These have
 * get_commands(), and are addressed by a string like "volume+" 2. two
 * parameter. These have max_devices, and max_commands, and are
 * addressed by two shorts, as device and command to be
 * interpreted. 3. Three parameters, interpreted as house, device, and command.
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

    /**
     * Frequency of the carrier, in the Pronto coding. Every sensible
     * subclass must override.
     */
    public int carrier_frequency_code = 0;
    protected ir_sequence intro_sequence;
    protected ir_sequence repeat_sequence;

    /**
     * Name of current package.
     */
    public static final String package_name = "harc";

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
	String s = Integer.toHexString(n);
	while (s.length() < 4)
	    s = "0" + s;

	return(s);
    }

    /**
     * Returns the carrier frequency in Hz.
     */
    public int carrier_frequency() {
	return carrier_frequency(carrier_frequency_code);
    }

    /**
     * Returns frequency code from frequency in Hz.
     */
    public static int carrier_frequency_code(int f) {
	return f == 0 
	    ? -1 // Invalid value
	    : (int) (1000000.0/((double)f * 0.241246));
    }

    /**
     * Returns the carrier frequency in Hz.
     */
    public static int carrier_frequency(int code) {
	return code == 0 
	    ? -1 // Invalid value
	    : (int) (1000000.0/((double)code * 0.241246));
    }

    public static double pulse_time(int code) { // in microseconds
	return code == 0 
	    ? -1 // Invalid value
	    : code * 0.241246;
    }

    public String description() {
	String vendr = "";
	String dev = "";
	try {
	    vendr = getClass().getField("vendor").get(this).toString();
	    dev   = getClass().getField("device_name").get(this).toString();
	} catch (java.lang.NoSuchFieldException e) {
	    System.err.println(e.getMessage());
	} catch (java.lang.IllegalAccessException e) {
	    System.err.println(e.getMessage());
	}
	return vendr + " " + dev + " " + command_name;
    }

    public void print() {
	System.out.println(description());
	System.out.println(ccf_string());
    }

   /**
     * Is this code using a toggle bit?
     */
    public static final boolean has_toggle = false;

    /**
     * Returns our command code for the argument.
     */
    public static int decode_command(String cmd) {
        String c = /*(cmd.length() >= 4 && cmd.substring(0, 4).equals("cmd_"))
                ? cmd.substring(4) :*/ cmd;
        for (int i = 0; i < commandnames.command_name_table.length; i++) {
            if (commandnames.command_name_table[i].equals(c)) {
                return i;
            }
        }
        return commandnames.cmd_invalid;
    }

    public static int [] array_msb(int x, int no_bytes) {
	int result[] = new int[no_bytes];
	for (int i = no_bytes-1; i >= 0; i--) {
	    result[i] = x % 2;
	    x = x / 2;
	}
	return result;
    }

    public static int [] array_lsb(int x, int no_bytes) {
	int result[] = new int[no_bytes];
	for (int i = 0; i < no_bytes; i++) {
	    result[i] = x % 2;
	    x = x / 2;
	}
	return result;
    }    

    public static int [] invert_bitarray(int[] a) {
	int out[] = new int[a.length];
	for (int i = 0; i < a.length; i++) 
	    out[i] = 1-a[i];
	return out;
    }

    protected static int flip8(int x) {
	int result = 0;
	int mask = 0x1;
	for (int i = 0; i < 8; i++) {
	    int b = ((x & mask) >> i);
	    result |= b << 7-i;
	    mask = mask << 1;
	}
	return result;
    }

    public static short get_command_code_simple(int cmd, int commands[][]) {
	for (int i = 0; i < commands.length; i++)
	    if (commands[i][0] == cmd)
		return (short) commands[i][1];

	// Get here only if the command was not found.
	return commandnames.cmd_invalid;
    }

    protected static short get_command_code(int cmd, int commands1[][])
	throws non_existing_command_exception 
    {
	short result = get_command_code_simple(cmd, commands1);
	if (result == commandnames.cmd_invalid) {
	    System.err.println("Command " + command_name(cmd) + " was not found.");
	    throw new non_existing_command_exception(cmd);
	}
	return result;
    }

    protected static short get_command_code(int cmd,
					    int commands1[][],
					    int commands2[][]) 
    throws non_existing_command_exception {
	short result = get_command_code_simple(cmd, commands1);
 	return
	    result != commandnames.cmd_invalid
	    ? result 
	    : get_command_code(cmd, commands2);
    }

    protected static short get_command_code(int cmd,
					    int commands1[][],
					    int commands2[][],
					    int commands3[][])
	throws non_existing_command_exception {
	short result = get_command_code_simple(cmd, commands1);
 	return
	    result != commandnames.cmd_invalid
	    ? result 
	    : get_command_code(cmd, commands2, commands3);
    }

    protected static int[] extract_commands(int [][] cmds1,
					    int [][] cmds2,
					    int [][] cmds3) {
	int [] cmds = new int[cmds1.length + cmds2.length + cmds3.length];
	int inx = 0;
	for (int i = 0; i < cmds1.length; i++)
	    cmds[inx++] = cmds1[i][0];
	for (int i = 0; i < cmds2.length; i++)
	    cmds[inx++] = cmds2[i][0];
	for (int i = 0; i < cmds3.length; i++)
	    cmds[inx++] = cmds3[i][0];

	return cmds;
    }
	
    protected static int[] extract_commands(int [][] cmds1, int [][] cmds2) {
	int [] cmds = new int[cmds1.length + cmds2.length];
	int inx = 0;
	for (int i = 0; i < cmds1.length; i++)
	    cmds[inx++] = cmds1[i][0];
	for (int i = 0; i < cmds2.length; i++)
	    cmds[inx++] = cmds2[i][0];

	return cmds;
    }
	
    protected static int [] extract_commands(int [][] commands) {
	int[] cmds = new int[commands.length];
	for (int i = 0; i < commands.length; i++)
	    cmds[i] = commands[i][0];
	return cmds;
    }

    /**
     * Returns all commands the class implements.
     */
    //    public abstract int[] get_commands();
//     {
// 	return extract_commands(commands);
//     }

//    protected static int [][] commands = {};

    /**
     * Returns the name of the command in the argument.
     * Inverse of decode_command().
     */
    public static String command_name(int command) {
	return 
	    (command >= 0) && (command < commandnames.command_name_table.length)
	    ? commandnames.command_name_table[command] : "????";
    }

    // Override when using different ir_sequences
    protected int[] get_intro_array() {
	return intro_sequence.int_array();
    }

    public int[] get_repeat_array() {
	return repeat_sequence.int_array();
    }

    public int[] raw_ccf_array() {
	int[] intro = get_intro_array();
	int[] repeat = get_repeat_array();

	int[] result = new int[4 + intro.length + repeat.length];
	int indx = 0;
	result[indx++] = ccf_type_learned;
	result[indx++] = carrier_frequency_code;
	result[indx++] = intro.length/2;
	result[indx++] = repeat.length/2;
	for (int i = 0; i < intro.length; i++)
	    result[indx++] = intro[i];
	for (int i = 0; i < repeat.length; i++)
	    result[indx++] = repeat[i];

	return result;
    };

    public String raw_ccf_string() {
	int[] intro = get_intro_array();
	int[] repeat = get_repeat_array();

	return
	    ccf_integer(ccf_type_learned) + " "
	    + ccf_integer(carrier_frequency_code) + " "
	    + ccf_integer(intro.length/2) + " "
      	    + ccf_integer(repeat.length/2) +
	    ir_code.ccf_joiner(intro) +
	    ir_code.ccf_joiner(repeat);
    };

    /**
     * Return a CCF string, cooked if possible (by overriding in rc5 etc).
     */
    public String ccf_string() {
	return raw_ccf_string();
    }

    public static String ccf_joiner(int [] array) {
	String result = "";
	for (int i = 0; i < array.length; i++) 
	    result = result + " " + ccf_integer(array[i]);
	return result;
    }

//     /**
//      * Sent this ir_code to the GlobalCache device, using default connector.
//      * @deprecated
//      */
//     protected int gc(boolean verbose) {
// 	return gc(1, verbose);
//     }

//     /**
//      * Command not existing within ir_code
//      */
//     public class CommandNotExistingException extends Exception {

// 	public CommandNotExistingException(String command, String remote) {
// 	    super("Command " + command + " not implemented in " + remote);
// 	}
// 	public CommandNotExistingException(int command, String remote) {
// 	    super("Command " + command_name(command) + " not implemented in " + remote);
// 	}
//     }

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
