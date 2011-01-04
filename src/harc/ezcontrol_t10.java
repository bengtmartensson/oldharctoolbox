/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;

public class ezcontrol_t10 {

    // UDP control presently not implemented.

    private String ezcontrol_host;
    public final static String default_ezcontrol_host = "192.168.1.42";
    private boolean verbose = true;

    public static String make_url(String ez_hostname, String system, String house, int device, String command_name)
            throws non_existing_command_exception {
        System.err.println(ez_hostname);
        System.err.println(system);
        System.err.println(house);
        System.err.println(device);
        System.err.println(command_name);
        ezcontrol_t10 ez = new ezcontrol_t10(ez_hostname);
        return ez.make_url_manual(system, "" + house, device, command_name, 1);
    }

    private class status {

        public final static int on = 1;
        public final static int off = 0;
        public final static int unknown = -1;
        public String name;
        public int state;

        public status(String n, int s) {
            name = n;
            state = s;
        }

        public String state_str() {
            return state == on ? "on" : state == off ? "off" : "?";
        }

        public String toString() {
            return name + ": " + state_str();
        }
    }
    public static final int t10_no_presets = 32;
    private status[] state = null; // Note: element 0 unused, 
    //starts with 0, T10 starts with 1
    public static String[] system_names = {
        "FS10", // 1
        "FS20",
        "RS200",
        "AB400",
        "AB601",
        "IT", // 6
        "REV",
        "BS-QU",
        "MARMI", // 9
        "OA-FM",
        "KO-FC",
        "RS862"
    };
    private final static String[] daynames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private class timer {

        boolean[] presets;
        boolean[] days;
        boolean enabled;

        private class clock {

            int hour;
            int minute;

            public clock(int h, int m) {
                hour = h;
                minute = m;
            }

            public String toString() {
                return "" + (hour >= 0
                        ? (hour < 10 ? "0" : "") + (hour + ":" + (minute < 10 ? "0" : "") + minute)
                        : "     ");
            }
        }
        clock on_time;
        clock off_time;

        public timer(boolean[] presets, boolean[] days, boolean enabled, int on_h, int on_m, int off_h, int off_m) {
            this.presets = presets;
            this.days = days;
            this.enabled = enabled;
            on_time = new clock(on_h, on_m);
            off_time = new clock(off_h, off_m);
        }

        public String toString() {
            String result = on_time.toString() + "-" + off_time.toString() + " ";

            boolean virgin = true;
            for (int i = 0; i < 7; i++) {
                if (days[i]) {
                    if (((i == 0) || !days[i - 1]) || ((i == 6) || !days[i + 1])) {
                        result = result + (virgin ? "" : (days[i - 1] ? "-" : ",")) + daynames[i];
                    }
                    virgin = false;
                }
            }

            for (int i = result.length(); i < 30; i++) {
                result = result + " ";
            }

            result = result + " ";
            virgin = true;
            for (int i = 1; i < t10_no_presets; i++) {
                if (presets[i]) {
                    result = result + (virgin ? "" : ", ") + state[i].name;
                    virgin = false;
                }
            }
            for (int i = result.length(); i < 65; i++) {
                result = result + " ";
            }
            return result + " " + (enabled ? "(enabled)" : "(disabled)");
        }
    }
    public static final int t10_no_timers = 26;
    private timer[] timers = null;
    private static int invalid_system = -1;

    private static int system_no(String system_name) {
        if (system_name.equalsIgnoreCase("intertechno")) {
            return 6;
        } else if (system_name.equalsIgnoreCase("conrad")) {
            return 3;
        } else if (system_name.equalsIgnoreCase("x10")) {
            return 9;
        }

        for (int i = 0; i < system_names.length; i++) {
            if (system_names[i].equals(system_name.toUpperCase())) {
                return i + 1;
            }
        }


        return invalid_system;
    }

    private static String cmd2string(command_t cmd) {
        return cmd == command_t.power_on
                ? "on" : cmd == command_t.power_off ? "off" : "?";
    }

    public ezcontrol_t10(String hostname, boolean verbose) {
        ezcontrol_host = hostname != null ? hostname : default_ezcontrol_host;
        this.verbose = verbose;
    }

    public ezcontrol_t10(String hostname) {
        this(hostname, false);
    }

    public ezcontrol_t10(boolean verbose) {
        this(default_ezcontrol_host, verbose);
    }

    public ezcontrol_t10() {
        this(default_ezcontrol_host, false);
    }

    public void set_verbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    public boolean send_manual(String system_name, String house, int device,
            String cmd_name, int n)
            throws non_existing_command_exception {
        return get_url(make_url_manual(system_name, house, device,
                cmd_name, n));
    }

    public boolean send_manual(String system_name, String house, int device,
            command_t cmd, int n)
            throws non_existing_command_exception {
        return send_manual(system_name, house, device, cmd2string(cmd), n);
    }

    public String make_url_manual(String system_name, String house, int device,
            String cmd_name, int n)
            throws non_existing_command_exception {
        int system = system_no(system_name);
        if (system == invalid_system) {
            throw new non_existing_command_exception("No such system: " + system_name);
        }
        String url = null;
        boolean success = true;
        switch (system) {
            case 3:		// rs200
                url = rs200_url(house, device, cmd_name, n);
                break;
            case 6:		// intertechno
                url = intertechno_url(house.charAt(0), device, cmd_name, n);
                break;
            case 9:		// Marmitek/x10
                url = x10_url(house.charAt(0), device, cmd_name, n);
                break;
            default:
                System.err.println("Sorry, system " + system_name + " is not yet implemented.");
                break;
        }
        return url;
    }

    public boolean send_preset(int switch_no, command_t cmd) throws non_existing_command_exception {
        return get_url(make_url_preset(switch_no, cmd));
    }

    public String intertechno_url(char house, int device, String cmd_name, int n)
            throws non_existing_command_exception {
        command_t cmd = command_t.parse(cmd_name);
        if (cmd == command_t.invalid) {
            if (cmd_name.equals("on")) {
                cmd = command_t.power_on;
            }
            if (cmd_name.equals("off")) {
                cmd = command_t.power_off;
            }
        }

        if (cmd != command_t.power_on && cmd != command_t.power_off) {
            throw new non_existing_command_exception(cmd_name);
        }
        return intertechno_url(Character.toUpperCase(house) - 'A' + 1,
                (device - 1) / 4 + 1,
                (device - 1) % 4 + 1,
                cmd, n);
    }

    public String intertechno_url(int hc1, int hc2, int addr, command_t cmd, int n)
            throws non_existing_command_exception {
        return "http://" + ezcontrol_host + "/send?system=6&hc1=" + hc1 + "&hc2=" + hc2 + "&addr=" + addr + "&value=" + (cmd == command_t.power_on ? "255" : "0") + "&n=" + n;
    }

    public String x10_url(char house, int device, String cmd_name, int n)
            throws non_existing_command_exception {
        command_t cmd = command_t.parse(cmd_name);
        if (cmd == command_t.invalid) {
            if (cmd_name.equals("on")) {
                cmd = command_t.power_on;
            }
            if (cmd_name.equals("off")) {
                cmd = command_t.power_off;
            }
        }

        if (cmd != command_t.power_on && cmd != command_t.power_off) {
            throw new non_existing_command_exception(cmd_name);
        }
        return x10_url(Character.toUpperCase(house) - 'A' + 1, device, cmd, n);
    }

    public String x10_url(int hc1, int addr, command_t cmd, int n)
            throws non_existing_command_exception {
        return "http://" + ezcontrol_host + "/send?system=9&hc1=" + hc1 + "&addr=" + addr + "&value=" + (cmd == command_t.power_on ? "255" : "0") + "&n=" + n;
    }

    public String rs200_url(String house, int device, String cmd_name, int n)
            throws non_existing_command_exception {
        command_t cmd = command_t.parse(cmd_name);
        if (cmd != command_t.power_on && cmd != command_t.power_off) {
            throw new non_existing_command_exception(cmd_name);
        }
        return "http://" + ezcontrol_host + "/send?system=3&hc1=" + house + "&addr=" + device + "&value=" + (cmd == command_t.power_on ? "255" : "0") + "&n=" + n;
    }

    public String make_url_preset(int switch_no, command_t cmd)
            throws non_existing_command_exception {
        if (cmd != command_t.power_on && cmd != command_t.power_off) {
            throw new non_existing_command_exception(cmd);
        }
        return "http://" + ezcontrol_host + "/preset?switch=" + switch_no + "&value=" + (cmd == command_t.power_on ? "on" : "off");
    }

    public boolean get_url(String url) {
        if (verbose) {
            System.err.println("Getting URL " + url);
        }
        boolean success = false;

        try {
            //(new URL(url)).getContent();
            (new URL(url)).openStream();
            success = true;
        } catch (java.net.MalformedURLException e) {
            System.err.println(e.getMessage());
            success = false;
        } catch (java.io.IOException e) {
            // FIXME: return false if no route etc, true if 204(?).
            //  204 No Content is normal, and should not be treated as failure.
            System.err.println("IOException: " + e.getMessage());
        }

        return success;
    }

    public String get_status() {
        setup_status();
        String result = "";
        for (int i = 0; i < t10_no_presets; i++) {
            if (state[i] != null) {
                result = result + i + ".\t" + state[i].toString() + "\n";
            }
        }
        return result;
    }

    public int get_status(int n) {
        setup_status();
        return state[n].state;
    }

    public String get_preset_status(int n) {
        setup_status();
        return state[n] != null ? state[n].state_str() : "n/a";
    }

    public String get_preset_name(int n) {
        setup_status();
        return state[n] != null ? state[n].name : "**not assigned**";
    }

    public String get_preset_str(int n) {
        setup_status();
        return state[n] != null ? state[n].toString() : "Preset " + n + " not assigned.";
    }

    private boolean setup_status() {
        if (state != null) {
            return true;
        }

        String url = "http://" + ezcontrol_host + "/";
        if (verbose) {
            System.err.println("Getting URL " + url);
        }
        String data = null;

        try {
            InputStream is = (new URL(url)).openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String str;
            do {
                str = r.readLine();
                data = data + str;
            } while (str != null);
        } catch (java.net.MalformedURLException e) {
            System.err.println(e.getMessage());
            return false;
        } catch (java.io.IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return false;
        }

        String[] snork = data.split("<tr>");
        state = new status[t10_no_presets + 1];

        for (int i = 1; i < snork.length; i++) {
            int p1 = snork[i].indexOf(">");
            int p2 = snork[i].indexOf("<", p1);
            int n = Integer.parseInt(snork[i].substring(p1 + 1, p2));
            p1 = snork[i].indexOf(">", p2 + 5);
            p2 = snork[i].indexOf("<", p1);
            String name = snork[i].substring(p1 + 1, p2);
            int stat =
                    snork[i].matches(".*background:lime.*") ? status.on
                    : snork[i].matches(".*background:red.*") ? status.off
                    : status.unknown;
            //System.out.println("" + n + ": " + name + stat);
            state[n] = new status(name, stat);
        }
        return true;
    }

    private int extract_value(String str) {
        int res = -1;
        if (str.matches(".*value.*")) {
            int p1 = str.indexOf("value");
            int p2 = str.indexOf("\"", p1 + 7);
            res = Integer.parseInt(str.substring(p1 + 7, p2));
        }
        return res;
    }

    private boolean setup_timers() {
        if (timers != null) {
            return true;
        }

        String url = "http://" + ezcontrol_host + "/timer.html";
        if (verbose) {
            System.err.println("Getting URL " + url);
        }
        String data = null;

        try {
            InputStream is = (new URL(url)).openStream();
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            String str;
            do {
                str = r.readLine();
                data = data + str;
            } while (str != null);
        } catch (java.net.MalformedURLException e) {
            System.err.println(e.getMessage());
            return false;
        } catch (java.io.IOException e) {
            System.err.println("IOException: " + e.getMessage());
            return false;
        }

        String[] snork = data.split("<table");
        timers = new timer[t10_no_timers];

        for (int i = 1; i < t10_no_timers + 1; i++) {
            boolean enabled = snork[i].matches(".*background:lime.*");
            int p1 = snork[i].indexOf("<b>");
            int n = (int) snork[i].charAt(p1 + 4) - (int) 'A';
            String[] inputs = snork[i].split(">");
            boolean[] presets = new boolean[t10_no_presets + 1];
            boolean[] days = new boolean[7];
            int indx = 0;

            do
                ;
            while (!inputs[indx++].matches("Switches.*"));
            for (int j = 1; j <= t10_no_presets;) {
                if (inputs[indx].matches(".*<input.*")) {
                    presets[j] = inputs[indx].matches(".*checked.*");
                    //System.out.println(inputs[indx] + j+ presets[j]);
                    j++;
                }
                indx++;
            }

            do
                ;
            while (!inputs[indx++].matches("Weekdays.*"));
            for (int j = 0; j < 7;) {
                if (inputs[indx].matches(".*<input.*")) {
                    days[j] = inputs[indx].matches(".*checked.*");
                    //System.out.println(inputs[indx] + j+ days[j]);
                    j++;
                }
                indx++;
            }

            do
                ;
            while (!inputs[indx++].matches("ON Time.*"));
            int on_h = extract_value(inputs[indx++]);
            int on_m = extract_value(inputs[indx++]);

            do
                ;
            while (!inputs[indx++].matches("OFF Time.*"));
            int off_h = extract_value(inputs[indx++]);
            int off_m = extract_value(inputs[indx++]);
            timers[i - 1] = new timer(presets, days, enabled, on_h, on_m, off_h, off_m);
        }
        return true;
    }

    public String get_timers() {
        boolean ok = setup_status() && setup_timers();
        if (!ok)
            return null;
        String result = "";
        for (int i = 0; i < t10_no_timers; i++) {
            if (timers[i].enabled) {
                result = result + (char) (i + (int) 'A') + ": " + timers[i].toString() + "\n";
            }
        }

        return result;
    }

    public String get_timer(int n) {
        boolean ok = setup_status() && setup_timers();
        if (!ok)
            return null;
        return timers[n].toString();
    }

    public String get_timer(String name) {
        int n = ((int) name.charAt(0) - (int) 'A') % 32;
        String result = "";
        if (n >= 0 && n < t10_no_timers) {
            result = get_timer(((int) name.charAt(0) - (int) 'A') % 32);
        } else {
            System.err.println("Erroneous timer name \"" + name + "\".");
        }
        return result;
    }

    public static String get_timers(String hostname) {
        return (new ezcontrol_t10(hostname)).get_timers();
    }

    public static String get_timer(String hostname, int n) {
        return (new ezcontrol_t10(hostname)).get_timer(n);
    }

    public static String get_timer(String hostname, String name) {
        return (new ezcontrol_t10(hostname)).get_timer(name);
    }

    public static String get_status(String hostname) {
        return (new ezcontrol_t10(hostname)).get_status();
    }

    public Document xml_config() {
        Document doc = harcutils.newDocument();
        Element root = doc.createElement("ezcontrol_t10");
        root.setAttribute("hostname", ezcontrol_host);
        doc.appendChild(root);
        Element presets = doc.createElement("presets");
        root.appendChild(presets);
        setup_status();
        for (int i = 1; i <= t10_no_presets; i++) {
            if (state[i] != null) {
                Element p = doc.createElement("preset");
                p.setAttribute("id", "preset_" + i);
                p.setAttribute("number", "" + i);
                p.setAttribute("state", state[i].state_str());
                p.setTextContent(state[i].name);
                presets.appendChild(p);
            }
        }

        Element preset_configuration = doc.createElement("preset_configuration");
        root.appendChild(preset_configuration);
        Element timers_ele = doc.createElement("timers");
        root.appendChild(timers_ele);
        setup_timers();
        for (int i = 0; i < t10_no_timers; i++) {
            if (timers[i] != null) {
                Element t = doc.createElement("timer");
                t.setAttribute("name", "" + (char) (i + (int) 'A'));
                t.setAttribute("enabled", timers[i].enabled ? "yes" : "no");
                if (timers[i].on_time.hour != -1) {
                    Element on = doc.createElement("on");
                    on.setAttribute("hour", "" + timers[i].on_time.hour);
                    on.setAttribute("minute", "" + timers[i].on_time.minute);
                    t.appendChild(on);
                }
                if (timers[i].off_time.hour != -1) {
                    Element off = doc.createElement("off");
                    off.setAttribute("hour", "" + timers[i].off_time.hour);
                    off.setAttribute("minute", "" + timers[i].off_time.minute);
                    t.appendChild(off);
                }
                boolean at_least_one_day = false;
                for (int j = 0; j < 7; j++) {
                    at_least_one_day = at_least_one_day || timers[i].days[j];
                }
                if (at_least_one_day) {
                    Element days = doc.createElement("days");
                    for (int j = 0; j < 7; j++) {
                        if (timers[i].days[j]) {
                            Element day = doc.createElement("day");
                            day.setAttribute("weekday", "" + (j + 1));
                            day.setAttribute("name", daynames[j]);
                            days.appendChild(day);
                        }
                    }
                    t.appendChild(days);
                }

                boolean at_least_one_preset = false;
                for (int j = 1; j <= t10_no_presets; j++) {
                    at_least_one_preset = at_least_one_preset || timers[i].presets[j];
                }
                if (at_least_one_preset) {
                    Element presetrefs = doc.createElement("presetrefs");
                    for (int j = 1; j <= t10_no_presets; j++) {
                        if (timers[i].presets[j]) {
                            Element presetref = doc.createElement("presetref");
                            presetref.setAttribute("preset", "preset_" + j);
                            presetref.setTextContent(state[j].name);
                            presetrefs.appendChild(presetref);
                        }
                    }
                    t.appendChild(presetrefs);
                }
                timers_ele.appendChild(t);
            }
        }

        Element network = doc.createElement("network");
        root.appendChild(network);
        return doc;
    }

    private void generate_xml() {
        harcutils.printDOM(System.out, xml_config(), "ezcontrol_t10_config.dtd");
    }

    public void generate_xml(String filename) throws FileNotFoundException {
        harcutils.printDOM(filename, xml_config(), "ezcontrol_t10_config.dtd");
    }

    public void get_configuration(String filename) {
        try {
            harcutils.printDOM(filename, xml_config(), "ezcontrol_t10_config.dtd");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void usage() {
        System.err.println("Usage:\n" + "ezcontrol [<options>] get_status [<preset_no>]\n" + "or\n" + "ezcontrol [<options>] get_timer [<timername>]\n" + "or\n" + "ezcontrol [<options>] <preset_no> <command>\n" + "or\n" + "ezcontrol [<options>] <system_name> <housecode> <device_no> <command>\n" + "\nwhere options=-h <hostname>,-d <debugcode>,-# <count>,-v");
        System.exit(1);
    }

    public static void main(String args[]) {
        boolean verbose = false;
        String ezcontrol_host = default_ezcontrol_host;
        int debug = 0;
        int arg_i = 0;
        int count = 1;
        boolean preset_mode = false;
        boolean do_get_status = false;
        boolean do_get_timers = false;
        boolean do_xml = false;
        command_t cmd = command_t.invalid;
        //String command_name = null;
        String timer_name = null;
        String system_name = null;
        String housecode = null;
        int device_no = -1;
        int num_arg = -1;

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-v")) {
                    verbose = true;
                    arg_i++;
                } else if (args[arg_i].equals("-h")) {
                    arg_i++;
                    ezcontrol_host = args[arg_i++];
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                } else {
                    usage();
                }
            }

            if (args[arg_i].equals("get_status")) {
                do_get_status = true;
                if (args.length - arg_i > 1) {
                    num_arg = Integer.parseInt(args[arg_i + 1]);
                }
            } else if (args[arg_i].equals("get_timer")) {
                do_get_timers = true;
                if (args.length - arg_i > 1) {
                    timer_name = args[arg_i + 1];
                }
            } else if (args[arg_i].equals("xml")) {
                do_xml = true;
            } else if (args.length - arg_i == 2) {// Preset command
                preset_mode = true;
                num_arg = Integer.parseInt(args[arg_i]);
                cmd = command_t.parse(args[arg_i + 1]);
            } else {
                system_name = args[arg_i];
                housecode = args[arg_i + 1];
                device_no = Integer.parseInt(args[arg_i + 2]);
                cmd = command_t.parse(args[arg_i + 3]);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        } catch (NumberFormatException e) {
            usage();
        }

        ezcontrol_t10 ez = new ezcontrol_t10(ezcontrol_host, verbose);
        if (num_arg != -1 && (num_arg < 1 || num_arg > t10_no_presets)) {
            System.err.println("Numerical argument not valid.");
            System.exit(45);
        }

        try {
            if (do_get_status) {
                if (num_arg > 0) {
                    System.out.println(ez.get_preset_str(num_arg));
                } else {
                    System.out.println(ez.get_status());
                }
            } else if (do_get_timers) {
                if (timer_name != null) {
                    System.out.println(ez.get_timer(timer_name));
                } else {
                    System.out.println(ez.get_timers());
                }
            } else if (do_xml) {
                ez.generate_xml();
            } else if (preset_mode) {
                ez.send_preset(num_arg, cmd);
            } else {
                ez.send_manual(system_name, housecode, device_no,
                        cmd, count);
            }
        } catch (non_existing_command_exception e) {
            System.err.println("Only commands power_on and power_off allowed.");
        }
    }
}
