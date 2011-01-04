/**
 * This is just a first shot, and needs substantial improvements.
 * TODO: use RemoteMaster.jar to read the rdf file and to create/write the rmdu
 * file, thus eliminating x2rmdu, jp1remote.dtd, rmdu.dtd etc. Keep: buttom_rules.
 *
 * In the meantime, it is sort-of useful, see Makefile
 */

package harc;

import java.io.*;
//import java.net.*;
import org.w3c.dom.*;
import org.xml.sax.*;


public class rmdu_export {

    public static final String rmdu_doctype = /*harcutils.dtd_dir +*/ "dtds/rmdu.dtd";
    private static int debug = 0;
    private static int idx = 0;
    private static final int mode_none = -1;
    private static final int mode_unshifted = 0;
    private static final int mode_shifted = 1;
    private static final int mode_xshifted = 2;

    private class fcn {

        public String name;
        public int hex;
        public int obc;
        public String one_button_name;

        public fcn(String name, int hex, int obc) {
            this.name = name.replaceAll("cmd_", "");
            this.hex = hex;
            this.obc = obc;
        }

        public String toString() {
            return name + "\t" + obc + "\t" + one_button_name;
        }
    }

    private class button {

        public String hardname;
        public String[] names;
        public boolean match_required;
        public boolean match_required_unshifted;
        public int number;
        public fcn unshifted;
        public fcn shifted;
        public fcn xshifted;

        @Override
        public String toString() {	// just for debugging
            String result = hardname;
            for (int i = 0; i < names.length; i++) {
                result = result + "\ta[" + i + "]=" + names[i];
            }
            result = result + (unshifted != null ? "\tunshifted=" + unshifted.name : "");
            result = result + (shifted != null ? "\tshifted=" + shifted.name : "");
            result = result + (xshifted != null ? "\txshifted=" + xshifted.name : "");
            return result;
        }

        public button(String name, int number) {
            this.hardname = name;
            this.number = number;
            unshifted = null;
            shifted = null;
            xshifted = null;
            names = new String[1];
            names[0] = name;
        }

	       public int assign(fcn f, boolean unshifted_only) {
            int result = mode_none;
            if (unshifted == null) {
                unshifted = f;
                result = mode_unshifted;
                f.one_button_name = hardname;
            } else if (!unshifted.name.equals(f.name) && shifted == null && !unshifted_only) {
                shifted = f;
                result = mode_shifted;
                f.one_button_name = hardname;
            } else if (!unshifted.name.equals(f.name) && (shifted != null) && !shifted.name.equals(f.name) && xshifted == null && !unshifted_only) {
                xshifted = f;
                result = mode_xshifted;
                f.one_button_name = hardname;
            }
            return result;
        }

        public String get_unshifted_name() {
            return unshifted == null ? "null" : unshifted.name;
        }

        public String get_shifted_name() {
            return shifted == null ? "null" : shifted.name;
        }

        public String get_xshifted_name() {
            return xshifted == null ? "null" : xshifted.name;
        }
    }
    Document jp1remote;
    Document device;
    Document rules;
    Document protocol;
    Document resultdoc;
    String jp1file;
    String devicefile;
    String rulesfile;
    fcn[] functions;
    button[] buttons;
    fcn reserved_fcn = new fcn("****", 0, 0);

    void dump_buttons() {
        System.err.println("------------ Buttons ----------");
        for (int i = 0; i < buttons.length; i++) {
            System.err.println(buttons[i].toString());
        }
    }

    void dump_functions() {
        System.err.println("------------ Functions ----------");
        for (int i = 0; i < functions.length; i++) {
            System.err.println(functions[i].toString());
        }
    }

    private static int jp1int(String s) {
        return s.startsWith("$") ? Integer.parseInt(s.substring(1), 16) : Integer.parseInt(s);
    }

    private static int cmdnoint(String s) {
        return s.startsWith("0x") ? Integer.parseInt(s.substring(2), 16) : Integer.parseInt(s);
    }

    public void print(String filename) throws FileNotFoundException {
        harcutils.printDOM(filename, resultdoc, rmdu_doctype);
    }

    private int obc2hex(String fname, String bytes, int obc) {
        if (fname.equals("reverse-invert"))
            return (255 - (Integer.reverse(obc) >> 24)) & 255;
        else if (fname.equals("invert_4"))
            return 4 * (63 - obc);
        else if (fname.equals("identity"))
            return obc;
        else {
            System.err.println("obc2hex function \"" + fname + "\" unknown, using identity");
            return obc;
        }
    }

    public rmdu_export(Document jp1remote, Document device, Document rules, String notes) throws IOException {
        this.jp1remote = jp1remote;
        this.device = device;
        this.rules = rules;

        NodeList commandsets = device.getElementsByTagName("commandset");
        int no_ir_commandsets = 0;
        Element the_commandset = null;
        for (int i = 0; i < commandsets.getLength(); i++) {
            Element cs = (Element) commandsets.item(i);
            if (cs.getAttribute("type").equals("ir"))
                if (no_ir_commandsets++ == 0)
                    the_commandset = cs;
                else
                    System.err.println("Warning: More than one ir commandset found. Ignoring all but the first.");
        }

        String protocol_name = the_commandset.getAttribute("protocol");
        try {
            protocol = harcutils.open_xmlfile(harcprops.get_instance().get_protocolsdir()
                    + File.separator + protocol_name + harcutils.protocolfile_extension);
        } catch (Exception e) {
            System.err.println("Protocol file error"+ e.getMessage());
            return;
        }

        Element tohex_element = (Element) ((Element) protocol.getElementsByTagName("jp1data").item(0)).getElementsByTagName("tohex").item(0);
        String obc2hex_functionname = tohex_element.getAttribute("function");
        String obc2hex_bits = tohex_element.getAttribute("bits");

        NodeList commands = the_commandset.getElementsByTagName("command");
        functions = new fcn[commands.getLength()];
        for (int i = 0; i < commands.getLength(); i++) {
            Element cmd = (Element) commands.item(i);
            int obc = cmdnoint(cmd.getAttribute("cmdno"));
            int hex = obc2hex(obc2hex_functionname, obc2hex_bits, obc);
            functions[i] = new fcn(cmd.getAttribute("cmdref"), hex, obc);
        }

        NodeList buttonnodes = jp1remote.getElementsByTagName("button");
        buttons = new button[buttonnodes.getLength()];
        for (int i = 0; i < buttons.length; i++) {
            Element btn = (Element) buttonnodes.item(i);
            buttons[i] = new button(btn.getAttribute("name"), jp1int(btn.getAttribute("value")));
        }

        interpret_rules();
        reserved_fcn.name = "null";

        resultdoc = harcutils.newDocument();
        Element root = resultdoc.createElement("rmdu");
        resultdoc.appendChild(root);

        Element Description = resultdoc.createElement("Description");
        String name = ((Element) device.getElementsByTagName("device").item(0)).getAttribute("name");
        Description.appendChild(resultdoc.createTextNode(name));
        root.appendChild(Description);

        Element Remote_name = resultdoc.createElement("Remote.name");
        String remotename = ((Element) jp1remote.getElementsByTagName("general").item(0)).getAttribute("name");
        Remote_name.appendChild(resultdoc.createTextNode(remotename));
        root.appendChild(Remote_name);

        Element Remote_signature = resultdoc.createElement("Remote.signature");
        String signature = ((Element) jp1remote.getElementsByTagName("general").item(0)).getAttribute("signature");
        Remote_signature.appendChild(resultdoc.createTextNode(signature));
        root.appendChild(Remote_signature);

        Element DeviceType = resultdoc.createElement("DeviceType");
        DeviceType.appendChild(resultdoc.createTextNode("Cable"));
        root.appendChild(DeviceType);

        Element DeviceIndex = resultdoc.createElement("DeviceIndex");
        NodeList devicetypesaliases = jp1remote.getElementsByTagName("devicetypealias");
        String devtypename = "";
        for (int i = 0; i < devicetypesaliases.getLength() && devtypename.equals(""); i++) {
            Element dta = (Element) devicetypesaliases.item(i);
            if (dta.getAttribute("value").equals("Cable"))
                devtypename = dta.getAttribute("name");
        }

        NodeList devicetypes = jp1remote.getElementsByTagName("devicetype");
        String index = "";
        for (int i = 0; i < devicetypes.getLength() && index.equals(""); i++) {
            Element devtype = (Element) devicetypes.item(i);
            if (devtype.getAttribute("name").equals(devtypename))
                index = devtype.getAttribute("value");
        }
        DeviceIndex.appendChild(resultdoc.createTextNode(index));
        root.appendChild(DeviceIndex);

        Element SetupCode = resultdoc.createElement("SetupCode");
        try {
            String code = ((Element) ((Element) device.getElementsByTagName("jp1data").item(0)).getElementsByTagName("setupcode").item(0)).getAttribute("value");
            SetupCode.appendChild(resultdoc.createTextNode(code));
            root.appendChild(SetupCode);
        } catch (NullPointerException e) {
            System.err.println("No JP1 setupcode in device file.");
            System.exit(3);
        }

        try {
            Element Protocol = resultdoc.createElement("Protocol");
            String protocol_number = ((Element) protocol.getElementsByTagName("protocol").item(0)).getAttribute("number");
            Protocol.appendChild(resultdoc.createTextNode(protocol_number));
            root.appendChild(Protocol);
        } catch (NullPointerException e) {
            System.err.println("No JP1 protocol number in protocol file.");
            System.exit(4);
        }

        Element ProtocolName = resultdoc.createElement("Protocol.name");
        String jp1_protocol_name = ((Element) ((Element) protocol.getDocumentElement().getElementsByTagName("jp1data").item(0)).getElementsByTagName("protocol").item(0)).getAttribute("name");
        ProtocolName.appendChild(resultdoc.createTextNode(jp1_protocol_name.equals("") ? protocol_name : jp1_protocol_name));
        root.appendChild(ProtocolName);

        String dev_no = the_commandset.getAttribute("deviceno");
        String subdev_no = the_commandset.getAttribute("subdevice");
        String protocol_params = subdev_no.equals("") ? (dev_no + " null null") : (dev_no + " " + subdev_no + " null");
        Element ProtocolParams = resultdoc.createElement("ProtocolParms");
        ProtocolParams.appendChild(resultdoc.createTextNode(protocol_params));
        root.appendChild(ProtocolParams);

// 	Element FixedData = resultdoc.createElement("FixedData");
// 	FixedData.appendChild(resultdoc.createTextNode(""));
// 	root.appendChild(FixedData);

        NodeList codes = protocol.getElementsByTagName("urc-code");
        for (int i = 0; i < codes.getLength(); i++) {
            Element c = (Element) codes.item(i);
            Element code = resultdoc.createElement("Code");
            code.setAttribute("cpu", c.getAttribute("cpu"));
            code.appendChild(resultdoc.createTextNode(c.getTextContent()));
            root.appendChild(code);
        }

        Element Notes = resultdoc.createElement("Notes");
        Notes.appendChild(resultdoc.createTextNode(notes));
        root.appendChild(Notes);

        for (int i = 0; i < functions.length; i++) {
            Element f = resultdoc.createElement("Function");
            f.setAttribute("index", "" + i);
            f.setAttribute("name", functions[i].name);
            f.setAttribute("hex", Integer.toHexString(functions[i].hex));
            f.setAttribute("obc", Integer.toString(functions[i].obc));
            if (functions[i].one_button_name != null)
                f.setAttribute("button", "" + functions[i].one_button_name);
            root.appendChild(f);
        }

        for (int i = 0; i < buttons.length; i++) {
            Element b = resultdoc.createElement("Button");
            b.setAttribute("name", buttons[i].hardname);
            b.setAttribute("number", Integer.toHexString(buttons[i].number));
            if (!buttons[i].get_unshifted_name().equals("null"))
                b.setAttribute("unshifted", buttons[i].get_unshifted_name());
            if (!buttons[i].get_shifted_name().equals("null"))
                b.setAttribute("shifted", buttons[i].get_shifted_name());
            if (!buttons[i].get_xshifted_name().equals("null"))
                b.setAttribute("xshifted", buttons[i].get_xshifted_name());
            root.appendChild(b);
        }
    }

    public rmdu_export(String jp1file, String devicefile, String rulesfile)
            throws IOException, SAXParseException {
        this(harcutils.open_xmlfile(jp1file),
                harcutils.open_xmlfile(devicefile),
                rulesfile != null ? harcutils.open_xmlfile(rulesfile) : null,
                "Created from " + jp1file + ", " + devicefile + ", and " + rulesfile + ".");
        this.jp1file = jp1file;
        this.devicefile = devicefile;
        this.rulesfile = rulesfile;
    }

    private fcn search_functions(button b, String functionname) {
        if (debug > 0)
            System.err.print("Button " + b.hardname + " searching for function \"" + functionname + "\"");
        for (int fn = 0; fn < functions.length; fn++) {
            if (functions[fn].name.equals(functionname)) {
                if (debug > 0)
                    System.err.println(" ... found");
                return functions[fn];
            }
        }
        if (debug > 0)
            System.err.println(" ... not found");
        return null;
    }

    // may the function f be assigned to button b?
    private boolean is_ok(button b, fcn f) {
        if (f == null)
            return false;
        return !(b.unshifted != null && b.unshifted.equals(f) || b.shifted != null && b.shifted.equals(f) || b.xshifted != null && b.xshifted.equals(f));
    }

    private void assign_buttons(boolean only_unshifted, boolean aliases) {
        for (int but = 0; but < buttons.length; but++) {
            button b = buttons[but];
            fcn f = null;
            for (int k = 0; k < b.names.length; k++) {
                f = search_functions(b, b.names[k]);
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
        boolean found = false;
        for (int i = 0; i < buttons.length && !found; i++) {
            button b = buttons[i];
            if (b.hardname.equals(buttonname)) {
                found = true;
                String devicetype = the_node.getAttribute("devicetype");
                String[] devicetypes = devicetype.split("\\|");
                boolean devicetype_ok = devicetype.equals("");
                for (int j = 0; j < devicetypes.length && !devicetype_ok; j++) {
                    devicetype_ok = devicetypes[j].equals(((Element) device.getElementsByTagName("device").item(0)).getAttribute("type"));
                }

                if (the_node.getTagName().equals("names")) {
                    if (devicetype_ok) {
                        b.match_required = match_required;
                        b.match_required_unshifted = match_required_unshifted;
                        NodeList newnames = the_node.getElementsByTagName("add-name");
                        int no_replaces = 0;
                        for (int j = 0; j < newnames.getLength(); j++) {
                            if (((Element) newnames.item(j)).getAttribute("replace").equals("yes"))
                                no_replaces++;
                        }
                        int names_new_length = newnames.getLength() - no_replaces + b.names.length;
                        String[] names_new = new String[names_new_length];
                        int indx = 0;
                        for (int j = 0; j < b.names.length; j++) {
                            names_new[indx++] = b.names[j];
                        }
                        for (int j = 0; j < newnames.getLength(); j++) {
                            Element name = (Element) newnames.item(j);
                            if (name.getAttribute("replace").equals("yes"))
                                names_new[0] = name.getAttribute("name");
                            else
                                names_new[indx++] = name.getAttribute("name");
                        }
                        b.names = names_new;
                    }
                } else if (the_node.getTagName().equals("reserve-button")) {
                    b.unshifted = reserved_fcn;
                    if (!the_node.getAttribute("unshifted-only").equals("yes")) {
                        b.shifted = reserved_fcn;
                        b.xshifted = reserved_fcn;
                    }
                } else
                    System.err.println("Unknown element: " + the_node.getTagName());
            }
        }
    }

    private void interpret_rules() {
        if (rules == null)
            return;
        NodeList rls = ((Element) rules.getElementsByTagName("button_rules").item(0)).getChildNodes();
        for (int i = 0; i < rls.getLength(); i++) {
            if (rls.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) rls.item(i);
                String tagname = e.getTagName();
                if (debug > 0)
                    System.err.println("Treating rule element: " + tagname + ", button = " + e.getAttribute("button"));
                if (tagname.equals("assign-buttons")) {
                    assign_buttons(e.getAttribute("unshifted-only").equals("yes"), e.getAttribute("aliases").equals("yes"));
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

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("rmdu [<options>] <devicefile>");
        System.err.println("where options=-j <xrdffile> (mandatory), -r <rulesfile>, -o outfile,-d");
        System.exit(1);
    }

    public static void main(String args[]) {
        //int debug = 0;
        String outfile = null;
        String jp1file = null;
        String devicefile = null;
        String rulesfile = null;
        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug++;
                } else if (args[arg_i].equals("-o")) {
                    arg_i++;
                    outfile = args[arg_i++];
                } else if (args[arg_i].equals("-j")) {
                    arg_i++;
                    jp1file = args[arg_i++];
                } else if (args[arg_i].equals("-r")) {
                    arg_i++;
                    rulesfile = args[arg_i++];
                } else
                    usage();
            }
            devicefile = args[arg_i++];
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("ArrayIndexOutOfBoundsException");
            usage();
        }
        if (jp1file == null)
            usage();

        try {
            rmdu_export r = new rmdu_export(jp1file, devicefile, rulesfile);
            r.print(outfile);
        } catch (IOException e) {
            System.err.println("IO error " + e.getMessage());
        } catch (SAXParseException e) {
            System.err.println("SAX " + e.getMessage());
        }
    }
}
