/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class dbox2_old extends biphase_ir implements commandnames {

    /**
     * Number of arguments considering the code as a remote contoller.
     */
    public final static int no_args = 1;
    public final static String vendor = "Nokia";
    public final static String device_name = "dbox2_old";
    public final static String remote_name = "dbox2_old";
    public static final int carrier_frequency_code = 0x006d;
    public static final int commands[][] = {
        // Commands residing on standard dBox remotes
        {cmd_0, 0x00},
        {cmd_1, 0x01},
        {cmd_2, 0x02},
        {cmd_3, 0x03},
        {cmd_4, 0x04},
        {cmd_5, 0x05},
        {cmd_6, 0x06},
        {cmd_7, 0x07},
        {cmd_8, 0x08},
        {cmd_9, 0x09},
        {cmd_right, 0x2e},
        {cmd_left, 0x2f},
        {cmd_up, 0x0e},
        {cmd_down, 0x0f},
        {cmd_ok, 0x30},
        {cmd_mute_toggle, 0x28},
        {cmd_power_toggle, 0x0c},
        {cmd_green, 0x55},
        {cmd_yellow, 0x52},
        {cmd_red, 0x2d},
        {cmd_blue, 0x3b},
        {cmd_volume_up, 0x16},
        {cmd_volume_down, 0x17},
        {cmd_info, 0x82},
        {cmd_setup, 0x27},
        {cmd_home, 0x20},
        // The "double arrows" (?) on very old remotes
        {cmd_page_down, 0x53},
        {cmd_page_up, 0x54},
        // To use these (other than the first one) modify
        // dbox2_fp_rc.c, line 71-74.
        {cmd_topleft, 0xff},
        {cmd_topright, 0xfb},
        {cmd_bottomleft, 0xfd},
        {cmd_bottomright, 0xfc},};

    public static int[] get_commands() {
        return extract_commands(commands);
    }
    public static int lirc_repeat_bit = 0;
    public static int lirc_bits = 8;
    public static String lirc_flags = "RC5|REVERSE";
    public static int[] lirc_header = {
        (int) (0x13 * pulse_time(carrier_frequency_code)),
        (int) (0x5f * pulse_time(carrier_frequency_code))
    };
    //public static int[] lirc_header = { 510,  2520 };
    public static int[] lirc_one = {450, 550};
    public static int[] lirc_zero = {450, 550};
    public static int lirc_pre_data_bits = 1;
    public static int lirc_pre_data = 0x0;
    public static int lirc_post_data_bits = 8;
    public static int lirc_post_data = 0xa3;
    public static int lirc_gap = (int) (0x0be1 * pulse_time(carrier_frequency_code));//59500;

    public String lirc_code_string() {
        return "0x" + Integer.toHexString(command_no ^ 0xff);
    }

    public dbox2_old(short code, String cmd_name) {
        super(carrier_frequency_code, 0x13, 0x13, 0x26, cmd_name);
        command_no = code;
        intro_sequence.setleadin(new pulse_pair(0x0013, 0x005f));
        intro_sequence.setleadout(new pulse_pair(0x0013, 0x030b));
        repeat_sequence.setleadin(new pulse_pair(0x0013, 0x005f));
        repeat_sequence.setleadout(new pulse_pair(0x0013, 0x0be1));
        int startbits[] = {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        intro_sequence.setdata(startbits);
        repeat_sequence.setdata(invert_bitarray(array_lsb(code, 8)));
        int morebits[] = {1, 1, 0, 0, 0, 1, 0};
        repeat_sequence.appenddata(morebits);
    }

    public dbox2_old(short cmdno) {
        this(cmdno, ir_code.command_name(cmdno));
    }

    public dbox2_old(int cmd, String cmd_name) throws non_existing_command_exception {
        this(get_command_code(cmd, commands), cmd_name);
    }

    public dbox2_old(String command) throws non_existing_command_exception {
        this(decode_command(command), command);
    }

    //public static void main(String args[]) {
    //    remote.process_args("dbox2_old", args);
    //}
}
/* Patch for dbox2_fp_rc.c, to recognize topright, bottomleft, bottomright

--- dbox2_fp_rc.c.orig  2005-05-20 02:28:48.000000000 +0200
+++ dbox2_fp_rc.c       2007-03-13 07:40:55.000000000 +0100
@@ -69,9 +69,9 @@
{KEY_HELP,                      0x17, 0x82},
{KEY_SETUP,                     0x18, 0x27},
{KEY_TOPLEFT,                   0x1B, 0xff},
-       {KEY_TOPRIGHT,                  0x1C, 0xff},
-       {KEY_BOTTOMLEFT,                0x1D, 0xff},
-       {KEY_BOTTOMRIGHT,               0x1E, 0xff},
+       {KEY_TOPRIGHT,                  0x1C, 0xfb},
+       {KEY_BOTTOMLEFT,                0x1D, 0xfd},
+       {KEY_BOTTOMRIGHT,               0x1E, 0xfc},
{KEY_HOME,                      0x1F, 0x20},
{KEY_PAGEDOWN,                  0x53, 0x53},
{KEY_PAGEUP,                    0x54, 0x54},

 */
