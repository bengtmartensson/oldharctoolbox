/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

import java.io.*;
import java.net.*;

public class globalcache {

    private String gc_host;
    private final static int gc_port = 4998;
    private final static int gc_first_serial_port = 4999;
    public final static String default_gc_host = "192.168.1.70";
    public final static int gc_100_06 = 1;
    public final static int gc_100_12 = 2;
    public final static int gc_100_18 = 3;

    // Configlock
    public final static int locked = 0;
    public final static int unlocked = 1;

    // IP-Address assingment
    public final static int dhcp = 0;
    public final static int static_ip = 1;

    // IR/Sensor in-output configuration
    public final static int ir = 0;
    public final static int sensor = 1;
    public final static int sensor_notify = 2;
    public final static int ir_nocarrier = 3;
    /**
     * Global Cache default module for ir communication
     */
    private int default_ir_module = 2; // right for gc_100_06, wrong for others
    /**
     * GlobalCache default relay module
     */
    private final static int default_relay_module = 3;
    /**
     * GlobalCache default connector
     */
    private final static int gc_default_connector = 1;
    /**
     * Global Cache ir index
     */

    // short, because should turn around at 65536, see GC API docs.
    private static short gc_index;

    private boolean valid_connector(int c) {
        return (c > 0) && (c <= 3);
    }

    /**
     * Turns a CCF string to a GC string.
     */
    public static String ccf_string2gc_string(String ccf_string, int count) {
        String gc_string = "";
        try {
            String sa[] = ccf_string.trim().split(" +");
            int type = Integer.parseInt(sa[0], 16);
            if (type != ir_code.ccf_type_learned) {
                System.err.println("Can only convert learned CCF codes.");
                System.exit(1);
            }
            int freq = ir_code.carrier_frequency(Integer.parseInt(sa[1], 16));
            int intro_length = Integer.parseInt(sa[2], 16);

            gc_string = "" + freq + "," + count + "," + (1 + 2 * intro_length);
            for (int i = 4; i < sa.length; i++) {
                //System.err.println(gc_string);
                gc_string = gc_string + "," + Integer.parseInt(sa[i], 16);
            }
        } catch (NumberFormatException e) {
            System.err.println("Parse error (number format)" + gc_string);
            gc_string = "???";
        }
        return gc_string;
    }
    private boolean verbose = true;
    private int gc_type = gc_100_06;
    private java.lang.Process gc_process;

    private static String gc_joiner(int[] array) {
        String result = "";
        for (int i = 0; i < array.length; i++) {
            result = result + "," + array[i];
        }
        return result;
    }

    private String gc_string(ir_code code, int count) {
        int[] intro = code.get_intro_array();
        int[] repeat = code.get_repeat_array();
        return code.carrier_frequency() + "," + count + "," + (1 + intro.length) + gc_joiner(intro) + gc_joiner(repeat);
    }
    ;

    public globalcache(String hostname, int type, boolean verbose) {
        gc_host = (hostname != null) ? hostname : default_gc_host;
        default_ir_module = (type == gc_100_06) ? 2 : 4;
        this.verbose = verbose;
    }

    public globalcache(String hostname, String type, boolean verbose) {
        this(hostname, type_as_int(type), verbose);
    }

    private static int type_as_int(String type) {
        return type.equals("gc_100_18") ? gc_100_18 : type.equals("gc_100_12") ? gc_100_12 : gc_100_06;
    }

    public globalcache(String hostname) {
        this(hostname, gc_100_06, false);
    }

    public globalcache(String hostname, boolean verbose) {
        this(hostname, gc_100_06, verbose);
    }

    public globalcache(int type) {
        this(default_gc_host, type, false);
    }

    public globalcache(boolean verbose) {
        this(default_gc_host, gc_100_06, verbose);
    }

    public globalcache() {
        this(default_gc_host, gc_100_06, false);
    }

    public void set_verbosity(boolean verbosity) {
        this.verbose = verbose;
    }

    private String connector_address(int module, int connector) {
        return "" + module + ":" + connector;
    }

    public String send_serial(String cmd, int serial_no, int return_lines,
            int count, int delay)
            throws UnknownHostException, IOException, NoRouteToHostException {
        Socket sock = null;
        /*DataOutputStream*/        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        sock = new Socket(gc_host, gc_first_serial_port + serial_no - 1);
        outToServer =
                new /*DataOutputStream*/ PrintStream(sock.getOutputStream());
        inFromServer =
                new BufferedReader(new InputStreamReader(sock.getInputStream()));

        for (int c = 0; c < count; c++) {
            try {
                if (delay > 0 && c > 0) {
                    Thread.currentThread().sleep(delay);
                }
                outToServer./*writeBytes*/print(cmd);
            } catch (InterruptedException e) {
            }
        }

        for (int i = 0; i < return_lines; i++) {
            if (i > 0) {
                result = result + "\n";
            }
            result = result + inFromServer.readLine();
        }
        sock.close();
        return result;
    }

    public String send_serial(String cmd, int serial_no, int return_lines)
            throws UnknownHostException, IOException, NoRouteToHostException {
        return send_serial(cmd, serial_no, return_lines, 1, 0);
    }


    // Note: does not return, but loops forever!
    public void listen_serial(int serial_no) {
        Socket sock = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(gc_host, gc_first_serial_port + serial_no - 1);
            inFromServer =
                    new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + gc_host + " does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + gc_host);
            System.exit(1);
        }

        try {
            for (;;) {
                result = inFromServer.readLine();
                // Ignore whitspace lines
                if (result.trim().length() > 0) {
                    System.out.println(result);
                }
            }
        } catch (IOException e) {
            System.err.println("Couldn't read from " + gc_host);
            System.exit(1);
        }
    }

    public String send_serial(String cmd) throws UnknownHostException, IOException {
        String result = send_serial(cmd, 1, 0);
        return result;
    }

    // Not used, at least not presently
    private String send_command_external(String command) {
        if (verbose) {
            System.err.println("Sending command `" + command + "' to GlobalCache");
        }

        String cmd_array[] = new String[2];
        cmd_array[0] = "/usr/local/bin/gc";
        cmd_array[1] = command;
        String result = "***";
        try {
            gc_process = java.lang.Runtime.getRuntime().exec(cmd_array);
            BufferedInputStream proc_stdout = new BufferedInputStream(gc_process.getInputStream());
            BufferedInputStream proc_stderr = new BufferedInputStream(gc_process.getErrorStream());
            int ch;
            proc_stderr.mark(100000); // How to get rid of this braindamage?
            while ((ch = proc_stderr.read()) != -1);
            proc_stderr.reset();
            gc_process.waitFor();
            int exit_value = gc_process.exitValue();
            if (exit_value != 0) {
                System.err.println("Exit value was " + exit_value);
            }
            if (verbose) {
                int c;
                while ((c = proc_stdout.read()) != -1) {
                    System.out.write(c);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not exec command `" + command + "'.");
        } catch (InterruptedException e) {
            System.err.println("Interrupted...");
        }
        return result;
    }

    private String send_command(String cmd, boolean blind) throws UnknownHostException, IOException {
        if (verbose) {
            System.err.println("Sending command `" + cmd + "' to GlobalCache (" + gc_host + ")");
        }
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        //InputStreamReader inFromServer = null;
        String result = "";

        sock = new Socket(gc_host, gc_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer =
                new BufferedReader(new InputStreamReader(sock.getInputStream()));

        outToServer.print(cmd + '\r');

        if (!blind) {
            result = inFromServer.readLine();// may hang
            while (inFromServer.ready()) {
                result = (result.equals("") ? "" : (result + '\n')) + inFromServer.readLine();
            }
        }

        sock.close();

        if (verbose) {
            System.out.println(result);
        }

        return result;
    }

    private String[] send_command_array(String[] cmd, boolean blind, int delay_ms) throws UnknownHostException, IOException {
        int length = cmd.length;
        String[] result = new String[length];
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        //InputStreamReader inFromServer = null;

        sock = new Socket(gc_host, gc_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer =
                new BufferedReader(new InputStreamReader(sock.getInputStream()));

        for (int i = 0; i < length; i++) {
            if (verbose) {
                System.err.println("Sending command (array) " + cmd[i] + " to GlobalCache (" + gc_host + ")");
            }
            outToServer.print(cmd[i] + '\r');

            try {
                Thread.currentThread().sleep(delay_ms); // Yes, this is necessary
            } catch (InterruptedException e) {
            }

            if (!blind) {
                result[i] = inFromServer.readLine();// may hang
                while (inFromServer.ready()) {
                    result[i] = (result[i].equals("") ? "" : (result[i] + '\n')) + inFromServer.readLine();
                }
            }
        }

        sock.close();

        if (verbose) {
            System.out.println(result[0]);
        }

        return result;
    }

    public String send_command(String cmd) throws UnknownHostException, IOException {
        return send_command(cmd, false);
    }

    public void stop_ir(int module, int connector) throws UnknownHostException, IOException {
        send_command("stopir," + connector_address(module, connector));
    }

    public void stop_ir(int connector) throws UnknownHostException, IOException {
        stop_ir(default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1);
    }

    public void stop_ir() throws UnknownHostException, IOException {
        stop_ir(gc_default_connector);
    }

    // FIXME returnstatus
    public boolean send_ir(ir_code code, int module, int connector, int count) throws UnknownHostException, IOException {
        if (!valid_connector(connector)) {
            connector = gc_default_connector;
        }

        String cmd = "sendir," + connector_address(module, connector) + "," + gc_index + "," + gc_string(code, count);
        gc_index++;
        String result = send_command(cmd);
        return result.startsWith("completeir");
    }

    public boolean send_ir(ir_code code, int connector, int count) throws UnknownHostException, IOException {
        return send_ir(code, default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1, count);
    }

    public boolean send_ir(ir_code code, int connector) throws UnknownHostException, IOException {
        return send_ir(code, connector, gc_default_connector);
    }

    public boolean send_ir(ir_code code) throws UnknownHostException, IOException {
        return send_ir(code, gc_default_connector);
    }

    public boolean send_ir(String ccf_string, int module, int connector, int count) throws UnknownHostException, IOException {
        if (!valid_connector(connector)) {
            connector = gc_default_connector;
        }

        String cmd = "sendir," + connector_address(module, connector) + "," + gc_index + "," + ccf_string2gc_string(ccf_string, count);
        gc_index++;
        String result = send_command(cmd);
        return result.startsWith("completeir");
    }

    public boolean send_ir(String ccf_string, int connector) throws UnknownHostException, IOException {
        return send_ir(ccf_string, default_ir_module, connector, gc_default_connector);
    }

    public boolean send_ir(String ccf_string, int connector, int count) throws UnknownHostException, IOException {
        return send_ir(ccf_string, default_ir_module + (connector - 1) / 3, (connector - 1) % 3 + 1, count);
    }

    public String getdevices() throws UnknownHostException, IOException {
        return send_command("getdevices");
    }

    public String getversion(int module) throws UnknownHostException, IOException {
        return send_command("getversion," + module);
    }

    public String getversion() throws UnknownHostException, IOException {
        return getversion(0);
    }

    public String getnet() throws UnknownHostException, IOException {
        return send_command("get_NET,0:1");
    }

    public String setnet(String arg) throws UnknownHostException, IOException {
        return send_command("set_NET,0:1," + arg);
    }

    public String getir(int module, int connector) throws UnknownHostException, IOException {
        return send_command("get_IR," + connector_address(module, connector));
    }

    private String connector_mode(int mode) {
        String result;
        switch (mode) {
            case ir:
                result = "IR";
                break;
            case sensor:
                result = "SENSOR";
                break;
            case sensor_notify:
                result = "SENSOR_NOTIFY";
                break;
            case ir_nocarrier:
                result = "IR_NOCARRIER";
                break;
            default:
                result = "UNKNOWN";
        }
        return result;
    }

    public String setir(int module, int connector, String modestr) throws UnknownHostException, IOException {
        return send_command("set_IR," + connector_address(module, connector) + "," + modestr);
    }

    public String setir(int module, int connector, int mode) throws UnknownHostException, IOException {
        return setir(module, connector, connector_mode(mode));
    }

    public String getserial(int module) throws UnknownHostException, IOException {
        return send_command("get_SERIAL," + connector_address(module, 1));
    }

    public String setserial(int module, String arg) throws UnknownHostException, IOException {
        return send_command("set_SERIAL," + connector_address(module, 1) + "," + arg);
    }

    public String setserial(int module, int baudrate) throws UnknownHostException, IOException {
        return send_command("set_SERIAL," + connector_address(module, 1) + "," + baudrate);
    }

    public int getstate(int module, int connector) throws UnknownHostException, IOException {
        int result = -1;
        try {
            result = Integer.parseInt(send_command("getstate," + connector_address(module,
                    connector)).substring(10, 11));
        } catch (NumberFormatException e) {
            System.err.println("Error getting status of input on module " + module + ", connector " + connector);
        }
        return result;
    }

    public int getstate(int connector) throws UnknownHostException, IOException {
        return getstate(default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1);
    }

    // Seems quite inefficient
    public boolean togglestate(int connector) throws UnknownHostException, IOException {
        return setstate(default_relay_module, connector,
                1 - getstate(default_relay_module, connector));
    }

    public boolean setstate(int module, int connector, int state) throws UnknownHostException, IOException {
        String result = send_command("setstate," + connector_address(module, connector) + "," + (state == 0 ? 0 : 1));
        if (verbose) {
            System.out.println(result.substring(0, 5));
        }
        return result.substring(0, 5).equals("state");
    }

    public boolean setstate(int connector, int state) throws UnknownHostException, IOException {
        return setstate(default_relay_module, connector, state);
    }

    public boolean setstate(int connector, boolean on_off) throws UnknownHostException, IOException {
        return setstate(connector, on_off ? 1 : 0);
    }

    public boolean pulsestate(int module, int connector) throws UnknownHostException, IOException {
        String[] cmd = new String[2];
        cmd[0] = "setstate," + connector_address(module, connector) + ",1";
        cmd[1] = "setstate," + connector_address(module, connector) + ",0";

        String[] result = send_command_array(cmd, true, 300);
        return true;
    }

    public boolean pulsestate(int connector) throws UnknownHostException, IOException {
        return pulsestate(default_relay_module, connector);
    }

    public void set_blink(int arg) throws UnknownHostException, IOException {
        send_command("blink," + arg, true);
    }

    public static void usage() {
        System.err.println("Usage:");
        System.err.println("globalcache [options] <command> [<argument>]");
        System.err.println("where options=-# <count>,-h <hostname>,-c <connector>,-m <module>,-b <baudrate>,-v");
        System.err.println("and command=send_ir,send_serial,listen_serial,set_relay,get_devices,get_version,set_blink,[set|get]_serial,[set|get]_ir,[set|get]_net,[get|set]_state");
        System.exit(1);
    }

    public static void main(String[] args) {
        String hostname = default_gc_host;
        int connector = 1;
        int module = 2;
        int count = 1;
        boolean verbose = false;
        int baudrate = 0; // invalid value

        int arg_i = 0;

        try {
            while (arg_i < args.length && args[arg_i].charAt(0) == '-') {
                if (args[arg_i].equals("-h")) {
                    hostname = args[arg_i + 1];
                    arg_i += 2;
                } else if (args[arg_i].equals("-m")) {
                    module = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-l") || args[arg_i].equals("-c")) {
                    connector = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-#")) {
                    count = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-b")) {
                    baudrate = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-v")) {
                    verbose = true;
                    arg_i++;
                } else {
                    usage();
                }
            }

            globalcache gc = new globalcache(hostname, verbose);

            String cmd = args[arg_i];
            String arg = (args.length > arg_i + 1) ? args[arg_i + 1] : "";

            String output = "";

            try {
                if (cmd.equals("set_blink")) {
                    if (arg.equals("0")) {
                        gc.set_blink(0);
                    } else {
                        gc.set_blink(1);
                    }
                } else if (cmd.equals("get_devices")) {
                    output = gc.getdevices();
                } else if (cmd.equals("get_version")) {
                    output = gc.getversion(module);
                } else if (cmd.equals("get_net")) { // Only v3
                    output = gc.getnet();
                } else if (cmd.equals("set_net")) { // Only v3
                    // Syntax: see API-document
                    output = gc.setnet(arg);
                } else if (cmd.equals("get_ir")) { //Only v3
                    output = gc.getir(module, connector);
                } else if (cmd.equals("set_ir")) {
                    output = gc.setir(module, connector, arg);
                } else if (cmd.equals("get_serial")) {
                    output = gc.getserial(module);
                } else if (cmd.equals("set_serial")) {
                    if (baudrate > 0) {
                        output = gc.setserial(module, baudrate);
                    } else {
                        output = gc.setserial(module, arg);
                    }
                } else if (cmd.equals("get_state")) {
                    output = gc.getstate(module, connector) == 1 ? "on" : "off";
                } else if (cmd.equals("toggle_state")) {
                    output = gc.togglestate(connector) ? "ok" : "not ok";
                } else if (cmd.equals("set_state")) {
                    output = gc.setstate(connector, (arg.equals("0") ? 0 : 1)) ? "on" : "off";
                } else if (cmd.equals("set_relay")) {
                    // Just a convenience version of the above
                    output = gc.setstate(connector, (arg.equals("0") ? 0 : 1)) ? "on" : "off";
                } else if (cmd.equals("send_ir")) {
                    String ccf = "";
                    for (int i = arg_i + 1; i < args.length; i++) {
                        ccf = ccf + " " + args[i];
                    }

                    gc.send_ir(ccf, module, connector, count);
                } else if (cmd.equals("send_serial")) {
                    String transmit = "";
                    for (int i = arg_i + 1; i < args.length; i++) {
                        transmit = transmit + " " + args[i];
                    }

                    output = gc.send_serial(transmit, module, 0);
                } else if (cmd.equals("listen_serial")) {
                    System.err.println("Press Ctrl-C to interrupt.");
                    // Never returns
                    gc.listen_serial(module);
                } else {
                    usage();
                }
            } catch (UnknownHostException e) {
                System.err.println("Host " + gc.gc_host + " does not resolve.");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("IOException occured.");
                System.exit(1);
            }
            System.out.println(output);
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }
    }
}
