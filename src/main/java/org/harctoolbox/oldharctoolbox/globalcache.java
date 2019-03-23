/*
Copyright (C) 2009-2011 Bengt Martensson.

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

package org.harctoolbox.oldharctoolbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;

public class globalcache {

    private String gc_host;
    private final static int gc_port = 4998;
    private final static int gc_first_serial_port = 4999;
    public final static String default_gc_host = "192.168.1.70";
    // Having this static is both illogical and slightly unflexible, but
    // MUCH simpler to implement.
    // TODO: fix
    private static int socket_timeout = 2000;
    private static int beacon_timeout = 30000; // There seems to be a bug somewhere in the SoTimeout code if larger than 30000...

    /** GlobalCache models */
    public enum gc_model {
        gc_100_06,
        gc_100_12,
        gc_100_18,
        wf2ir,
        ip2ir,
        ip2ir_p,
        wf2sl,
        ip2sl,
        ip2sl_p,
        wf2cc,
        ip2cc,
        ip2cc_p,
        gc_unknown;

        public static int default_ir_module(gc_model m) {
            return m == gc_100_12 || m == gc_100_18 ? 4
                    : m == gc_100_06 ? 2
                    : m == wf2ir || m == ip2ir || m == ip2ir_p ? 1
                    : -1;
        }
    }

    // Configlock
    public final static int locked = 0;
    public final static int unlocked = 1;

    // IP-Address assingment
    public final static int dhcp = 0;
    public final static int static_ip = 1;

    /** Type of configuration of a GlobalCache in/output */
    public enum gc_io_configuration {
        ir,
        sensor,
        sensor_notify,
        ir_nocarrier;
    }
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

    // should turn around at 65536, see GC API docs.
    private static int gc_index;

    private boolean valid_connector(int c) {
        return (c > 0) && (c <= 3);
    }

    public static void set_serial_timeout(int timeout) {
        socket_timeout = timeout;
    }

    /**
     * Turns a CCF string to a GC string.
     */
    /*public static String ccf_string2gc_string(String ccf_string, int count) {
        String gc_string = "";
        try {
            String sa[] = ccf_string.trim().split(" +");
            int type = Integer.parseInt(sa[0], 16);
            if (type != ir_code.ccf_type_learned) {
                System.err.println("Can only convert learned CCF codes.");
                System.exit(1);
            }
            int freq = ir_code.get_frequency(Integer.parseInt(sa[1], 16));
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
    }*/

    private boolean verbose = true;
    private gc_model gc_type = gc_model.gc_100_06;
    private java.lang.Process gc_process;

    private static String gc_joiner(int[] array) {
        String result = "";
        for (int i = 0; i < array.length; i++) {
            result = result + "," + array[i];
        }
        return result;
    }

    private String gc_string(IrSignal code, int count) {
        int[] intro = code.getIntroPulses();
        int[] repeat = code.getRepeatPulses();
        return code.getFrequency().intValue() + "," + count + "," + (1 + intro.length) + gc_joiner(intro) + gc_joiner(repeat);
    }

    public globalcache(String hostname, gc_model type, boolean verbose) {
        gc_host = (hostname != null) ? hostname : default_gc_host;
        default_ir_module = gc_model.default_ir_module(type);
        this.verbose = verbose;
    }

    public globalcache(String hostname, String type, boolean verbose) {
        this(hostname, gc_model.valueOf(type), verbose);
    }

    public globalcache(String hostname) {
        this(hostname, /*gc_model.gc_100_06*/ gc_model.gc_unknown, false);
    }

    public globalcache(String hostname, boolean verbose) {
        this(hostname, gc_model.gc_100_06, verbose);
    }

    public globalcache(gc_model type) {
        this(default_gc_host, type, false);
    }

    public globalcache(boolean verbose) {
        this(default_gc_host, gc_model.gc_100_06, verbose);
    }

    public globalcache() {
        this(default_gc_host, gc_model.gc_100_06, false);
    }

    public void set_verbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    private String connector_address(int module, int connector) {
        return "" + module + ":" + connector;
    }

    public byte[] send_read_serial(byte[] cmd, int serial_no, int return_bytes)
            throws UnknownHostException, IOException, NoRouteToHostException, InterruptedException {
        Socket sock = null;
        OutputStream outToServer = null;
        InputStream inFromServer = null;

        sock = socket_storage.getsocket(gc_host, gc_first_serial_port + serial_no - 1);
        if (sock == null)
            throw new IOException("got a null socket");
        sock.setSoTimeout(socket_timeout);
        outToServer = sock.getOutputStream();
        inFromServer = sock.getInputStream();

        byte[] result = new byte[return_bytes];
        try {
            outToServer.write(cmd);
            if (return_bytes > 0)
                inFromServer.read(result);
        } catch (SocketTimeoutException e) {
            System.err.println("Sockettimeout Globalcache: " + e.getMessage());
            result = null;
        } finally {
            outToServer.close();
            inFromServer.close();
            socket_storage.returnsocket(sock, false);//sock.close();
        }
        return result;
    }

    public String send_serial(String cmd, int serial_no, int return_lines,
            int count, int delay)
            throws UnknownHostException, IOException, NoRouteToHostException, InterruptedException {
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        sock = socket_storage.getsocket(gc_host, gc_first_serial_port + serial_no - 1);
        if (sock == null)
            throw new IOException("got a null socket");
        sock.setSoTimeout(socket_timeout);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        try {
            if (cmd != null)
                for (int c = 0; c < count; c++) {
                    if (delay > 0 && c > 0)
                        Thread.sleep(delay);

                    outToServer.print(cmd);
                    Thread.sleep(10);
                }

            for (int i = 0; i < return_lines; i++) {
                result = result + ((i > 0) ? "\n" : "") + inFromServer.readLine();
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Sockettimeout Globalcache: " + e.getMessage());
            result = null;
        } finally {
            outToServer.close();
            inFromServer.close();
            socket_storage.returnsocket(sock, false);//sock.close();
        }
        return result;
    }

    public String send_serial(String cmd, int serial_no, int return_lines)
            throws UnknownHostException, IOException, NoRouteToHostException, InterruptedException {
        return send_serial(cmd, serial_no, return_lines, 1, 0);
    }

    // Note: does not return, but loops forever!
    public void listen_serial(int serial_no)
            throws UnknownHostException, IOException, NoRouteToHostException, InterruptedException {
        Socket sock = null;
        BufferedReader inFromServer = null;
        String result = "";
        sock = socket_storage.getsocket(gc_host, gc_first_serial_port + serial_no - 1);
        inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        while (true) {
            result = inFromServer.readLine();
            // Ignore whitspace lines
            if (result.trim().length() > 0) {
                System.err.println(result);
            }
        }
    }

    public String send_serial(String cmd) throws UnknownHostException, IOException, InterruptedException {
        String result = send_serial(cmd, 1, 0);
        return result;
    }

    private String send_command(String cmd, boolean blind)
            throws UnknownHostException, IOException, InterruptedException {
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to GlobalCache (" + gc_host + ")");

        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        //InputStreamReader inFromServer = null;
        String result = "";

        try {
            sock = socket_storage.getsocket(gc_host, gc_port, false);
            if (sock == null)
                result = "Could not get socket to Globalcache";
            else {
                outToServer = new PrintStream(sock.getOutputStream());
                inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                outToServer.print(cmd + '\r');

                if (!blind) {
                    while (!inFromServer.ready())
                        Thread.sleep(20);

                    result = inFromServer.readLine();
                    while (inFromServer.ready()) {
                        result = (result.equals("") ? "" : (result + '\n')) + inFromServer.readLine();
                    }
                }
            }
        //} catch (Exception e) {
        //    System.err.println(e.toString() + e.getMessage());
        } finally {
            if (outToServer != null)
                outToServer.close();
            if (inFromServer != null)
                inFromServer.close();
            if (sock != null)
                socket_storage.returnsocket(sock, false);//sock.close();
        }

        if (verbose) {
            System.err.println(result);
        }

        return result;
    }

    private String[] send_command_array(String[] cmd, boolean blind, int delay_ms)
            throws UnknownHostException, IOException, InterruptedException {
        int length = cmd.length;
        String[] result = new String[length];
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        boolean was_interrupted = false;

        sock = socket_storage.getsocket(gc_host, gc_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        try {
            for (int i = 0; i < length; i++) {
                if (verbose)
                    System.err.println("Sending command (array) " + cmd[i] + " to GlobalCache (" + gc_host + ")");

                outToServer.print(cmd[i] + '\r');

                Thread.sleep(delay_ms);

                if (!blind) {
                    result[i] = inFromServer.readLine();// may hang
                    while (inFromServer.ready()) {
                        result[i] = (result[i].equals("") ? "" : (result[i] + '\n')) + inFromServer.readLine();
                    }
                }
            }
        } finally {
            outToServer.close();
            inFromServer.close();
            socket_storage.returnsocket(sock, false);//sock.close();
        }
        if (verbose)
            System.err.println(result[0]);

        return result;
    }

    public String send_command(String cmd) throws UnknownHostException, IOException, InterruptedException {
        return send_command(cmd, false);
    }

    public void stop_ir(int module, int connector) throws UnknownHostException, IOException, InterruptedException {
        send_command("stopir," + connector_address(module, connector), true);
    }

    public void stop_ir(int connector) throws UnknownHostException, IOException, InterruptedException {
        stop_ir(default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1);
    }

    public void stop_ir() throws UnknownHostException, IOException, InterruptedException {
        stop_ir(gc_default_connector);
    }

//    /**
//     * Transforms a GlobalCache send string to a CCF string.
//     *
//     * @param gc_string command string for GlobalCache
//     * @return CCF command as string
//     */
//    public static String gc2ccf(String gc_string) {
//        String[] str = gc_string.split(",");
//        int offset = 0;
//        while (str[offset].matches("\\D+")) // nukes off "sendir"
//            offset++;
//        while (str[offset].matches("\\d:\\d")) // nukes off module:connector
//            offset++;
//        while (Integer.parseInt(str[offset]) < 1000) // nukes off count
//            offset++;
//
//        int frequency = Integer.parseInt(str[offset++]);
//        offset++;
//        int intro_length = Integer.parseInt(str[offset++]) - 1;
//        int tot_length = str.length - offset;
//        int arr[] = new int[tot_length];
//        String result = String.format("0000 %04X %04X %04X",
//                Pronto.getProntoCode(frequency), intro_length/2, (tot_length-intro_length)/2);
//        for (int i = offset; i < str.length; i++)
//            result = result + String.format(" %04X", Integer.parseInt(str[i]));
//
//        return result;
//    }

    public boolean send_ir(IrSignal code, int module, int connector, int count) throws UnknownHostException, IOException, InterruptedException {
        if (!valid_connector(connector))
            connector = gc_default_connector;

        String cmd = "sendir," + connector_address(module, connector) + "," + gc_index + "," + gc_string(code, count);
        gc_index = (gc_index + 1) % 65536;
        String result = send_command(cmd);
        return result.startsWith("completeir");
    }

    public boolean send_ir(IrSignal code, int connector, int count) throws UnknownHostException, IOException, InterruptedException {
        return send_ir(code, default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1, count);
    }

    public boolean send_ir(IrSignal code, int connector) throws UnknownHostException, IOException, InterruptedException {
        return send_ir(code, connector, gc_default_connector);
    }

    public boolean send_ir(IrSignal code) throws UnknownHostException, IOException, InterruptedException {
        return send_ir(code, gc_default_connector);
    }

    public boolean send_ir(String ccf_string, int module, int connector, int count) throws UnknownHostException, IOException, InterruptedException, Pronto.NonProntoFormatException, InvalidArgumentException {
        return send_ir(Pronto.parse(ccf_string), module, connector, count);
        /*        if (!valid_connector(connector))
            connector = gc_default_connector;

        String cmd = "sendir," + connector_address(module, connector) + "," + gc_index + "," + ccf_string2gc_string(ccf_string, count);
        gc_index = (gc_index + 1) % 65536;
        String result = send_command(cmd);
        return result.startsWith("completeir");*/
    }

    public boolean send_ir(String ccf_string, int connector) throws UnknownHostException, IOException, InterruptedException, Pronto.NonProntoFormatException, InvalidArgumentException  {
        return send_ir(ccf_string, default_ir_module, connector, gc_default_connector);
    }

    public boolean send_ir(String ccf_string, int connector, int count) throws UnknownHostException, IOException, InterruptedException, Pronto.NonProntoFormatException, InvalidArgumentException {
        return send_ir(ccf_string, default_ir_module + (connector - 1) / 3, (connector - 1) % 3 + 1, count);
    }

    public String getdevices() throws UnknownHostException, IOException, InterruptedException {
        return send_command("getdevices");
    }

    public String getversion(int module) throws UnknownHostException, IOException, InterruptedException {
        return send_command("getversion," + module);
    }

    public String getversion() throws UnknownHostException, IOException, InterruptedException {
        return getversion(0);
    }

    public String getnet() throws UnknownHostException, IOException, InterruptedException {
        return send_command("get_NET,0:1");
    }

    public String setnet(String arg) throws UnknownHostException, IOException, InterruptedException {
        return send_command("set_NET,0:1," + arg);
    }

    public String getir(int module, int connector) throws UnknownHostException, IOException, InterruptedException {
        return send_command("get_IR," + connector_address(module, connector));
    }

    /*
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
     */
    public String setir(int module, int connector, String modestr) throws UnknownHostException, IOException, InterruptedException {
        return send_command("set_IR," + connector_address(module, connector) + "," + modestr.toUpperCase());
    }

    public String setir(int module, int connector, gc_io_configuration mode) throws UnknownHostException, IOException {
        return setir(module, connector, mode);
    }

    public String getserial(int module) throws UnknownHostException, IOException, InterruptedException {
        return send_command("get_SERIAL," + connector_address(module, 1));
    }

    public String setserial(int module, String arg) throws UnknownHostException, IOException, InterruptedException {
        return send_command("set_SERIAL," + connector_address(module, 1) + "," + arg);
    }

    public String setserial(int module, int baudrate) throws UnknownHostException, IOException, InterruptedException {
        return send_command("set_SERIAL," + connector_address(module, 1) + "," + baudrate);
    }

    public int getstate(int module, int connector) throws UnknownHostException, IOException, InterruptedException {
        int result = -1;
        try {
            result = Integer.parseInt(send_command("getstate," + connector_address(module,
                    connector)).substring(10, 11));
        } catch (NumberFormatException e) {
            System.err.println("Error getting status of input on module " + module + ", connector " + connector);
        }
        return result;
    }

    public int getstate(int connector) throws UnknownHostException, IOException, InterruptedException {
        return getstate(default_ir_module + ((connector - 1) / 3), (connector - 1) % 3 + 1);
    }

    // Seems quite inefficient
    public boolean togglestate(int connector) throws UnknownHostException, IOException, InterruptedException {
        return setstate(default_relay_module, connector,
                1 - getstate(default_relay_module, connector));
    }

    public boolean setstate(int module, int connector, int state) throws UnknownHostException, IOException, InterruptedException {
        String result = send_command("setstate," + connector_address(module, connector) + "," + (state == 0 ? 0 : 1));
        if (verbose) {
            System.err.println(result.substring(0, 5));
        }
        return result.substring(0, 5).equals("state");
    }

    public boolean setstate(int connector, int state) throws UnknownHostException, IOException, InterruptedException {
        return setstate(default_relay_module, connector, state);
    }

    public boolean setstate(int connector, boolean on_off) throws UnknownHostException, IOException, InterruptedException {
        return setstate(connector, on_off ? 1 : 0);
    }

    public boolean pulsestate(int module, int connector) throws UnknownHostException, IOException, InterruptedException {
        String[] cmd = new String[2];
        cmd[0] = "setstate," + connector_address(module, connector) + ",1";
        cmd[1] = "setstate," + connector_address(module, connector) + ",0";

        String[] result = send_command_array(cmd, true, 300);
        return true;
    }

    public boolean pulsestate(int connector) throws UnknownHostException, IOException, InterruptedException {
        return pulsestate(default_relay_module, connector);
    }

    public void set_blink(int arg) throws UnknownHostException, IOException, InterruptedException {
        send_command("blink," + arg, true);
    }

    public boolean is_valid() {
        try {
            String result = getdevices();
        } catch (Exception e) {
            System.err.println(e.toString());
            return false;
        }
        return true;
    }

    public static amx_beacon.result listen_beacon() {
        return amx_beacon.listen_for("-Make", "GlobalCache", beacon_timeout);
    }

    public static void usage() {
        System.err.println("Usage:");
        System.err.println("globalcache [options] <command> [<argument>]");
        System.err.println("where options=-# <count>,-h <hostname>,-c <connector>,-m <module>,-b <baudrate>,-v,-B");
        System.err.println("and command=send_ir,send_serial,listen_serial,set_relay,get_devices,get_version,set_blink,[set|get]_serial,[set|get]_ir,[set|get]_net,[get|set]_state,ccf");
        System.exit(1);
    }
/*
    public static void main(String[] args) {
        String hostname = default_gc_host;
        int connector = 1;
        int module = 2;
        int count = 1;
        boolean verbose = false;
        boolean beacon = false;
        int baudrate = 0; // invalid value
        globalcache gc = null;
        String cmd = null;
        String arg = null;

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
                } else if (args[arg_i].equals("-B")) {
                    beacon = true;
                    arg_i++;
                } else {
                    usage();
                }
            }
            if (!beacon) {
                gc = new globalcache(hostname, verbose);
                cmd = args[arg_i];
                arg = (args.length > arg_i + 1) ? args[arg_i + 1] : "";
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }



        String output = "";

        if (beacon) {
            amx_beacon.result r = listen_beacon();
            System.err.println(r);
        } else {
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
                } else if (cmd.equals("stop_ir")) {
                    gc.stop_ir(module, connector);
                } else if (cmd.equals("listen_serial")) {
                    System.err.println("Press Ctrl-C to interrupt.");
                    // Never returns
                    gc.listen_serial(module);
                } else if (cmd.equals("ccf")) {
                    String s = "";
                    for (int i = arg_i + 1; i < args.length; i++)
                        s = s + args[i];
                    System.out.println(gc2ccf(s));
                } else {
                    usage();
                }
            } catch (UnknownHostException e) {
                System.err.println("Host " + gc.gc_host + " does not resolve.");
                System.exit(1);
            } catch (IOException e) {
                System.err.println("IOException occured.");
                System.exit(1);
            } catch (IrpMasterException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            } catch (InterruptedException e) {
            }
            System.err.println(output);
        }
    }*/
}
