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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.comm.Wol;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.misc.EzControlT10;
import org.harctoolbox.harchardware.misc.SonySerialCommand;
import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.XmlUtils;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class is immutable
 *
 */
public final class Home {

    private int max_hops = 10;

    private final HashMap<String, String> alias_table;
    // The get method of device_table should not be used, use get_dev(String) instead.
    private final LinkedHashMap<String, Dev> device_table;
    private final LinkedHashMap<String, DeviceGroup> device_groups_table; // indexed by name, not id
    private final HashMap<String, Gateway> gateway_table;
    private static final Logger logger = Logger.getLogger(Home.class.getName());

    public Home(String home_filename/*, boolean verbose, int debug*/) throws IOException, SAXParseException, SAXException {
        Document doc = XmlUtils.openXmlFile(new File(home_filename));
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
        return d != null ? d.get_zone_names().toArray(new String[0]) : null;
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
        ArrayList<String> v = new ArrayList<String>(n);
        //for (Enumeration<String> e = device_table.keys(); e.hasMoreElements();) {
        //    String id = e.nextElement();
        for (String id : device_table.keySet()) {
            if (!device_table.get(id).get_class().equals("null"))
                v.add(id);
        }
        return v.toArray(new String[0]);
    }

    public String[] get_selecting_devices() {
        ArrayList<String> v = new ArrayList<String>();
        for (Dev d : device_table.values())
            if (!d.get_inputs().isEmpty())
                v.add(d.get_id());

        return v.toArray(new String[0]);
    }

    public int get_arguments(String devname, command_t cmd, CommandType_t cmdtype) {
        Device dev = get_device(expand_alias(devname));
        if (dev == null || !dev.is_valid())
            return -1;

        Command kommand = dev.get_command(cmd, cmdtype);
        return kommand != null ? dev.get_command(cmd, cmdtype).get_arguments().length : 0;
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
        return device_groups_table.keySet().toArray(new String[0]);
    }

    public String[] get_sources(String devname, String zone) {
        Dev d = get_dev(devname);
        return d == null ? null : d.get_sources(zone).toArray(new String[0]);
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
        return hs.toArray(new ConnectionType[0]);
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
        Input.querycommand query_command = inp.get_query_command(zone, the_mediatype);

        if (select_command == command_t.invalid) {
            System.err.println("No command found for turning " + devname + " to " + src_device + ((zone != null && !zone.equals("")) ? (" in zone " + zone) : "") + (the_mediatype == MediaType.audio_only ? " (audio only)"
                    : the_mediatype == MediaType.video_only ? " (video only)"
                    : "") + (conn_type == null ? "" : " using connection_type " + conn_type) + ".");
            return false;
        }

        if (DebugArgs.dbg_decode_args())
            System.err.println("Found select command: " + select_command + (query_command != null ? (", query: " + query_command.get_command() + "==" + query_command.get_response() + ".") : ", no query command found."));

        if (query_command != null) {
            String result = do_command(devname, query_command.get_command(),
                    new String[0], type, 1, ToggleType.toggle_0, false);
            if (result != null && result.equals(query_command.get_response())) {
                if (DebugArgs.dbg_decode_args())
                    System.err.println(devname + " already turned to " + src_device + ", ignoring.");

                return true;
            }
        }

        return do_command(devname, select_command, new String[0], type, 1, ToggleType.toggle_0, false) != null;
    }

    // Really generate and send the command, if possible

    // TODO: This function should be restructured. Really...
    private String transmit_command(String dev_class, command_t cmd,
            String[] arguments,
            String house, int deviceno,
            Gateway gw, GatewayPort fgw,
            CommandType_t type, int count,
            //String gw_class, String gw_hostname,
            //int portnumber,
            //String gw_connector, String gw_model,
            //String gw_interface,
            ToggleType toggle/*, String mac*/, HashMap<String, String> attributes, String flavor)
            throws InterruptedException {
        if (DebugArgs.dbg_transmit())
            System.err.println("transmit_command: device " + dev_class
                    + ", command " + cmd + ", house " + house
                    + ", deviceno " + deviceno + ", type " + type
                    + ", count " + count + ", gw_class " + gw.get_class()
                    + ", gw_hostname " + gw.get_hostname() + ", port_hostname " + fgw.get_hostname()
                    + ", port " + fgw.get_portnumber() + ", connectortype " + fgw.get_connectortype()
                    + ", connectorno " + fgw.get_connectorno() + ", model " + gw.get_model()
                    + ", interface " + gw.get_interface() + ", toggle " + toggle);

        int portnumber = fgw.get_portnumber(); // FIXME
        String output = null;
        boolean success = false;
        boolean failure = false;
        Device dev = null;
        // Use arguments_length instead of arguments.length to allow for arguments == null
        int arguments_length = arguments == null ? 0 : arguments.length;
        try {
            dev = Device.new_device(dev_class, attributes/*, false*/);
        } catch (IOException e) {
            // May be ok, e.g. when using Intertechno and T-10.
            if (DebugArgs.dbg_transmit())
                System.err.println("Could not open file " + HarcProps.get_instance().get_devicesdir() + File.separator + dev_class + HarcUtils.devicefile_extension + ".");
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            System.err.println(e.getMessage());
        }
        Command the_command = null;
        String result = "";
        String subst_transmitstring;
        String subst_transmitstring_printable;

        if (type == CommandType_t.www) {
            if (HarcProps.get_instance().get_browser() == null || (cmd != command_t.browse && !UserPrefs.get_instance().get_use_www_for_commands())) {
                if (UserPrefs.get_instance().get_verbose())
                    System.err.println("Command of type www ignored.");

                failure = true;
            }
        } else {
            if (!gw.get_class().equals("ezcontrol_t10")) {
                if (dev == null || !dev.is_valid()) { // FIXME
                    failure = true;
                } else {
                    the_command = dev.get_command(cmd, type);
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
                            IrSignal code = dev.get_code(cmd, CommandType_t.ir, toggle, DebugArgs.dbg_ir_protocols(), house, (short) (deviceno - 1));
                            if (code == null) {
                                if (UserPrefs.get_instance().get_verbose())
                                    System.err.println("Command " + cmd + " exists, but has no ir code.");

                                failure = true;
                            } else {
                                String raw_ccf = Pronto.toString(code);
                                GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ UserPrefs.get_instance().get_verbose());
                                if (gc != null) {
                                    //success = gc.send_ir(raw_ccf, Integer.parseInt(gw_connector.substring(3)), count);
                                    GlobalCache.GlobalCacheIrTransmitter transmitter = gc.newTransmitter(fgw.get_connectorno());
                                    success = gc.sendCcf(raw_ccf, count, transmitter);
                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.println("Globalcache " + (success ? "succeeded" : "failed"));
                                    }
                                }
                            }
//                        } else if (gw.get_class().equals("irtrans")) {
//                            if (debugargs.dbg_transmit()) {
//                                System.err.println("Trying an Irtrans (" + gw.get_hostname() + ")...");
//                            }
//                            irtrans irt = new irtrans(gw.get_hostname(), userprefs.get_instance().get_verbose());
//                            command c = dev.get_command(cmd, commandtype_t.ir);
//                            if (c == null) {
//                                if (userprefs.get_instance().get_verbose()) {
//                                    System.err.println("Command " + cmd + " exists, but has no ir code.");
//                                }
//                                failure = true;
//                            } else {
//                                if (gw.get_interface().equals("preprog_ascii")) {
//                                    success = irt.send_flashed_command(the_command.get_remotename(), c.get_cmd(), fgw.get_connectorno(), count);
//                                } else if (gw.get_interface().equals("web_api")) {
//                                    if (count > 1)
//                                        System.err.println("** Warning: count > 1 (= " + count + ") ignored.");
//
//                                    String url = irtrans.make_url(gw.get_hostname(), the_command.get_remotename(), cmd/*c.getcmd().toString()*/, fgw.get_connectorno());
//                                    if (debugargs.dbg_transmit() || userprefs.get_instance().get_verbose())
//                                        System.err.println("Getting URL " + url);
//
//                                    success = (new URL(url)).getContent() != null;
//                                } else if (gw.get_interface().equals("udp")) {
//                                    IrSignal code = dev.get_code(cmd, commandtype_t.ir, toggle, debugargs.dbg_ir_protocols());
//                                    success = irt.send_ir(code, fgw.get_connectorno(), count);
//                                } else {
//                                    System.err.println("Interface `" + gw.get_interface() + "' for IRTrans not implemented.");
//                                    success = false;
//                                }
//                            }
//                        } else if (gw.get_class().equals("lirc_server")) {
//                            if (debugargs.dbg_transmit())
//                                System.err.println("Trying a Lirc server...");
//
//                            command c = dev.get_command(cmd, commandtype_t.ir);
//                            if (c == null) {
//                                if (userprefs.get_instance().get_verbose())
//                                    System.err.println("Command " + cmd + " exists, but has no ir code.");
//
//                                failure = true;
//                            } else {
//                                lirc lirc_client = new lirc(gw.get_hostname(), userprefs.get_instance().get_verbose());
//                                if (lirc_client != null)
//                                    // TODO: evaluate connector
//                                    success = lirc_client.send_ir(the_command.get_remotename(), c.get_cmd(), count);
//                            }
                        }
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw.get_hostname());
                    } catch (IOException e) {
                        System.err.println("IOException with host " + gw.get_hostname() + ": " + e.getMessage());
                    } catch (Pronto.NonProntoFormatException | InvalidArgumentException | NoSuchTransmitterException ex) {
                        Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;

                case rf:
                    if (DebugArgs.dbg_transmit()) {
                        System.err.println("Trying rf to " + gw.get_class());
                    }
                    if (gw.get_class().equals("ezcontrol_t10")) {
                        int power = 0;
                        int arg = -1;
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
                            EzControlT10 t10 = new EzControlT10(gw.get_hostname(), UserPrefs.get_instance().get_verbose());
                            int preset = -1;
                            try {
                                //preset = Integer.parseInt(gw_connector.substring(7));
                                preset = fgw.get_connectorno();
                                if (DebugArgs.dbg_transmit())
                                    System.err.println("Preset no = " + preset);
                            } catch (StringIndexOutOfBoundsException e) {
                                System.err.println("preset number not parseable");
                                failure = true;
                            } catch (NumberFormatException e) {
                                System.err.println("preset number not parseable");
                                failure = true;
                            }

                            try {
                                EzControlT10.Command t10cmd = EzControlT10.Command.valueOf(cmd.toString());
                                if (preset != -1 && t10cmd.isPresetCommand())
                                    switch (cmd) {
                                        case get_status:
                                            output = t10.getPresetStatus(preset);
                                            break;
                                        case set_power:
                                            success = t10.sendPreset(preset, EzControlT10.Command.power_on, count);
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
                                        EzControlT10.EZSystem system = EzControlT10.EZSystem.valueOf(dev_class);
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
                    subst_transmitstring = dev.get_command(cmd, CommandType_t.tcp).get_transmitstring(true);
                    subst_transmitstring_printable = dev.get_command(cmd, CommandType_t.tcp).get_transmitstring(false);
                    for (int i = 0; i < arguments_length; i++) {
                        subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                        subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                    }
                    if (DebugArgs.dbg_transmit())
                        System.err.println("Trying TCP socket to " + fgw.get_hostname()+ ":" + portnumber + " \"" + subst_transmitstring_printable + "\"");

                    Socket sock = null;
                    PrintStream outToServer = null;
                    BufferedReader inFromServer = null;

                    try {
                        sock = SocketStorage.getsocket(fgw.get_hostname(), portnumber);//new Socket(gw_hostname, portnumber);
                        if (sock == null)
                            throw new IOException("Got a null socket");
                        sock.setSoTimeout(fgw.get_timeout());
                        if (DebugArgs.dbg_transmit())
                            System.err.println("Setting timeout on TCP socket to " + fgw.get_timeout());
                        //System.err.println(sock.getSoLinger() + " " + sock.getSoTimeout() + " " + sock.getReuseAddress() + " " + sock.getKeepAlive());

                        //DataOutputStream outToServer = new DataOutputStream(sock.getOutputStream());
                        outToServer = new PrintStream(sock.getOutputStream());
                        inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

                        String opener = dev.get_open(cmd, CommandType_t.tcp);
                        if (opener != null && !opener.isEmpty()) {
                            if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
                                System.err.println("Sending opening command \"" + opener + "\" to socket " + fgw.get_hostname() + ":" + portnumber);
                            outToServer.print(opener);
                        }

                        if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose()) {
                            System.err.println("Sending command \"" + subst_transmitstring_printable + "\" to socket " + fgw.get_hostname() + ":" + portnumber + (count == 1 ? " (one time)" : (" (" + count + " times)")));
                        }
                        for (int c = 0; c < count; c++) {
                            int delay_between_reps = the_command.get_delay_between_reps();
                            if (delay_between_reps > 0 && c > 0) {
                                if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
                                    System.err.println("Waiting for " + delay_between_reps + "ms, then sending.");

                                Thread.sleep(delay_between_reps);
                            }
                            //outToServer.writeBytes(subst_transmitstring);
                            outToServer.print(subst_transmitstring);
                        }

                        if (dev.get_command(cmd, CommandType_t.tcp).get_response_lines() < 0) {
                            if (dev.get_command(cmd, CommandType_t.tcp).get_response_ending().equals("")) {
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
                                        result = result + "\n";
                                    }
                                    while (!inFromServer.ready())
                                        Thread.sleep(20);
                                    String l = inFromServer.readLine();
                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.println("Got: " + l);
                                    }
                                    result = result + l;
                                    found = l.equals(dev.get_command(cmd, CommandType_t.tcp).get_response_ending());
                                } while (!found);
                            }
                        } else {
                            for (int i = 0; i < dev.get_command(cmd, CommandType_t.tcp).get_response_lines(); i++) {
                                if (i > 0) {
                                    result = result + "\n";
                                }
                                //while (!inFromServer.ready())
                                //    Thread.sleep(20); // Danger of hanging
                                result = result + inFromServer.readLine();
                            }
                        }

                        String closer = dev.get_close(cmd, CommandType_t.tcp);
                        if (closer != null && !closer.isEmpty()) { // This code is not tested
                            if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
                                System.err.println("Sending closing command \"" + closer + "\" to socket " + gw.get_hostname() + ":" + portnumber);
                            outToServer.print(closer);
                        }
                        if (dev.get_command(cmd, CommandType_t.tcp).get_response_lines() != 0) {
                            //System.err.println("Response: " + result);
                            output = result;
                        }
                        success = true;
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw.get_hostname());
                    } catch (IOException e) {
                        System.err.println("Could not get I/O for the connection to "
                                + fgw.get_hostname() + ":" + portnumber + " " + e.getMessage());
                        failure = true;
                    } finally {
                        try {
                        outToServer.close();
                        inFromServer.close();
                        SocketStorage.returnsocket(sock, false);//sock.close();
                        } catch (Exception e) {
                            System.err.println("Socket problem: " + e.getMessage());
                        }
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
                        URLConnection url_connection = null;
                        String urlstr = "http://" + fgw.get_hostname() + ":" + portnumber + "/" + subst_transmitstring;
                        try {
                            int response_lines = the_command.get_response_lines();
                            if (response_lines == 0 && !the_command.get_expected_response().equals("")) // this is contradictory, fix
                                response_lines = 1;


                            URL the_url = new URL(urlstr);
                            url_connection = the_url.openConnection();
                            url_connection.setConnectTimeout(fgw.get_timeout());
                            url_connection.setReadTimeout(fgw.get_timeout());
                            if (DebugArgs.dbg_transmit())
                                System.err.println("Set timeout to " + fgw.get_timeout() + "ms");
                            String charset = dev.get_command(cmd, CommandType_t.web_api).get_charset();

                            for (int c = 0; c < count; c++) {
                                if (c > 0) {
                                    int delay_between_reps = the_command.get_delay_between_reps();
                                    if (delay_between_reps > 0) {
                                        if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose()) {
                                            System.err.println("Waiting for " + delay_between_reps + "ms.");
                                        }
                                        Thread.sleep(delay_between_reps);
                                    }
                                }

                                if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
                                    System.err.println("Getting URL " + urlstr + ".");

                                if (response_lines == 0) {
                                    url_connection.getContent();
                                } else if (response_lines > 0) {
                                    //BufferedReader inFromServer = null;
                                    inFromServer = new BufferedReader(new InputStreamReader(url_connection.getInputStream(), charset));
                                    //inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    output = "";
                                    for (int i = 0; i < response_lines; i++) {
                                        result = inFromServer.readLine();
                                        if (result != null)
                                            output = output.equals("") ? result : output + "\n" + result;
                                        if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose() || (the_command.get_expected_response().equals("") && the_command.get_response_lines() == 0)) {
                                            System.err.println("Got: " + result);	// ??
                                        }
                                    }
                                } else {
                                    //BufferedReader inFromServer = null;
                                    //inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    //URLConnection uc = the_url.openConnection();
                                    //System.err.println(uc.getContentEncoding());
                                    //for (int k = 0; k < 20; k++)
                                    //    System.err.println(uc.getHeaderField(k));
                                    inFromServer = new BufferedReader(new InputStreamReader(url_connection.getInputStream(), charset));
                                    output = "";
                                    do {
                                        result = inFromServer.readLine();
                                        if (result != null)
                                            output = output.equals("") ? result : output + "\n" + result;
                                        if ((DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose()) && result != null) {
                                            System.out.println(result);	// ??
                                        }
                                    } while (result != null);
                                }

                                if (!the_command.get_expected_response().equals("")) {
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
                        } catch (java.io.IOException e) {
                            failure = true;
                            System.err.println("IOException for " + urlstr + ": " + e.getMessage());
                        //} catch (java.net.MalformedURLException e) {
                        //System.err.println(e.getMessage());
                        } finally {
                            if (url_connection != null) {
                                try {
                                    InputStream is = url_connection.getInputStream();
                                    if (is != null)
                                        is.close();
                                    OutputStream os = url_connection.getOutputStream();
                                    if (os != null)
                                        os.close();
                                    ((HttpURLConnection)url_connection).disconnect();
                                } catch (IOException ex) {
                                }
                            }
                        }
                    }
                    break;

                case serial:
                    try {
                        // TODO: handle cases expected_lines < 0 (loop forever) senisble, (if possible).
                        if (gw.get_class().equals("globalcache")) {
                            GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ UserPrefs.get_instance().get_verbose());
                            GlobalCache.SerialPort gcSerialPort = gc.getSerialPort(fgw.get_connectorno());
                            subst_transmitstring = dev.get_command(cmd, CommandType_t.serial).get_transmitstring(true);
                            subst_transmitstring_printable = dev.get_command(cmd, CommandType_t.serial).get_transmitstring(false);
                            if (flavor.equals("SonySerialCommand")) {

                                //gc.setserial(1, "38400,FLOW_NONE,PARITY_EVEN");
                                String[] cmds = subst_transmitstring_printable.split(",");
                                byte[] bytes = SonySerialCommand.bytes(Integer.parseInt(cmds[1]), Integer.parseInt(cmds[2]), cmds[0]);
//                                byte[] answer = gc.send_read_serial(bytes, fgw.get_connectorno(),
//                                        the_command.get_response_lines() > 0 ? SonySerialCommand.size : 0);
                                gcSerialPort.sendBytes(bytes);
//                                output = the_command.get_response_lines() > 0
//                                        ? Integer.toString(SonySerialCommand.interpret(answer).getData())
//                                        : "";
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

                                //try {
                                if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
                                    System.err.println("Trying Globalcache (" + gw.get_hostname() + ") for serial using " + fgw.get_connectorno() + ", \"" + subst_transmitstring_printable + "\"");

                                int delay_between_reps = the_command.get_delay_between_reps();
                                int no_read_lines = the_command.get_response_lines();

                                //result = gc.send_serial(subst_transmitstring, fgw.get_connectorno(), no_read_lines == 0 ? 0 : 1, count, delay_between_reps);
                                gcSerialPort.sendString(subst_transmitstring);
                                if (no_read_lines > 0) {
                                    if (delay_between_reps > 0)
                                        Thread.sleep(delay_between_reps);
                                    result = gcSerialPort.readString(true);
                                     no_read_lines--;
                                } else
                                    result = "";
//                                if (no_read_lines > 0)
//                                    no_read_lines--;
//                                Thread.sleep(10);

                                if (!the_command.get_expected_response().equals("")) {
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
                                    //String answ = gc.send_serial(null, fgw.get_connectorno(), 1, count, delay_between_reps);
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
                                //success = result != null;
                                if (the_command.get_response_lines() > 0)
                                    output = result;
//                            } catch (Exception e) {
//                                System.err.println("Silly exception: " + e.getClass().getName() + e.getMessage());
//                                e.printStackTrace();
                                //} catch (NoRouteToHostException e) {
                                //    System.err.println("No route to " + gw.get_hostname());
                                //} catch (IOException e) {
                                //    System.err.println("IOException with host " + gw.get_hostname());
                            }
                        } else {
                            System.err.println("Not implemented.");
                            failure = true;
                        }
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    } catch (NoSuchTransmitterException ex) {
                        Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;

                case www:
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    String url = "http://" + fgw.get_hostname() + ":" + portnumber + "/";
                    if (DebugArgs.dbg_transmit()) {
                        System.err.println("Starting " + HarcProps.get_instance().get_browser() + " " + url);
                    }
                    String cmd_array[] = new String[2];
                    cmd_array[0] = HarcProps.get_instance().get_browser();
                    cmd_array[1] = url;
                    try {
                        Process gc_process = java.lang.Runtime.getRuntime().exec(cmd_array);
                        success = true;
                    } catch (IOException e) {
                        System.err.println("Could not exec command \"" + HarcProps.get_instance().get_browser() + " " + url + "'.");
                    }

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
                        try {
                            GlobalCache gc = new GlobalCache(gw.get_hostname(), /*gw.get_model(),*/ UserPrefs.get_instance().get_verbose());
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
                                //if (gw_connector.startsWith("relay_")) {
                                if (fgw.get_connectortype()==CommandType_t.on_off) {

                                    if (DebugArgs.dbg_transmit()) {
                                        System.err.print("Trying to turn Globalcache relay #" + con + " ");
                                    }
                                    if (cmd == command_t.power_toggle) {
                                        if (DebugArgs.dbg_transmit()) {
                                            System.err.println("TOGGLE");
                                        }
                                        success = gc.toggleState(con);
                                    } else if (cmd == command_t.power_pulse) {
                                        if (DebugArgs.dbg_transmit())
                                            System.err.println("PULSE");

                                        success = gc.pulseState(con);

                                    } else {
                                        if (DebugArgs.dbg_transmit()) {
                                            System.err.println(cmd == command_t.power_on ? "ON" : "OFF");
                                        }
                                        success = gc.setState(con, cmd == command_t.power_on);
                                    }
                                } else {
                                    failure = true;
                                }
                            }
                        } catch (java.net.NoRouteToHostException e) {
                            System.err.println("No route to " + gw.get_hostname());
                        } catch (java.net.UnknownHostException e) {
                            System.err.println("Unknown host " + gw.get_hostname());
                        } catch (IOException e) {
                            System.err.println("IOException with host " + gw.get_hostname());
                        } catch (NoSuchTransmitterException ex) {
                    Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
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
                        if (cmd == command_t.ping) {
                            try {
                                if (UserPrefs.get_instance().get_verbose())
                                    System.err.print("Pinging hostname " + fgw.get_hostname() + "... ");

                                success = InetAddress.getByName(fgw.get_hostname()).isReachable(HarcUtils.ping_timeout);
                                if (!success) {
                                    // Java's isReachable may fail due to insufficient privileges;
                                    // the OSs ping command may be more successful (is suid on Unix).
                                    if (UserPrefs.get_instance().get_verbose())
                                        System.err.print("isReachable() failed, trying the ping program...");
                                    String[] args = {"ping", "-w", Integer.toString((int) (HarcUtils.ping_timeout / 1000)), fgw.get_hostname()};
                                    Process proc = Runtime.getRuntime().exec(args);
                                    proc.waitFor();
                                    success = proc.exitValue() == 0;
                                }
                                if (UserPrefs.get_instance().get_verbose())
                                    System.err.println(success ? "succeded." : "failed.");
                            } catch (IOException e) {
                                System.err.println(e.getMessage());
                            }
                        } else if (cmd == command_t.wol) {
                            String mac = fgw.get_mac();
                            if ((mac == null) || mac.isEmpty()) {
                                System.err.println("WOL to host " + fgw.get_hostname() + " requested, but MAC unknown.");
                            } else {
                                if (UserPrefs.get_instance().get_verbose()) {
                                    System.err.println("Sending a WOL package to " + fgw.get_hostname() + " (" +  mac + ").");
                            }
                            try {
                                    Wol.wol(fgw.get_mac());
                                    success = true;
                                } catch (NumberFormatException e) {
                                    System.err.println(e.getMessage());
                                } catch (IOException e) {
                                    System.err.println(e.getMessage());
                                } catch (HarcHardwareException ex) {
                                    Logger.getLogger(Home.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        } else {
                            System.err.println("Command " + cmd + " of type ip not implemented.");
                            failure = true;
                        }
                    }
                    break;
                case special:
                    try {
                        if (DebugArgs.dbg_transmit() || UserPrefs.get_instance().get_verbose())
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
                                : something.getClass().isArray() ? String.join(" ", (String[]) something)
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
            for (CommandType_t t : types)
                if (dv.has_commandtype(t))
                    return true;

            return false;
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
                HashMap<String, String>attributes = get_attributes(devname);
                dev = Device.new_device(dev_class, attributes/*, false*/);
            } catch (IOException e) {
                //if (debug_dispatch())
                System.err.println("Cannot read device file " + dev_class + ".");
            } catch (SAXParseException e) {
                System.err.println(e.getMessage());
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

    public HashMap<String, String> get_attributes(String devname) {
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
        return dev != null ? dev.get_delay(delaytype) : -1;
    }

    public int get_pin(String devname) {
        Dev d = get_dev(devname);
        if (d == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return -1;
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
            if (UserPrefs.get_instance().get_verbose() || DebugArgs.dbg_dispatch()) {
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
        ArrayList<GatewayPort> gateway_ports = the_dev.get_gateway_ports();
        boolean success = false;
        //for (Enumeration<gateway_port> e = gateway_ports.elements(); e.hasMoreElements() && !success;) {
        //    gateway_port gwp = e.nextElement();
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
                output = dispatch_command2gateway(
                        gwp,
                        dev_class,
                        cmd,
                        arguments,
                        null, // house
                        (short) -1, //deviceno,
                        type,
                        count,
                        toggle, 0, the_dev.get_attributes(), the_command.get_flavor());
            }
            success = output != null;
        }
        return output;
    }

    // Sends the gateway described by gw_name the
    // command described by dev_class and cmd, type, count, and toggle.
    // For this, the contained from-gateway elements is tried.
    // Either calls itself recursively, or calls transmit_command.
    private String dispatch_command2gateway(
            GatewayPort fgw,
            String dev_class,
            command_t cmd,
            String[] arguments,
            String house,
            short deviceno,
            CommandType_t type,
            int count, ToggleType toggle, int hop, HashMap<String, String>attributes, String flavor)
            throws InterruptedException {
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

        //if (!(type == commandtype_t.any || type == act_type)) {
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
                    deviceno,
                    gw,
                    fgw,
                    act_type, count, //gw_class, fgw_hostname,
                    //fgw_portnumber, fgw_connector,
                    //gw_model, gw_interface,
                    toggle/*, mac*/, attributes, flavor);
        //} else if (!gw.get_hostname().isEmpty()) {
        // This gateway has a hostname, issue the command.
        //     outputs = transmit_command(dev_class, cmd, arguments, house,
        //             deviceno,
        //             gw,
        //             fgw,
        //             act_type, count, //gw_class, gw_hostname,
        //fgw_portnumber, fgw_connector,
        //gw_model, gw_interface,
        //             toggle/*, mac*/);
        } else {
            // None of the above, hope that the gateway is reachable from other
            // gateways.
            ArrayList<GatewayPort> in_ports = gw.get_gateway_ports();

            boolean success = false;
            //for (Enumeration<gateway_port> p = in_ports.elements(); p.hasMoreElements() && !success;) {
            //    gateway_port gwp = p.nextElement();
            for (GatewayPort gwp : in_ports) {
                if (success)
                    break;
                Port outport = gw.get_port(act_type, fgw.get_connectorno());
                //commandtype_t output_type = gwp.get_connectortype();
                if (gwp.get_connectortype().is_compatible(type)) {/*output.getAttribute("connector").equals(fgw_connector) &&*/
                    //    (type == commandtype_t.any || type == output_type)) {
                    //String cmd_name = cmd.toString();
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
                    outputs = dispatch_command2gateway(
                            gwp,
                            //fgw.getAttribute("gateway"),
                            //fgw.getAttribute("connector"),
                            //gw_hostname,
                            //fgw_portnumber,
                            //mac,
                            remotename,
                            actual_command,
                            arguments,
                            new_house,
                            new_deviceno,
                            type,
                            count,
                            toggle,
                            hop + 1,
                            attributes,
                            flavor);
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
        ArrayList<String> v = new ArrayList<String>();
        for (Gateway gw : gateway_table.values()) {
            if (gw.get_class().equals(gateway_class))
                v.add(gw.get_id());
        }
        return v.toArray(new String[0]);
    }

    public boolean lirc_conf_export() {
        boolean success = true;
        String[] lircservers = gateway_instances("lirc_server");
        for (int i = 0; i < lircservers.length; i++) {
            String filename = HarcProps.get_instance().get_exportdir() + File.separatorChar + lircservers[i] + ".lirc.conf";
            //String[] devs = this.gateway_client_classes(lircservers[i]);
            String[] devs = device_table.keySet().toArray(new String[0]);// FIXME
            /*try {
                lirc_export.export(filename, devs);
                if (userprefs.get_instance().get_verbose())
                    System.err.println("LIRC Export: " + filename + " was successfully created.");
            } catch (FileNotFoundException ex) {
                System.err.println(ex);
                success = false;
            } catch (IOException ex) {
                System.err.println(ex);
                success = false;
            } catch (SAXParseException ex) {
                System.err.println(ex);
                success = false;
            } catch (SAXException ex) {
                System.err.println(ex);
                success = false;
            }*/
        }
        return success;
    }

    private static void usage(int errorcode) {
        System.err.println("Usage:\n"
                + "home [<options>] <device_instancename> <command> [<command_args>]*"
                + "\nwhere options=-h <filename>,-t "
                + CommandType_t.valid_types('|')
                + ",-m,-T 0|1,-# <count>,-v,-d <debugcode>,-b <browserpath>, -p <propsfile>\n"
                + "or\n"
                + "home -s [-z zone][-A,-V][-c connection_type] <device_instancename> <src_device>");
        System.exit(errorcode);
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
        String browser = null;
        String[] arguments = null;
        String propsfilename = null;
        DebugArgs db = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-b")) {
                    arg_i++;
                    browser = args[arg_i++];
                    HarcProps.get_instance().set_browser(browser);
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    connection_type = ConnectionType.valueOf(args[arg_i++]);
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-h")) {
                    arg_i++;
                    home_filename = args[arg_i++];
                } else if (args[arg_i].equals("-m")) {
                    arg_i++;
                    smart_memory = true;
                } else if (args[arg_i].equals("-p")) {
                    arg_i++;
                    propsfilename = args[arg_i++];
                } else if (args[arg_i].equals("-s")) {
                    arg_i++;
                    select_mode = true;
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    String typename = args[arg_i++];
                    if (!CommandType_t.is_valid(typename)) {
                        usage();
                    }
                    type = CommandType_t.valueOf(typename);
                } else if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose = true;
                } else if (args[arg_i].equals("-z")) {
                    arg_i++;
                    zone = args[arg_i++];
                } else if (args[arg_i].equals("-A")) {
                    arg_i++;
                    the_mediatype = MediaType.audio_only;
                } else if (args[arg_i].equals("-V")) {
                    arg_i++;
                    the_mediatype = MediaType.video_only;
                } else if (args[arg_i].equals("-T")) {
                    arg_i++;
                    toggle = ToggleType.decode_toggle(args[arg_i++]);
                } else {
                    usage();
                }
            }

            db = new DebugArgs(debug);
            devname = args[arg_i];

            // Setup properites
            if (propsfilename != null)
                HarcProps.initialize(propsfilename);
            else
                HarcProps.initialize();

            if (home_filename == null)
                home_filename = HarcProps.get_instance().get_homefilename();
            //if (browser == null)
            //    browser = harcprops.get_instance().get_browser();

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

                if (!list_commands && cmd == command_t.invalid) {
                    System.err.println("Command \"" + args[arg_i + 1] + "\" not recognized, aborting.");
                    System.exit(7);
                }

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

        UserPrefs.get_instance().set_verbose(verbose);
        UserPrefs.get_instance().set_debug(debug);
        try {
            Home hm = new Home(home_filename/*, verbose, debug*/);
            //harcutils.printtable("xxx", hm.gateway_client_classes("irtrans"));
            //harcutils.printtable("remotes", device.devices2remotes(hm.gateway_client_classes("irtrans")));
            if (select_mode) {
                //harcutils.printtable("blaa", hm.get_selecting_devices());
                if (src_device.equals("?")) {
                    HarcUtils.printtable("Valid inputs for " + devname + (zone != null ? (" in zone " + zone) : "") + ":", hm.get_sources(devname, zone));
                } else {
                    hm.select(devname, src_device, type, zone, the_mediatype, connection_type);
                }
            } else if (devname.equals("?")) {
                HarcUtils.printtable("Valid devices:", hm.get_devices());
            } else if (list_commands) // FIXME1: if devname is nonexisting, should produce an
            // error message instead of just returning nothing.
            {
                HarcUtils.printtable("Valid commands for " + devname + " of type " + type + ":", hm.get_commands(devname, type));
            } else {
                //harcutils.printtable("blubb", hm.get_zones(devname));
                String output = hm.do_command(devname, cmd, arguments, type, count, toggle, smart_memory);
                if (output == null) {
                    System.out.println("** Failure **");
                    System.exit(1);
                } else if (!output.equals("")) {
                    System.out.println("Command output: \"" + output + "\"");
                }
            }
        } catch (IOException e) {
            System.err.println("Cannot read file " + home_filename + " (" + e.getMessage() + ").");
            System.exit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        } catch (SAXParseException e) {
            System.err.println("Parse error in " + home_filename + " (" + e.getMessage() + ").");
            System.exit(IrpUtils.EXIT_XML_ERROR);
        } catch (SAXException e) {
            System.err.println("Parse error in " + home_filename + " (" + e.getMessage() + ").");
            System.exit(IrpUtils.EXIT_XML_ERROR);
        } catch (InterruptedException e) {
            System.err.println("** Interrupted **");
            System.exit(18);
        }
    }
}
