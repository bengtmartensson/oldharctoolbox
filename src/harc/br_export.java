/*
Copyright (C) 2009 Bengt Martensson.

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

package harc;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;

/**
 * Class for generating <a href="http://www.hifi-remote.com/ofa/"/>JP1</a> device extensions for RemoteMaster from a devicefile.
 */
public class br_export {

    public static final String extension = "br";

    // TODO: replace by numbered, abstract states.
    private enum assignment {
        none,
        unshifted,
        shifted,
        xshifted
    }

    private class fcn {

        public command_t cmd;
        public int obc; // = my commandno
        public String remark;
        public String one_button_name;// One of the buttons that are bound to this function

        public fcn(command_t cmd, int obc, String remark) {
            this.cmd = cmd;
            this.obc = obc;
            this.remark = remark;
        }

        @Override
        public String toString() {
            return cmd + "\t" + obc + "\t" + one_button_name;
        }
    }

    private class button {

        public String hardname;
        public Vector<command_t> commands;  // Possible function names
        public boolean match_required;
        public boolean match_required_unshifted;
        public short keycode;
        public fcn unshifted;
        public fcn shifted;
        public fcn xshifted;

        @Override
        public String toString() {	// just for debugging
            String result = hardname + ":";
            for (command_t c : commands) {
                result = result + "\t" + c;
            }
            result = result + (unshifted != null ? "\tunshifted=" + unshifted.cmd : "");
            result = result + (shifted != null ? "\tshifted=" + shifted.cmd : "");
            result = result + (xshifted != null ? "\txshifted=" + xshifted.cmd : "");
            return result;
        }

        public button(String name, short keycode) {
            this.hardname = name;
            this.keycode = keycode;
            unshifted = null;
            shifted = null;
            xshifted = null;
            commands = new Vector<command_t>();
            command_t c = command_t.parse(name.toLowerCase());
            if (c == command_t.invalid)
                c = command_t.parse("cmd_" + name.toLowerCase());
            if (c != command_t.invalid)
                commands.add(c);
        }

        /** Assing function in the argument to this button */
        public assignment assign(fcn f, boolean unshifted_only) {
            assignment result = assignment.none;
            if (unshifted == null) {
                unshifted = f;
                result = assignment.unshifted;
                f.one_button_name = hardname;
            } else if (!unshifted_only && unshifted.cmd != f.cmd ) {
                if (shifted == null) {
                    shifted = f;
                    result = assignment.shifted;
                    f.one_button_name = hardname;
                } else if (remote.get_no_states() >= 3 && shifted.cmd != f.cmd && xshifted == null) {
                    xshifted = f;
                    result = assignment.xshifted;
                    f.one_button_name = hardname;
                }
            }
            return result;
        }

        public command_t get_unshifted_cmd() {
            return unshifted == null ? null : unshifted.cmd;
        }

        public command_t get_shifted_cmd() {
            return shifted == null ? null : shifted.cmd;
        }

        public command_t get_xshifted_cmd() {
            return xshifted == null ? null : xshifted.cmd;
        }
    }

    public static final String doctype = ".." + File.separator + harcprops.get_instance().get_dtddir() + File.separator + "br_export.dtd";//FIXME
    private static int debug = 0;
    private button_remote remote = null;
    private device dev;
    private Document rules;
    private Document resultdoc;
    private Hashtable<command_t, fcn> functions;
    private fcn[] function_array; // Just to record the original ordering of the commands.
    private Hashtable<String,button> buttons;
    private fcn reserved_fcn = new fcn(command_t.invalid, -1, "Dummy entry");
    private String protocol_name; // Our name for the protocol
    private short dev_no = -1;
    private short subdev_no = -1;
    private int setup_code = -1;
    private device_type dev_type;
    private jp1protocoldata jp1info;

    private boolean valid = false;

    private void dump_buttons() {
        System.err.println("------------ Buttons ----------");
        for (button b : buttons.values()) {
            System.err.println(b.toString());
        }
    }

    private void dump_functions() {
        System.err.println("------------ Functions ----------");
        for (fcn f : functions.values()) {
            System.err.println(f.toString());
        }
    }

    private static int jp1int(String s) {
        return s.startsWith("$") ? Integer.parseInt(s.substring(1), 16) : Integer.parseInt(s);
    }

    private static int cmdnoint(String s) {
        return s.startsWith("0x") ? Integer.parseInt(s.substring(2), 16) : Integer.parseInt(s);
    }

    public br_export(String jp1remotename, String device_or_filename, String rulesfile, String remotename)
            throws IOException, SAXParseException, SAXException {
        this(jp1remotename, device_or_filename, rulesfile, remotename, null);
    }

    public br_export(String button_remotename, String device_or_filename, String rulesfile, String remotename, device_type devtype)
            throws IOException, SAXParseException, SAXException {
        remote = new button_remote(button_remotename);
        if (!remote.is_valid()) {
            System.err.println("Error: cannot find button_remote " + button_remotename);
            return;
        }

        dev = new device(device_or_filename);
        dev_type = devtype != null ? devtype : dev.get_type();
        rules = rulesfile != null && !rulesfile.isEmpty() ? harcutils.open_xmlfile(rulesfile) : null;

        commandset[] cmdsets;
        if (remotename != null) {
            cmdsets = dev.get_commandsets(remotename, commandtype_t.ir);
            if (cmdsets.length == 0) {
                System.err.println("Error: No ir commandset for remote " + remotename + " found.");
                return;
            }
        } else {
            cmdsets = dev.get_commandsets(commandtype_t.ir);
            if (cmdsets.length == 0) {
                System.err.print("Error: No ir commandset found.");
                return;
            }
        }

        if (cmdsets.length > 1)
                System.err.println("Warning: More than one ir commandset found. Ignoring all but the first.");
        commandset the_commandset = cmdsets[0];

        dev_no = the_commandset.get_deviceno();
        subdev_no = the_commandset.get_subdevice();

        protocol_name = the_commandset.get_protocol();

        jp1info = protocol_parser.get_jp1data(protocol_name);

        setup_code = dev.get_jp1_setupcode();
        if (setup_code == -1) {
            setup_code = dev.get_name().hashCode() % 2048;
            if (setup_code < 0)
                setup_code = -setup_code;
            System.err.println("Warning: no JP1 setupcode found, constructing one (" + setup_code + ").");
        }

        command[] commands = the_commandset.get_commands();
        functions = new Hashtable<command_t,fcn>(commands.length);
        function_array = new fcn[commands.length];
        for (int i = 0; i < commands.length; i++) {
            int obc = commands[i].get_commandno();
            fcn f = new fcn(commands[i].get_cmd(), obc, commands[i].get_remark());
            functions.put(commands[i].get_cmd(), f);
            function_array[i] = f;
        }

        buttons = new Hashtable<String,button>(100);
        for (int i = 0; i < remote.get_no_buttons(); i++) {
            button_remote.button b = remote.get_button(i);
            buttons.put(b.get_name().toLowerCase(), new button(b.get_name(), b.get_keycode()));
        }
        if (debug > 0)
            dump_buttons();

        check_reserved_buttons();
        check_rules_buttons();

        interpret_rules();

        valid = true;
    }

    private boolean check_buttons(String tagname) {
        if (rules == null)
            return true;

        boolean success = true;
        NodeList nl = rules.getElementsByTagName(tagname);
        for (int i = 0; i < nl.getLength(); i++) {
            String button_name = ((Element) nl.item(i)).getAttribute("button");
            boolean found = buttons.containsKey(button_name.toLowerCase());

            if (!found) {
                success = false;
                System.err.println("Warning: " + tagname + " `" + button_name + "' not found in present remote");
            }
        }
        return success;
    }

    private void check_reserved_buttons() {
        check_buttons("reserve-button");
    }

    private void check_rules_buttons() {
        check_buttons("names");
    }

    private command_t parse_functionname(String name) {
        command_t c = command_t.parse(name.toLowerCase());
        return c != command_t.invalid ? c : command_t.parse("cmd_" + name.toLowerCase());
    }

    // may the function f be assigned to button b?
    private boolean is_ok(button b, fcn f) {
        if (f == null)
            return false;
        return !(b.unshifted != null && b.unshifted == f || b.shifted != null && b.shifted == f || remote.get_no_states() >= 3 && b.xshifted != null && b.xshifted == f);
    }

    private void assign_buttons(boolean only_unshifted) {
        for (button b : buttons.values()) {
            //button b = buttons[but];
            //fcn f = null;
            for (command_t c : b.commands) {
                fcn f = functions.get(c);
                if (is_ok(b, f))
                    b.assign(f, only_unshifted);
            }
        }
    }

    private void do_names(Element the_node) {
        String buttonname = the_node.getAttribute("button");
        boolean match_required = false;
        boolean match_required_unshifted = false;
        if (the_node.getAttribute("unshifted-only").equals("yes"))
            match_required_unshifted = the_node.getAttribute("match-required").equals("yes");
        else
            match_required = the_node.getAttribute("match-required").equals("yes");

        button b = buttons.get(buttonname.toLowerCase());
        //System.err.println("-------------" + buttonname.toLowerCase() + "\t" + b.toString());
        if (b == null)
            return;

        String devicetype = the_node.getAttribute("devicetype");
        String[] devicetypes = devicetype.split("\\|");
        boolean devicetype_ok = devicetype.equals("");
        for (int j = 0; j < devicetypes.length && !devicetype_ok; j++) {
            //System.err.println(dev.get_type()+devicetypes[j]);
            devicetype_ok = device_type.parse(devicetypes[j]) == dev.get_type();
        }

        if (the_node.getTagName().equals("names")) {
            if (devicetype_ok) {
                b.match_required = match_required;
                b.match_required_unshifted = match_required_unshifted;
                NodeList newnames = the_node.getElementsByTagName("add-name");
                for (int j = 0; j < newnames.getLength(); j++) {
                    Element name = (Element) newnames.item(j);
                    String cmd_name = name.getAttribute("name");
                    command_t new_cmd = command_t.parse(cmd_name);
                    if (new_cmd == command_t.invalid)
                        new_cmd = command_t.parse("cmd_" + cmd_name);

                    if (new_cmd == command_t.invalid)
                        System.err.println("Warning: Function `" + name.getAttribute("name") + "' does not exist.");
                    else
                        b.commands.add(new_cmd);
                }
            }
        } else if (the_node.getTagName().equals("reserve-button")) {
            String fcn = the_node.getAttribute("function");
            b.unshifted = reserved_fcn;
            if (!the_node.getAttribute("unshifted-only").equals("yes")) {
                b.shifted = reserved_fcn;
                b.xshifted = reserved_fcn;
            }
        } else
            System.err.println("Unknown element: " + the_node.getTagName());
    }

    private void interpret_rules() {
        if (rules == null) {
            assign_buttons(true);
            return;
        }
        NodeList rls = ((Element) rules.getElementsByTagName("button_rules").item(0)).getChildNodes();
        for (int i = 0; i < rls.getLength(); i++) {
            if (rls.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) rls.item(i);
                String tagname = e.getTagName();
                if (debug > 0)
                    System.err.println("Rule element: " + tagname + ", button = " + e.getAttribute("button"));
                if (tagname.equals("assign-buttons")) {
                    assign_buttons(e.getAttribute("unshifted-only").equals("yes"));
                } else if (tagname.equals("names")) {
                    do_names(e);
                } else if (tagname.equals("reserve-button")) {
                    do_names(e);
                } else if (tagname.equals("dump-buttons")) {
                    dump_buttons();
                } else if (tagname.equals("dump-functions")) {
                    dump_functions();
                } else {
                    System.err.println("Not implemented: " + tagname);
                }
            }
        }
    }
/*
    public boolean remotemasteroutput(String filename) throws IOException {
        if (!valid)
            return false;
        
        com.hifiremote.jp1.DeviceUpgrade dev_upgrade = new com.hifiremote.jp1.DeviceUpgrade(new String[]{});
        dev_upgrade.setRemote(remote);
        Properties properties = new Properties();
        if (cpu_code != null)
            properties.setProperty("Code." + cpu, cpu_code); // FIXME: does not take effect
        properties.setProperty("ProtocolParms", "4 0 224");
        properties.setProperty("FixedData", "04 00 E0");
        //DevParms=Device 1\=0,Device 2\=0,Device 3\=0
        //DeviceTranslator=Translator(0) Translator(1,8,8) Translator(2,8,16)
        //properties.setProperty("CmdParms", "OBC");
        //properties.setProperty("CmdTranslator", "Translator(0)");
        //properties.setProperty("DefaultCmd", "00");
        //properties.setProperty("CmdIndex", "0");

        com.hifiremote.jp1.Protocol prot = new com.hifiremote.jp1.Protocol(this.jp1info.get_protocol_name(),//jp1_protocol_name,
            new com.hifiremote.jp1.Hex(new short[]{(short)(jp1info.get_protocol_number()>>8), (short)(jp1info.get_protocol_number() & 255)}), properties);
        //System.err.println(prot.needsCode(remote));
        //System.err.println(prot.getCode(remote));
        prot.setDeviceParms(new com.hifiremote.jp1.Value[]{new com.hifiremote.jp1.Value(dev_no), new com.hifiremote.jp1.Value(dev_no)});
        dev_upgrade.setSetupCode(setup_code);
        
        dev_upgrade.setParmValues(new com.hifiremote.jp1.Value[]{
            new com.hifiremote.jp1.Value(dev_no), subdev_no == -1 ? null : new com.hifiremote.jp1.Value(subdev_no), null});
        dev_upgrade.setDescription(dev.device_name);
        dev_upgrade.setNotes("Generated by " + harcutils.version_string);
        dev_upgrade.setDeviceTypeAliasName(remotemaster.jp1_devicetype(dev_type));
        dev_upgrade.setProtocol(prot);

        //System.err.println(dev_upgrade.getCode());

        List<com.hifiremote.jp1.Function> funcs = dev_upgrade.getFunctions();
        //for (fcn f : functions.values()) {
        for (int i = 0; i < function_array.length; i++) {
            fcn f = function_array[i];
            funcs.add(new com.hifiremote.jp1.Function(f.cmd.toString().replaceFirst("^cmd_", ""),
                    new com.hifiremote.jp1.Hex(new short[]{jp1info.obc2hex(f.obc)}), f.remark.isEmpty() ? null : f.remark));
        }

        for (button b : buttons.values()) {
            com.hifiremote.jp1.Button but = new com.hifiremote.jp1.Button("standard" + b.hardname, b.hardname, b.keycode, remote);
            define_button(dev_upgrade, but, b.get_unshifted_cmd(), com.hifiremote.jp1.Button.NORMAL_STATE);
            define_button(dev_upgrade, but, b.get_shifted_cmd(), com.hifiremote.jp1.Button.SHIFTED_STATE);
            if (has_xshift)
                define_button(dev_upgrade, but, b.get_xshifted_cmd(), com.hifiremote.jp1.Button.XSHIFTED_STATE);
        }
        dev_upgrade.store(new File(filename));
        return true;
    }*/

    /*private void define_button(com.hifiremote.jp1.DeviceUpgrade dev_upgrade, com.hifiremote.jp1.Button but, command_t c, int state) {
        if (c != null && c != command_t.invalid) {
            fcn f = functions.get(c);
            com.hifiremote.jp1.Function fn = new com.hifiremote.jp1.Function(f.cmd.toString().replaceFirst("^cmd_", ""),
                    new com.hifiremote.jp1.Hex(new short[]{jp1info.obc2hex(f.obc)}), null);
            dev_upgrade.setFunction(but, fn, state);
        }
    }*/

    public boolean generate_xml(String filename) throws FileNotFoundException {
        if (!valid)
            return false;

        resultdoc = harcutils.newDocument();
        Element root = resultdoc.createElement("br_export");
        resultdoc.appendChild(root);

        Element device = resultdoc.createElement("device");
        device.setAttribute("name", dev.device_name);
        device.setAttribute("id", dev.id);
        device.setAttribute("type", dev.get_type().toString());
        root.appendChild(device);

        Element remote_el = resultdoc.createElement("remote");
        remote_el.setAttribute("name", remote.get_name());
        remote_el.setAttribute("id", remote.get_id());
        root.appendChild(remote_el);


/*        Element DeviceIndex = resultdoc.createElement("DeviceIndex");

        com.hifiremote.jp1.DeviceType devtype = remote.getDeviceTypeByAliasName(remotemaster.jp1_devicetype(dev_type));
        int deviceindex = -1;
        for (int i = 0; i < remote.getDeviceTypes().length; i++) {
            if (remote.getDeviceTypeByIndex(i).equals(devtype))
                deviceindex = i;
        }

        DeviceIndex.appendChild(resultdoc.createTextNode(Integer.toString(deviceindex)));
        root.appendChild(DeviceIndex);
*/

        Element setupcode = resultdoc.createElement("setupcode");
        setupcode.setAttribute("code", Integer.toString(setup_code));
        root.appendChild(setupcode);

        Element protocol = resultdoc.createElement("protocol");
        if (jp1info != null) {
            protocol.setAttribute("number", String.format("%02x %02x", jp1info.get_protocol_number() >> 8, jp1info.get_protocol_number() % 256));
            protocol.setAttribute("name", jp1info.get_protocol_name());
        }
        protocol.setAttribute("deviceno", Short.toString(dev_no));
        if (subdev_no !=  -1)
            protocol.setAttribute("subdevice", Short.toString(subdev_no));
        root.appendChild(protocol);

        Element Notes = resultdoc.createElement("notes");
        Notes.appendChild(resultdoc.createTextNode("Created by " + harcutils.version_string));
        root.appendChild(Notes);

        // Use function_array instead of functions here, because I prefer to
        // keep the original ordering; although strictly speaking irrelevant,
        // there may be some login in the original authors ordering that I should not just throw away.
        for (int i = 0; i < function_array.length; i++) {
            fcn func = function_array[i];
            Element f = resultdoc.createElement("function");
            f.setAttribute("name", func.cmd.toString().replaceFirst("^cmd_", ""));
            f.setAttribute("index", "" + i);
            if (jp1info != null)
                f.setAttribute("hex", Integer.toHexString(jp1info.obc2hex(func.obc)));
            f.setAttribute("obc", Integer.toString(func.obc));
            if (!func.remark.isEmpty())
                f.setAttribute("remark", func.remark);
            if (func.one_button_name != null)
                f.setAttribute("button", "" + func.one_button_name);
            root.appendChild(f);
        }

        for (button but : buttons.values()) {
            Element b = resultdoc.createElement("button");
            b.setAttribute("name", but.hardname);
            b.setAttribute("keycode", Integer.toHexString(but.keycode));
            if (but.get_unshifted_cmd() != null && but.get_unshifted_cmd() != command_t.invalid)
                b.setAttribute("unshifted", but.get_unshifted_cmd().toString().replaceFirst("^cmd_", ""));
            if (but.get_shifted_cmd() != null && but.get_shifted_cmd() != command_t.invalid)
                b.setAttribute("shifted", but.get_shifted_cmd().toString().replaceFirst("^cmd_", ""));
            if (but.get_xshifted_cmd() != null && but.get_xshifted_cmd() != command_t.invalid)
                b.setAttribute("xshifted", but.get_xshifted_cmd().toString().replaceFirst("^cmd_", ""));
            root.appendChild(b);
        }
        
        harcutils.printDOM(filename, resultdoc, doctype);
        return true;
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("rmdu [<options>] <devicefile>");
        System.err.println("where options=-r <rulesfile>, -o <outfile>, -p <remotemaster_properties>, -t <devicetype>, -R remotename, -d");
        System.exit(1);
    }

    public static void main(String args[]) {
        //int debug = 0;
        String outfile = null;
        String button_remotename = "urc-778x";//URC-7781 Digital 12";// In JP1-i
        String remotename = null;
        String devicefile = null;
        String rulesfile = null;
        String rm_properties = "RemoteMaster.properties";
        device_type devtype = null;
        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug++;
                } else if (args[arg_i].equals("-o")) {
                    arg_i++;
                    outfile = args[arg_i++];
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    devtype = device_type.valueOf(args[arg_i++]);
                } else if (args[arg_i].equals("-p")) {
                    arg_i++;
                    rm_properties = args[arg_i++];
                } else if (args[arg_i].equals("-r")) {
                    arg_i++;
                    rulesfile = args[arg_i++];
                } else if (args[arg_i].equals("-R")) {
                    arg_i++;
                    remotename = args[arg_i++];
                } else
                    usage();
            }
            devicefile = args[arg_i++];
        } catch (Exception e) {
            usage();
        }

        try {
            //remotemaster.init("digitmaps.bin", "protocols.ini", rm_properties);
            br_export r = new br_export(button_remotename, devicefile, rulesfile, remotename, devtype);
            r.generate_xml(outfile);
        } catch (IOException e) {
            System.err.println("IO error " + e.getMessage());
        } catch (SAXParseException e) {
            System.err.println("SAXParse " + e.getMessage());
        } catch (SAXException e) {
            System.err.println("SAX " + e.getMessage());
        }
    }
}
