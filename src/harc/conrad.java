/**
 * @deprecated
 */

package harc;

/**
 * The Conrad 433 MHz Power switches.
 * Called "SLG Kunststoff-Fabrik GmbH - RS200" by ezcontrol.
 * Sort of obsolete.
 */
public final class conrad extends pwm_ir {
    public final static String vendor = "Conrad";
    public final static String device_name = "Power Switches";
    public final static String remote_name = "conrad";

    public final static int no_houses = 256;
    public final static int no_devices = 6;

    public final static command_t commands[] = {
        command_t.power_on,
        command_t.power_off,
        command_t.power_toggle
    };

    private static int[] housecode2intarray(String housecode) {
        int adr = Integer.parseInt(housecode);
        int address[] = new int[4];
        address[0] = adr / 1000 - 1;
        address[1] = (adr % 1000) / 100 - 1;
        address[2] = (adr % 100) / 10 - 1;
        address[3] = adr % 10 - 1;

        return address;
    }

    private static String pretty_name(int housecode[], int devicecode,
            command_t command) {
        String s = "";
        for (int i = 0; i < 4; i++)
            s += (new Integer(housecode[i] + 1)).toString();

        s += "_" + (new Integer(devicecode + 1)).toString() + "_" + command;
        return s;
    }

    public conrad(int housecode[], int devicecode, command_t command) {
        super(0x74,
                new pulse_pair(0x0032, 0x0078), new pulse_pair(0x0015, 0x0078),
                pretty_name(housecode, devicecode, command));

        // preable
        int start[] = {0, 1, 1, 0, 0, 1};// size = 6
        repeat_sequence.setdata(start);

        // housecode
        for (int i = 3; i >= 0; i--) { // size = 6 + 4*2 = 14
            int a[] = new int[2];
            a[0] = housecode[i] / 2;
            a[1] = housecode[i] % 2;
            repeat_sequence.appenddata(a);
        }

        // dummy
        int dummy[] = {0};// size = 14 + 1 = 15
        repeat_sequence.appenddata(dummy);

        // devicecode
        int d[] = new int[3];
        d[0] = devicecode / 4;
        d[1] = (devicecode % 4) / 2;
        d[2] = devicecode % 2;
        repeat_sequence.appenddata(d);

        int c[] = {0, 0, 0, 0};

        c[0] = command == command_t.power_toggle ? 1 : 0;
        c[1] = command == command_t.power_off ? 1 : 0;
        repeat_sequence.appenddata(c);

        repeat_sequence.setdata(14, repeat_sequence.get_parity(15, 21) == 0 ? 1 : 0);

        int sum = 0;
        for (int j = 0; j < 5; j++) {
            sum += 8 * repeat_sequence.getdata(4 * j + 0 + 2) +
                    4 * repeat_sequence.getdata(4 * j + 1 + 2) +
                    2 * repeat_sequence.getdata(4 * j + 2 + 2) +
                    1 * repeat_sequence.getdata(4 * j + 3 + 2);
        }

        sum %= 16;
        int checksum[] = {0, 0, 0};

        checksum[0] = sum / 8;
        sum %= 8;
        checksum[1] = sum / 4;
        sum %= 4;
        checksum[2] = sum / 2;
        sum %= 2;
        //checksum[3] = sum;

        repeat_sequence.appenddata(checksum);
        repeat_sequence.setleadout(new pulse_pair(sum == 0 ? 0x0032 : 0x0015, 0x0500));
    }

    /**
     * housecode = 1111,...,4444
     * devicecode = 1,2,...,6
     * command = "power_on", "power_off", "power_toggle"
     */ 

    public conrad(String housecode, int devicecode, String command) {
        this(housecode2intarray(housecode), devicecode - 1,
                command_t.parse(command));
    }

    public static void print_table() {
        int a[] = new int[4];
        for (int a0 = 0; a0 < 4; a0++) {
            a[0] = a0;
            for (int a1 = 0; a1 < 4; a1++) {
                a[1] = a1;
                for (int a2 = 0; a2 < 4; a2++) {
                    a[2] = a2;
                    for (int a3 = 0; a3 < 4; a3++) {
                        a[3] = a3;
                        for (int device = 0; device < no_devices; device++) {
                            for (int i = 0; i < commands.length; i++) {
                                (new conrad(a, device, commands[i])).print();
                                System.out.println();
                            }
                        }
                    }
                }
            }
        }
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("conrad [housecode deviceaddress power_on|power_off|power_toggle]");
        System.exit(1);
    }

    public static void main(String args[]) {
        System.err.println((int)'A');
        System.exit(0);
        if (args.length == 0)
            print_table();
        else try {
            (new conrad(args[0], Integer.parseInt(args[1]), args[2])).print();
        } catch (Exception e) {
            usage();
        }
    }
}
