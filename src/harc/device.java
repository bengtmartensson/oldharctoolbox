/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

import java.io.*;
import org.w3c.dom.*;
//import org.xml.sax.*;
//import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class device /*extends ir_code*/ implements commandnames {

    /**
     * Number of arguments considering the code as a remote contoller.
     */
    public static final int no_args = 1;
    public static final String doctype_systemid_filename = "devices.dtd";
    public static final String doctype_publicid = "-//bengt-martensson.de//devices//en";
    public String vendor;
    public String device_name;
    public String id;
    public String model;
    public String type;
    private boolean has_toggle = false; // Not QUITE correct, property
    // of a commandset, not of a
    // device
    private Document doc = null;
    private Element device_el = null;
    private commandset[] commandsets;
    private String[][] attributes;
    private int no_attributes = 0;
    private String[][] aliases;
    private int no_aliases = 0;
    private static int debug = 0;

    // This should better be called get_ir_commands or such, I keep to
    // the present name for compatibility with old IR-oriented code.
    public int[] get_commands() {
        return get_commands(commandset.ir);
    }

    public int[] get_ir_commands() {
        return get_commands(commandset.ir);
    }

    public int[] get_commands(String cmdtype) {
        return get_commands(commandset.toInt(cmdtype));
    }

    public int[] get_commands(int cmdtype) {
        int len = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (cmdtype == commandset.any || cmdtype == commandsets[i].gettype())
                len += commandsets[i].getno_commands();
        }
        int[] cmds = new int[len];
        int index = 0;
        for (int i = 0; i < commandsets.length; i++) {
            if (cmdtype == commandset.any || cmdtype == commandsets[i].gettype())
                for (int j = 0; j < commandsets[i].getno_commands(); j++) {
                    cmds[index++] = commandsets[i].getentry(j).getcmd();
                }
        }
        return cmds;
    }

    public boolean has_toggle() {
        return has_toggle;
    }

    public command get_command(int cmd, int type) {
        for (int i = 0; i < commandsets.length; i++) {
            command c = commandsets[i].get_command(cmd, type);
            if (c != null)
                return c;
        }

        // Not found...
        if (cmd == commandnames.cmd_power_on) {
            for (int i = 0; i < commandsets.length; i++) {
                String ppo = commandsets[i].getpseudo_power_on();
                if (!ppo.equals("")) {
                    int ppo_cmd = ir_code.decode_command(ppo);
                    command c = commandsets[i].get_command(ppo_cmd, type);
                    if (c != null)
                        return c;
                }
            }
        }
        return null;
    }

    private command get_command_by_index(int commandset, int commandindex) {
        return new command(commandsets[commandset], commandindex);
    }

    public ir_code get_code(int cmd, int type, toggletype toggle) {
        command c = get_command(cmd, type);
        return (c != null) ? c.get_ir_code() : null;
    }

    public ir_code get_ir_code(int cmd, toggletype toggle) {
        return get_code(cmd, commandset.ir, toggle);
    }

    public String getirremotename(String cmdname) {
        for (int i = 0; i < commandsets.length; i++) {
            if (commandsets[i].type_ir() && (commandsets[i].get_command(cmdname, commandset.ir) != null))
                return commandsets[i].getremotename();
        }
        return "*not found*";
    }

    public String get_attribute(String attribute_name) {
        String result = null;
        for (int i = 0; i < no_attributes && result == null; i++) {
            if (attributes[i][0].equals(attribute_name))
                result = attributes[i][1];
        }
        return result;
    }

    public String get_alias(String alias_name) {
        String result = null;
        for (int i = 0; i < no_aliases && result == null; i++) {
            if (aliases[i][0].equals(alias_name))
                result = aliases[i][1];
        }
        return result;
    }

    public String get_alias(int cmd) {
        return get_alias(ir_code.command_name(cmd));
    }

    public String info() {
        String infostr =
                "id = " + id + "\n" +
                "name = " + device_name + "\n" +
                "vendor = " + vendor + "\n" +
                "model = " + model + "\n" +
                "type = " + type + "\n";

        for (int i = 0; i < no_attributes; i++) {
            infostr = infostr + "@" + attributes[i][0] + "=" + attributes[i][1] + "\n";
        }

        for (int i = 0; i < no_aliases; i++) {
            infostr = infostr + "alias: " + aliases[i][0] + "->" + aliases[i][1] + "\n";
        }

        infostr = infostr + "# commandsets = " + commandsets.length;

        for (int i = 0; i < commandsets.length; i++) {
            infostr = infostr + "\n" + commandsets[i].info();
        }
        return infostr;
    }

    public boolean is_valid() {
        return doc != null;
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

    /*
    private static Element find_device_el(Document doc, String dev_name) {
    if (doc == null)
    return null;
    Element root = doc.getDocumentElement();
    Element el = null;
    if (root.getTagName().equals("device"))
    el = root;
    else {
    NodeList devices = root.getElementsByTagName("device");
    if (dev_name == null)
    el = (Element) devices.item(0);
    else
    for (int i = 0; i < devices.getLength(); i++)
    if (((Element) devices.item(i)).getAttribute("id").equals(dev_name))
    el = (Element) devices.item(i);
    }
    return el;
    }
     */

    /**
     * Returns the available devices. For this, just look at the file names
     * in the device directory. This is not absolutely fool proof, in particular
     * on systems with case insensitive file system.
     *
     * @param dirname Directory
     * @return Array of strings of the device names.
     */
    public static String[] get_devices(String dirname) {
        File dir = new File(dirname);
        if (!dir.isDirectory())
            return null;

        String[] files = dir.list(new extension_filter(".xml"));
        String[] result = new String[files.length];
        for (int i =0; i < files.length; i++)
            result[i] = files[i].substring(0, files[i].lastIndexOf(".xml"));
        return result;//String[] {"sjkdfld"};
    }
    
    public static String[] get_devices() {
        return get_devices(harcprops.get_instance().get_devices_dir());
    }

     public device(String filename) throws IOException {
         this( (filename.contains(File.separator) ? "" : harcprops.get_instance().get_devices_dir() + File.separator)
                 + filename
                 + ((filename.endsWith(harcutils.devicefile_extension)) ? "" : harcutils.devicefile_extension),
                 null);
    }

    public device(String filename, String devicename) throws IOException {
        this(harcutils.open_xmlfile(filename), devicename);
    }

    public device(Document doc) {
        this(doc, (String) null);
    }

    public device(Document doc, String dev_name) {
        this(doc, find_device_el(doc, dev_name));
    }

    public device(Document doc, Element device_el) {
        if (doc == null) {
            return;
        }
        this.doc = doc;
        this.device_el = device_el;
        device_name = device_el.getAttribute("name");
        vendor = device_el.getAttribute("vendor");
        id = device_el.getAttribute("id");
        model = device_el.getAttribute("model");
        type = device_el.getAttribute("type");

        NodeList attributes_nodes = device_el.getElementsByTagName("attribute");
        no_attributes = attributes_nodes.getLength();
        attributes = new String[no_attributes][2];
        for (int i = 0; i < no_attributes; i++) {
            Element attr = (Element) attributes_nodes.item(i);
            attributes[i][0] = attr.getAttribute("name");
            attributes[i][1] = attr.getAttribute("value");
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
        for (int i = 0; i < no_commandsets; i++) {
            Element cs = (Element) commandsets_nodes.item(i);
            if (cs.getAttribute("toggle") != null && cs.getAttribute("toggle").equals("yes"))
                has_toggle = true;

            // This does not work for hierarchical commandgrouprefs
            NodeList cgrs = cs.getElementsByTagName("commandgroupref");
            for (int j = 0; j < cgrs.getLength(); j++) {
                Element cgr = (Element) cgrs.item(j);
                Element cg = find_commandgroup_el(doc, cgr.getAttribute("commandgroup"));
                Element par = (Element) cgr.getParentNode();
                par.replaceChild(cg.cloneNode(true), cgr);
            }

            NodeList cmd_nodes = cs.getElementsByTagName("command");
            commandset_entry[] cmds = new commandset_entry[cmd_nodes.getLength()];
            for (int j = 0; j < cmd_nodes.getLength(); j++) {
                Element cmd_el = (Element) cmd_nodes.item(j);
                NodeList al = cmd_el.getElementsByTagName("argument");
                String[] arguments = new String[al.getLength()];
                for (int a = 0; a < arguments.length; a++) {
                    arguments[a] = ((Element) al.item(a)).getAttribute("name");
                }
                cmds[j] = new commandset_entry(cmd_el.getAttribute("cmdref"),
                        cmd_el.getAttribute("cmdno"),
                        cmd_el.getAttribute("name"),
                        cmd_el.getAttribute("transmit"),
                        cmd_el.getAttribute("response_lines"),
                        cmd_el.getAttribute("response_ending"),
                        cmd_el.getAttribute("expected_response"),
                        arguments);
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
                    cs.getAttribute("delay_between_reps"));
        }
    }

    private void print(String filename) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, harcprops.get_instance().get_dtd_dir() + File.separatorChar + doctype_systemid_filename);
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

    private void augment_dom() {
        NodeList commandsets_nodes = device_el.getElementsByTagName("commandset");
        for (int i = 0; i < commandsets_nodes.getLength(); i++) {
            Element cs = (Element) commandsets_nodes.item(i);
            if (cs.getAttribute("type").equals("ir") || cs.getAttribute("type").equals("rf433") || cs.getAttribute("type").equals("rf868")) {
                NodeList cmd_nodes = cs.getElementsByTagName("command");
                boolean has_toggle = cs.getAttribute("toggle").equals("yes");
                for (int j = 0; j < cmd_nodes.getLength(); j++) {
                    Element cmd_el = (Element) cmd_nodes.item(j);
                    Element code = null;
                    if (has_toggle) {
                        code = doc.createElement("toggle_pair");
                        for (int toggle = 0; toggle < 2; toggle++) {
                            Element c = doc.createElement("ccf");
                            c.setAttribute("toggle", toggle == 0 ? "0" : "1");
                            code.appendChild(c);
                            ir_code ir = get_command_by_index(i, j).get_ir_code(toggle);
                            c.appendChild(doc.createTextNode(ir.raw_ccf_string()));
                        }
                    } else {
                        code = doc.createElement("ccf");
                        ir_code ir = get_command_by_index(i, j).get_ir_code();
                        //code.appendChild(doc.createTextNode(ir.ccf_string()));
                        code.appendChild(doc.createTextNode(ir.raw_ccf_string()));
                    }
                    cmd_el.appendChild(code);
                }
            }
        }
    }

    private static void usage() {
        System.err.println("Usage:\n" + "\tdevice [<options>] [<cmd_name>]"
                + "\nwhere options=-f <input_filename> (mandatory),-o <filename>,-@ <attributename>,-a aliasname,-l,-d,-c,-t "
                + commandset.valid_types('|'));
        System.exit(1);
    }

    public static void main(String args[]) {
        int type = commandset.any;
        boolean get_code = false;
        boolean list_commands = false;
        String in_filename = null;
        String out_filename = null;
        String attribute_name = null;
        String alias_name = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) // ???
                    && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug++;
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    String typename = args[arg_i++];
                    if (!commandset.valid(typename))
                        usage();
                    type = commandset.toInt(typename);
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    get_code = true;
                } else if (args[arg_i].equals("-l")) {
                    arg_i++;
                    list_commands = true;
                } else if (args[arg_i].equals("-f")) {
                    arg_i++;
                    in_filename = args[arg_i++];
                } else if (args[arg_i].equals("-o")) {
                    arg_i++;
                    out_filename = args[arg_i++];
                } else if (args[arg_i].equals("-@")) {
                    arg_i++;
                    attribute_name = args[arg_i++];
                } else if (args[arg_i].equals("-a")) {
                    arg_i++;
                    alias_name = args[arg_i++];
                } else
                    usage();
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }
        if (in_filename == null) {
            //usage();
            harcutils.printtable("Known devices", device.get_devices());
            System.exit(harcutils.exit_success);
        }

        if ((args.length != arg_i) && (args.length != arg_i + 1))
            usage();

        device dev = null;
        try {
            dev = new device(in_filename);
        } catch (IOException e) {
            System.err.println("IOException with " + in_filename);
            System.exit(3);
        }

        if (!dev.is_valid()) {
            System.err.println("Failure!");
            System.exit(2);
        }

        if (debug != 0)
            System.out.println(dev.info());

        if (out_filename != null) {
            dev.augment_dom();
            dev.print(out_filename);
        }

        if (list_commands) {
            int[] cmds = dev.get_commands(type);
            for (int i = 0; i < cmds.length; i++) {
                System.out.println(ir_code.command_name(cmds[i]));
            }
        }

        if (attribute_name != null)
            System.out.println("@" + attribute_name + "=" + dev.get_attribute(attribute_name));

        if (alias_name != null)
            System.out.println("alias: " + alias_name + "->" + dev.get_alias(alias_name));
        if (args.length == arg_i + 1) {
            String cmdname = args[arg_i];
            command c = dev.get_command(ir_code.decode_command(cmdname), type);
            if (c == null)
                System.out.println("No such command with specified type");
            else {
                System.out.println(c.toString());
                if (get_code) {
                    ir_code ircode = c.get_ir_code(0);
                    if (ircode == null) {
                        System.err.println("No such IR command: " + cmdname);
                        System.exit(2);
                    } else {
                        ircode.print();
                        if (c.gettoggle())
                            c.get_ir_code(1).print();
                    }
                }
            }
        }
    }
}
