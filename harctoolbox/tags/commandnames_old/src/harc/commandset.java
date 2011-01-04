/**
 *
 * @version 0.01
 * @author Bengt Martensson
 */
package harc;

public class commandset {

    public final static int any = -2;
    public final static int cmdtype_invalid = -1;
    public final static int ir = 0;
    public final static int rf433 = 1;
    public final static int rf868 = 2;
    public final static int www = 3;
    public final static int web_api = 4;
    public final static int tcp = 5;
    public final static int udp = 6;
    public final static int serial = 7;
    public final static int bluetooth = 8;
    public final static int on_off = 9;
    public final static int ip = 10;
    public final static int no_cmdtypes = 11;

    public static String toString(int type) {
        return (type >= 0 && type < no_cmdtypes)
                ? commandtypes_table[type]
                : type == any ? "any" : null;
    }

    public static int toInt(String s) {
        for (int i = 0; i < commandtypes_table.length; i++) {
            if (s.equals(commandtypes_table[i])) {
                return i;
            }
        }

        return s.equals("any") ? any : cmdtype_invalid;
    }

    public static boolean valid(String s) {
        return toInt(s) != cmdtype_invalid;
    }
    public final static String commandtypes_table[] = {
        "ir",
        "rf433",
        "rf868",
        "www",
        "web_api",
        "tcp",
        "udp",
        "serial",
        "bluetooth",
        "on_off",
        "ip"
    };
    private int type;
    private String protocol;
    private short deviceno;
    private short subdevice;
    private boolean toggle;
    private String name;
    private String remotename;
    private String pseudo_power_on;
    private String prefix;
    private String suffix;
    private int delay_between_reps;
    private commandset_entry[] entries;

    public static String valid_types(char sep) {
        String res = commandtypes_table[0];
        for (int i = 1; i < no_cmdtypes; i++) {
            res = res + sep + commandtypes_table[i];
        }
        return res;
    }

    public int getno_commands() {
        return entries.length;
    }

    public int gettype() {
        return type;
    }

    public boolean type_ir() {
        return type == ir;
    }

    public String getremotename() {
        return remotename;
    }

    public String getpseudo_power_on() {
        return pseudo_power_on;
    }

    public String getprotocol() {
        return protocol;
    }

    public boolean gettoggle() {
        return toggle;
    }

    public short getdeviceno() {
        return deviceno;
    }

    public short getsubdevice() {
        return subdevice;
    }

    public String getprefix() {
        return prefix;
    }

    public String getsuffix() {
        return suffix;
    }

    public int get_delay_between_reps() {
        return delay_between_reps;
    }

    public commandset_entry getentry(int index) {
        return entries[index];
    }

    public String info() {
        String s =
                "*** Commandset\n" +
                "   type = " + type + " (" + toString(type) + ")\n" +
                "   protocol = " + protocol + "\n" +
                "   toggle = " + toggle + "\n" +
                "   deviceno = " + deviceno + "\n" +
                "   subdevice = " + subdevice + "\n" +
                "   remotename = " + remotename + "\n" +
                "   prefix = " + prefix + "\n" +
                "   suffix = " + suffix + "\n" +
                "   pseudo_power_on = " + pseudo_power_on + "\n" +
                "   # commands = " + entries.length;
        for (int i = 0; i < entries.length; i++) {
            s = s + "\n" + entries[i].toString(this, true);
        }

        return s;
    }

    public command get_command(int cmd, int type) {
        if (type == this.type || type == any) {
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].getcmd() == cmd) {
                    return new command(this, i);
                }
            }
        }
        return null;
    }

    public command get_command(String cmdname, int type) {
        return get_command(ir_code.decode_command(cmdname), type);
    }

    public commandset(commandset_entry[] commands, int type, String protocol,
            short deviceno, short subdevice, boolean toggle, String name,
            String remotename, String pseudo_power_on, String prefix,
            String suffix, int delay_between_reps) {
        this.entries = commands;
        this.type = type;
        this.deviceno = deviceno;
        this.subdevice = subdevice;
        this.protocol = protocol;
        this.toggle = toggle;
        this.name = name;
        this.remotename = remotename;
        this.pseudo_power_on = pseudo_power_on;
        this.prefix = prefix;
        this.suffix = suffix;
        this.delay_between_reps = delay_between_reps;
    }

    public commandset(commandset_entry[] commands, String type,
            String protocol, String deviceno, String subdevice, String toggle,
            String name, String remotename, String pseudo_power_on,
            String prefix, String suffix, String delay_between_reps) {
        this(commands, toInt(type), protocol,
                deviceno.equals("") ? -1 : Short.parseShort(deviceno),
                subdevice.equals("") ? -1 : Short.parseShort(subdevice),
                toggle.equals("yes"), name, remotename, pseudo_power_on,
                prefix, suffix, Integer.parseInt(delay_between_reps));
    }
}
