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

package org.harctoolbox;

import IrpMaster.IrSignal;
import IrpMaster.IrpMasterException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;



// NOTE: "attributes" are properties that distinguish different instances of the
// device from one another, for example a regional code. It is not implemented
// very much of yet. Do not comfuse with attributes in XML sense.

public class device {

    public static final String doctype_systemid_filename = "devices.dtd";
    public static final String doctype_publicid = "-//bengt-martensson.de//devices//en";
    public String vendor;
    public String device_name;
    public String id;
    public String model;
    private device_type type;
    private Document doc = null;
    private Element device_el = null;
    private commandset[] commandsets;
    private HashMap<String, String> attributes;
    private String[][] aliases;
    private int no_aliases = 0;
    private static int debug = 0;
    private int jp1_setupcode = -1;
    private boolean pingable_on;
    private boolean pingable_standby;

    private static class device_with_attributes {
        String device_classname;
        HashMap<String, String> attributes;
        public device_with_attributes(String device_classname, HashMap<String, String>attributes) {
            this.device_classname = device_classname;
            this.attributes = attributes;
        }
        @Override
        public String toString() {
            return device_classname + attributes.toString(); // May not be portable, i.e. work with all implementations
        }
    }

    private static HashMap<String, device> device_storage = new HashMap<String, device>();

    public static device new_device(String devicename, HashMap<String, String>attributes)
            throws IOException, SAXParseException, SAXException {
        String dwa = (new device_with_attributes(devicename, attributes)).toString();
        device d = device_storage.get(dwa);
        if (d == null) {
            d = new device(devicename, attributes);
            device_storage.put(dwa, d);
        }
        return d;
    }

    public static void flush_storage() {
        device_storage.clear();
    }

    public command_t[] get_commands(commandtype_t type) {
        return get_commands(type, null);
    }

    public command_t[] get_commands() {
        return get_commands(commandtype_t.any);
    }

    public command_t[] get_commands(commandtype_t cmdtype, String remotename) {
        if (commandsets == null)
            return null;
        int len = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if ((commandsets[i].get_remotename().equals(remotename) || remotename == null)
                    && (cmdtype == commandtype_t.any || cmdtype == commandsets[i].get_type()))
                len += commandsets[i].get_no_commands();
        }
        command_t[] cmds = new command_t[len];
        int index = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if ((commandsets[i].get_remotename().equals(remotename) || remotename == null)
                    && (cmdtype == commandtype_t.any || cmdtype == commandsets[i].get_type()))
                for (int j = 0; j < commandsets[i].get_no_commands(); j++)
                    cmds[index++] = commandsets[i].get_entry(j).get_cmd();
        }
        return cmds;
    }

    public /*??*/ commandset get_commandset(command_t cmd, commandtype_t cmdtype) {
        for (int i = 0; i < commandsets.length; i++)
            if (commandsets[i].get_type() == cmdtype && commandsets[i].get_command(cmd, cmdtype) != null)
                return commandsets[i];

        return null;
    }

    public int get_portnumber(command_t cmd, commandtype_t cmdtype) {
        commandset cs = get_commandset(cmd, cmdtype);
        return cs != null ? cs.get_portnumber() : harcutils.portnumber_invalid;
    }

    public String get_open(command_t cmd, commandtype_t cmdtype) {
        commandset cs = get_commandset(cmd, cmdtype);
        return cs != null ? cs.get_open() : null;
    }

    public String get_close(command_t cmd, commandtype_t cmdtype) {
        commandset cs = get_commandset(cmd, cmdtype);
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

    public commandset[] get_commandsets(String remotename, commandtype_t type) {
        ArrayList<commandset> vect = new ArrayList<commandset>();
        //int len = 0;
        //for (int i = 0; i < commandsets.length; i++) {
        //    if (commandsets[i].get_remotename().equals(remotename))
        //        len++;
        //}
        //commandset[] cmds = new commandset[len];
        //int index = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].get_remotename().equals(remotename)
                    && (type == commandtype_t.any || type == commandsets[i].get_type()))
                vect.add(commandsets[i]);
        }
        return (commandset[])vect.toArray(new commandset[vect.size()]);
    }

    public commandset[] get_commandsets(commandtype_t type) {
        int len = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].get_type() == type)
                len++;
        }
        commandset[] cmds = new commandset[len];
        int index = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].get_type() == type)
                cmds[index++] = commandsets[i];
        }
        return cmds;
    }
    
    public String[] get_protocols(String remotename) {
        String[] work = new String[commandsets.length];
        for (int i = 0; i < commandsets.length; i++)
            if (commandsets[i].get_remotename().equals(remotename))
                work[i] = commandsets[i].get_protocol();

        return harcutils.sort_unique(harcutils.nonnulls(work));
    }

    public ArrayList<commandtype_t> get_commandtypes(command_t cmd) {
        ArrayList<commandtype_t> v = new ArrayList<commandtype_t>();
        for (commandtype_t t : commandtype_t.values())
            if (t != commandtype_t.any && get_command(cmd, t) != null)
                v.add(t);

        return v;
    }

    public command get_command(command_t cmd) {
        return get_command(cmd, commandtype_t.any);
    }

    public command get_command(command_t cmd, commandtype_t type) {
        for (int i = 0; i < commandsets.length; i++) {
            command c = commandsets[i].get_command(cmd, type);
            if (c != null)
                return c;
        }

        // Not found...
        if (cmd == command_t.power_on) {
            for (int i = 0; i < commandsets.length; i++) {
                String ppo = commandsets[i].get_pseudo_power_on();
                if (!ppo.equals("")) {
                    command_t ppo_cmd = command_t.parse(ppo);
                    command c = commandsets[i].get_command(ppo_cmd, type);
                    if (c != null)
                        return c;
                }
            }
        }
        return null;
    }

    public command get_command(command_t cmd, commandtype_t type, String remote) {
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].get_remotename().equals(remote)) {
                command c = commandsets[i].get_command(cmd, type);
                if (c != null)
                    return c;
            }
        }

        // Not found...
        if (cmd == command_t.power_on) {
            for (int i = 0; i < commandsets.length; i++) {
                if (commandsets[i].get_remotename().equals(remote)) {
                    String ppo = commandsets[i].get_pseudo_power_on();

                    if (!ppo.equals("")) {
                        command_t ppo_cmd = command_t.parse(ppo);
                        command c = commandsets[i].get_command(ppo_cmd, type);
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
                    IrSignal irc = get_command_by_index(cs, 0).get_ir_code(toggletype.toggle_0, false);
                    //System.out.println(irc.);
                    return irc == null ? 0 : (int) irc.getFrequency();
                    //if (gap > max)
                      //  max = gap;
                //}
            }
        }
        return 0;
    }

    private command get_command_by_index(int commandset, int commandindex) {
        return new command(commandsets[commandset], commandindex);
    }

    public IrSignal get_code(command_t cmd, commandtype_t type, toggletype toggle, boolean verbose) {
        command c = get_command(cmd, type);
        return (c != null) ? c.get_ir_code(toggle, verbose) : null;
    }

    public IrSignal get_code(command_t cmd, commandtype_t type, toggletype toggle, boolean verbose, String house, short deviceno) {
        command c = get_command(cmd, type);
        if (c == null)
            return null;

        return (house != null && !house.equals(""))
                ? c.get_ir_code(toggle, verbose, (short)((int)house.charAt(0)-(short)'A'), deviceno)
        : c.get_ir_code(toggle, verbose);
    }

    public IrSignal get_ir_code(command_t cmd, toggletype toggle, boolean verbose) {
        return get_code(cmd, commandtype_t.ir, toggle, verbose);
    }

    public String[] get_remotenames() {
        String[] work = new String[commandsets.length];
        for (int i = 0; i < commandsets.length; i++)
            work[i] = commandsets[i].get_remotename();

        return harcutils.nonnulls(harcutils.sort_unique(work));
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

    public static String[] devices2remotes(String[] devices) throws IOException, SAXParseException, SAXException {
        ArrayList<String> v = new ArrayList<String>();
        for (int i = 0; i < devices.length; i++) {
            String[] remotes = (new device(devices[i])).get_remotenames();
            v.addAll(Arrays.asList(remotes));
        }
        return (String[]) v.toArray(new String[v.size()]);
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

    public device_type get_type() {
        return type;
    }

    public String get_alias(command_t cmd) {
        return get_alias(cmd.toString());
    }

    public String info() {
        String infostr =
                "id = " + id + "\n" +
                "name = " + device_name + "\n" +
                "vendor = " + vendor + "\n" +
                "model = " + model + "\n" +
                "type = " + type + "\n";

        for (String s : attributes.keySet())
            infostr = infostr + "@" + s + "=" + attributes.get(s) + "\n";

        for (int i = 0; i < no_aliases; i++)
            infostr = infostr + "alias: " + aliases[i][0] + "->" + aliases[i][1] + "\n";

        infostr = infostr + "Remotenames: " + harcutils.join(get_remotenames(), ',', 0) + "\n";

        infostr = infostr + "# commandsets = " + commandsets.length;

        for (int i = 0; i < commandsets.length; i++)
            infostr = infostr + "\n" + commandsets[i].get_info();
        return infostr;
    }

    public boolean is_valid() {
        return doc != null;
    }

    public int get_jp1_setupcode() {
        return this.jp1_setupcode;
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
        boolean hit = attributes.containsKey(s) && attributes.get(s).equalsIgnoreCase("yes");
        return positive ? hit : ! hit;
    }

    private boolean evaluate_ifattribute(Element e) {
        return evaluate_ifattribute(e.getAttribute("ifattribute"));
    }

    /**
     * Returns the names of the devices available to us. For this, just look at the file names
     * in the device directory. This is not absolutely fool proof, in particular
     * on systems with case insensitive file system.
     *
     * @return Array of strings of the device names.
     */
    public static String[] get_devices() {
        return harcutils.get_basenames(harcprops.get_instance().get_devicesdir(), harcutils.devicefile_extension);
    }

    private device(String filename, HashMap<String, String>attributes, boolean barf_for_invalid)
            throws IOException, SAXParseException, SAXException {
         this( (filename.contains(File.separator) ? "" : harcprops.get_instance().get_devicesdir() + File.separator)
                 + filename
                 + ((filename.endsWith(harcutils.devicefile_extension)) ? "" : harcutils.devicefile_extension),
                 null, attributes, barf_for_invalid);
    }

    private device(String filename, String devicename, HashMap<String, String> attributes, boolean barf_for_invalid)
            throws IOException, SAXParseException, SAXException {
        this(harcutils.open_xmlfile(filename), devicename, attributes, barf_for_invalid);
    }

    private device(Document doc, HashMap<String, String>attributes, boolean barf_for_invalid) {
        this(doc, (String) null, attributes, barf_for_invalid);
    }

    //public device(Document doc, String dev_name, boolean barf_for_invalid) {
    //    this(doc, find_device_el(doc, dev_name), barf_for_invalid);
    //}

    /**
     *
     * @param name Either file name or device name.
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXParseException
     */
    public device(String name, HashMap<String, String>attributes) throws IOException, SAXParseException, SAXException {
        this(name, attributes, true);
    }
    
    public device(String name) throws IOException, SAXParseException, SAXException {
        this(name, null);
    }
    private device(Document doc, String dev_name, HashMap<String, String>instance_attributes, boolean barf_for_invalid) {
        this.doc = doc;
        if (doc == null)
            return;

        Element device_el = find_device_el(doc, dev_name);
        this.device_el = device_el;
        device_name = device_el.getAttribute("name");
        vendor = device_el.getAttribute("vendor");
        id = device_el.getAttribute("id");
        model = device_el.getAttribute("model");
        type = device_type.valueOf(device_el.getAttribute("type"));
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
        attributes = new HashMap<String, String>(no_attributes);
        for (int i = 0; i < no_attributes; i++) {
            Element attr = (Element) attributes_nodes.item(i);
            String val =
            attributes.put(
                    attr.getAttribute("name"),
                    attr.getAttribute("defaultvalue").isEmpty() ? "no" : attr.getAttribute("defaultvalue"));
        }
        // ... then, to the extent applicable, overwrite with actual instance values
        if (instance_attributes != null)
            for (String s : instance_attributes.keySet()) {
                if (attributes.containsKey(s))
                    attributes.put(s, instance_attributes.get(s));
                else
                    System.err.println("WARNING: Attribute named `" + s + "' does not exist in device `" + device_name + "', ignored.");
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
        commandsets = new commandset[no_commandsets];
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
            commandset_entry[] cmds = new commandset_entry[no_valids];
            int pos = 0;
            for (int j = 0; j < cmd_nodes.getLength(); j++) {
                Element cmd_el = (Element) cmd_nodes.item(j);
                String commandname = cmd_el.getAttribute("cmdref");
                //String ifattr = cmd_el.getAttribute("ifattribute");
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
                    cmds[pos++] = new commandset_entry(cmd_el.getAttribute("cmdref"),
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
            commandsets[i] = new commandset(cmds,
                    cs.getAttribute("type"),
                    cs.getAttribute("protocol"),
                    cs.getAttribute("deviceno"),
                    cs.getAttribute("subdevice"),
                    cs.getAttribute("toggle"),
                    cs.getAttribute("name"),
                    cs.getAttribute("remotename"),
                    cs.getAttribute("pseudo_power_on"),
                    cs.getAttribute("prefix"),
                    cs.getAttribute("suffix"),
                    cs.getAttribute("delay_between_reps"),
                    cs.getAttribute("open"),
                    cs.getAttribute("close"),
                    cs.getAttribute("portnumber"),
                    cs.getAttribute("charset"));
        }
    }

    private void print(String filename) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, harcprops.get_instance().get_dtddir() + File.separatorChar + doctype_systemid_filename);
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
                    if (!command_t.is_valid(cmd_el.getAttribute("cmdref"))) {
                        System.err.println("Ignoring invalid command " + cmd_el.getAttribute("cmdref"));
                        continue;
                    }

                    if (cmd_el.getElementsByTagName("ccf").getLength() > 0) {
                        System.err.println("Already ccf information in " + cmd_el.getAttribute("cmdref") + ", ignoring.");
                        continue;
                    }
                    
                    short obc = get_command_by_index(i, cmd_index).get_commandno();
                    IrSignal ir = get_command_by_index(i, cmd_index++/*j*/).get_ir_code(toggletype.dont_care, verbose);
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

                    try {
                        Element code = null;
                    
                    if (has_toggle) {
                        code = doc.createElement("toggle_pair");

                        Element c = doc.createElement("ccf");
                        c.setAttribute("toggle", "0");
                        code.appendChild(c);
                        ir = get_command_by_index(i, j).get_ir_code(toggletype.toggle_0, verbose);
                        c.appendChild(doc.createTextNode(ir.ccfString()));

                        c = doc.createElement("ccf");
                        c.setAttribute("toggle", "1");
                        code.appendChild(c);
                        ir = get_command_by_index(i, j).get_ir_code(toggletype.toggle_1, verbose);
                        c.appendChild(doc.createTextNode(ir.ccfString()));
                    } else {
                        code = doc.createElement("ccf");
                        //ir_code ir = get_command_by_index(i, j).get_ir_code(toggletype.no_toggle, verbose);
                        //code.appendChild(doc.createTextNode(ir.ccf_string()));
                        code.appendChild(doc.createTextNode(ir.ccfString()));
                    }
                    cmd_el.appendChild(code);
                    } catch (IrpMasterException e) {
                        System.err.println(e.getMessage());
                    }
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

    public static boolean export_device(String export_dir, String devname) {
        String out_filename = export_dir + File.separator + devname + harcutils.devicefile_extension;
        System.err.println("Exporting " + devname + " to " + out_filename + ".");
        device dev = null;
        try {
            dev = new device(devname, null, true);
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
        for (int i = 0; i < devs.length; i++)
            export_device(export_dir, devs[i]);

        return true;
    }

    private static void usage(int exitcode) {
        System.err.println("Usage:");
        System.err.println("device [<options>] -f <input_filename> [<cmd_name>]");
        System.err.println("or");
        System.err.println("device -x <export_directory>");
        System.err.println("where options=-o <filename>,-@ <attributename>,-a aliasname,-l,-d,-c,-t "
                + commandtype_t.valid_types('|'));
        if (exitcode >= 0)
            System.exit(exitcode);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }

    public static void main(String args[]) {
        commandtype_t type = commandtype_t.any;
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

                if (args[arg_i].equals("-@")) {
                    arg_i++;
                    attribute_name = args[arg_i++];
                } else if (args[arg_i].equals("-D")) {
                    arg_i++;
                    deviceno = Short.parseShort(args[arg_i++]);
                } else if (args[arg_i].equals("-S")) {
                    arg_i++;
                    subdevice = Short.parseShort(args[arg_i++]);
                } else if (args[arg_i].equals("-a")) {
                    arg_i++;
                    alias_name = args[arg_i++];
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    get_code = true;
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug++;
                } else if (args[arg_i].equals("-l")) {
                    arg_i++;
                    list_commands = true;
                } else if (args[arg_i].equals("-f")) {
                    arg_i++;
                    in_filename = args[arg_i++];
                } else if (args[arg_i].equals("-o")) {
                    arg_i++;
                    out_filename = args[arg_i++];
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    String typename = args[arg_i++];
                    if (!commandtype_t.is_valid(typename))
                        usage();
                    type = commandtype_t.valueOf(typename);
                } else if (args[arg_i].equals("-x")) {
                    arg_i++;
                    export_dir = args[arg_i++];
                } else
                    usage(harcutils.exit_usage_error);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }

        if (export_dir != null) {
            boolean success = export_all_devices(export_dir);
            System.exit(success ? harcutils.exit_success : harcutils.exit_config_write_error);
        } else if (in_filename == null) {
            usage(-1);
            harcutils.printtable("Known devices:", get_devices());
            System.exit(harcutils.exit_success);
        } else if ((args.length != arg_i) && (args.length != arg_i + 1))
            usage();

        device dev = null;
        try {
            dev = new device(in_filename, null, true);
        } catch (IOException e) {
            System.err.println("IOException with " + in_filename);
            System.exit(harcutils.exit_config_read_error);
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
            for (int i = 0; i < cmds.length; i++) {
                System.out.println(cmds[i]);
            }
        }

        if (attribute_name != null)
            System.out.println("@" + attribute_name + "=" + dev.get_attribute(attribute_name));

        if (alias_name != null)
            System.out.println("alias: " + alias_name + "->" + dev.get_alias(alias_name));
        if (args.length == arg_i + 1) {
            String cmdname = args[arg_i];
            command c = dev.get_command(command_t.parse(cmdname), type);
            if (c == null)
                System.out.println("No such command with specified type");
            else {
                System.out.println(c.toString());
                if (get_code) {
                    IrSignal ircode = (deviceno == -1) ? c.get_ir_code(toggletype.toggle_0, true)
                            : c.get_ir_code(toggletype.toggle_0, true, deviceno, subdevice);
                    if (ircode == null) {
                        System.err.println("No such IR command: " + cmdname);
                        System.exit(2);
                    } else {
                        System.out.println(ircode);
                        if (c.get_toggle())
                            System.out.println(c.get_ir_code(toggletype.toggle_1, true));
                    }
                }
            }
        }
    }
}
