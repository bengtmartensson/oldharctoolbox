/*
Copyright (C) 2009-2011, 2019 Bengt Martensson.

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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.Wol;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.misc.EzControlT10;
import org.harctoolbox.harchardware.misc.SonySerialCommand;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.XmlUtils;
import org.harctoolbox.irp.IrpUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class is immutable
 *
 */
public final class Home {

    private final static int max_hops = 10;
    private static final Logger logger = Logger.getLogger(Home.class.getName());

    private static void usage(int errorcode) {
        String message = "Usage:\n"
                + "home [<options>] <device_instancename> <command> [<command_args>]*"
                + "\nwhere options=-h <filename>,-t "
                + CommandType_t.valid_types('|')
                + ",-m,-T 0|1,-# <count>,-v,-d <debugcode>, -p <propsfile>\n"
                + "or\n"
                + "home -s [-z zone][-A,-V][-c connection_type] <device_instancename> <src_device>";
        HarcUtils.doExit(errorcode, message);
    }

    private static void usage() {
        usage(IrpUtils.EXIT_USAGE_ERROR);
    }

    public static void main(String[] args) {
        CommandType_t type = CommandType_t.any;
        String home_filename = null;
        int debug = 0;
        boolean verbose = false;
        int count = 1;
        MediaType the_mediatype = MediaType.audio_video;
        boolean smart_memory = false;
        boolean select_mode = false;
        boolean list_commands = false;
        String devname = "";
        String src_device = "";
        String zone = null;
        ConnectionType connection_type = ConnectionType.any;
        command_t cmd = command_t.invalid;
        ToggleType toggle = ToggleType.dont_care;
        String[] arguments = null;
        String propsfilename = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                switch (args[arg_i]) {
                    case "-#":
                        arg_i++;
                        count = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-c":
                        arg_i++;
                        connection_type = ConnectionType.valueOf(args[arg_i++]);
                        break;
                    case "-d":
                        arg_i++;
                        debug = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-h":
                        arg_i++;
                        home_filename = args[arg_i++];
                        break;
                    case "-m":
                        arg_i++;
                        smart_memory = true;
                        break;
                    case "-p":
                        arg_i++;
                        propsfilename = args[arg_i++];
                        break;
                    case "-s":
                        arg_i++;
                        select_mode = true;
                        break;
                    case "-t":
                        arg_i++;
                        String typename = args[arg_i++];
                        if (!CommandType_t.is_valid(typename)) {
                            usage();
                        }   type = CommandType_t.valueOf(typename);
                        break;
                    case "-v":
                        arg_i++;
                        verbose = true;
                        break;
                    case "-z":
                        arg_i++;
                        zone = args[arg_i++];
                        break;
                    case "-A":
                        arg_i++;
                        the_mediatype = MediaType.audio_only;
                        break;
                    case "-V":
                        arg_i++;
                        the_mediatype = MediaType.video_only;
                        break;
                    case "-T":
                        arg_i++;
                        toggle = ToggleType.decode_toggle(args[arg_i++]);
                        break;
                    default:
                        usage();
                        break;
                }
            }

            DebugArgs.setState(debug);
            devname = args[arg_i];

            Props props = new Props(propsfilename, "oldharctoolbox");

            if (home_filename == null)
                home_filename = Main.getProperties().getHomeConf();

            if (select_mode) {
                src_device = args[arg_i + 1];
                if (DebugArgs.dbg_decode_args()) {
                    System.err.println("Select mode: devname = " + devname
                            + ", src_device = " + src_device
                            + " (connection_type = " + connection_type + ").");
                }
            } else if (!devname.equals("?")) {
                String cmdname = args[arg_i + 1];
                list_commands = cmdname.equals("?");
                cmd = command_t.parse(cmdname);
                // Compatibility with old command names. Or simply silly?
                //if (cmd == command_t.invalid) {
                //    System.err.println("Warning: Substituting non-existing commands " + cmdname + " by cmd_" + cmdname);
                //    cmd = ir_code.decode_command("cmd_" + cmdname);
                //}
                if (DebugArgs.dbg_decode_args()) {
                    System.err.println("devname = " + devname + ", commandname = " + args[arg_i + 1] + "(#" + cmd + ")");
                }

                if (!list_commands && cmd == command_t.invalid)
                    HarcUtils.doExit(IrpUtils.EXIT_SEMANTIC_USAGE_ERROR, "Command \"" + args[arg_i + 1] + "\" not recognized, aborting.");

                int no_arguments = args.length - arg_i - 2;
                arguments = new String[no_arguments];

                if (DebugArgs.dbg_decode_args() && no_arguments > 0) {
                    System.err.print("Command arguments: ");
                }

                for (int i = 0; i < no_arguments; i++) {
                    arguments[i] = args[arg_i + 2 + i];
                    if (DebugArgs.dbg_decode_args()) {
                        System.err.print("arguments[" + i + "] = " + arguments[i] + (i == no_arguments - 1 ? ".\n" : ", "));
                    }
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            if (DebugArgs.dbg_decode_args()) {
                System.err.println("ArrayIndexOutOfBoundsException");
            }
            usage();
        } catch (NumberFormatException e) {
            if (DebugArgs.dbg_decode_args()) {
                System.err.println("NumberFormatException");
            }
            usage();
        }

        Main.getProperties().setVerbose(verbose);
        Main.getProperties().setDebug(debug);
        try {
            Home hm = new Home(home_filename);
            if (select_mode) {
                if (src_device.equals("?")) {
                    HarcUtils.printtable("Valid inputs for " + devname + (zone != null ? (" in zone " + zone) : "") + ":", hm.get_sources(devname, zone));
                } else {
                    hm.select(devname, src_device, type, zone, the_mediatype, connection_type);
                }
            } else if (devname.equals("?")) {
                HarcUtils.printtable("Valid devices:", hm.get_devices());
            } else if (list_commands) {
                // FIXME: if devname is nonexisting, should produce an error message instead of just returning nothing.
                HarcUtils.printtable("Valid commands for " + devname + " of type " + type + ":", hm.get_commands(devname, type));
            } else {
                String output = hm.do_command(devname, cmd, arguments, type, count, toggle, smart_memory);
                if (output == null) {
                    HarcUtils.doExit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE, "** Failure **");
                } else if (!output.isEmpty()) {
                    System.out.println("Command output: \"" + output + "\"");
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot read file " + home_filename + " (" + e.getMessage() + ").");
            HarcUtils.doExit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        } catch (SAXParseException e) {
            System.err.println("Parse error in " + home_filename + " (" + e.getMessage() + ").");
            HarcUtils.doExit(IrpUtils.EXIT_XML_ERROR);
        } catch (SAXException e) {
            System.err.println("Parse error in " + home_filename + " (" + e.getMessage() + ").");
            HarcUtils.doExit(IrpUtils.EXIT_XML_ERROR);
        } catch (InterruptedException e) {
            System.err.println("** Interrupted **");
            HarcUtils.doExit(18);
        }
    }

    private final HashMap<String, String> alias_table;
    // The get method of device_table should not be used, use get_dev(String) instead.
    private final LinkedHashMap<String, Dev> device_table;
    private final LinkedHashMap<String, DeviceGroup> device_groups_table; // indexed by name, not id
    private final HashMap<String, Gateway> gateway_table;

    public Home(String home_filename) throws IOException, SAXParseException, SAXException {
        this(new File(home_filename));
    }

    public Home(File home_filename/*, boolean verbose, int debug*/) throws IOException, SAXParseException, SAXException {
        Document doc = XmlUtils.openXmlFile(home_filename);
        if (DebugArgs.dbg_dom())
            System.err.println("Home configuration " + home_filename + " parsed.");

       HomeParser parser = new HomeParser(doc);
       alias_table = parser.get_alias_table();
       device_table = parser.get_device_table();
       device_groups_table = parser.get_device_groups_table();
       gateway_table = parser.get_gateway_table();
    }

    // was private, cannot remember if there was a reason :-\
    public Dev get_dev(String device) {
        device = expand_alias(device);
        return device == null ? null : device_table.get(device);
    }

    public String[] get_zones(String device) {
        Dev d = get_dev(device);
        return d != null ? d.get_zone_names().toArray(new String[d.get_zone_names().size()]) : null;
    }

    public boolean has_zone(String device, String zone) {
        if (zone == null || zone.isEmpty())
            // assume this means "main" or "any" or "not applicable"
            return true;

        Dev d = get_dev(device);
        if (d == null)
            return false;
        HashSet<String>zones = d.get_zone_names();
        return zones.contains(zone);
    }

    /**
     *
     * @param devicegroup name (not id) of devicegroup in home file
     * @return array of strings of device names
     */ //TODO use id instead of name?
    public String[] get_devices(String devicegroup) {
        DeviceGroup dg = device_groups_table.get(devicegroup);
        return dg != null ? dg.get_devices().toArray(new String[0]) : null;
    }

    /**
     *
     * @return all devices in the home file, except for those of class "null"
     */
    public String[] get_devices() {
        int n = device_table.size();
        ArrayList<String> v = new ArrayList<>(n);
        device_table.keySet().stream().filter((id) -> (!device_table.get(id).get_class().equals("null"))).forEachOrdered((id) -> {
            v.add(id);
        });

        return v.toArray(new String[v.size()]);
    }

    public String[] get_selecting_devices() {
        ArrayList<String> v = new ArrayList<>(32);
        device_table.values().stream().filter((d) -> (!d.get_inputs().isEmpty())).forEachOrdered((d) -> {
            v.add(d.get_id());
        });

        return v.toArray(new String[v.size()]);
    }

    public int get_arguments(String devname, command_t cmd, CommandType_t cmdtype) {
        Device dev = get_device(expand_alias(devname));
        if (dev == null || !dev.is_valid())
            return (int) IrCoreUtils.INVALID;

        Command command = dev.get_command(cmd, cmdtype);
        return command != null ? dev.get_command(cmd, cmdtype).get_arguments().length : 0;
    }

    public String[] get_commands(String devname, CommandType_t cmdtype) {
        Device dev = get_device(devname);

        if (dev == null || !dev.is_valid()) {
            return null;
        }

        command_t[] cmds = dev.get_commands(cmdtype);
        String[] result = new String[cmds.length];
        for (int i = 0; i < cmds.length; i++) {
            result[i] = cmds[i].toString();
        }

        return HarcUtils.sort_unique(result);
    }

    public String[] get_commands(String devname) {
        return get_commands(devname, CommandType_t.any);
    }

    public String[] get_devicegroups() {
        return device_groups_table.keySet().toArray(new String[device_groups_table.size()]);
    }

    public String[] get_sources(String devname, String zone) {
        Dev d = get_dev(devname);
        return d == null ? null : d.get_sources(zone).toArray(new String[d.get_sources(zone).size()]);
    }

    /**
     *
     * @param devname Name of device
     * @return true if the device has commands for selecting only audio or video.
     */
    public boolean has_av_only(String devname) {
        Dev d = get_dev(devname);
        return d != null && d.has_separate_av_commands();
    }

    // Does not take zones into account. This is the right way.
    public ConnectionType[] get_connection_types(String devname, String src_device) {
        Dev d = get_dev(devname);
        if (d == null) {
            System.err.println("No such device \"" + devname + "\".");
            return null;
        }
        HashSet<ConnectionType> hs = d.get_connection_types(expand_alias(src_device));
        return hs.toArray(new ConnectionType[hs.size()]);
    }

    public boolean select(String devname, String src_device, CommandType_t type,
            String zone, MediaType the_mediatype, ConnectionType conn_type)
            throws InterruptedException {
        if (DebugArgs.dbg_dispatch())
            System.err.println("select called: devname = " + devname + ", src_device = " + src_device
                    + ", type = "  + type + ", zone = " + zone + ", mediatype = " + the_mediatype
                    + ", connection = " + conn_type);
        Dev d = get_dev(devname);

        if (d == null) {
            System.err.println("No device " + devname + ".");
            return false;
        }
        if (!has_zone(devname, zone)) {
            System.err.println("No such zone `" + zone + "'");
            return false;
        }

        String source = expand_alias(src_device);
        if (source == null) {
            System.err.println("No source device " + src_device + ".");
            return false;
        }
        Input inp = d.find_input(source, conn_type);
        if (inp == null) {
            System.err.println("No input exists on " + devname + " to " + src_device + " using connection_type " + conn_type + ".");
            return false;
        }

        if ((zone == null || zone.isEmpty()) && !d.get_defaultzone().isEmpty())
            zone = d.get_defaultzone();

        command_t select_command = inp.get_select_command(zone, the_mediatype);
        Input.QueryCommand query_command = inp.get_query_command(zone, the_mediatype);

        if (select_command == command_t.invalid) {
            System.err.println("No command found for turning " + devname + " to " + src_device + ((zone != null && !zone.isEmpty()) ? (" in zone " + zone) : "") + (the_mediatype == MediaType.audio_only ? " (audio only)"
                    : the_mediatype == MediaType.video_only ? " (video only)"
                    : "") + (conn_type == null ? "" : " using connection_type " + conn_type) + ".");
            return false;
        }

        if (DebugArgs.dbg_decode_args())
            System.err.println("Found select command: " + select_command + (query_command != null ? (", query: " + query_command.get_command() + "==" + query_command.get_expected_response() + ".") : ", no query command found."));

        // Queuery has problems for the current setup, where everything is closed between invocations.
        // Besides, this is better done in higher-level macros.
        // Commenting out for now.

//        if (query_command != null) {
//            String result = do_command(devname, query_command.get_command(),
//                    new String[0], type, 1, ToggleType.toggle_0, false);
//            if (result != null) {
//                Device device = null;
//                try {
//                    device = Device.newDevice(d.get_class(), d.get_attributes());
//                } catch (IOException | SAXException ex) {
//                    Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                Command command = device.get_command(query_command.get_command());
//                int lines = command.get_response_lines();
//                if (lines > 1) {
//                    String[] res = result.split("\n");
//                    result = res[lines - 1];
//                }
//                if (result.equals(query_command.get_expected_response())) { // FIXME
//                    if (DebugArgs.dbg_decode_args())
//                        System.err.println(devname + " already turned to " + src_device + ", ignoring.");
//
//                    return true;
//                }
//            }
//        }

        return do_command(devname, select_command, new String[0], type, 1, ToggleType.toggle_0, false) != null;
    }

    // Really generate and send the command, if possible

    // TODO: This function should be restructured. Really...
    @SuppressWarnings("SleepWhileInLoop")
    private String transmit_command(String dev_class, command_t cmd, String[] arguments, String house, int deviceno,
            Gateway gw, GatewayPort fgw, CommandType_t type, int count, ToggleType toggle,
            Map<String, String> attributes, String flavor) throws InterruptedException {
        if (DebugArgs.dbg_transmit())
            System.err.println("transmit_command: device " + dev_class
                    + ", command " + cmd + ", house " + house
                    + ", deviceno " + deviceno + ", type " + type
                    + ", count " + count + ", gw_class " + gw.get_class()
                    + ", gw_hostname " + gw.get_hostname() + ", port_hostname " + fgw.get_hostname()
                    + ", port " + fgw.get_portnumber() + ", connectortype " + fgw.get_connectortype()
                    + ", connectorno " + fgw.get_connectorno() + ", model " + gw.get_model()
                    + ", interface " + gw.get_interface() + ", toggle " + toggle);

        int portnumber = fgw.get_portnumber();
        String output = null;
        boolean success = false;
        boolean failure = false;
        Device dev = null;
        Command the_command = null;
        // Use arguments_length instead of arguments.length to allow for arguments == null
        int arguments_length = arguments == null ? 0 : arguments.length;
        try {
            dev = Device.newDevice(dev_class, attributes/*, false*/);
            the_command = dev.get_command(cmd, type);
            if (the_command != null && the_command.get_minsends() > count)
                count = the_command.get_minsends();
        } catch (IOException | SAXException e) {
            // May be ok, e.g. when using Intertechno and T-10.
//            if (DebugArgs.dbg_transmit())
//                System.err.println("Could not open file " + Main.getInstance().getProperties().getDevicesDir()+ File.separator + dev_class + HarcUtils.devicefile_extension + ".");
            logger.warning(e.getMessage());
        }

        String result = "";

        if (type == CommandType_t.www) {
            if (cmd != command_t.browse && !Main.getProperties().getUseWwwForCommands()) {
                if (Main.getProperties().getVerbose())
                    System.err.println("Command of type www ignored.");

                failure = true;
            }
        } else {
            if (!gw.get_class().equals("ezcontrol_t10")) {
                if (!dev.is_valid()) { // FIXME
                    failure = true;
                } else {
                    if (the_command == null) {
                        if (DebugArgs.dbg_transmit())
                            System.err.println("No such command " + cmd + " of type " + type + " (" + type + ")");
                        failure = true;
                    } else {
                        if (DebugArgs.dbg_transmit()) {
                            System.err.println("Command is: " + the_command.toString());
                        }
                        if (the_command.get_arguments().length > arguments_length) {
                            System.err.println("This command requires " + the_command.get_arguments().length + " argument(s), however only " + arguments_length + " were given.");
                            failure = true;
                        }
                    }
                }
            }
        }

        if (!failure) {
            switch (type) {
                case any:
                    System.err.println("Programming/configuration error: transmit_command called with type=any.");
                    failure = true;
                    break;
                case ir:
                    if (arguments_length > 0)
                        System.err.println("Warning: arguments to command igored.");

                    try {
                        if (gw.get_class().equals("globalcache")) {
                            if (DebugArgs.dbg_transmit()) {
                                System.err.println("Trying Globalcache (" + gw.get_hostname() + ")...");
                            }
                            IrSignal irSignal = dev.get_code(cmd, CommandType_t.ir, toggle, DebugArgs.dbg_ir_protocols(), house, (short) (deviceno - 1));
                            if (irSignal == null) {
                                if (Main.getProperties().getVerbose())
                                    System.err.println("Command " + cmd + " exists, but has no ir code.");

                                failure = true;
                            } else {
                                try (GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ Main.getProperties().getVerbose())) {
                                    GlobalCache.GlobalCacheIrTransmitter transmitter = gc.newTransmitter(fgw.get_connectorno());
                                    success = gc.sendIr(irSignal, count, transmitter);
                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.println("Globalcache " + (success ? "succeeded" : "failed"));
                                    }
                                }
                            }
                        }
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw.get_hostname());
                    } catch (IOException e) {
                        System.err.println("IOException with host " + gw.get_hostname() + ": " + e.getMessage());
                    } catch (NoSuchTransmitterException ex) {
                        Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;

                case rf:
                    if (DebugArgs.dbg_transmit()) {
                        System.err.println("Trying rf to " + gw.get_class());
                    }
                    if (gw.get_class().equals("ezcontrol_t10")) {
                        int power = 0;
                        int arg = (int) IrCoreUtils.INVALID;
                        if (cmd == command_t.set_power || cmd == command_t.dim_value_time) {
                            if (arguments.length > 0)
                                power = Integer.parseInt(arguments[0]);
                            if (arguments.length > 1)
                                arg = Integer.parseInt(arguments[1]);
                        } else if (arguments.length > 0)
                            arg = Integer.parseInt(arguments[0]);

                        if (power < 0 || power > 100) {
                            System.err.println("Invalid power argument.");
                            failure = true;
                        } else {
                            EzControlT10 t10 = new EzControlT10(gw.get_hostname(), Main.getProperties().getVerbose());
                            int preset = (int) IrCoreUtils.INVALID;
                            try {
                                preset = fgw.get_connectorno();
                                if (DebugArgs.dbg_transmit())
                                    System.err.println("Preset no = " + preset);
                            } catch (StringIndexOutOfBoundsException | NumberFormatException e) {
                                System.err.println("preset number not parseable");
                                failure = true;
                            }

                            try {
                                EzControlT10.Command t10cmd = EzControlT10.Command.valueOf(cmd.toString());
                                if (preset != IrCoreUtils.INVALID && t10cmd.isPresetCommand())
                                    switch (cmd) {
                                        case get_status:
                                            output = t10.getPresetStatus(preset);
                                            break;
                                        case set_power:
                                            success = t10.sendPreset(preset, power);
                                            break;
                                        default:
                                            success = t10.sendPreset(preset, t10cmd, count);
                                            break;
                                    }
                                else {
                                    if (cmd == command_t.get_status || cmd == command_t.power_toggle) {
                                        System.err.println("This command only implemented for presets.");
                                        failure = true;
                                    } else {
                                        EzControlT10.EZSystem system = EzControlT10.EZSystem.parse(dev_class);
                                        success = t10.sendManual(system, house, deviceno, t10cmd, power, arg, count);
                                    }
                                }
                            } catch (IllegalArgumentException e) {
                                System.err.println("Command not implemented");
                                failure = true;
                            } catch (HarcHardwareException ex) {
                                Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    break;
                case tcp:
                    if (portnumber == IrCoreUtils.INVALID)
                        portnumber = dev.get_portnumber(cmd, CommandType_t.tcp);
                    String subst_transmitstring = dev.get_command(cmd, CommandType_t.tcp).get_transmitstring(true);
                    String subst_transmitstring_printable = dev.get_command(cmd, CommandType_t.tcp).get_transmitstring(false);
                    for (int i = 0; i < arguments_length; i++) {
                        subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                        subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                    }
                    if (DebugArgs.dbg_transmit())
                        System.err.println("Trying TCP socket to " + fgw.get_hostname() + ":" + portnumber + " \"" + subst_transmitstring_printable + "\"");

                    try (Socket sock = new Socket(fgw.get_hostname(), portnumber)) {
                        //Socket sock = SocketStorage.getsocket(fgw.get_hostname(), portnumber); //new Socket(gw_hostname, portnumber);
                        sock.setSoTimeout(fgw.get_timeout());
                        if (DebugArgs.dbg_transmit())
                            System.err.println("Setting timeout on TCP socket to " + fgw.get_timeout());
                        //System.err.println(sock.getSoLinger() + " " + sock.getSoTimeout() + " " + sock.getReuseAddress() + " " + sock.getKeepAlive());
                        PrintStream outToServer = new PrintStream(sock.getOutputStream(), true, IrCoreUtils.DEFAULT_CHARSET_NAME);
                        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream(), IrCoreUtils.DEFAULT_CHARSET_NAME));

                        String opener = dev.get_open(cmd, CommandType_t.tcp);
                        if (opener != null && !opener.isEmpty()) {
                            if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                                System.err.println("Sending opening command \"" + opener + "\" to socket " + fgw.get_hostname() + ":" + portnumber);
                            outToServer.print(opener);
                        }

                        if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose()) {
                            System.err.println("Sending command \"" + subst_transmitstring_printable + "\" to socket " + fgw.get_hostname() + ":" + portnumber + (count == 1 ? " (one time)" : (" (" + count + " times)")));
                        }
                        for (int c = 0; c < count; c++) {
                            int delay_between_reps = the_command.get_delay_between_reps();
                            if (delay_between_reps > 0 && c > 0) {
                                if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                                    System.err.println("Waiting for " + delay_between_reps + "ms, then sending.");

                                Thread.sleep(delay_between_reps);
                            }
                            outToServer.print(subst_transmitstring);
                        }

                        if (dev.get_command(cmd, CommandType_t.tcp).get_response_lines() < 0) {
                            if (dev.get_command(cmd, CommandType_t.tcp).get_response_ending().isEmpty()) {
                                // Loop until interrupted
                                System.err.println("*** This will loop until interrupted");
                                for (;;) {
                                    while (!inFromServer.ready())
                                        Thread.sleep(20);
                                    System.out.println(inFromServer.readLine());
                                }
                            } else {
                                int i = 0;
                                boolean found = false;
                                do {
                                    if (i++ > 0) {
                                        result += "\n";
                                    }
                                    while (!inFromServer.ready())
                                        Thread.sleep(20);
                                    String l = inFromServer.readLine();
                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.println("Got: " + l);
                                    }
                                    result += l;
                                    found = l.equals(dev.get_command(cmd, CommandType_t.tcp).get_response_ending());
                                } while (!found);
                            }
                        } else {
                            for (int i = 0; i < dev.get_command(cmd, CommandType_t.tcp).get_response_lines(); i++) {
                                if (i > 0) {
                                    result += "\n";
                                }
                                //while (!inFromServer.ready())
                                //    Thread.sleep(20); // Danger of hanging
                                result += inFromServer.readLine();
                            }
                        }

                        String closer = dev.get_close(cmd, CommandType_t.tcp);
                        if (closer != null && !closer.isEmpty()) { // This code is not tested
                            if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                                System.err.println("Sending closing command \"" + closer + "\" to socket " + gw.get_hostname() + ":" + portnumber);
                            outToServer.print(closer);
                        }
                        if (dev.get_command(cmd, CommandType_t.tcp).get_response_lines() != 0) {
                            output = result;
                        }
                        success = true;
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw.get_hostname());
                    } catch (IOException e) {
                        System.err.println("Could not get I/O for the connection to "
                                + fgw.get_hostname() + ":" + portnumber + " " + e.getMessage());
                        failure = true;
                    }
                    break;

                case web_api:
                    subst_transmitstring = dev.get_command(cmd, CommandType_t.web_api).get_transmitstring(true);
                    subst_transmitstring_printable = dev.get_command(cmd, CommandType_t.web_api).get_transmitstring(false);
                    for (int i = 0; i < arguments_length; i++) {
                        subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                        subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                    }
                    if (gw.get_class().equals("lan")) {
                        String urlstr = "http://" + fgw.get_hostname() + ":" + portnumber + "/" + subst_transmitstring;
                        int response_lines = the_command.get_response_lines();
                        if (response_lines == 0 && !the_command.get_expected_response().isEmpty()) // this is contradictory, fix
                            response_lines = 1;
                        HttpURLConnection url_connection = null;
                        try {
                            URL the_url = new URL(urlstr);
                            url_connection = (HttpURLConnection) the_url.openConnection();
                            url_connection.setConnectTimeout(fgw.get_timeout());
                            url_connection.setReadTimeout(fgw.get_timeout());
                            if (DebugArgs.dbg_transmit())
                                System.err.println("Set timeout to " + fgw.get_timeout() + "ms");
                            String charset = dev.get_command(cmd, CommandType_t.web_api).get_charset();

                            try (BufferedReader inFromServer = new BufferedReader(new InputStreamReader(url_connection.getInputStream(), charset))) {
                                for (int c = 0; c < count; c++) {
                                    if (c > 0) {
                                        int delay_between_reps = the_command.get_delay_between_reps();
                                        if (delay_between_reps > 0) {
                                            if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose()) {
                                                System.err.println("Waiting for " + delay_between_reps + "ms.");
                                            }
                                            Thread.sleep(delay_between_reps);
                                        }
                                    }

                                    if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                                        System.err.println("Getting URL " + urlstr + ".");

                                    if (response_lines == 0) {
                                        url_connection.getContent();
                                    } else if (response_lines > 0) {
                                        output = "";
                                        for (int i = 0; i < response_lines; i++) {
                                            result = inFromServer.readLine();
                                            if (result != null)
                                                output = output.isEmpty() ? result : output + "\n" + result;
                                            if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose() || (the_command.get_expected_response().isEmpty() && the_command.get_response_lines() == 0)) {
                                                System.err.println("Got: " + result);	// ??
                                            }
                                        }
                                    } else {
                                        output = "";
                                        do {
                                            result = inFromServer.readLine();
                                            if (result != null)
                                                output = output.isEmpty() ? result : output + "\n" + result;
                                            if ((DebugArgs.dbg_transmit() || Main.getProperties().getVerbose()) && result != null) {
                                                System.out.println(result);	// ??
                                            }
                                        } while (result != null);
                                    }

                                    if (!the_command.get_expected_response().isEmpty()) {
                                        if (result.equals(the_command.get_expected_response())) {
                                            if (DebugArgs.dbg_transmit()) {
                                                System.err.println("response equals expected.");
                                            }
                                            success = true;
                                        } else {
                                            System.err.println("response (= " + result + ") does not equal expected (= " + the_command.get_expected_response() + ").");
                                            success = false;
                                        }
                                    } else {
                                        success = true;
                                    }
                                }
                            }
                        } catch (java.io.IOException e) {
                            failure = true;
                            System.err.println("IOException for " + urlstr + ": " + e.getMessage());
                        } finally {
                            if (url_connection != null) {
                                url_connection.disconnect();
                            }
                        }
                    }
                    break;

                case serial:
                    // TODO: handle cases expected_lines < 0 (loop forever) senisble, (if possible).
                    if (gw.get_class().equals("globalcache")) {
                        try (GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ Main.getProperties().getVerbose())) {
                            GlobalCache.SerialPort gcSerialPort = gc.getSerialPort(fgw.get_connectorno());
                            subst_transmitstring = dev.get_command(cmd, CommandType_t.serial).get_transmitstring(true);
                            subst_transmitstring_printable = dev.get_command(cmd, CommandType_t.serial).get_transmitstring(false);
                            if (flavor.equals("SonySerialCommand")) {

                                String[] cmds = subst_transmitstring_printable.split(",");
                                byte[] bytes = SonySerialCommand.bytes(Integer.parseInt(cmds[1]), Integer.parseInt(cmds[2]), cmds[0]);
                                gcSerialPort.sendBytes(bytes);
                                if (the_command.get_response_lines() > 0) {
                                    byte[] answer = gcSerialPort.readBytes(SonySerialCommand.size);
                                    output = Integer.toString(SonySerialCommand.interpret(answer).getData());
                                } else
                                    output = "";
                            } else {
                                // TODO: expand expected_response similarly.
                                for (int i = 0; i < arguments_length; i++) {
                                    subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                                    subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                                }

                                if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                                    System.err.println("Trying Globalcache (" + gw.get_hostname() + ") for serial using " + fgw.get_connectorno() + ", \"" + subst_transmitstring_printable + "\"");

                                int delay_between_reps = the_command.get_delay_between_reps();
                                int no_read_lines = the_command.get_response_lines();

                                gcSerialPort.sendString(subst_transmitstring);
                                if (no_read_lines > 0) {
                                    if (delay_between_reps > 0)
                                        Thread.sleep(delay_between_reps);
                                    result = gcSerialPort.readString(true);
                                     no_read_lines--;
                                } else
                                    result = "";

                                if (!the_command.get_expected_response().isEmpty()) {
                                    if (result.equals(the_command.get_expected_response())) {
                                        if (DebugArgs.dbg_transmit()) {
                                            System.err.println("response equals expected.");
                                        }
                                        success = true;
                                    } else {
                                        System.err.println("response (= " + result + ") does not equal expected (= " + the_command.get_expected_response() + ").");
                                        success = false;
                                    }
                                } else {
                                    success = result != null;
                                }
                                if (no_read_lines != 0)
                                    System.err.println(">>> " + result);
                                for (int i = 0; i < no_read_lines; i++) {
                                    String answ = gcSerialPort.readString(true);
                                    System.err.println(">>> " + answ);
                                    result = result.isEmpty() ? answ : (result + "\n" + answ);
                                }
                                if (no_read_lines < 0)
                                    for (;;) {
                                        //result = gc.send_serial(null, fgw.get_connectorno(), 1, count, delay_between_reps);
                                        result = gcSerialPort.readString(true);
                                        System.err.println(">>> " + result);
                                    }
                                if (the_command.get_response_lines() > 0)
                                    output = result;
                            }
                        } catch (IOException | NoSuchTransmitterException ex) {
                            logger.log(Level.SEVERE, ex.getMessage());
                        }
                    } else {
                        System.err.println("Not implemented.");
                        failure = true;
                    }
                    break;

                case www:
                    if (arguments_length > 0)
                        System.err.println("Warning: arguments to command igored.");
                    String url = "http://" + fgw.get_hostname() + (portnumber == IrCoreUtils.INVALID ? "" : (":" + portnumber));
                    HarcUtils.browse(url);
                    success = true;
                    break;

                case on_off:
                case sensor:
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    if ((cmd != command_t.power_on) && (cmd != command_t.power_off) && (cmd != command_t.power_toggle) && (cmd != command_t.power_pulse) && (cmd != command_t.get_state)) {
                        System.err.println("Nonappropriate command for on_off");
                        failure = true;
                    } else if (gw.get_class().equals("globalcache")) {
                        try (GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ Main.getProperties().getVerbose())) {
                            int con = fgw.get_connectorno();

                            if (cmd == command_t.get_state) {
                                //if (gw_connector.startsWith("sensor_")) {
                                if (fgw.get_connectortype() == CommandType_t.sensor) {
                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.print("Trying to inquiry Globalcache sensor " + fgw.get_connectorno() + " ");
                                    }
                                    output = gc.getState(con) == 1 ? "on" : "off";
                                } else {
                                    failure = true;
                                }
                            } else {
                                if (fgw.get_connectortype()==CommandType_t.on_off) {

                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.print("Trying to turn Globalcache relay #" + con + " ");
                                    }
                                    if (null == cmd) {
                                        if (DebugArgs.dbg_transmit())
                                            System.err.println(cmd == command_t.power_on ? "ON" : "OFF");

                                        success = gc.setState(con, cmd == command_t.power_on);
                                    } else switch (cmd) {
                                        case power_toggle:
                                            if (DebugArgs.dbg_transmit()) {
                                                System.err.println("TOGGLE");
                                            }   success = gc.toggleState(con);
                                            break;
                                        case power_pulse:
                                            if (DebugArgs.dbg_transmit())
                                                System.err.println("PULSE");
                                            success = gc.pulseState(con);
                                            break;
                                        default:
                                            if (DebugArgs.dbg_transmit()) {
                                                System.err.println(cmd == command_t.power_on ? "ON" : "OFF");
                                            }   success = gc.setState(con, cmd == command_t.power_on);
                                            break;
                                    }
                                } else {
                                    failure = true;
                                }
                            }
                        } catch (IOException | NoSuchTransmitterException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    } else {
                        System.err.println("Not implemented.");
                        failure = true;
                    }
                    break;

                case ip:
                    if (!gw.get_class().equals("lan")) {
                        System.err.println("Programming/configuration error: gateway class lan expected.");
                        failure = true;
                    } else {
                        if (null == cmd) {
                            System.err.println("Command " + cmd + " of type ip not implemented.");
                            failure = true;
                        } else switch (cmd) {
                    case ping:
                        try {
                            if (Main.getProperties().getVerbose())
                                System.err.print("Pinging hostname " + fgw.get_hostname() + "... ");

                            success = InetAddress.getByName(fgw.get_hostname()).isReachable(HarcUtils.ping_timeout);
                            if (!success) {
                                // Java's isReachable may fail due to insufficient privileges;
                                // the OSs ping command may be more successful (is suid on Unix).
                                if (Main.getProperties().getVerbose())
                                    System.err.print("isReachable() failed, trying the ping program...");
                                String[] args = {"ping", "-w", Integer.toString((int) (HarcUtils.ping_timeout / 1000)), fgw.get_hostname()};
                                Process proc = Runtime.getRuntime().exec(args);
                                proc.waitFor();
                                success = proc.exitValue() == 0;
                            }
                            if (Main.getProperties().getVerbose())
                                System.err.println(success ? "succeded." : "failed.");
                        } catch (IOException e) {
                            System.err.println(e.getMessage());
                        }
                        break;

                    case wol:
                        String mac = fgw.get_mac();
                        if ((mac == null) || mac.isEmpty()) {
                            System.err.println("WOL to host " + fgw.get_hostname() + " requested, but MAC unknown.");
                        } else {
                            if (Main.getProperties().getVerbose()) {
                                System.err.println("Sending a WOL package to " + fgw.get_hostname() + " (" +  mac + ").");
                            }
                            try {
                                Wol.wol(fgw.get_mac());
                                success = true;
                            } catch (NumberFormatException | IOException | HarcHardwareException e) {
                                System.err.println(e.getMessage());
                            }
                        }
                        break;

                    default:
                        System.err.println("Command " + cmd + " of type ip not implemented.");
                        failure = true;
                        break;
                }
                    }
                    break;

                case special: // ????
                    try {
                        if (DebugArgs.dbg_transmit() || Main.getProperties().getVerbose())
                            System.err.println("Trying special with class = " + dev_class + ", method = " + cmd + ", hostname = " + fgw.get_hostname());

                        // ???????????????????????
                        Class cl = Class.forName(/*ir_code*/IrSignal.class.getPackage().getName() + "." + dev_class);
                        Object d = cl.getConstructor(new Class[]{String.class}).newInstance(new Object[]{fgw.get_hostname()});
                        Class[] args_class = new Class[arguments.length];

                        for (int i = 0; i < arguments.length; i++)
                            args_class[i] = String.class;

                        Method m = cl.getMethod(cmd.toString(), args_class);
                        Object something = m.invoke(d, (Object[])arguments);
                        output = something == null ? ""
                                : something.getClass().isArray() ? String.join(" ", (CharSequence[]) something)
                                : (String) something;
                        success = true;
                    } catch (NoSuchMethodException e) {
                        System.err.println("NoSuchMethod: " + e.getMessage());
                    } catch (ClassNotFoundException e) {
                        System.err.println("ClassNotFound: " + e.getMessage());
                    } catch (InstantiationException e) {
                        System.err.println("Instatnt " + e.getMessage());
                    } catch (IllegalAccessException e) {
                        System.err.println("Illegalaccess " + e.getMessage());
                    } catch (InvocationTargetException e) {
                        System.err.println("InvocationTarget " + e.getMessage());
                    } catch (ClassCastException e) {
                        System.err.println("ClassCastException " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        System.err.println("IllegalArgument " + e.getMessage());
                    }

                    break;
                case udp:
                case bluetooth:
                default:
                    System.err.println("Command of type " + type + " not yet implemented");
                    failure = true;
            }
        }

        if (output != null) {
            success = true;
        }
        if (success && output == null) {
            output = "";
        }

        if (DebugArgs.dbg_transmit()) {
            System.err.println("transmit_command returns \"" + (success && output != null ? output : "null") + "\"");
        }
        return success ? output : null;
    }

    //  Return null if argument not alias and not device
    public String expand_alias(String dname) {
        if (dname == null)
            return null;
        String expanded = alias_table.get(dname);
        return expanded != null ? expanded : this.device_table.containsKey(dname) ? dname : null;
    }

    public String get_canonical_name(String dev_name) {
        Dev d = get_dev(dev_name);
        return d == null ? null : d.get_canonical_name();
    }

    /**
     *
     * @param devicename Name of the device in question
     * @return true if a device with that name exists.
     */
    public boolean has_device(String devicename) {
        return get_dev(devicename) != null;
    }

    // Bug/limitation: does not evaluate the attributes in the home file.
    public boolean has_command(String devname, CommandType_t type, command_t command) {
        Device d = get_device(devname);
        if (d == null || !d.is_valid()) {
            System.err.println("Device \"" + devname + "\" not found.");
            return false;
        }
        Dev dv = get_dev(devname);

        if (type != CommandType_t.any)
            return dv.has_commandtype(type) && d.get_command(command, type) != null;
        else {
            ArrayList<CommandType_t> types = d.get_commandtypes(command);
            return types.stream().anyMatch((t) -> (dv.has_commandtype(t)));
        }
    }

    public boolean has_command(String devname, String cmd) {
        return has_command(devname, CommandType_t.any, command_t.parse(cmd));
    }

    public Device get_device(String devname) {
        Device dev = null;
        String dev_class = get_deviceclass(devname);

        if (dev_class != null) {
            try {
                Map<String, String>attributes = get_attributes(devname);
                dev = Device.newDevice(dev_class, attributes/*, false*/);
            } catch (IOException e) {
                //if (debug_dispatch())
                System.err.println("Cannot read device file " + dev_class + ".");
            } catch (SAXException e) {
                System.err.println(e.getMessage());
            }
        }
        return dev;
    }

    public String get_deviceclass(String devname) {
        Dev d = get_dev(devname);
        //Element dev_node = find_node("device", devname);
        if (d == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return null;
        }
        return d.get_class();
    }

    public Map<String, String> get_attributes(String devname) {
        Dev d = get_dev(devname);
        //Element dev_node = find_node("device", devname);
        if (d == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return null;
        }
        return d.get_attributes();
    }

    public int get_delay(String devname, String delaytype) {
        Device dev = get_device(devname);
        return dev != null ? dev.get_delay(delaytype) : (int) IrCoreUtils.INVALID;
    }

    public int get_pin(String devname) {
        Dev d = get_dev(devname);
        if (d == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return (int) IrCoreUtils.INVALID;
        }
        return d.get_pin();
    }

    public String get_powered_through(String devname) {
        return get_dev(devname).get_powered_through();
    }

    public String do_command(String devname, String command) throws InterruptedException {
        return do_command(devname, command, 1);
    }

    public String do_command(String devname, String command, int count) throws InterruptedException {
        return do_command(devname, command_t.parse(command), count);
    }

    public String do_command(String devname, String command, String arg) throws InterruptedException {
        String[] args = new String[1];
        args[0] = arg;
        return do_command(devname, command_t.parse(command), args, CommandType_t.any,
                1, ToggleType.dont_care, false);
    }

    public String do_command(String devname, String command, String arg1, String arg2) throws InterruptedException {
        String[] args = new String[2];
        args[0] = arg1;
        args[1] = arg2;
        return do_command(devname, command_t.parse(command), args, CommandType_t.any,
                1, ToggleType.dont_care, false);
    }

  public String do_command(String devname, command_t cmd, int count) throws InterruptedException {
        return do_command(devname, cmd, new String[0],
                CommandType_t.any, count, ToggleType.dont_care, false);
    }

    // Returns null by failure, otherwise output from command (possibly "").
    public String do_command(String devname, command_t cmd, String[] arguments,
            CommandType_t type, int count,
            ToggleType toggle, boolean smart_memory)
            throws InterruptedException {
        String output = "";
        Device the_device = get_device(devname);
        Dev the_dev = get_dev(devname);
        if (the_dev == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return null;
        }
        String dev_class = the_dev.get_class();
        if (!(the_device != null && the_device.is_valid()))
            return null;

        String alias = the_device.get_alias(cmd);
        if (alias != null) {
            if (Main.getProperties().getVerbose() || DebugArgs.dbg_dispatch()) {
                System.err.println("Command " + cmd + " aliased to " + alias);
            }
            cmd = command_t.parse(alias);
        }
        // FIXME: Bug: If type == any, takes the first occurance, which is not necessarily the one
        // that will actually be used.
        Command the_command = the_device.get_command(cmd, type /* FIXME: , toggle*/);
        if (the_command == null && type != CommandType_t.www) {
            if (DebugArgs.dbg_transmit())
                System.err.println("No such command " + cmd + " of type " + type + " (" + type + ")");
            return null;
        }

        String mem = the_device.get_attribute("memory");
        boolean has_memory = (mem != null) && mem.equals("yes");
        if (DebugArgs.dbg_dispatch())
            System.err.println("do_command: device " + devname + ", device_class " + dev_class + ", command " + cmd + ", type " + type + ", memory " + has_memory + ", toggle " + toggle);
        List<GatewayPort> gateway_ports = the_dev.get_gateway_ports();
        boolean success = false;
        for (GatewayPort gwp : gateway_ports) {
            if (success)
                break;

            if (gwp == null) {
                System.out.println("Configuration error: gateway port is null.");
                return null;
            }
            int no_times =
                    (((cmd == command_t.power_on) || (cmd == command_t.power_reverse_on)) && smart_memory && has_memory)
                    ? 2 : 1;
            output = "";
            for (int j = 0; j < no_times && output != null; j++) {
                output = dispatch_command2gateway(gwp, dev_class, cmd, arguments, null, // house
                        (short) IrCoreUtils.INVALID /*deviceno*/, type, count, toggle, 0, the_dev.get_attributes(), the_command.get_flavor());
            }
            success = output != null;
        }
        return output;
    }

    // Sends the gateway described by gw_name the
    // command described by dev_class and cmd, type, count, and toggle.
    // For this, the contained from-gateway elements is tried.
    // Either calls itself recursively, or calls transmit_command.
    private String dispatch_command2gateway(GatewayPort fgw, String dev_class, command_t cmd, String[] arguments,
            String house, short deviceno, CommandType_t type, int count, ToggleType toggle, int hop,
            Map<String, String> attributes, String flavor) throws InterruptedException {
        if (DebugArgs.dbg_dispatch()) {
            for (int i = 0; i <= hop; i++)
                System.err.print("<");
            System.err.println("dispatch_command2gateway: gw " + fgw.get_gateway() + ", connectortype " + fgw.get_connectortype() + ", connectorno " + fgw.get_connectorno() + ", hostname " + fgw.get_hostname() + ", portnumber " + fgw.get_portnumber() + ", device " + dev_class + ", command " + cmd + /*", house " + house + ", deviceno " + deviceno +*/ ", type " + type + ", hops " + hop);
        }

        if (hop >= max_hops) {
            System.err.println("Max hops (= " + max_hops + ") reached, aborting.");
            return null;
        }

        String outputs = null;
        Gateway gw = gateway_table.get(fgw.get_gateway());
        if (gw == null) {
            System.err.println("Did not find gateway " + fgw.get_gateway());
            return null;
        }

        CommandType_t act_type = fgw.get_connectortype();

        if (!act_type.is_compatible(type)) {
            // Cannot use this connector, wrong type
            if (DebugArgs.dbg_dispatch())
                System.err.println("Gateway " + gw.get_id() + ", connectortype " + fgw.get_connectortype() + " not capable of type " + type + ", returning");
            outputs = null;
        } else if (gw.get_class().equals("lan") || !gw.get_hostname().isEmpty()) {
            // Can reach this device on LAN, do it.
            // This gateway has a hostname, issue the command.
            //System.err.println("**************************" + act_type);
            outputs = transmit_command(dev_class, cmd, arguments, house,
                    deviceno, gw, fgw, act_type, count, toggle, attributes, flavor);
        } else {
            // None of the above, hope that the gateway is reachable from other gateways.
            ArrayList<GatewayPort> in_ports = gw.get_gateway_ports();

            boolean success = false;
            for (GatewayPort gwp : in_ports) {
                if (success)
                    break;
                Port outport = gw.get_port(act_type, fgw.get_connectorno());
                if (gwp.get_connectortype().is_compatible(type)) {
                    command_t actual_command = cmd;
                    String remotename = dev_class;
                    String new_house = house;
                    short new_deviceno = deviceno;
                    CommandMapping cmdmap = outport.get_commandmapping(cmd);
                    if (cmdmap != null) {
                        if (DebugArgs.dbg_dispatch())
                            System.err.println("Substituting command " + cmd + " by " + cmdmap.get_new_cmd() + ", remotename " + cmdmap.get_remotename() + ", deviceno " + cmdmap.get_deviceno() + ", house " + cmdmap.get_house());
                        remotename = cmdmap.get_remotename();
                        actual_command = cmdmap.get_new_cmd();
                        new_house = cmdmap.get_house();
                        new_deviceno = cmdmap.get_deviceno();
                    }

                    // must invoke dispatch_command again.
                    outputs = dispatch_command2gateway(gwp, remotename, actual_command, arguments,
                            new_house, new_deviceno, type, count, toggle, hop + 1, attributes, flavor);
                    success = outputs != null;
                }
            }
        }

        if (DebugArgs.dbg_dispatch()) {
            for (int i = 0; i <= hop; i++)
                System.err.print(">");
            System.err.println("dispatch_command2gateway returns: " + outputs);
        }
        return outputs;
    }

    public String[] gateway_instances(String gateway_class) {
        ArrayList<String> v = new ArrayList<>(8);
        gateway_table.values().stream().filter((gw) -> (gw.get_class().equals(gateway_class))).forEachOrdered((gw) -> {
            v.add(gw.get_id());
        });
        return v.toArray(new String[v.size()]);
    }
}
