// FIXME: Nondefault portnumbers presently are not implemented.
package harc;

import java.io.*;
import java.net.*;
import org.w3c.dom.*;

import wol.*;
import wol.configuration.*;

/**
 * This class ...
 * 
 */
public class home implements commandnames {

    private int verbose = 0;
    private Document doc = null;
    private String devicedir = null;
    private String browser = null;
    private NodeList from_gateways;
    private debugargs db = null;

    public home(Document doc, int verbose, int debug, String browser) {
        this.verbose = verbose;
        db = new debugargs(debug);
        this.doc = doc;
        this.browser = browser;
        devicedir = harcprops.get_instance().get_devices_dir();
        Element root = doc.getDocumentElement();
        from_gateways = root.getElementsByTagName("from-gateway");
    }

    public home(String home_filename, int verbose, int debug, String browser) throws IOException {
        this(harcutils.open_xmlfile(home_filename), verbose, debug, browser);
        if (db.dom()) {
            System.err.println("Home configuration " + home_filename + " parsed.");
        }
    }

    public home() throws IOException {
        this(harcprops.get_instance().get_home_file(), 0, 0, harcprops.get_instance().get_browser());
    }

    public void set_verbosity(int verbosity) {
        this.verbose = verbosity;
    }

    public void set_debug(int debug) {
        db.set_state(debug);
    }

    private Element find_from_gateway(String id) {
        for (int i = 0; i < from_gateways.getLength(); i++) {
            Element e = (Element) from_gateways.item(i);
            if (e.getAttribute("id").equals(id)) {
                return e;
            }
        }
        return null;
    }

    public String[] get_zones(String device) {
        Element d = find_node("device", device);
        NodeList zones = d.getElementsByTagName("zone");
        String[] temp = new String[zones.getLength()];
        for (int i = 0; i < zones.getLength(); i++)
            temp[i] = ((Element) zones.item(i)).getAttribute("name");

        return harcutils.sort_unique(temp);
    }

    private String[] get_devices(Element e) {
        NodeList nl = e.getElementsByTagName("deviceref");
        String[] result = new String[nl.getLength()];
        for (int i = 0; i < nl.getLength(); i++) {
            result[i] = find_node("device", ((Element) nl.item(i)).getAttribute("device")).getAttribute("id");
        }
        return result;
    }

    public String[] get_devices(String devicegroup) {
        NodeList devicegroups = doc.getElementsByTagName("device-group");
        for (int i = 0; i < devicegroups.getLength(); i++) {
            Element e = (Element) devicegroups.item(i);
            if (e.getAttribute("name").equals(devicegroup))
                return get_devices(e);
        }
        return null;
    }

    public String[] get_devices() {
        Element root = doc.getDocumentElement();
        NodeList things = root.getElementsByTagName("device");
        String[] result = new String[things.getLength()];
        for (int i = 0; i < things.getLength(); i++) {
            result[i] = ((Element) things.item(i)).getAttribute("id");
        }

        return result;
    }

    public String[] get_selecting_devices() {
        NodeList devices = doc.getElementsByTagName("device");
        String [] temp = new String[devices.getLength()];
        int pos = 0;
        for (int i = 0; i < devices.getLength(); i++) {
            Element d = (Element) devices.item(i);
            NodeList inputslist = d.getElementsByTagName("inputs");
            if (inputslist.getLength() > 0) {
                if (((Element) inputslist.item(0)).getElementsByTagName("deviceref").getLength() > 0)
                    temp[pos++] = d.getAttribute("id");
            }
        }

        String[] result = new String[pos];
        for (int i = 0; i < pos; i++)
           result[i] = temp[i];

        return result;
    }

    public int get_arguments(String devname, int cmd, int cmdtype) {
        Element dev_node = find_node("device", expand_alias(devname));
        if (dev_node == null) {
            return -1;
        }

        String dev_class = dev_node.getAttribute("class");
        device dev = null;
        try {
            dev = new device(dev_class);
        } catch (IOException e) {
            //if (debug_dispatch())
            System.err.println(e.getMessage());
        }
        if (dev == null || !dev.is_valid()) {
            return -1;
        }

        return dev.get_command(cmd, cmdtype).get_arguments().length;
    }

    // TODO: sort & eliminate duplicates
    public String[] get_commands(String devname, int cmdtype) {
        Element dev_node = find_node("device", expand_alias(devname));
        if (dev_node == null) {
            return null;
        }

        String dev_class = dev_node.getAttribute("class");
        device dev = null;
        try {
            dev = new device(dev_class);
        } catch (IOException e) {
            //if (debug_dispatch())
            System.err.println(/*"IO Exception with file "
                    + devicedir + File.separator + dev_class + devfile_suffix
                    + " (" + */
                    e.getMessage() /*+ ")."*/);
        }
        if (dev == null || !dev.is_valid()) {
            return null;
        }

        int[] cmds = dev.get_commands(cmdtype);
        String[] result = new String[cmds.length];
        for (int i = 0; i < cmds.length; i++) {
            result[i] = ir_code.command_name(cmds[i]);
        }

        return harcutils.sort_unique(result);
    }

    public String[] get_commands(String devname) {
        return get_commands(devname, commandset.any);
    }

    public String[] get_devicegroups() {
        NodeList nl = doc.getElementsByTagName("device-group");
        String[] result = new String[nl.getLength()];
        for (int i = 0; i < nl.getLength(); i++) {
            result[i] = ((Element) nl.item(i)).getAttribute("name");
        }
        return result;
    }

    public String[] get_sources(String devname, String zone) {
        Element dev_node = find_node("device", devname);
        if (dev_node == null) {
            return null;
        }
        NodeList inputs = dev_node.getElementsByTagName("input");
        String[] inputnames = new String[100]; // FIXME
        int indx = 0;
        for (int i = 0; i < inputs.getLength(); i++) {
            Element input = (Element) inputs.item(i);
            boolean zone_ok = zone == null;
            if (!zone_ok) {
                NodeList selectcmds = input.getElementsByTagName("selectcommand");
                for (int j = 0; j < selectcmds.getLength(); j++) {
                    zone_ok = zone_ok || ((Element) selectcmds.item(j)).getAttribute("zone").equals(zone);
                }
            }
            if (zone_ok) {
                NodeList devrefs = input.getElementsByTagName("deviceref");
                for (int j = 0; j < devrefs.getLength(); j++) {
                    inputnames[indx++] = ((Element) devrefs.item(j)).getAttribute("device");
                }

                NodeList internalsrcs = input.getElementsByTagName("internalsrc");
                for (int j = 0; j < internalsrcs.getLength(); j++) {
                    inputnames[indx++] = ((Element) internalsrcs.item(j)).getAttribute("name");
                }

                NodeList externalsrcs = input.getElementsByTagName("externalsrc");
                for (int j = 0; j < externalsrcs.getLength(); j++) {
                    inputnames[indx++] = ((Element) externalsrcs.item(j)).getAttribute("name");
                }
            }
        }
        String[] result = new String[indx];
        for (int i = 0; i < indx; i++) // FIXME
        {
            result[i] = inputnames[i];
        }
        return result;
    }

    private Element find_node(String nodename, String id_name) {
        Element root = doc.getDocumentElement();
        NodeList things = root.getElementsByTagName(nodename);
        for (int i = 0; i < things.getLength(); i++) {
            if (((Element) things.item(i)).getAttribute("id").equals(id_name)) {
                return (Element) things.item(i);
            }
        }

        //System.err.println("Did not find " + nodename + " called " + id_name);
        return null;
    }

/*
 public boolean select(String devname, String src_device, int type,
            String zone, String mediatype, String connection_type)
            throws InterruptedException {
        return select(devname, src_device, type, zone,
                harcutils.encode_mediatype(mediatype), connection_type);
    }
*/
    
    public boolean select(String devname, String src_device, int type,
            String zone, mediatype the_mediatype, String connection_type)
            throws InterruptedException {
        Element dev_node = find_node("device", expand_alias(devname));
        if (dev_node == null) {
            System.err.println("No such device \"" + devname + "\".");
            return false;
        }
        src_device = expand_alias(src_device);
        NodeList inputs = dev_node.getElementsByTagName("input");
        boolean found = false;
        String actual_command = null;
        String querycommand = null;
        String expected_response = null;
        for (int i = 0; i < inputs.getLength() && !found; i++) {
            Element input = (Element) inputs.item(i);
            boolean connectiontype_ok;
            try {
                connectiontype_ok = connection_type == null || ((Element) input.getElementsByTagName("connectiontype").item(0)).getAttribute("type").equals(connection_type);
            } catch (NullPointerException e) {
                connectiontype_ok = false;
            }
            if (!connectiontype_ok) {
                if (db.dispatch()) {
                    System.err.println("Input nr " + i + " rejected since connection_type = " + connection_type + " requested");
                }
            } else {
                NodeList devicerefs = input.getElementsByTagName("deviceref");
                for (int j = 0; j < devicerefs.getLength() && !found; j++) {
                    Element dev = (Element) devicerefs.item(j);
                    found = src_device.equals(dev.getAttribute("device"));
                }
                NodeList internalsrcs = input.getElementsByTagName("internalsrc");
                for (int j = 0; j < internalsrcs.getLength() && !found; j++) {
                    Element internal = (Element) internalsrcs.item(j);
                    found = src_device.equals(internal.getAttribute("name"));
                }
                NodeList externalsrcs = input.getElementsByTagName("externalsrc");
                for (int j = 0; j < externalsrcs.getLength() && !found; j++) {
                    Element external = (Element) externalsrcs.item(j);
                    found = src_device.equals(external.getAttribute("name"));
                }
            }
            if (found) {
                String attname = the_mediatype == mediatype.audio_video ? "select" : the_mediatype.toString();
                        //mediatype == harcutils.audio_only ? "audio_only"
                        //: mediatype == harcutils.video_only ? "video_only"
                        //: "select";
                if (input.getElementsByTagName("zone").getLength() == 0) {
                    NodeList commands = input.getElementsByTagName("selectcommand");
                    for (int j = 0; j < commands.getLength() && actual_command == null; j++) {
                        Element command = (Element) commands.item(j);
                        String cmdzone = command.getAttribute("zone");
                        if (cmdzone.equals("") || cmdzone.equals("1")) {
                            actual_command = command.getAttribute(attname);
                        //System.err.println("*******" + actual_command);
                        }
                    }
                } else {
                    String zname = (zone == null || zone.equals("")) ? "1" : zone;
                    NodeList zones = input.getElementsByTagName("zone");
                    for (int j = 0; j < zones.getLength(); j++) {
                        Element z = (Element) zones.item(j);
                        if (z.getAttribute("name").equals(zname)) {
                            actual_command = ((Element) z.getElementsByTagName("selectcommand").item(0)).getAttribute(attname);
                            NodeList qcommands = z.getElementsByTagName("querycommand");
                            for (int k = 0; k < qcommands.getLength(); k++) {
                                if ((the_mediatype == mediatype.audio_only && ((Element) qcommands.item(k)).getAttribute("mediatype").equals("audio_only")) || (the_mediatype == mediatype.video_only && ((Element) qcommands.item(k)).getAttribute("mediatype").equals("video_only")) || (the_mediatype == mediatype.audio_video && ((Element) qcommands.item(k)).getAttribute("mediatype").equals("audio_video"))) {
                                    querycommand = ((Element) qcommands.item(k)).getAttribute("cmd");
                                    expected_response = ((Element) qcommands.item(k)).getAttribute("val");
                                }
                            }
                        //System.err.println("*******" + actual_command);
                        }
                    }
                }
            }
        }
        if (actual_command != null && !actual_command.equals("")) {
            if (db.decode_args()) {
                System.err.println("Found command: " + actual_command + ", query: " + querycommand + "==" + expected_response + ".");
            }
        } else {
            // FIXME
            System.err.println("No command found for turning " + devname + " to " + src_device + ((zone != null && !zone.equals("")) ? (" in zone " + zone) : "") + (the_mediatype == mediatype.audio_only ? " (audio only)"
                    : the_mediatype == mediatype.video_only ? " (video only)"
                    : "") + (connection_type == null ? "" : " using connection_type " + connection_type) + ".");
        }

        if (querycommand != null && expected_response != null && do_command(devname, ir_code.decode_command(querycommand),
                new String[0], type, 1, toggletype.toggle_0/*???*/, false).equals(expected_response)) {
            if (db.decode_args()) {
                System.err.println(devname + " already turned to " + src_device + ", ignoring");
            }
            return true;
        } else {
            return ((actual_command != null && !actual_command.equals(""))
                    ? do_command(devname, ir_code.decode_command(actual_command),
                    new String[0], type, 1, toggletype.toggle_0/*???*/, false) != null
                    : false);
        }
    }

    // Really generate and send the command, if possible
    private String transmit_command(String dev_class, int cmd,
            String[] arguments,
            String house, int deviceno,
            int type, int count,
            String gw_class, String gw_hostname,
            int portnumber,
            String gw_connector, String gw_model,
            String gw_interface,
            toggletype toggle, String mac)
            throws InterruptedException {
        if (db.transmit()) {
            System.err.println("transmit_command: device " + dev_class + ", command " + ir_code.command_name(cmd) + ", house " + house + ", deviceno " + deviceno + ", type " + commandset.toString(type) + ", count " + count + ", gw_class " + gw_class + ", gw_hostname " + gw_hostname + ":" + portnumber + ", connector " + gw_connector + ", model " + gw_model + ", interface " + gw_interface + ", toggle " + toggle);
        }
        String output = null;
        boolean success = false;
        boolean failure = false;
        device dev = null;
        // Use arguments_length instead of arguments.length to allow for arguments == null
        int arguments_length = arguments == null ? 0 : arguments.length;
        try {
            dev = new device(devicedir + File.separator + dev_class + harcutils.devicefile_extension);
        } catch (IOException e) {
            // May be ok, e.g. when using Intertechno and T-10.
            if (db.transmit()) {
                System.err.println("Could not open file " + devicedir + File.separator + dev_class + harcutils.devicefile_extension + ".");
            }
        }
        command the_command = null;
        String result = "";
        String subst_transmitstring;
        String subst_transmitstring_printable;

        if (type == commandset.www) {
            if (browser == null) {
                if (verbose > 0) {
                    System.err.println("Command of type www ignored since no browser defined.");
                }
                failure = true;
            }
        } else {
            if (!gw_class.equals("ezcontrol_t10")) {
                if (!dev.is_valid()) {
                    failure = true;
                } else {
                    the_command = dev.get_command(cmd, type);
                    if (the_command == null) {
                        System.err.println("No such command " + ir_code.command_name(cmd) + " of type " + commandset.toString(type) + " (" + type + ")");
                        failure = true;
                    } else {
                        if (db.transmit()) {
                            System.err.println("Command is: " + the_command.toString());
                        }
                        if (the_command.get_arguments().length > arguments_length) {
                            System.err.println("This command requires " + the_command.get_arguments().length + " arguments, however only " + arguments_length + " were given.");
                            failure = true;
                        }
                    }
                }
            }
        }

        if (!failure) {
            switch (type) {
                case commandset.any:
                    System.err.println("Programming/configuration error: transmit_command called with type=any.");
                    failure = true;
                    break;
                case commandset.ir:
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    try {
                        if (gw_class.equals("globalcache")) {
                            if (db.transmit()) {
                                System.err.println("Trying Globalcache (" + gw_hostname + ")...");
                            }
                            ir_code code = dev.get_code(cmd, commandset.ir, toggle);
                            if (code == null) {
                                if (verbose > 0) {
                                    System.err.println("Command " + ir_code.command_name(cmd) + " exists, but has no ir code.");
                                }
                                failure = true;
                            } else {
                                String raw_ccf = code.raw_ccf_string();
                                globalcache gc = new globalcache(gw_hostname, gw_model, verbose > 0);
                                if (gc != null) {
                                    success = gc.send_ir(raw_ccf, Integer.parseInt(gw_connector.substring(3)), count);
                                    if (db.transmit()) {
                                        System.err.println("Globalcache " + (success ? "succeeded" : "failed"));
                                    }
                                }
                            }
                        } else if (gw_class.equals("irtrans")) {
                            if (db.transmit()) {
                                System.err.println("Trying an Irtrans...");
                            }
                            irtrans irt = new irtrans(gw_hostname, verbose > 0);
                            command c = dev.get_command(cmd, commandset.ir);
                            if (c == null) {
                                if (verbose > 0) {
                                    System.err.println("Command " + ir_code.command_name(cmd) + " exists, but has no ir code.");
                                }
                                failure = true;
                            } else {
                                if (gw_interface.equals("preprog_ascii")) {
                                    success = irt.send_flashed_command(the_command.get_remotename(), c.getcmd(), gw_connector, count);
                                } else if (gw_interface.equals("web_api")) {
                                    if (count > 1) {
                                        System.err.println("** Warning: count > 1 (= " + count + ") ignored.");
                                    }
                                    String url = irtrans.make_url(gw_hostname, the_command.get_remotename(), ir_code.command_name(c.getcmd()), gw_connector);
                                    if (db.transmit() || verbose > 0) {
                                        System.err.println("Getting URL " + url);
                                    }
                                    URL the_url = new URL(url);
                                    success = the_url.getContent() != null;
                                } else if (gw_interface.equals("udp")) {
                                    System.err.println(gw_interface);
                                    ir_code code = dev.get_code(cmd, commandset.ir, toggle);
                                    success = irt.send_ir(code, gw_connector, count);
                                } else {
                                    System.err.println("Interface \"" + gw_interface + "\" for IRTrans not implemented.");
                                    success = false;
                                }
                            }
                        } else if (gw_class.equals("lirc_server")) {
                            if (db.transmit()) {
                                System.err.println("Trying a Lirc server...");
                            }

                            command c = dev.get_command(cmd, commandset.ir);
                            if (c == null) {
                                if (verbose > 0) {
                                    System.err.println("Command " + ir_code.command_name(cmd) + " exists, but has no ir code.");
                                }
                                failure = true;
                            } else {
                                lirc lirc_client = new lirc(gw_hostname, verbose > 0);
                                if (lirc_client != null) {
                                    // TODO: evaluate connector
                                    success = lirc_client.send_ir(the_command.get_remotename(), c.getcmd(), count);
                                }
                            }
                        }
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw_hostname);
                    } catch (IOException e) {
                        System.err.println("IOException with host " + gw_hostname + ", server not running?");
                    }
                    break;

                case commandset.rf433:
                case commandset.rf868:
                    if (db.transmit()) {
                        System.err.println("Trying rfxxx to " + gw_class);
                    }
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    if (gw_class.equals("ezcontrol_t10")) {
                        ezcontrol t10 = new ezcontrol(gw_hostname, verbose > 0);
                        if (cmd == commandnames.get_status) {
                            int preset = -1;
                            try {
                                preset = Integer.parseInt(gw_connector.substring(7));
                                int stat = t10.get_preset_status(preset);
                                //System.out.println("Response: " + (stat == 1 ? "on"
// 							       : stat == 0 ? "off"
// 							       : "unknown"));
                                output = stat == 1 ? "on"
                                        : stat == 0 ? "off"
                                        : "unknown";
                            } catch (StringIndexOutOfBoundsException e) {
                                System.err.println("preset number not parseable");
                                failure = true;
                            } catch (NumberFormatException e) {
                                System.err.println("preset number not parseable");
                                failure = true;
                            }
                        } else {
                            try {
                                success = t10.send_manual(dev_class, house, deviceno, cmd, count);
                            } catch (non_existing_command_exception e) {
                                System.err.println("Command not implemented");
                            }
                        }

                    } else {
                        System.err.println("Not implemented");
                        failure = true;
                    }

                    break;
                case commandset.tcp:
                    subst_transmitstring = dev.get_command(cmd, commandset.tcp).get_transmitstring(true);
                    subst_transmitstring_printable = dev.get_command(cmd, commandset.tcp).get_transmitstring(false);
                    for (int i = 0; i < arguments_length; i++) {
                        subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                        subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                    }
                    if (db.transmit()) {
                        System.err.println("Trying TCP socket to " + gw_hostname + ":" + portnumber + " \"" + subst_transmitstring_printable + "\"");
                    }
                    Socket sock = null;
                    try {
                        sock = new Socket(gw_hostname, portnumber);
                        //DataOutputStream outToServer = new DataOutputStream(sock.getOutputStream());
                        PrintStream outToServer = new PrintStream(sock.getOutputStream());
                        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        if (db.transmit() || verbose > 0) {
                            System.err.println("Sending command \"" + subst_transmitstring_printable + "\" to socket " + gw_hostname + ":" + portnumber + (count == 1 ? " (one time)" : (" (" + count + " times)")));
                        }

                        for (int c = 0; c < count; c++) {
                            int delay_between_reps = the_command.get_delay_between_reps();
                            if (delay_between_reps > 0 && c > 0) {
                                if (db.transmit() || verbose > 0) {
                                    System.err.println("Waiting for " + delay_between_reps + "ms, then sending.");
                                }
                                Thread/*.currentThread()*/.sleep(delay_between_reps);
                            }
                            //outToServer.writeBytes(subst_transmitstring);
                            outToServer.print(subst_transmitstring);
                        }

                        if (dev.get_command(cmd, commandset.tcp).get_response_lines() < 0) {
                            if (dev.get_command(cmd, commandset.tcp).get_response_ending().equals("")) {
                                // Loop until interrupted
                                System.err.println("*** This will loop until interrupted");
                                for (;;) {
                                    System.out.println(inFromServer.readLine());
                                }
                            } else {
                                int i = 0;
                                boolean found = false;
                                do {
                                    if (i++ > 0) {
                                        result = result + "\n";
                                    }
                                    String l = inFromServer.readLine();
                                    if (db.transmit()) {
                                        System.err.println("Got: " + l);
                                    }
                                    result = result + l;
                                    found = l.equals(dev.get_command(cmd, commandset.tcp).get_response_ending());
                                } while (!found);
                            }
                        } else {
                            for (int i = 0; i < dev.get_command(cmd, commandset.tcp).get_response_lines(); i++) {
                                if (i > 0) {
                                    result = result + "\n";
                                }
                                result = result + inFromServer.readLine();
                            }
                        }
                        sock.close();
                        if (dev.get_command(cmd, commandset.tcp).get_response_lines() != 0) //System.err.println("Response: " + result);
                        {
                            output = result;
                        }
                        success = true;
                    } catch (java.net.NoRouteToHostException e) {
                        System.err.println("No route to " + gw_hostname);
                    } catch (IOException e) {
                        System.err.println("Could not get I/O for the connection to " + gw_hostname + ":" + portnumber);
                        failure = true;
                    }

                    break;

                case commandset.web_api:
                    subst_transmitstring = dev.get_command(cmd, commandset.web_api).get_transmitstring(true);
                    subst_transmitstring_printable = dev.get_command(cmd, commandset.web_api).get_transmitstring(false);
                    for (int i = 0; i < arguments_length; i++) {
                        subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                        subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                    }
                    if (gw_class.equals("lan")) {
                        String urlstr = "http://" + gw_hostname + ":" + portnumber + "/" + subst_transmitstring;
                        try {
                            for (int c = 0; c < count; c++) {
                                if (c > 0) {
                                    int delay_between_reps = the_command.get_delay_between_reps();
                                    if (delay_between_reps > 0) {
                                        if (db.transmit() || verbose > 0) {
                                            System.err.println("Waiting for " + delay_between_reps + "ms.");
                                        }
                                        Thread./*currentThread().*/sleep(delay_between_reps);
                                    }
                                }

                                if (db.transmit() || verbose > 0) {
                                    System.err.println("Getting URL " + urlstr + ".");
                                }
                                int response_lines = the_command.get_response_lines();
                                if (response_lines == 0 && !the_command.get_expected_response().equals("")) // this is contradictory, fix
                                {
                                    response_lines = 1;
                                }

                                URL the_url = new URL(urlstr);

                                if (response_lines == 0) {
                                    the_url.getContent();
                                } else if (response_lines > 0) {
                                    BufferedReader inFromServer = null;
                                    inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    //inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    output = "";
                                    for (int i = 0; i < response_lines; i++) {
                                        result = inFromServer.readLine();
                                        if (result != null)
                                            output = output.equals("") ? result : output + "\n" + result;
                                        if (db.transmit() || verbose > 0 || (the_command.get_expected_response().equals("") && the_command.get_response_lines() == 0)) {
                                            System.out.println("Got: " + result);	// ??
                                        }
                                    }
                                } else {
                                    BufferedReader inFromServer = null;
                                    //inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    //URLConnection uc = the_url.openConnection();
                                    //System.err.println(uc.getContentEncoding());
                                    //for (int k = 0; k < 20; k++)
                                    //    System.err.println(uc.getHeaderField(k));
                                    inFromServer = new BufferedReader(new InputStreamReader(the_url.openStream()));
                                    output = "";
                                    do {
                                        result = inFromServer.readLine();
                                        if (result != null)
                                            output = output.equals("") ? result : output + "\n" + result;
                                        if ((db.transmit() || verbose > 0) && result != null) {
                                            System.out.println(result);	// ??
                                        }
                                    } while (result != null);
                                }

                                if (!the_command.get_expected_response().equals("")) {
                                    if (result.equals(the_command.get_expected_response())) {
                                        if (db.transmit()) {
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
                        }
                    }
                    break;

                case commandset.serial:
                    if (gw_class.equals("globalcache")) {
                        subst_transmitstring = dev.get_command(cmd, commandset.serial).get_transmitstring(true);
                        subst_transmitstring_printable = dev.get_command(cmd, commandset.serial).get_transmitstring(false);
                        for (int i = 0; i < arguments_length; i++) {
                            subst_transmitstring = subst_transmitstring.replaceAll("\\$" + (i + 1), arguments[i]);
                            subst_transmitstring_printable = subst_transmitstring_printable.replaceAll("\\$" + (i + 1), arguments[i]);
                        }
                        globalcache gc = new globalcache(gw_hostname, gw_model, verbose > 0);
                        try {
                            if (db.transmit() || verbose > 0) {
                                System.err.println("Trying a Globalcache for serial using " + gw_connector + ", \"" + subst_transmitstring_printable + "\"");
                            }
                            int delay_between_reps = the_command.get_delay_between_reps();
                            result = gc.send_serial(subst_transmitstring, Integer.parseInt(gw_connector.substring(7)), the_command.get_response_lines(), count, delay_between_reps);
                            success = true;
                            if (the_command.get_response_lines() > 0) //System.out.println("Response: " + result);
                            {
                                output = result;
                            }
                        } catch (java.net.NoRouteToHostException e) {
                            System.err.println("No route to " + gw_hostname);
                        } catch (IOException e) {
                            System.err.println("IOException with host " + gw_hostname);
                        }
                    } else {
                        System.err.println("Not implemented.");
                        failure = true;
                    }
                    break;

                case commandset.www:
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    String url = "http://" + gw_hostname + ":" + portnumber + "/";
                    if (db.transmit()) {
                        System.err.println("Starting " + browser + " " + url);
                    }
                    String cmd_array[] = new String[2];
                    cmd_array[0] = browser;
                    cmd_array[1] = url;
                    try {
                        Process gc_process = java.lang.Runtime.getRuntime().exec(cmd_array);
                    } catch (IOException e) {
                        System.err.println("Could not exec command \"" + browser + " " + url + "'.");
                    }

                    break;
                case commandset.on_off:
                    if (arguments_length > 0) {
                        System.err.println("Warning: arguments to command igored.");
                    }
                    if ((cmd != cmd_power_on) && (cmd != cmd_power_off) && (cmd != cmd_power_toggle) && (cmd != cmd_power_pulse) && (cmd != get_state)) {
                        System.err.println("Nonappropriate command for on_off");
                        failure = true;
                    } else if (gw_class.equals("globalcache")) {
                        globalcache gc = new globalcache(gw_hostname, gw_model, verbose > 0);
                        int con = Integer.parseInt(gw_connector.substring(gw_connector.indexOf('_') + 1));
                        try {
                            if (cmd == get_state) {
                                if (gw_connector.startsWith("sensor_")) {
                                    if (db.transmit()) {
                                        System.err.print("Trying to inquiry Globalcache sensor " + gw_connector + " ");
                                    }
                                    output = gc.getstate(con) == 1 ? "on" : "off";
                                } else {
                                    failure = true;
                                }
                            } else {
                                if (gw_connector.startsWith("relay_")) {

                                    if (db.transmit()) {
                                        System.err.print("Trying to turn Globalcache relay #" + con + " ");
                                    }
                                    if (cmd == cmd_power_toggle) {
                                        if (db.transmit()) {
                                            System.err.println("TOGGLE");
                                        }
                                        success = gc.togglestate(con);
                                    } else if (cmd == cmd_power_pulse) {
                                        if (db.transmit()) {
                                            System.err.println("PULSE");
                                        }
                                        success = gc.pulsestate(con);
                                    } else {
                                        if (db.transmit()) {
                                            System.err.println(cmd == cmd_power_on ? "ON" : "OFF");
                                        }
                                        success = gc.setstate(con, cmd == cmd_power_on);
                                    }
                                } else {
                                    failure = true;
                                }
                            }
                        } catch (java.net.NoRouteToHostException e) {
                            System.err.println("No route to " + gw_hostname);
                        } catch (java.net.UnknownHostException e) {
                            System.err.println("Unknown host " + gw_hostname);
                        } catch (IOException e) {
                            System.err.println("IOException with host " + gw_hostname);
                        }
                    } else {
                        System.err.println("Not implemented.");
                        failure = true;
                    }
                    break;

                case commandset.ip:
                    if (!gw_class.equals("lan")) {
                        System.err.println("Programming/configuration error: gateway class lan expected.");
                        failure = true;
                    } else {
                        if (cmd == ping) {
                            try {
                                if (verbose > 0) {
                                    System.err.print("Trying to ping " + gw_hostname + "...");
                                }
                                success = InetAddress.getByName(gw_hostname).isReachable(null, 0, harcutils.ping_timeout);
                                if (verbose > 0) {
                                    System.err.println(success ? "succeded." : "failed.");
                                }
                            } catch (IOException e) {
                                System.err.println(e.getMessage());
                            }
                        } else if (cmd == wol) {
                            if (verbose > 0) {
                                System.err.println("Sending a WOL package to " + gw_hostname + ".");
                            }
                            try {

                                WakeUpUtil.wakeup(new EthernetAddress(mac));
                                success = true;
                            } catch (IllegalEthernetAddressException e) {
                                System.err.println(e.getMessage());
                            } catch (IOException e) {
                                System.err.println(e.getMessage());
                            }
                        } else {
                            System.err.println("Command " + ir_code.command_name(cmd) + " of type ip not implemented.");
                            failure = true;
                        }
                    }
                    break;
                case commandset.udp:
                case commandset.bluetooth:
                default:
                    System.err.println("Command of type " + commandset.toString(type) + " not yet implemented");
                    failure = true;
            }
        }

        if (output != null) {
            success = true;
        }
        if (success && output == null) {
            output = "";
        }

        if (db.transmit()) {
            System.err.println("transmit_command returns \"" + (success && output != null ? output : "null") + "\"");
        }
        return success ? output : null;
    }

    public String expand_alias(String dname) {
        Element alias_node = find_node("alias", dname);
        if (alias_node != null) {
            return alias_node.getAttribute("device");
        } else {
            return dname;
        }
    }

    /**
     * 
     * @param devicename Name of the device in question
     * @return true if a device with that name exists.
     */
    public boolean has_device(String devicename) {
        devicename = expand_alias(devicename);
        return find_node("device", devicename) != null;
    }

    // Returns null by failure, otherwise output from command (possibly "").
    public String do_command(String devname, int cmd, String[] arguments,
            int type, int count,
            toggletype toggle, boolean smart_memory)
            throws InterruptedException {
        String output = "";
        devname = expand_alias(devname);
        Element dev_node = find_node("device", devname);
        if (dev_node == null) {
            System.err.println("Device \"" + devname + "\" not found.");
            return null;
        }
        String dev_class = dev_node.getAttribute("class");
        device dev = null;
        try {
            dev = new device(dev_class);
        } catch (IOException e) {
            //if (debug_dispatch())
            System.err.println("Cannot read device file " + dev_class + ".");
        }
        if (!(dev != null && dev.is_valid())) {
            return null;
        }

        String alias = dev.get_alias(cmd);
        if (alias != null) {
            if (verbose > 0 || db.dispatch()) {
                System.err.println("Command " + ir_code.command_name(cmd) + " aliased to " + alias);
            }
            cmd = ir_code.decode_command(alias);
        }
        command the_command = dev.get_command(cmd, type /* FIXME: , toggle*/);
        if (the_command == null && type != commandset.www) {
            System.err.println("No such command " + ir_code.command_name(cmd) + " of type " + commandset.toString(type) + " (" + type + ")");
            return null;
        }

        String mem = dev.get_attribute("memory");
        boolean has_memory = (mem != null) && mem.equals("yes");
        String house = dev_node.getAttribute("house");
        if (db.dispatch()) {
            System.err.println("do_command: device " + devname + ", device_class " + dev_class + ", command " + ir_code.command_name(cmd) + ", type " + commandset.toString(type) + ", memory " + has_memory + ", toggle " + toggle);
        }
        String deviceno_str = dev_node.getAttribute("deviceno");
        int deviceno = deviceno_str.equals("") ? -1 : Integer.parseInt(deviceno_str);
        //NodeList from_gateways = dev_node.getElementsByTagName("from-gateway");
        NodeList from_gateways = dev_node.getChildNodes();
        boolean success = false;
        for (int i = 0; i < from_gateways.getLength() && !success; i++) {
            Node n = from_gateways.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element fgw = (Element) n;
                if (n.getNodeName().equals("from-gateway-ref")) {
                    fgw = find_from_gateway(fgw.getAttribute("from-gateway"));
                }
                if (fgw.getNodeName().equals("from-gateway")) {

                    String fgw_name = fgw.getAttribute("gateway");
                    String fgw_hostname = fgw.getAttribute("hostname");
                    String fgw_portnumber_str = fgw.getAttribute("portnumber");
                    int fgw_portnumber = (fgw_portnumber_str.equals("")) ? -1 : Integer.parseInt(fgw_portnumber_str);
                    String fgw_connector = fgw.getAttribute("connector");
                    String mac = fgw.getAttribute("mac");
                    int no_times =
                            (((cmd == cmd_power_on) || (cmd == cmd_power_reverse_on)) && smart_memory && has_memory)
                            ? 2 : 1;
                    output = "";
                    for (int j = 0; j < no_times && output != null; j++) {
                        output = dispatch_command2gateway(fgw_name,
                                fgw_connector,
                                fgw_hostname,
                                fgw_portnumber,
                                mac,
                                dev_class,
                                cmd,
                                arguments,
                                house,
                                deviceno,
                                type,
                                count,
                                toggle, 0);
                    }
                    success = success || output != null;
                }
            }
        }
        return success ? output : null;
    }

    // Sends the gateway described by gw_name the
    // command described by dev_class and cmd, type, count, and toggle.
    // For this, the contained from-gateway elements are tried.
    // Either calls itself recursively, or calls transmit_command.
    private String dispatch_command2gateway(String fgw_name,
            String fgw_connector,
            String fgw_hostname,
            int fgw_portnumber,
            String mac,
            String dev_class,
            int cmd, String[] arguments,
            String house,
            int deviceno, int type,
            int count, toggletype toggle, int hop)
            throws InterruptedException {
        if (db.dispatch()) {
            System.err.println("dispatch_command2gateway: gw " + fgw_name + ", connector " + fgw_connector + ", hostname " + fgw_hostname + ", portnumber " + fgw_portnumber + ", device " + dev_class + ", command " + ir_code.command_name(cmd) + ", house " + house + ", deviceno " + deviceno + ", type " + commandset.toString(type) + ", hops " + hop);
        }
        String outputs = "";
        Element gateway = find_node("gateway", fgw_name);
        if (gateway == null) {
            System.err.println("Did not find gateway " + fgw_name);
            System.exit(3);
        }

        NodeList chldlst = gateway.getElementsByTagName("hostname");
        String gw_hostname = chldlst.getLength() > 0 ? ((Element) chldlst.item(0)).getAttribute("ipname") : "";

        boolean is_lan = gateway.getElementsByTagName("lan").getLength() > 0;
        String gw_class = gateway.getAttribute("class");
        String gw_model = gateway.getAttribute("model");
        String gw_interface = gateway.getAttribute("interface");

        int act_type = type;
        NodeList gw_outputs = gateway.getElementsByTagName("output");
        for (int j = 0; j < gw_outputs.getLength(); j++) {
            Element output = (Element) gw_outputs.item(j);
            if (output.getAttribute("connector").equals(fgw_connector)) {
                act_type = commandset.toInt(output.getAttribute("type"));
            }
        }

        if (!(type == commandset.any || type == act_type)) {
            if (db.dispatch()) {
                System.err.println("Gateway " + fgw_name + ", connector " + fgw_connector + " not capable of type " + commandset.toString(type) + ", returning");
            }
            outputs = null;
        } else if (is_lan) {
            outputs = transmit_command(dev_class, cmd, arguments, house,
                    deviceno,
                    act_type, count, gw_class, fgw_hostname,
                    fgw_portnumber, fgw_connector,
                    gw_model, gw_interface,
                    toggle, mac);
        } else if (!gw_hostname.equals("")) {
            outputs = transmit_command(dev_class, cmd, arguments, house,
                    deviceno,
                    act_type, count, gw_class, gw_hostname,
                    fgw_portnumber, fgw_connector,
                    gw_model, gw_interface,
                    toggle, mac);
        } else {
            //NodeList fgwlist = gateway.getElementsByTagName("from-gateway");
            NodeList fgwlist = gateway.getChildNodes();
            int actual_command = cmd;
            String remotename = dev_class;
            for (int i = 0; i < fgwlist.getLength(); i++) {
                Node n = fgwlist.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE) {
                    Element fgw = (Element) n;
                    if (fgw.getNodeName().equals("from-gateway-ref")) {
                        fgw = find_from_gateway(fgw.getAttribute("from-gateway"));
                    }

                    if (fgw.getNodeName().equals("from-gateway")) {
                        for (int j = 0; j < gw_outputs.getLength(); j++) {
                            // Gateway with a number of outputs, try them in turn.
                            Element output = (Element) gw_outputs.item(j);
                            int output_type = commandset.toInt(output.getAttribute("type"));
                            if (output.getAttribute("connector").equals(fgw_connector) && (type == commandset.any || type == output_type)) {
                                String cmd_name = ir_code.command_name(cmd);

                                NodeList cmds = output.getElementsByTagName("command");

                                // Alias command if appropriate
                                for (int k = 0; k < cmds.getLength(); k++) {
                                    Element the_cmd = (Element) cmds.item(k);
                                    if (cmd_name.equals(the_cmd.getAttribute("cmd"))) {
                                        actual_command = ir_code.decode_command(the_cmd.getAttribute("commandname"));
                                        remotename = the_cmd.getAttribute("remotename");
                                        house = the_cmd.getAttribute("house");
                                        String deviceno_str = the_cmd.getAttribute("deviceno");
                                        if (!deviceno_str.equals("")) {
                                            deviceno = Integer.parseInt(deviceno_str);
                                        }
                                        if (db.dispatch()) {
                                            System.err.println("Substituting command " + cmd_name + " by " + the_cmd.getAttribute("commandname") + ", remotename " + remotename + ", deviceno " + deviceno + ", house " + house);
                                        }
                                    }
                                }

                                // must invoke dispatch_command again.
                                outputs = dispatch_command2gateway(fgw.getAttribute("gateway"),
                                        fgw.getAttribute("connector"),
                                        gw_hostname,
                                        fgw_portnumber,
                                        mac,
                                        remotename,
                                        actual_command,
                                        arguments,
                                        house,
                                        deviceno,
                                        type,
                                        count,
                                        toggle,
                                        hop + 1);
                            }
                        }
                    }
                }
            }
        }
        return outputs;
    }

    private static void usage(int errorcode) {
        System.err.println("Usage:\n" + "home [<options>] <device_instancename> <command> [<command_args>]*" + "\nwhere options=-h <filename>,-t " + commandset.valid_types('|') + ",-m,-T 0|1,-# <count>,-v,-d <debugcode>,-b <browserpath>, -p <propsfile>\n" + "or\n" + "home -s [-z zone][-A,-V][-c connection_type] <device_instancename> <src_device>");
        System.exit(errorcode);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }

    public static void main(String[] args) {
        int type = commandset.any;
        String home_filename = null;
        int debug = 0;
        int verbose = 0;
        int count = 1;
        mediatype the_mediatype = mediatype.audio_video;
        boolean smart_memory = false;
        boolean select_mode = false;
        boolean list_commands = false;
        String devname = "";
        String src_device = "";
        String zone = null;
        String connection_type = null;
        int cmd = commandnames.cmd_invalid;
        toggletype toggle = toggletype.no_toggle;
        String browser = null;
        String[] arguments = null;
        String propsfilename = null;
        debugargs db = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-b")) {
                    arg_i++;
                    browser = args[arg_i++];
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    connection_type = args[arg_i++];
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
                    if (!commandset.valid(typename)) {
                        usage();
                    }
                    type = commandset.toInt(typename);
                } else if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose++;
                } else if (args[arg_i].equals("-z")) {
                    arg_i++;
                    zone = args[arg_i++];
                } else if (args[arg_i].equals("-A")) {
                    arg_i++;
                    the_mediatype = mediatype.audio_only;
                } else if (args[arg_i].equals("-V")) {
                    arg_i++;
                    the_mediatype = mediatype.video_only;
                } else if (args[arg_i].equals("-T")) {
                    arg_i++;
                    toggle = harcutils.decode_toggle(args[arg_i++]);
                } else {
                    usage();
                }
            }

            db = new debugargs(debug);
            devname = args[arg_i];

            // Setup properites
            if (propsfilename != null)
                harcprops.initialize(propsfilename);
            else
                harcprops.initialize();

            if (home_filename == null)
                home_filename = harcprops.get_instance().get_home_file();
            if (browser == null)
                browser = harcprops.get_instance().get_browser();

            if (select_mode) {
                src_device = args[arg_i + 1];
                if (db.decode_args()) {
                    System.err.println("Select mode: devname = " + devname + ", src_device = " + src_device + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
                }
            } else if (!devname.equals("?")) {
                String cmdname = args[arg_i + 1];
                list_commands = cmdname.equals("?");
                cmd = ir_code.decode_command(cmdname);
                // Compatibility with old command names. Or simply silly?
                if (cmd == commandnames.cmd_invalid) {
                    System.err.println("Warning: Substituting non-existing commands " + cmdname + " by cmd_" + cmdname);
                    cmd = ir_code.decode_command("cmd_" + cmdname);
                }
                if (db.decode_args()) {
                    System.err.println("devname = " + devname + ", commandname = " + args[arg_i + 1] + "(#" + cmd + ")");
                }

                if (!list_commands && cmd == commandnames.cmd_invalid) {
                    System.err.println("Command \"" + args[arg_i + 1] + "\" not recognized, aborting.");
                    System.exit(7);
                }

                int no_arguments = args.length - arg_i - 2;
                arguments = new String[no_arguments];

                if (db.decode_args() && no_arguments > 0) {
                    System.err.print("Command arguments: ");
                }

                for (int i = 0; i < no_arguments; i++) {
                    arguments[i] = args[arg_i + 2 + i];
                    if (db.decode_args()) {
                        System.err.print("arguments[" + i + "] = " + arguments[i] + (i == no_arguments - 1 ? ".\n" : ", "));
                    }
                }
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            if (db.decode_args()) {
                System.err.println("ArrayIndexOutOfBoundsException");
            }
            usage();
        } catch (NumberFormatException e) {
            if (db.decode_args()) {
                System.err.println("NumberFormatException");
            }
            usage();
        }

        try {
            home hm = new home(home_filename, verbose, debug, browser);
            if (select_mode) {
                //harcutils.printtable("blaa", hm.get_selecting_devices());
                if (src_device.equals("?")) {
                    harcutils.printtable("Valid inputs for " + devname + (zone != null ? (" in zone " + zone) : "") + ":", hm.get_sources(devname, zone));
                } else {
                    hm.select(devname, src_device, type, zone, the_mediatype, connection_type);
                }
            } else if (devname.equals("?")) {
                harcutils.printtable("Valid devices:", hm.get_devices());
            } else if (list_commands) // FIXME1: if devname is nonexisting, should produce an
            // error message instead of just returning nothing.
            {
                harcutils.printtable("Valid commands for " + devname + " of type " + commandset.toString(type) + ":", hm.get_commands(devname, type));
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
            System.exit(17);
        } catch (InterruptedException e) {
            System.err.println("** Interrupted **");
            System.exit(18);
        }
    }
}
