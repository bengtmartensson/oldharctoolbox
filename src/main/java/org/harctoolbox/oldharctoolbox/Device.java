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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.ircore.XmlUtils;
import org.harctoolbox.irp.IrpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



// NOTE: "attributes" are properties that distinguish different instances of the
// device from one another, for example a regional code. It is not implemented
// very much of yet. Do not comfuse with attributes in XML sense.

public final class Device {

    public static final String doctype_systemid_filename = "devices.dtd";
    public static final String doctype_publicid = "-//bengt-martensson.de//devices//en";
    private static int debug = 0;
    private static HashMap<String, Device> device_storage = new HashMap<String, Device>(16);

    public static Device new_device(String devicename, HashMap<String, String>attributes)
            throws IOException, SAXParseException, SAXException {
        String dwa = (new device_with_attributes(devicename, attributes)).toString();
        Device d = device_storage.get(dwa);
        if (d == null) {
            d = new Device(devicename, attributes);
            device_storage.put(dwa, d);
        }
        return d;
    }

    public static void flush_storage() {
        device_storage.clear();
    }

    public static String[] devices2remotes(String[] devices) throws IOException, SAXParseException, SAXException {
        ArrayList<String> v = new ArrayList<>(8);
        for (String device : devices) {
            String[] remotes = (new Device(device)).get_remotenames();
            v.addAll(Arrays.asList(remotes));
        }
        return v.toArray(new String[v.size()]);
    }

    private static Element find_thing_el(Document doc, String thing_tagname,
            String thing_name) {
        if (doc == null)
            return null;
        Element root = doc.getDocumentElement();
        Element el = null;
        if (root.getTagName().equals(thing_tagname))
            el = root;
        else {
            NodeList things = root.getElementsByTagName(thing_tagname);
            if (thing_name == null)
                el = (Element) things.item(0);
            else
                for (int i = 0; i < things.getLength(); i++) {
                    if (((Element) things.item(i)).getAttribute("id").equals(thing_name))
                        el = (Element) things.item(i);
                }
        }
        return el;
    }

    private static Element find_device_el(Document doc, String dev_name) {
        return find_thing_el(doc, "device", dev_name);
    }

    private static Element find_commandgroup_el(Document doc, String id) {
        return find_thing_el(doc, "commandgroup", id);
    }

    /**
     * Returns the names of the devices available to us. For this, just look at the file names
     * in the device directory. This is not absolutely fool proof, in particular
     * on systems with case insensitive file system.
     *
     * @return Array of strings of the device names.
     */
    public static String[] get_devices() {
        return get_basenames(Main.getProperties().getDevicesDir(), HarcUtils.devicefile_extension, false);
    }

    private static String[] get_basenames(String dirname, String extension, boolean toLowercase) {
        File dir = new File(dirname);
        if (!dir.isDirectory())
            return null;

        if (extension.charAt(0) != '.')
            extension = "." + extension;
        String[] files = dir.list(new extension_filter(extension));
        String[] result = new String[files.length];
        for (int i =0; i < files.length; i++)
            result[i] = toLowercase ? files[i].toLowerCase().substring(0, files[i].lastIndexOf(extension))
                    : files[i].substring(0, files[i].lastIndexOf(extension));
        return result;
    }

    public static boolean export_device(String export_dir, String devname) {
        String out_filename = export_dir + File.separator + devname + HarcUtils.devicefile_extension;
        System.err.println("Exporting " + devname + " to " + out_filename + ".");
        Device dev = null;
        try {
            dev = new Device(devname, null, true);
        } catch (IOException e) {
            System.err.println("IOException with " + devname);
            return false;
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
            return false;
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            return false;
        }

        if (!dev.is_valid()) {
            System.err.println("Failure!");
            return false;
        }

        if (debug != 0)
            System.out.println(dev.info());

        dev.augment_dom(false);
        dev.print(out_filename);

        return true;
    }

    public static boolean export_all_devices(String export_dir) {
        File dir = new File(export_dir);
        if (dir.isFile()) {
            System.err.println("Error: File " + export_dir + " exists, but is not a directory.");
            return false;
        } else if (!dir.exists()) {
            System.err.println("Directory " + export_dir + " does not exist, creating.");
            boolean stat = dir.mkdir();
            if (!stat) {
                System.err.println("Directory creation failed.");
                return false;
            }
        }
        String[] devs = get_devices();
        // Ignore errors, just continue
        for (String dev : devs)
            export_device(export_dir, dev);

        return true;
    }

    private static void usage(int exitcode) {
        System.err.println("Usage:");
        System.err.println("device [<options>] -f <input_filename> [<cmd_name>]");
        System.err.println("or");
        System.err.println("device -x <export_directory>");
        System.err.println("where options=-o <filename>,-@ <attributename>,-a aliasname,-l,-d,-c,-t "
                + CommandType_t.valid_types('|'));
        if (exitcode >= 0)
            System.exit(exitcode);
    }

    private static void usage() {
        usage(IrpUtils.EXIT_USAGE_ERROR);
    }

    public static void main(String args[]) {
        CommandType_t type = CommandType_t.any;
        boolean get_code = false;
        boolean list_commands = false;
        String in_filename = null;
        String out_filename = null;
        String attribute_name = null;
        String alias_name = null;
        String export_dir = null;

        short deviceno = -1;
        short subdevice = 0;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) // ???
                    && args[arg_i].charAt(0) == '-') {

                switch (args[arg_i]) {
                    case "-@":
                        arg_i++;
                        attribute_name = args[arg_i++];
                        break;
                    case "-D":
                        arg_i++;
                        deviceno = Short.parseShort(args[arg_i++]);
                        break;
                    case "-S":
                        arg_i++;
                        subdevice = Short.parseShort(args[arg_i++]);
                        break;
                    case "-a":
                        arg_i++;
                        alias_name = args[arg_i++];
                        break;
                    case "-c":
                        arg_i++;
                        get_code = true;
                        break;
                    case "-d":
                        arg_i++;
                        debug++;
                        break;
                    case "-l":
                        arg_i++;
                        list_commands = true;
                        break;
                    case "-f":
                        arg_i++;
                        in_filename = args[arg_i++];
                        break;
                    case "-o":
                        arg_i++;
                        out_filename = args[arg_i++];
                        break;
                    case "-t":
                        arg_i++;
                        String typename = args[arg_i++];
                        if (!CommandType_t.is_valid(typename))
                            usage();
                        type = CommandType_t.valueOf(typename);
                        break;
                    case "-x":
                        arg_i++;
                        export_dir = args[arg_i++];
                        break;
                    default:
                        usage(IrpUtils.EXIT_USAGE_ERROR);
                        break;
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }

        if (export_dir != null) {
            boolean success = export_all_devices(export_dir);
            System.exit(success ? IrpUtils.EXIT_SUCCESS : IrpUtils.EXIT_CONFIG_WRITE_ERROR);
        } else if (in_filename == null) {
            usage(-1);
            HarcUtils.printtable("Known devices:", get_devices());
            System.exit(IrpUtils.EXIT_SUCCESS);
        } else if ((args.length != arg_i) && (args.length != arg_i + 1))
            usage();

        Device dev = null;
        try {
            dev = new Device(in_filename, null, true);
        } catch (IOException e) {
            System.err.println("IOException with " + in_filename);
            System.exit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            System.err.println(e.getMessage());
        }

        if (!dev.is_valid()) {
            System.err.println("Failure!");
            System.exit(2);
        }

        if (debug != 0)
            System.out.println(dev.info());

        if (out_filename != null) {
            dev.augment_dom(false);
            dev.print(out_filename);
        }

        if (list_commands) {
            command_t[] cmds = dev.get_commands(type);
            for (command_t cmd : cmds) {
                System.out.println(cmd);
            }
        }

        if (attribute_name != null)
            System.out.println("@" + attribute_name + "=" + dev.get_attribute(attribute_name));

        if (alias_name != null)
            System.out.println("alias: " + alias_name + "->" + dev.get_alias(alias_name));
        if (args.length == arg_i + 1) {
            String cmdname = args[arg_i];
            Command c = dev.get_command(command_t.parse(cmdname), type);
            if (c == null)
                System.out.println("No such command with specified type");
            else {
                System.out.println(c.toString());
                if (get_code) {
                    IrSignal ircode = (deviceno == -1) ? c.get_ir_code(ToggleType.toggle_0, true)
                            : c.get_ir_code(ToggleType.toggle_0, true, deviceno, subdevice);
                    if (ircode == null) {
                        System.err.println("No such IR command: " + cmdname);
                        System.exit(2);
                    } else {
                        System.out.println(ircode);
                        if (c.get_toggle())
                            System.out.println(c.get_ir_code(ToggleType.toggle_1, true));
                    }
                }
            }
        }
    }

    private String vendor;
    private String device_name;
    private String id;
    private String model;
    private DeviceType type;
    private Document doc = null;
    private Element device_el = null;
    private CommandSet[] commandsets;
    private HashMap<String, String> attributes;
    private String[][] aliases;
    private int no_aliases = 0;
    private int jp1_setupcode = -1;
    private boolean pingable_on;
    private boolean pingable_standby;

    private Device(String filename, HashMap<String, String>attributes, boolean barf_for_invalid)
            throws IOException, SAXParseException, SAXException {
        this( (filename.contains(File.separator) ? "" : Main.getProperties().getDevicesDir()+ File.separator)
                + filename
                + ((filename.endsWith(HarcUtils.devicefile_extension)) ? "" : HarcUtils.devicefile_extension),
                null, attributes, barf_for_invalid);
    }

    private Device(String filename, String devicename, HashMap<String, String> attributes, boolean barf_for_invalid)
            throws IOException, SAXParseException, SAXException {
        this(XmlUtils.openXmlFile(new File(filename)), devicename, attributes, barf_for_invalid);
    }

    private Device(Document doc, HashMap<String, String>attributes, boolean barf_for_invalid) {
        this(doc, (String) null, attributes, barf_for_invalid);
    }

    //public device(Document doc, String dev_name, boolean barf_for_invalid) {
    //    this(doc, find_device_el(doc, dev_name), barf_for_invalid);
    //}

    /**
     *
     * @param name Either file name or device name.
     * @param attributes
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXParseException
     */
    public Device(String name, HashMap<String, String>attributes) throws IOException, SAXParseException, SAXException {
        this(name, attributes, true);
    }

    public Device(String name) throws IOException, SAXParseException, SAXException {
        this(name, null);
    }

    private Device(Document doc, String dev_name, HashMap<String, String>instance_attributes, boolean barf_for_invalid) {
        this.doc = doc;
        if (doc == null)
            return;

        Element device_el = find_device_el(doc, dev_name);
        this.device_el = device_el;
        device_name = device_el.getAttribute("name");
        vendor = device_el.getAttribute("vendor");
        id = device_el.getAttribute("id");
        model = device_el.getAttribute("model");
        type = DeviceType.valueOf(device_el.getAttribute("type"));
        pingable_on = device_el.getAttribute("pingable_on").equals("yes"); // i.e. default false
        pingable_standby = device_el.getAttribute("pingable_standby").equals("yes"); // i.e. default false

        NodeList nl = device_el.getElementsByTagName("jp1data");
        if (nl.getLength() > 0) {
            nl = ((Element)nl.item(0)).getElementsByTagName("setupcode");
            jp1_setupcode = Integer.parseInt(((Element)nl.item(0)).getAttribute("value"));
        }

        // First read the attributes of the device file, considered as defaults...
        NodeList attributes_nodes = device_el.getElementsByTagName("attribute");
        int no_attributes = attributes_nodes.getLength();
        attributes = new HashMap<>(no_attributes);
        for (int i = 0; i < no_attributes; i++) {
            Element attr = (Element) attributes_nodes.item(i);
            String val =
                    attributes.put(
                            attr.getAttribute("name"),
                            attr.getAttribute("defaultvalue").isEmpty() ? "no" : attr.getAttribute("defaultvalue"));
        }
        // ... then, to the extent applicable, overwrite with actual instance values
        // if instance_attributes == null I am exporting, set everything to true.
        if (instance_attributes == null) {
            attributes.clear();
            //for (String attributeName : attributes.keySet())
            //    attributes.put(attributeName, "export");
        } else {
            instance_attributes.entrySet().forEach((kvp) -> {
                if (attributes.containsKey(kvp.getKey()))
                    attributes.put(kvp.getValue(), instance_attributes.get(kvp.getKey()));
                else
                    System.err.println("WARNING: Attribute named `" + kvp.getKey() + "' does not exist in device `" + device_name + "', ignored.");
            });
        }

        NodeList aliases_nodes = device_el.getElementsByTagName("alias");
        no_aliases = aliases_nodes.getLength();
        aliases = new String[no_aliases][2];
        for (int i = 0; i < no_aliases; i++) {
            Element ali = (Element) aliases_nodes.item(i);
            aliases[i][0] = ali.getAttribute("name");
            aliases[i][1] = ali.getAttribute("command");
        }

        NodeList commandsets_nodes = device_el.getElementsByTagName("commandset");
        int no_commandsets = commandsets_nodes.getLength();
        commandsets = new CommandSet[no_commandsets];
        // TODO: evaluate ifattribute for commandsets
        for (int i = 0; i < no_commandsets; i++) {
            Element cs = (Element) commandsets_nodes.item(i);
            //if (cs.getAttribute("toggle") != null && cs.getAttribute("toggle").equals("yes"))
            //    has_toggle = true;

            // This does not work for hierarchical commandgrouprefs
            NodeList cgrs = cs.getElementsByTagName("commandgroupref");
            for (int j = 0; j < cgrs.getLength(); j++) {
                Element cgr = (Element) cgrs.item(j);
                Element cg = find_commandgroup_el(doc, cgr.getAttribute("commandgroup"));
                Element par = (Element) cgr.getParentNode();
                par.replaceChild(cg.cloneNode(true), cgr);
            }

            NodeList cmd_nodes = cs.getElementsByTagName("command");
            int no_valids = 0;
            for (int j = 0; j < cmd_nodes.getLength(); j++) {
                Element e = (Element) cmd_nodes.item(j);
                if (command_t.is_valid(e.getAttribute("cmdref"))
                        && evaluate_ifattribute(e))
                    no_valids++;
            }
            CommandSetEntry[] cmds = new CommandSetEntry[no_valids];
            int pos = 0;
            for (int j = 0; j < cmd_nodes.getLength(); j++) {
                Element cmd_el = (Element) cmd_nodes.item(j);
                String commandname = cmd_el.getAttribute("cmdref");
                String ifattr = cmd_el.getAttribute("ifattribute");
                if (ifattr.startsWith("!"))
                    ifattr = ifattr.substring(1).trim();
                if (!ifattr.isEmpty() && !attributes.isEmpty() && !attributes.containsKey(ifattr))
                    System.err.println("WARNING: command " + commandname + " has undeclared attribute " + ifattr);
                if (!command_t.is_valid(commandname)) {
                    if (barf_for_invalid)
                        System.err.println("Warning: Command " + commandname + " is invalid.");
                } else if (evaluate_ifattribute(cmd_el)) {
                    NodeList al = cmd_el.getElementsByTagName("argument");
                    String[] arguments = new String[al.getLength()];
                    for (int a = 0; a < arguments.length; a++) {
                        arguments[a] = ((Element) al.item(a)).getAttribute("name");
                    }
                    String ccf_toggle_0 = null;
                    String ccf_toggle_1 = null;
                    NodeList ccfs = cmd_el.getElementsByTagName("ccf");
                    for (int k = 0; k < ccfs.getLength(); k++) {
                        Element ccf = (Element)ccfs.item(k);
                        if (ccf.getAttribute("toggle").equals("1"))
                            ccf_toggle_1 = ccf.getTextContent();
                        else
                            ccf_toggle_0 = ccf.getTextContent();
                    }
                    cmds[pos++] = new CommandSetEntry(cmd_el.getAttribute("cmdref"),
                            cmd_el.getAttribute("cmdno"),
                            cmd_el.getAttribute("name"),
                            cmd_el.getAttribute("transmit"),
                            cmd_el.getAttribute("response_lines"),
                            cmd_el.getAttribute("response_ending"),
                            cmd_el.getAttribute("expected_response"),
                            cmd_el.getAttribute("remark"),
                            arguments,
                            ccf_toggle_0,
                            ccf_toggle_1);
                }
            }
            /*
            NodeList cmdgrouprefs_nodes = cs.getElementsByTagName("commandgroupref");
            for (int k = 0; k < cmdgrouprefs_nodes.length; k++) {
            Element cmdgroupref = (Element)cmdgrouprefs_nodes.item[k];
            Element cmdgroup = find_commandgroup_el(doc, cmdgroupref.getAttribute("commandgroup"));
            NodeList cmds = cmdgroup

            }
            */
            commandsets[i] = new CommandSet(cmds,
                    cs.getAttribute("type"),
                    cs.getAttribute("protocol"),
                    cs.getAttribute("deviceno"),
                    cs.getAttribute("subdevice"),
                    cs.getAttribute("toggle"),
                    cs.getAttribute("additional_parameters"),
                    cs.getAttribute("name"),
                    cs.getAttribute("remotename"),
                    cs.getAttribute("pseudo_power_on"),
                    cs.getAttribute("prefix"),
                    cs.getAttribute("suffix"),
                    cs.getAttribute("delay_between_reps"),
                    cs.getAttribute("open"),
                    cs.getAttribute("close"),
                    cs.getAttribute("portnumber"),
                    cs.getAttribute("charset"),
                    cs.getAttribute("flavor"));
        }
    }

    public command_t[] get_commands(CommandType_t type) {
        return get_commands(type, null);
    }

    public command_t[] get_commands() {
        return get_commands(CommandType_t.any);
    }

    public command_t[] get_commands(CommandType_t cmdtype, String remotename) {
        if (commandsets == null)
            return null;
        int len = 0;
        for (CommandSet commandset : commandsets) {
            if ((commandset.get_remotename().equals(remotename) || remotename == null) && (cmdtype == CommandType_t.any || cmdtype == commandset.get_type()))
                len += commandset.get_no_commands();
        }
        command_t[] cmds = new command_t[len];
        int index = 0;
        for (CommandSet commandset : commandsets) {
            if ((commandset.get_remotename().equals(remotename) || remotename == null) && (cmdtype == CommandType_t.any || cmdtype == commandset.get_type()))
                for (int j = 0; j < commandset.get_no_commands(); j++)
                    cmds[index++] = commandset.get_entry(j).get_cmd();
        }
        return cmds;
    }

    public /*??*/ CommandSet get_commandset(command_t cmd, CommandType_t cmdtype) {
        for (CommandSet commandset : commandsets)
            if (commandset.get_type() == cmdtype && commandset.get_command(cmd, cmdtype) != null)
                return commandset;

        return null;
    }

    public int get_portnumber(command_t cmd, CommandType_t cmdtype) {
        CommandSet cs = get_commandset(cmd, cmdtype);
        return cs != null ? cs.get_portnumber() : (int) IrCoreUtils.INVALID;
    }

    public String get_open(command_t cmd, CommandType_t cmdtype) {
        CommandSet cs = get_commandset(cmd, cmdtype);
        return cs != null ? cs.get_open() : null;
    }

    public String get_close(command_t cmd, CommandType_t cmdtype) {
        CommandSet cs = get_commandset(cmd, cmdtype);
        return cs != null ? cs.get_close() : null;
    }

    /*public command_t[] get_commands(String remotename) {
        int len = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].getremotename().equals(remotename))
                len += commandsets[i].getno_commands();
        }
        command_t[] cmds = new command_t[len];
        int index = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].getremotename().equals(remotename))
                for (int j = 0; j < commandsets[i].getno_commands(); j++)
                    cmds[index++] = commandsets[i].getentry(j).getcmd();
        }
        return cmds;
    }*/

    public CommandSet[] get_commandsets(String remotename, CommandType_t type) {
        ArrayList<CommandSet> vect = new ArrayList<>(8);
        //int len = 0;
        //for (int i = 0; i < commandsets.length; i++) {
        //    if (commandsets[i].get_remotename().equals(remotename))
        //        len++;
        //}
        //commandset[] cmds = new commandset[len];
        //int index = 0;
        for (CommandSet commandset : commandsets) {
            if (commandset.get_remotename().equals(remotename) && (type == CommandType_t.any || type == commandset.get_type()))
                vect.add(commandset);
        }
        return vect.toArray(new CommandSet[vect.size()]);
    }

    public CommandSet[] get_commandsets(CommandType_t type) {
        int len = 0;
        for (CommandSet commandset : commandsets) {
            if (commandset.get_type() == type)
                len++;
        }
        CommandSet[] cmds = new CommandSet[len];
        int index = 0;
        for (CommandSet commandset : commandsets) {
            if (commandset.get_type() == type)
                cmds[index++] = commandset;
        }
        return cmds;
    }

    public String[] get_protocols(String remotename) {
        String[] work = new String[commandsets.length];
        for (int i = 0; i < commandsets.length; i++)
            if (commandsets[i].get_remotename().equals(remotename))
                work[i] = commandsets[i].get_protocol();

        return HarcUtils.sort_unique(HarcUtils.nonnulls(work));
    }

    public ArrayList<CommandType_t> get_commandtypes(command_t cmd) {
        ArrayList<CommandType_t> v = new ArrayList<>(8);
        for (CommandType_t t : CommandType_t.values())
            if (t != CommandType_t.any && get_command(cmd, t) != null)
                v.add(t);

        return v;
    }

    public Command get_command(command_t cmd) {
        return get_command(cmd, CommandType_t.any);
    }

    public Command get_command(command_t cmd, CommandType_t type) {
        for (CommandSet commandset : commandsets) {
            Command c = commandset.get_command(cmd, type);
            if (c != null)
                return c;
        }

        // Not found...
        if (cmd == command_t.power_on) {
            for (CommandSet commandset : commandsets) {
                String ppo = commandset.get_pseudo_power_on();
                if (!ppo.isEmpty()) {
                    command_t ppo_cmd = command_t.parse(ppo);
                    Command c = commandset.get_command(ppo_cmd, type);
                    if (c != null)
                        return c;
                }
            }
        }
        return null;
    }

    public Command get_command(command_t cmd, CommandType_t type, String remote) {
        for (CommandSet commandset : commandsets) {
            if (commandset.get_remotename().equals(remote)) {
                Command c = commandset.get_command(cmd, type);
                if (c != null)
                    return c;
            }
        }

        // Not found...
        if (cmd == command_t.power_on) {
            for (CommandSet commandset : commandsets) {
                if (commandset.get_remotename().equals(remote)) {
                    String ppo = commandset.get_pseudo_power_on();
                    if (!ppo.isEmpty()) {
                        command_t ppo_cmd = command_t.parse(ppo);
                        Command c = commandset.get_command(ppo_cmd, type);
                        if (c != null)
                            return c;
                    }
                }
            }
        }
        return null;
    }

    // This is not very smart... here for LIRC compatibility
    /*public int get_gap() {
        int max = 0;
        for (int cs = 0; cs < commandsets.length; cs++) {
            if (commandsets[cs].is_type_ir()) {
                for (int i = 0; i < commandsets[cs].get_no_commands(); i++) {
                    ir_code irc = get_command_by_index(cs, i).get_ir_code(toggletype.toggle_0, false);
                    int gap = irc == null ? 0 : irc.get_gap();
                    if (gap > max)
                        max = gap;
                }
            }
        }
        return max;
    }*/

    public int get_frequency() {
        for (int cs = 0; cs < commandsets.length; cs++) {
            if (commandsets[cs].is_type_ir()) {
                //for (int i = 0; i < commandsets[cs].get_no_commands(); i++) {
                    IrSignal irc = get_command_by_index(cs, 0).get_ir_code(ToggleType.toggle_0, false);
                    //System.out.println(irc.);
                    return irc == null ? 0 : irc.getFrequency().intValue();
                    //if (gap > max)
                      //  max = gap;
                //}
            }
        }
        return 0;
    }

    private Command get_command_by_index(int commandset, int commandindex) {
        return new Command(commandsets[commandset], commandindex);
    }

    public IrSignal get_code(command_t cmd, CommandType_t type, ToggleType toggle, boolean verbose) {
        Command c = get_command(cmd, type);
        return (c != null) ? c.get_ir_code(toggle, verbose) : null;
    }

    public IrSignal get_code(command_t cmd, CommandType_t type, ToggleType toggle, boolean verbose, String house, short deviceno) {
        Command c = get_command(cmd, type);
        if (c == null)
            return null;

        return (house != null && !house.isEmpty())
                ? c.get_ir_code(toggle, verbose, (short)((int)house.charAt(0)-(short)'A'), deviceno)
        : c.get_ir_code(toggle, verbose);
    }

    public IrSignal get_ir_code(command_t cmd, ToggleType toggle, boolean verbose) {
        return get_code(cmd, CommandType_t.ir, toggle, verbose);
    }

    public String[] get_remotenames() {
        String[] work = new String[commandsets.length];
        for (int i = 0; i < commandsets.length; i++)
            work[i] = commandsets[i].get_remotename();

        return HarcUtils.nonnulls(HarcUtils.sort_unique(work));
    }

    public int get_delay(String type) {
        Element el = find_thing_el(doc, "delays", null);
        if (el == null)
            return -1;
        NodeList nl = el.getElementsByTagName("delay");
        for (int i = 0; i < nl.getLength(); i++) {
            Element e = (Element) nl.item(i);
            if (e.getAttribute("type").equals(type))
                return Integer.parseInt(e.getAttribute("delay"));
        }
        System.err.println("Delay type \"" + type + "\" not found.");
        return -1;
    }

    /*
     public String getirremotename(String cmdname) {
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].type_ir() && (commandsets[i].get_command(cmdname, commandtype_t.ir) != null))
                return commandsets[i].getremotename();
        }
        return "*not found*";
    }*/

    public boolean get_pingable_on() {
        return pingable_on;
    }

    public boolean get_pingable_standby() {
        return pingable_standby;
    }

    public String get_attribute(String attribute_name) {
        return attributes.get(attribute_name);
    }

    public String get_alias(String alias_name) {
        String result = null;
        for (int i = 0; i < no_aliases && result == null; i++) {
            if (aliases[i][0].equals(alias_name))
                result = aliases[i][1];
        }
        return result;
    }

    public String get_name() {
        return this.device_name;
    }

    public DeviceType get_type() {
        return type;
    }

    public String get_alias(command_t cmd) {
        return get_alias(cmd.toString());
    }

    public String info() {
        StringBuilder infostr = new StringBuilder(
                "id = " + id + "\n" +
                "name = " + device_name + "\n" +
                "vendor = " + vendor + "\n" +
                "model = " + model + "\n" +
                "type = " + type + "\n");

        attributes.keySet().forEach((s) -> {
            infostr.append("@").append(s).append("=").append(attributes.get(s)).append("\n");
        });

        for (int i = 0; i < no_aliases; i++)
            infostr.append("alias: ").append(aliases[i][0]).append("->").append(aliases[i][1]).append("\n");

        infostr.append("Remotenames: ").append(String.join(",", get_remotenames())).append("\n");

        infostr.append("# commandsets = ").append(commandsets.length);

        for (CommandSet commandset : commandsets)
            infostr.append("\n").append(commandset.get_info());
        return infostr.toString();
    }

    public boolean is_valid() {
        return doc != null;
    }

    public int get_jp1_setupcode() {
        return this.jp1_setupcode;
    }

    // TODO: Implement conjunctions, disjunctions, equalitytest.
    private boolean evaluate_ifattribute(String expr) {
        String s = expr.trim();
        if (s.isEmpty())
            return true;

        boolean positive = true;
        if (s.startsWith("!")) {
            positive = false;
            s = s.substring(1).trim();
        }

        //System.err.println(expr);
        if (!attributes.containsKey(s))
            return true;

        boolean hit = attributes.get(s).equalsIgnoreCase("yes");
        return positive ? hit : ! hit;
    }

    private boolean evaluate_ifattribute(Element e) {
        return evaluate_ifattribute(e.getAttribute("ifattribute"));
    }

    private void print(String filename) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, Main.getProperties().getDtdDir()+ File.separatorChar + doctype_systemid_filename);
            tr.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype_publicid);
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            if (filename.equals("-"))
                tr.transform(new DOMSource(doc), new StreamResult(System.out));
            else {
                FileOutputStream fos = new FileOutputStream(filename, false);
                tr.transform(new DOMSource(doc), new StreamResult(fos));
                System.err.println("File " + filename + " written.");
            }
        } catch (TransformerConfigurationException e) {
        } catch (TransformerException e) {
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private boolean augment_dom(boolean verbose) {
        NodeList commandsets_nodes = device_el.getElementsByTagName("commandset");
        for (int i = 0; i < commandsets_nodes.getLength(); i++) {
            Element cs = (Element) commandsets_nodes.item(i);
            if (cs.getAttribute("type").equals("ir") || cs.getAttribute("type").equals("rf433") || cs.getAttribute("type").equals("rf868")) {
                NodeList cmd_nodes = cs.getElementsByTagName("command");
                boolean has_toggle = cs.getAttribute("toggle").equals("yes");
                String protocol = cs.getAttribute("protocol");
                //jp1protocoldata jp1data = protocol_parser.get_jp1data(protocol);
                // This weird stuff (using cmd_index instead of j)
                // counteracts for the fact that commands with invalid names.
                // (such not in command_t) are not sorted in.
                // A better fix would be to fix this.
                int cmd_index = 0;
                for (int j = 0; j < cmd_nodes.getLength(); j++) {
                    Element cmd_el = (Element) cmd_nodes.item(j);
                    System.err.println(cmd_el.getAttribute("cmdref"));
                    if (!command_t.is_valid(cmd_el.getAttribute("cmdref"))) {
                        System.err.println("Ignoring invalid command " + cmd_el.getAttribute("cmdref"));
                        continue;
                    }

                    if (cmd_el.getElementsByTagName("ccf").getLength() > 0) {
                        System.err.println("Already ccf information in " + cmd_el.getAttribute("cmdref") + ", ignoring.");
                        cmd_index++;
                        continue;
                    }

                    //short obc = get_command_by_index(i, cmd_index).get_commandno();
                    IrSignal ir = get_command_by_index(i, cmd_index++/*j*/).get_ir_code(ToggleType.dont_care, verbose);
                    if (ir == null) {
                        //if (verbose)
                            System.err.println("No IR-code for command " + get_command_by_index(i, j).get_cmd());
                        return false;
                    }
                    //String cooked_ccf = ir.cooked_ccf_string();
                    //if (cooked_ccf != null) {
                    //    Element cooked = doc.createElement("ccf_cooked");
                    //    cooked.appendChild(doc.createTextNode(cooked_ccf));
                    //    cmd_el.appendChild(cooked);
                    //}

//                    try {
                        Element code;

                    if (has_toggle) {
                        code = doc.createElement("toggle_pair");

                        Element c = doc.createElement("ccf");
                        c.setAttribute("toggle", "0");
                        code.appendChild(c);
                        ir = get_command_by_index(i, j).get_ir_code(ToggleType.toggle_0, verbose);
                        c.appendChild(doc.createTextNode(Pronto.toString(ir)));

                        c = doc.createElement("ccf");
                        c.setAttribute("toggle", "1");
                        code.appendChild(c);
                        ir = get_command_by_index(i, j).get_ir_code(ToggleType.toggle_1, verbose);
                        c.appendChild(doc.createTextNode(Pronto.toString(ir)));
                    } else {
                        code = doc.createElement("ccf");
                        //ir_code ir = get_command_by_index(i, j).get_ir_code(toggletype.no_toggle, verbose);
                        //code.appendChild(doc.createTextNode(ir.ccf_string()));
                        code.appendChild(doc.createTextNode(Pronto.toString(ir)));
                    }
                    cmd_el.appendChild(code);
//                    } catch (IrpException e) {
//                        System.err.println(e.getMessage());
//                    }
                    //if (jp1data != null) {
                    //    short hex = jp1data.obc2hex(obc);
                    //    cmd_el.setAttribute("hex", String.format("%02x", hex));
                    //    cmd_el.setAttribute("efc", String.format("%03d", jp1protocoldata.hex2efc(hex)));
                    //}
                }
            }
        }
        return true;
    }

    private static class device_with_attributes {
        String device_classname;
        HashMap<String, String> attributes;
        device_with_attributes(String device_classname, HashMap<String, String>attributes) {
            this.device_classname = device_classname;
            this.attributes = attributes;
        }
        @Override
        public String toString() {
            return device_classname + attributes.toString(); // May not be portable, i.e. work with all implementations
        }
    }
    private static class extension_filter implements FilenameFilter {

        protected String extension;

        extension_filter(String extension) {
            this.extension = extension;
        }

        @Override
        public boolean accept(File directory, String name) {
            return name.toLowerCase().endsWith(extension.toLowerCase());
        }
    }
}
