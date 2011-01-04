/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 *
 * 
 *
 */
package harc;

import java.io.*;
//import java.lang.*;
import java.net.*;
import org.w3c.dom.*;
//import javax.xml.parsers.*;
//import javax.xml.transform.*;
//import javax.xml.transform.dom.*;
//import javax.xml.transform.stream.*;

/**
 * Makes a "remote control" out of a ir_code class.
 */
public class remote {

    private int[] commands;
    //private boolean has_get_commands;
    //private String[] command_names; // only if ! has_get_commands
    private Class code_class;
    private String class_name;
    private String device_name;
    private int no_args;
    private short max_device;
    private short max_command;
    private short max_house;
    private String input_filename;
    private String device_filename;
    private device dev = null;
    private final String default_input_filename = "remote.xml";

    private static int debug = 0;
    public static String doctype_systemid = null;

/*
 public static int decode_toggle(String t) {
        if (t.equals("yes")) {
            return harcutils.do_toggle;
        } else if (t.equals("0")) {
            return harcutils.toggle_0;
        } else if (t.equals("1")) {
            return harcutils.toggle_1;
        } else {
            return harcutils.no_toggle;
        }
    }
*/
    
    public remote(Class c, String class_name, String input_filename)
            throws NoSuchMethodException, NoSuchFieldException {
        doctype_systemid = harcprops.get_instance().get_dtd_dir() + "/remote.dtd";
        if (input_filename == null) {
            input_filename = default_input_filename;
        }

        this.class_name = class_name;
        code_class = c;

        // call a static setup method, if present
        try {
            code_class.getMethod("setup", (new Class[]{String.class})).invoke((Object) null, (new Object[]{input_filename}));
        } catch (java.lang.reflect.InvocationTargetException e) {
        } catch (IllegalAccessException e) {
        } catch (NoSuchMethodException e) {
        }

        System.out.println(class_name);

        try {
            this.no_args = code_class.getField("no_args").getInt(null);

            if (debug > 0) {
                System.err.println("*** no_args = " + no_args);
            }

            switch (no_args) {
                case 1:
                    commands = (int[]) code_class.getMethod("get_commands", (Class[]) null).invoke((Object) null, (Object[]) null);
                    if (commands == null) {
                        System.err.println("Warning: commands is null");
                    }
                    if (commands.length == 0) {
                        System.err.println("Warning: commands is empty");
                    }
                    break;
                case 2:
                    max_device = code_class.getField("max_device").getShort(null);
                    max_command = code_class.getField("max_command").getShort(null);
                    if (debug > 0) {
                        System.err.println("two parameter succeeded: " + max_device + ", " + max_command);
                    }
                    break;
                case 3:
                    max_device = code_class.getField("max_device").getShort(null);
                    max_house = code_class.getField("max_house").getShort(null);
                    commands = (int[]) code_class.getMethod("get_commands", (Class[]) null).invoke((Object) null, (Object[]) null);
                    if (commands == null) {
                        System.err.println("Warning: commands is null");
                    }
                    if (commands.length == 0) {
                        System.err.println("Warning: commands is empty");
                    }
                    break;
                default:
                    System.err.println("This can not happen");
                    System.exit(55);
                    break;
            }

        } catch (IllegalAccessException e) {
        } catch (java.lang.reflect.InvocationTargetException e) {
        }
    }

    public remote(String codename, String input_filename)
            throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        this(Class.forName(codename), codename, input_filename);
    }

    // TODO: gracefully handle case of nonexisting device_name
    // note: if the root node is of type device, always succeeds.
    public remote(String codename, String device_filename,
            String device_name) throws IOException {
        doctype_systemid = harcprops.get_instance().get_dtd_dir() + "/remote.dtd";
        if (codename.equals(ir_code.package_name + ".device")) {
            device dev = new device(device_filename, device_name);
            class_name = codename;
            try {
                code_class = Class.forName(codename);
            } catch (ClassNotFoundException e) {
                System.err.println("This cannot happen");
            }
            this.device_name = dev.device_name;
            this.dev = dev;
            no_args = dev.no_args;
            commands = dev.get_ir_commands();
        } else {
            System.err.println("ouch");
        }
    }

    // Do not define any ccf_string version without toggle, will be
    // too confusing!

    // Provided has_toggle(), toggles are handled as follows:
    // case no_toggle: just call the constructor without toggle arg.
    // case toggle_0, toggle_1: call the constructor with appropriate toggle
    // case do_toggle: call the toggle constructor twice, concatenate results.

    // This code simply assumes that min_device == 0 for
    // two-parameter, and min_device == 1 for three-parameter
    // remotes. This is a flaw, however, fixing it might not be worth
    // it.
    private String ccf_string(int command, toggletype toggle, boolean raw) {
        String result = "";
        if (dev != null) {
            // TODO: handle toggles
            ir_code code = dev.get_ir_code(command, toggle);
            if (has_toggle() && toggle == toggletype.do_toggle) {
                System.err.println("Note: The generated CCF string contains TWO commands (separated with a line feed), the first with toggle = 0, the second with toggle = 1.");
                return ccf_string(command, toggletype.toggle_0, raw) + "\n"
                        + ccf_string(command, toggletype.toggle_1, raw);
            } else {
                result = raw ? code.raw_ccf_string() : code.ccf_string();
            }
        } else {
            String get_func = raw ? "raw_ccf_string" : "ccf_string";
            try {
                if (has_toggle() && toggle != toggletype.no_toggle) {
                    if (toggle == toggletype.do_toggle) {
                        System.err.println("Note: The generated CCF string contains TWO commands (separated with a line feed), the first with toggle = 0, the second with toggle = 1.");
                        return ccf_string(command, toggletype.toggle_0, raw) + "\n"
                                + ccf_string(command, toggletype.toggle_1, raw);
                    } else {
                        // toggle == toggle_0 (= 0) or toggle_1 (= 1)
                        Object ir = code_class.getConstructor(new Class[]{String.class, int.class}).newInstance(new Object[]{ir_code.command_name(command), toggle});
                        result = (String) code_class.getMethod(get_func, (Class[]) null).invoke(ir, (Object[]) null);
                    }
                } else {
                    Object ir = code_class.getConstructor(new Class[]{String.class}).newInstance(new Object[]{ir_code.command_name(command)});
                    result = (String) code_class.getMethod(get_func, (Class[]) null).invoke(ir, (Object[]) null);
                }
            //	} catch (ClassNotFoundException e) {
            //	    System.err.println("ClassNotFoundException");
            } catch (NoSuchMethodException e) {
                System.err.println("NoSuchMethodException: " + e.getMessage());
            } catch (InstantiationException e) {
                System.err.println("InstantiationException");
            } catch (IllegalAccessException e) {
                System.err.println("IllegalAccessException");
            } catch (java.lang.reflect.InvocationTargetException e) {
                System.err.println("InvocationTargetException");
            }
        }
        return result;
    }

    private String ccf_string(short device, short command, toggletype toggle,
            boolean raw) {
        String get_func = raw ? "raw_ccf_string" : "ccf_string";
        String result = "";
        try {
            if (has_toggle() && toggle != toggletype.no_toggle) {
                if (toggle == toggletype.do_toggle) {
                    System.err.println("Note: The generated CCF string contains TWO commands (separated with a line feed), the first with toggle = 0, the second with toggle = 1.");
                    result =
                            ccf_string(device, command, toggletype.toggle_0, raw) + "\n"
                            + ccf_string(device, command, toggletype.toggle_1, raw);
                } else {
                    Object ir = code_class.getConstructor(new Class[]{short.class, short.class, int.class}).newInstance(new Object[]{new Short(device), new Short(command), toggle});
                    result = (String) code_class.getMethod(get_func, (Class[]) null).invoke(ir, (Object[]) null);
                }
            } else {
                Object ir = code_class.getConstructor(new Class[]{short.class, short.class}).newInstance(new Object[]{new Short(device), new Short(command)});
                result = (String) code_class.getMethod(get_func, (Class[]) null).invoke(ir, (Object[]) null);
            }
        //} catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
            System.err.println(e.getMessage());
        } catch (InstantiationException e) {
            System.err.println(e.getMessage());
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
        } catch (java.lang.reflect.InvocationTargetException e) {
            System.err.println(e.getMessage());
        }

        return result;
    }

    private String ccf_string(char house, short device /* 1-based */,
            String command, toggletype toggle, boolean raw)
            throws NoSuchMethodException {
        int cmd = ir_code.decode_command(command);
        if (cmd == commandnames.cmd_invalid) {
            throw new NoSuchMethodException("Command " + command + " unknown");
        }

        return ccf_string(house, device, cmd, toggle, raw);
    }

    private String ccf_string(char house, short device /* 1-based */,
            int command, toggletype toggle, boolean raw) {
        String get_func = raw ? "raw_ccf_string" : "ccf_string";
        String result = "";

        if (has_toggle()) {
            System.err.println("I am not programmed to handle three parameter remotes with toggles.");
            System.exit(111);
        }
        try {
            Object ir = code_class.getConstructor(new Class[]{char.class, short.class, int.class}).newInstance(new Object[]{new Character(house), new Short(device), new Integer(command)});
            result = (String) code_class.getMethod(get_func, (Class[]) null).invoke(ir, (Object[]) null);
        } catch (NoSuchMethodException e) {
            System.err.println(e.getMessage());
        } catch (InstantiationException e) {
            System.err.println(e.getMessage());
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
        } catch (java.lang.reflect.InvocationTargetException e) {
            System.err.println(e.getMessage());
        }

        return result;
    }

    private String ccf_string(String cmd, toggletype toggle, boolean raw)
            throws NoSuchMethodException {
        int command = ir_code.decode_command(cmd);
        if (command == commandnames.cmd_invalid) {
            throw new NoSuchMethodException("Command " + cmd + " unknown");
        }
        return ccf_string(command, toggle, raw);
    }

    private String get_string_field(String fld) throws NoSuchFieldException {
        String result = "";
        try {
            result = (String) code_class.getField(fld).get(null);
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
// 	} catch (NoSuchFieldException e) {
// 	    System.err.println("Field " + fld + " missing");
// 	    System.err.println("Class: " + class_name);
        }
        return result;
    }

    private int[] get_int_array_field(String fld) throws NoSuchFieldException {
        int result[] = null;
        try {
            result = (int[]) code_class.getField(fld).get(null);
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
// 	} catch (NoSuchFieldException e) {
// 	    System.err.println("Field " + fld + " missing");
// 	    System.err.println("Class: " + class_name);
        }
        return result;
    }

    private int get_int_field(String fld) throws NoSuchFieldException {
        int result = -1;
        try {
            result = code_class.getField(fld).getInt(null);
        } catch (IllegalAccessException e) {
            System.err.println(e.getMessage());
// 	} catch (NoSuchFieldException e) {
// 	    System.err.println("Field " + fld + " missing");
// 	    System.err.println("Class: " + class_name);
        }
        return result;
    }

    private String getremotename(String cmd) {
        return (dev != null) ? dev.getirremotename(cmd) : name();
    }

    private String rem_code_string(int command, toggletype toggle) throws NoSuchMethodException {
        String result = "";
        try {
            if (has_toggle()) {
                Object ir = code_class.getConstructor(new Class[]{String.class, int.class}).newInstance(new Object[]{ir_code.command_name(command), toggle});
                result = (String) code_class.getMethod("rem_code_string", (Class[]) null).invoke(ir, (Object[]) null);
            } else {
                Object ir = code_class.getConstructor(new Class[]{String.class}).newInstance(new Object[]{ir_code.command_name(command)});
                result = (String) code_class.getMethod("rem_code_string", (Class[]) null).invoke(ir, (Object[]) null);
            }
        } catch (InstantiationException e) {
            System.err.println("InstantiationException");
        } catch (IllegalAccessException e) {
            System.err.println("IllegalAccessException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            //System.err.println("InvocationTargetException");
        }

        return result;
    }
    private final static int padded_length = 24;

    private String pad(String str) {
        return str + ("                    ").substring(0, padded_length - str.length());
    }

    private String format_lirc_string(String param) {
        try {
            String p = get_string_field("lirc_" + param);
            return p != null
                    ? pad("  " + param) + p + "\n"
                    : "";
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("LIRC parameter " + param + " not found");
            }
            return "";
        }
    }

    private String format_lirc_int_parameter(String param, int radix) {
        try {
            int p = get_int_field("lirc_" + param);
            return p > 0
                    ? pad("  " + param) + (radix == 16 ? "0x" : "") + Integer.toString(p, radix) + "\n"
                    : "";
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("LIRC parameter " + param + " not found");
            }
            return "";
        }
    }

    private String format_lirc_int_parameter1(String param, int radix) {
        try {
            int p = get_int_field("lirc_" + param);
            return p >= 0
                    ? pad("  " + param) + (radix == 16 ? "0x" : "") + Integer.toString(p, radix) + "\n"
                    : "";
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("LIRC parameter " + param + " missing");
            }
            return "";
        }
    }

    private String format_lirc_2int(String param) {
        try {
            int p[] = get_int_array_field("lirc_" + param);
            return p != null
                    ? pad("  " + param) + p[0] + "\t" + p[1] + "\n"
                    : "";
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("LIRC parameter " + param + " missing");
            }
            return "";
        }
    }

    private String format_lirc_flags() {
        try {
            return pad("  flags") + get_string_field("lirc_flags") + "\n";
        } catch (NoSuchFieldException e) {
            System.err.println("Fatal error: no LIRC flags found");
            System.exit(66);
        }
        return "not reached";
    }

    private String format_lirc_int_parameter(String param) {
        return format_lirc_int_parameter(param, 10);
    }

    private String lirc_string(toggletype toggle, boolean raw) {
        String result = "# Generated by de.bengt_martensson.ir\n" + "#\n" + "begin remote\n\n" + pad("  name") + name() + "\n" + pad("  frequency") + carrier_frequency() + "\n" + format_lirc_int_parameter("eps") + format_lirc_int_parameter("aeps") + format_lirc_int_parameter("ptrail") + format_lirc_int_parameter("gap");
        if (raw) {
            result = result + "  flags\t\tRAW_CODES\n";
        } else {
            result = result + format_lirc_int_parameter("bits") + format_lirc_flags() + format_lirc_2int("header") + format_lirc_2int("one") + format_lirc_2int("zero") + format_lirc_int_parameter("pre_data_bits") + format_lirc_int_parameter1("pre_data", 16) + format_lirc_string("pre_data_string") + format_lirc_int_parameter("plead") + format_lirc_int_parameter("post_data_bits") + format_lirc_int_parameter1("post_data", 16) + format_lirc_int_parameter1("repeat_bit", 10) + format_lirc_int_parameter1("toggle_bit", 10);
        }

        result = result + "\n    begin " + (raw ? "raw_codes" : "codes") + "\n";
        try {
            for (int i = 0; i < commands.length; i++) {
                if (has_toggle()) {
                    Object ir = code_class.getConstructor(new Class[]{String.class, int.class}).newInstance(new Object[]{ir_code.command_name(commands[i]), toggle});
                    result = result + "\t" + (raw ? "name " : "") + ir_code.command_name(commands[i]) + (raw ? "\n" : (ir_code.command_name(commands[i]).length() < 8 ? "\t\t" : "\t")) + (raw
                            ? format_int_arrays(((ir_code) ir).get_intro_array(),
                            ((ir_code) ir).get_repeat_array())
                            : (String) code_class.getMethod("lirc_code_string", (Class[]) null).invoke(ir, (Object[]) null)) + "\n";
                } else {
                    Object ir = code_class.getConstructor(new Class[]{String.class}).newInstance(new Object[]{ir_code.command_name(commands[i])});
                    result = result + "\t" + (raw ? "name " : "") + ir_code.command_name(commands[i]) + (raw ? "\n" : (ir_code.command_name(commands[i]).length() < 8 ? "\t\t" : "\t")) + (raw
                            ? format_int_arrays(((ir_code) ir).get_intro_array(),
                            ((ir_code) ir).get_repeat_array())
                            : (String) code_class.getMethod("lirc_code_string", (Class[]) null).invoke(ir, (Object[]) null)) + "\n";
                }
            }
        } catch (NoSuchMethodException e) {
            System.err.println("NoSuchMethodException: " + e.getMessage());
            System.exit(77);
        } catch (InstantiationException e) {
            System.err.println("InstantiationException");
        } catch (IllegalAccessException e) {
            System.err.println("IllegalAccessException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            //System.err.println("InvocationTargetException");
        }


        result = result + "    end " + (raw ? "raw_codes" : "codes") + "\n";

        return result + "\nend remote\n";
    }

    private String format_int_arrays(int[] a, int[] b) {
        int[] ab = new int[a.length + b.length];
        for (int i = 0; i < a.length; i++) {
            ab[i] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            ab[a.length + i] = b[i];
        }

        String result = "";
        for (int i = 0; i < ab.length - 1; i++) {
            result = result + " " + 1000000 * ab[i] / carrier_frequency();
        }

        return result;
    }

    private String name() {
        String result = null;
        try {
            result = (dev != null)
                    ? dev.device_name
                    : get_string_field("remote_name");
        } catch (NoSuchFieldException e) {
            System.err.println("Fatal error: No remote_name found");
            System.exit(14);
        }
        return result;
    }

    private String vendor() {
        String result = "";
        try {
            result = (dev != null) ? dev.vendor : get_string_field("vendor");
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("No vendor found");
            }
        }
        return result;
    }

    private String device_name() {
        String result = "";
        try {
            result = (dev != null) ? dev.device_name : get_string_field("device_name");
        } catch (NoSuchFieldException e) {
            if (debug > 0) {
                System.err.println("No vendor found");
            }
        }
        return result;
    }

    private int carrier_frequency() {
        int f = -1;
        try {
            f = code_class.getField("carrier_frequency_code").getInt(null);
        } catch (IllegalAccessException e) {
            System.err.println("sssssssss" + e.getMessage());
        } catch (NoSuchFieldException e) {
            System.err.println("Field carrier_frequency_code missing");
            System.err.println("Class: " + class_name);
        }
        //System.err.println(F.intValue());
        //System.err.println(f);


        return ir_code.carrier_frequency(f);//carrier_frequency(get_field());
    }

    // Present remote has toggle?
    private boolean has_toggle() {
        boolean result = false;
        if (dev != null) {
            result = dev.has_toggle();
        } else {
            try {
                result = code_class.getField("has_toggle").getBoolean(null);
            } catch (IllegalAccessException e) {
                System.err.println("Illegal access:" + e.getMessage());
            } catch (NoSuchFieldException e) {
                System.err.println("Field has_toggle missing");
                System.err.println("Class: " + class_name);
            }
        }

        if (debug > 0) {
            System.err.println("has_toggle() returned " + result);
        }
        return result;
    }

    private String format_toggle_attribute(toggletype toggle) {
        return (toggle == toggletype.no_toggle || !has_toggle())
                ? ""
                : "\" toggle=\"" + toggle;
    }

    private Document dom(int command, toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        Element e = null;

        if (toggle == toggletype.do_toggle && has_toggle()) {
            e = doc.createElement("toggle_pair");
            e.setAttribute("command", ir_code.command_name(command));
            doc.appendChild(e);
            e.appendChild(element(command, toggletype.toggle_0, raw, doc));
            e.appendChild(element(command, toggletype.toggle_1, raw, doc));
        } else if (command == -1) {
            e = null;
        } else {
            doc.appendChild(element(command, toggle, raw, doc));
        }
        return doc;
    }

    private Element element(int command, toggletype toggle, boolean raw, Document doc) {
        Element e = null;
        if (toggle == toggletype.do_toggle && has_toggle()) {
            e = doc.createElement("toggle_pair");
            e.setAttribute("command", ir_code.command_name(command));
            e.appendChild(element(command, toggletype.toggle_0, raw, doc));
            e.appendChild(element(command, toggletype.toggle_1, raw, doc));
        } else if (command == -1) {
            e = null;
        } else {
            e = doc.createElement("code");
            e.setAttribute("vendor", vendor());
            e.setAttribute("device", device_name());
            e.setAttribute("command", ir_code.command_name(command));
            if (toggle == toggletype.no_toggle || !has_toggle()); else {
                e.setAttribute("toggle", harcutils.format_toggle(toggle));
            }
            Element ccf = doc.createElement("ccf");
            ccf.appendChild(doc.createTextNode(ccf_string(command, toggle, raw)));
            e.appendChild(ccf);
        }
        return e;
    }

    private Element element(short device, short command, toggletype toggle,
            boolean raw, Document doc) {
        Element e = null;
        if (toggle == toggletype.do_toggle && has_toggle()) {
            e = doc.createElement("toggle_pair");
            e.setAttribute("command", ir_code.command_name(command));
            e.appendChild(element(device, command, toggletype.toggle_0, raw, doc));
            e.appendChild(element(device, command, toggletype.toggle_1, raw, doc));
        } else if (command == -1) {
            e = null;
        } else {
            e = doc.createElement("code");
            //e.setAttribute("device", (new Integer(device)).toString());
            e.setAttribute("command", (new Integer(command)).toString());
            if (toggle == toggletype.no_toggle || !has_toggle()); else {
                e.setAttribute("toggle", harcutils.format_toggle(toggle));
            }
            Element ccf = doc.createElement("ccf");
            e.appendChild(ccf);
            ccf.appendChild(doc.createTextNode(ccf_string(device, command, toggle, raw)));
        }
        return e;
    }

    private Document dom(short device, short command, toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        doc.appendChild(element(device, command, toggle, raw, doc));
        return doc;
    }

    private Element element(char house, short device /* 1-based */,
            int command, toggletype toggle, boolean raw,
            Document doc) {
        if (has_toggle()) {
            System.err.println("I am not programmed to handle three argument remotes with toggles.");
            System.exit(111);
        }
        // device+1 is ad hoc, for intertechno.

        Element e = null;

        if (command != -1) {
            e = doc.createElement("code");
            //e.setAttribute("remote", name());
            //e.setAttribute("device", (new Integer(device)).toString());
            //e.setAttribute("house", "" + house);
            e.setAttribute("command", ir_code.command_name(command));
            if (toggle == toggletype.no_toggle || !has_toggle()); else {
                e.setAttribute("toggle", harcutils.format_toggle(toggle));
            }
            Element ccf = doc.createElement("ccf");
            e.appendChild(ccf);
            ccf.appendChild(doc.createTextNode(ccf_string(house, device, command, toggle, raw)));
        }
        return e;
    }

    private Document dom(char house, short device /* 1-based */,
            String commandname, toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        Element e = element(house, device, commandname, toggle, raw, doc);
        doc.appendChild(e);
        return doc;
    }

    private Element element(char house, short device /*1-based*/, toggletype toggle,
            boolean raw, Document doc) {
        if (has_toggle()) {
            System.err.println("I am not programmed to handle three argument remotes with toggles.");
            System.exit(111);
        }
        // cheating...
        // device+1 is ad hoc, for intertechno.

        Element e = doc.createElement("device");
        //e.setAttribute("remote", name());
        e.setAttribute("device", (new Integer(device)).toString());
        e.appendChild(element(house, device, commandnames.cmd_power_on, toggle, raw, doc));
        e.appendChild(element(house, device, commandnames.cmd_power_off, toggle, raw, doc));
        return e;
    }

    private Document dom(char house, short device /* 1-based */,
            toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        Element e = element(house, device, toggle, raw, doc);
        doc.appendChild(e);
        return doc;
    }

    private Element element(char house, toggletype toggle, boolean raw, Document doc) {
        Element e = doc.createElement("house");
        e.setAttribute("house=", "" + house);
        //e.setAttribute("remote", name());
        e.setAttribute("has_toggle", "false");

        for (short device = 1; device <= max_device; device++) {
            e.appendChild(element(house, device, toggle, raw, doc));
        }

        return e;
    }

    private Element house_element(char house, toggletype toggle, boolean raw, Document doc) {
        Element e;
        e = doc.createElement("house");
        e.setAttribute("house", "" + house);
        //e.setAttribute("remote", name());
        //e.setAttribute("has_toggle", "false");

        for (short device = 1; device <= max_device; device++) {
            e.appendChild(element(house, device, toggle, raw, doc));
        }

        return e;
    }

    private Document dom_house(char house, toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        doc.appendChild(house_element(house, toggle, raw, doc));
        return doc;
    }

    private Element element(char house, short device/*1-based*/,
            String command, toggletype toggle, boolean raw,
            Document doc) {
        return element(house, device, ir_code.decode_command(command),
                toggle, raw, doc);
    }

    public Document dom(toggletype toggle, boolean raw) {
        Document doc = harcutils.newDocument();
        Element remote = null;

        switch (no_args) {
            case 1:
                remote = doc.createElement("remote");
                doc.appendChild(remote);
                remote.setAttribute("name", name());
                remote.setAttribute("vendor", vendor());
                remote.setAttribute("device", device_name());
                remote.setAttribute("has_toggle", has_toggle() ? "true" : "false");
                for (int i = 0; i < commands.length; i++) {
                    remote.appendChild(element(commands[i], toggle, raw, doc));
                }
                break;
            case 2:
                remote = doc.createElement("code_scheme");
                doc.appendChild(remote);
                remote.setAttribute("name", name());
                remote.setAttribute("has_toggle", has_toggle() ? "true" : "false");

                for (short dev = 0; dev <= max_device; dev++) {
                    Element device = doc.createElement("device");
                    device.setAttribute("device", (new Integer(dev)).toString());
                    remote.appendChild(device);
                    for (short cmd = 0; cmd <= max_command; cmd++) {
                        device.appendChild(element(dev, cmd, toggle, raw, doc));
                    }
                }

                break;
            case 3:
                remote = doc.createElement("remote");
                doc.appendChild(remote);
                remote.setAttribute("name", name());
                remote.setAttribute("has_toggle", has_toggle() ? "true" : "false");
                for (int ihouse = 0; ihouse <= max_house; ihouse++) {
                    remote.appendChild(house_element((char) (ihouse + (int) 'A'), toggle, raw, doc));
                }
                break;
            default:
                System.err.println("this cannot happen");
                break;
        }
        return doc;
    }

    private String rem_timings_string() throws NoSuchMethodException {
        String result = "** No timings found";
        try {
            result = (String) code_class.getMethod("get_rem_timings", (Class[]) null).invoke((Object) null, (Object[]) null);
        } catch (IllegalAccessException e) {
            System.err.println("IllegalAccessException");
        } catch (java.lang.reflect.InvocationTargetException e) {
            //System.err.println("InvocationTargetException");
        }
        return result;
    }

    public String rem_string_compressed(toggletype toggle, boolean raw) {
        try {
            String result = "[REMOTE]\n  [NAME]" + name() + "\n\n[TIMING]\n  " + rem_timings_string() + "\n\n[COMMANDS]\n";
            for (int i = 0; i < commands.length; i++) {
                if (ir_code.command_name(commands[i]).length() > irtrans.max_name_length) {
                    System.err.println("WARNING: name " + ir_code.command_name(commands[i]) + " too long (" + ir_code.command_name(commands[i]).length() + ")");
                }
                if (toggle == toggletype.do_toggle && has_toggle()) {
                    // FIXME
                    // Generate both toggles as different commands
                    result = result + " [" + ir_code.command_name(commands[i])
                            + "][CCF]" + ccf_string(commands[i], toggletype.toggle_0, raw) + '\n';
                    result = result + " [" + ir_code.command_name(commands[i])
                            + "_1][CCF]" + ccf_string(commands[i], toggletype.toggle_1, raw) + '\n';
                } else {
                    result = result + "  [" + ir_code.command_name(commands[i]) + "]" + rem_code_string(commands[i], toggle) + "\n";
                }
            }
            return result;
        } catch (NoSuchMethodException e) {
            System.err.println("Warning: compressed rem not possible, falling back to CCF format " + e.getMessage());
            return rem_string_ccf(toggle, raw);
        }
    }

    public String rem_string_ccf(toggletype toggle, boolean raw) {
        String result = "[REMOTE]\n [NAME]" + name() + "\n\n[COMMANDS]\n";
        for (int i = 0; i < commands.length; i++) {
            if (toggle == toggletype.do_toggle && has_toggle()) {
                // Generate both toggles as different commands
                result = result + " [" + ir_code.command_name(commands[i])
                        + "][CCF]" + ccf_string(commands[i], toggletype.toggle_0, raw) + '\n';
                result = result + " [" + ir_code.command_name(commands[i])
                        + "_1][CCF]" + ccf_string(commands[i], toggletype.toggle_1, raw) + '\n';
            } else {
                result = result + " [" + ir_code.command_name(commands[i])
                        + "][CCF]" + ccf_string(commands[i], toggle, raw) + '\n';
            }
        }
        return result;
    }

    public String rem_string_ccf(char house, toggletype toggle, boolean raw) {
        // again, no toggles assumed
        // device+1 is ad hoc, for intertechno.
        String result = "[REMOTE]\n [NAME]" + name() + "_" + house + "\n\n[COMMANDS]\n";

        for (short device = 1; device <= max_device; device++) {
            for (int i = 0; i < commands.length; i++) {
                result = result + " [" + device + ir_code.command_name(commands[i]) + "][CCF]" + ccf_string(house, device, commands[i], toggle, raw) + '\n';
            }
        }

        return result;
    }

    public String commands_string(String separator) {
        String result = ir_code.command_name(commands[0]);
        for (int i = 1; i < commands.length; i++) {
            result = result + separator + ir_code.command_name(commands[i]);
        }
        return result;
    }

    // TODO: make the rest of this file into a class command_processor.
    public static final int list_commands = 0;
    public static final int print_ccf = 1;
    public static final int print_info = 3;
    public static final int print_xml = 4;
    public static final int print_rem = 5;
    public static final int send_globalcache = 6;
    public static final int send_irtrans_udp = 7;
    public static final int send_irtrans_ascii = 8;
    public static final int send_irtrans_http = 9;
    public static final int print_lirc = 12;
    public static final int send_lirc = 13;
    public static final int send_ezcontrol_http = 14;
    public static final int cmd_invalid = -1;

    public static int parse_cmd(String cmd) throws NoSuchMethodException {
        int c =
                cmd.equals("commands") ? list_commands : cmd.equals("ccf") ? print_ccf : cmd.equals("info") ? print_info : cmd.equals("xml") ? print_xml : cmd.equals("rem") ? print_rem : cmd.equals("globalcache") ? send_globalcache : cmd.equals("irtrans_udp") ? send_irtrans_udp : cmd.equals("irtrans_ascii") ? send_irtrans_ascii : cmd.equals("irtrans_http") ? send_irtrans_http : cmd.equals("lirc_conf") ? print_lirc : cmd.equals("lirc") ? send_lirc : cmd.equals("ezcontrol") ? send_ezcontrol_http : cmd_invalid;
        if (c == cmd_invalid) {
            throw new NoSuchMethodException("No command " + cmd + " exists.");
        }

        return c;
    }

    private static void usage() {
        System.err.println("Usage:\n" + " <cmd> [<cmd_option>]* <remote> [command_name]\n" + "or\n" + " <cmd> [<cmd_option>]* <ir_class> [device_no] [command_no]\n" + "or\n" + " <cmd> [<cmd_option>]* <ir_class> [house_letter] [device_no] [command_name]\n" + "\nwhere cmd=commands,ccf,info,xml,rem,sendccf,lirc_conf,globalcache,irtrans_udp,irtrans_ascii,irtrans_http,lirc,ezcontrol\n" + "and\n" + "cmd_option=-o <filename>,-f <input_filename>,-x <device_filename>,-c connector,-m module,-# <count>,-l led,-h hostname,-v,-r,-t[0,1],-d");
    }

    public static boolean process_args(String[] args) {
        return process_args((String) null, args);
    }

    public static boolean process_args(String remote, String[] args) {

        String filename = null;
        String input_filename = null;
        String device_filename = null;
        String hostname = null;
        String remote_name = null;
        String[] remote_args = null;
        int connector = -1;
        int module = 2;
        int count = 1;
        toggletype toggle = toggletype.no_toggle;
        boolean verbose = false;
        boolean raw = false;

        int arg_i = 0;

        try {
            // Decode command
            int cmd = parse_cmd(args[arg_i++]);

            // Decode options
            while (arg_i < args.length && args[arg_i].charAt(0) == '-') {
                if (args[arg_i].equals("-o")) {
                    filename = args[arg_i + 1];
                    arg_i += 2;
                } else if (args[arg_i].equals("-f")) {
                    input_filename = args[arg_i + 1];
                    arg_i += 2;
                } else if (args[arg_i].equals("-x")) {
                    device_filename = args[arg_i + 1];
                    arg_i += 2;
                } else if (args[arg_i].equals("-h")) {
                    hostname = args[arg_i + 1];
                    arg_i += 2;
                } else if (args[arg_i].equals("-l") || args[arg_i].equals("-c")) {
                    connector = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-m")) {
                    module = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-#")) {
                    count = Integer.parseInt(args[arg_i + 1]);
                    arg_i += 2;
                } else if (args[arg_i].equals("-v")) {
                    verbose = true;
                    arg_i++;
                } else if (args[arg_i].equals("-r")) {
                    raw = true;
                    arg_i++;
                } else if (args[arg_i].equals("-d")) {
                    debug++;
                    arg_i++;
                } else if (args[arg_i].equals("-t")) {
                    toggle = toggletype.do_toggle;
                    arg_i++;
                } else if (args[arg_i].equals("-t0")) {
                    toggle = toggletype.toggle_0;
                    arg_i++;
                } else if (args[arg_i].equals("-t1")) {
                    toggle = toggletype.toggle_1;
                    arg_i++;
                } else {
                    throw new NoSuchMethodException("Unknown option");
                }
            }

            // Decode remote name
            if (remote == null) {
                remote_name = args[arg_i++];
            } else {
                remote_name = remote;
            }

            if (arg_i < args.length) {
                remote_args = new String[args.length - arg_i];
                for (int i = 0; i < args.length - arg_i; i++) {
                    remote_args[i] = args[arg_i + i];
                }
            }

            process_args(cmd, filename, hostname, module, connector, verbose,
                    toggle, remote_name, remote_args, count, raw,
                    input_filename, device_filename);

        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
            System.exit(1);
            return false;
        } catch (NoSuchMethodException e) {
            usage();
            System.exit(2);
            return false;
        } catch (NumberFormatException e) {
            usage();
            System.exit(3);
            return false;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(4);
            return false;
        }
        return true;
    }

    private static void print(String filename, String payload)
            throws FileNotFoundException {
        if (filename != null) {
            FileOutputStream fos = new FileOutputStream(filename, false);
            PrintStream ps = new PrintStream(fos);
            ps.print(payload);
            System.err.println("File " + filename + " written.");
        } else {
            System.out.print(payload);
        }
    }

    // This procedure determines what type (1, 2, 3 parameter) the
    // remote is, constructs the appropriate remote classobject, and
    // dispatches to the 1-, 2-, or 3-parameter process_args.
    private static void process_args(int cmd, String filename, String hostname,
            int module, int connector, boolean verbose,
            toggletype toggle,
            String remote_name, String[] ir_args,
            int count, boolean raw,
            String input_filename,
            String device_filename)
            throws IOException {

        if (debug > 0) {
            System.err.println(
                    "This is the dispatching process_args.\n" + " cmd = " + cmd + ", filename = " + filename + ", hostname = " + hostname + ", module = " + module + ", connector = " + connector + ", verbose = " + verbose + ", toggle = " + toggle + ", raw = " + raw + ", remote_name = " + remote_name + ", input_filename = " + input_filename + ", device_filename = " + device_filename + ", ir_args = " +
                    (ir_args == null ? "null" : (ir_args.length == 1) ? ("{" + ir_args[0] + "}") : (ir_args.length == 2) ? ("{" + ir_args[0] + "," + ir_args[1] + "}") : (ir_args.length == 3) ? ("{" + ir_args[0] + "," + ir_args[1] + "," + ir_args[2] + "}") : (ir_args.length > 3) ? ("{" + ir_args[0] + "," + ir_args[1] + "," + ir_args[2] + ", ... <more> }")
                    : ir_args.toString()) + ", debug = " + debug + ", count = " + count);
        }

        boolean done = false;
        remote r;

        try {
            r = (device_filename != null)
                    ? new remote(ir_code.package_name + ".device",
                    device_filename, remote_name)
                    : new remote(ir_code.package_name + "." + remote_name,
                    input_filename);
            //if (device_filename != null)
            //r.device_name = remote_name;// BARF!

            if (debug > 0) {
                if (r.dev != null) {
                    System.err.println("Instantiating device \"" + r.device_name + "\" (remote " + remote_name + ") succeded");
                } else {
                    System.err.println("Instantiating " + remote_name + " succeded");
                }
            }

            done = true;
            // ... yes, it worked, dispatch
            switch (r.no_args) {
                case 1:
                    process_args(cmd, filename, hostname, module, connector, verbose,
                            toggle, r, ir_args == null ? null : ir_args[0],
                            count, raw);
                    break;
                case 2:
                    short dev = ir_args == null || ir_args.length < 1
                            ? -1 : Short.parseShort(ir_args[0]);
                    short com = ir_args == null || ir_args.length < 2
                            ? -1 : Short.parseShort(ir_args[1]);
                    process_args(cmd, filename, hostname, module, connector, verbose,
                            toggle, r, dev, com, count, raw);
                    break;

                case 3:
                    process_args(cmd, filename, hostname, module, connector, verbose,
                            toggle, r,
                            ir_args == null || ir_args.length < 1
                            ? '0' : ir_args[0].charAt(0),
                            ir_args == null || ir_args.length < 2
                            ? -1 : Short.parseShort(ir_args[1]),
                            ir_args == null || ir_args.length < 3
                            ? null : ir_args[2],
                            count, raw);
                    break;
                default:
                    System.err.println("this cannot happen");
                    break;
            }
        } catch (NoSuchFieldException e) {
            System.err.println("Field " + e.getMessage() + " not found.");
        } catch (java.lang.ClassNotFoundException e) {
            if (debug > 0) {
                System.err.println("Remote " + remote_name + " does not exist." + e.getMessage());
            } else {
                System.err.println("Remote " + remote_name + " does not exist.");
            }
            return;
        } catch (NoSuchMethodException e) {
            System.err.println("Method not found: " + e.getMessage()); // cannot happen
        }

        if (!done) {
            System.err.println("Giving up on remote " + remote_name);
            System.exit(42);
        }
    }

    // Handle a one argument remote
    private static void process_args(int cmd, String filename, String hostname,
            int module, int connector, boolean verbose,
            toggletype toggle, remote r, String ir_arg,
            int count, boolean raw) {
        try {
            switch (cmd) {

                case list_commands:
                    print(filename, r.commands_string("\n") + "\n");
                    break;
                case print_ccf:
                    if (ir_arg == null) {
                        throw new NullPointerException("ccf requires an argument");
                    }

                    print(filename, r.ccf_string(ir_arg, toggle, raw) + "\n");
                    break;
                case print_info:
                    print(filename,
                            "Device: " + r.device_name() + "; " + ((r.dev == null)
                            ? ("Remote (one-parameter): " + r.name() + "; ")
                            : "") + "Vendor: " + r.vendor() /*
                            + "; Has Toggle: " + r.has_toggle()
                            + "; Freq.: " + r.carrier_frequency()
                            + "; #commands: " + r.commands.length
                            //+ r.commands_string("\n")
                             */ + ".\n");
                    break;
                case print_xml:
                    if (ir_arg == null) {
                        harcutils.printDOM(filename, r.dom(toggle, raw),
                                doctype_systemid);
                    } else {
                        harcutils.printDOM(filename,
                                r.dom(ir_code.decode_command(ir_arg), toggle,
                                raw), doctype_systemid);
                    }
                    break;
                case print_rem:
                    if (raw) {
                        print(filename, r.rem_string_ccf(toggle, false));
                    } else {
                        print(filename, r.rem_string_compressed(toggle, false));
                    }
                    break;
                case print_lirc:
                    print(filename, r.lirc_string(toggle, raw));
                    break;
                case send_globalcache:
                    if (toggle == toggletype.do_toggle && r.has_toggle()) // nonsense...
                    {
                        throw new NoSuchMethodException("-t makes no sense with globalcache");
                    }
                    if (ir_arg == null) {
                        usage();
                    }

                    globalcache gc = new globalcache(hostname, verbose);
                    gc.send_ir(r.ccf_string(ir_arg, toggle, true), module, connector, count);
                    break;
                case send_irtrans_udp:
                     {
                        if (ir_arg == null) {
                            usage();
                        }
                        irtrans it = new irtrans(hostname, verbose);
                        for (int i = 1; i <= count; i++) {
                            it.send_ir(r.ccf_string(ir_arg, toggle, false),
                                    connector, i != 1);
                        }
                    }
                    break;
                case send_irtrans_ascii:
                    if (ir_arg == null) {
                        usage();
                    }
                    // if -t1, assume rem-files from us are used
                    irtrans it = new irtrans(hostname, verbose);
                    for (int i = 1; i <= count; i++) {
                        try {
                            Thread.currentThread().sleep(10);
                        } catch (InterruptedException e) {
                        }
                        //System.out.println("" + i + "\n" +  count);
                        it.send_flashed_command(r.getremotename(ir_arg),
                                ir_arg + ((toggle == toggletype.toggle_1)
                                ? "_1" : ""),
                                connector, i != 1);
                    }
                    break;
                case send_irtrans_http:
                    if (ir_arg == null) {
                        usage();
                    }
                    // if -t1, assume rem-files from us are used
                    String url = irtrans.make_url(hostname, r.name(),
                            ir_arg + ((toggle == toggletype.toggle_1)
                            ? "_1"
                            : ""),
                            connector);
                    if (verbose) {
                        System.out.println("Getting URL " + url);
                    }
                    try {
                        for (int i = 1; i <= count; i++) {
                            (new URL(url)).getContent();
                        }

                    } catch (java.net.MalformedURLException e) {
                        System.err.println(e.getMessage());
                    } catch (java.io.IOException e) {
                        System.err.println(e.getMessage());
                    }
                    break;
                case send_lirc:
                    if (ir_arg == null) {
                        usage();
                    }
                    // only SEND_ONCE presently implemented
                    lirc lrc = new lirc(hostname, verbose);
                    if (raw) {
                        lrc.send_ccf(r.ccf_string(ir_arg, toggle, true), count);
                    } else {
                        lrc.send_ir(r.name(), ir_arg, count);
                    }
                    break;
                case send_ezcontrol_http:
                    throw new NoSuchMethodException("ezcontrol cannot do this");
                default:
                    throw new NoSuchMethodException("Command not understood or meaningful");
            }
        } catch (NoSuchMethodException e) {
            // User asking for not implemented command
            System.err.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Could not write file: " + e.getMessage());
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception");
        } catch (IOException e) {
            System.err.println("IO exception");
        }
    }

    // handle a two argument remote
    private static void process_args(int cmd, String filename, String hostname,
            int module, int connector,
            boolean verbose,
            toggletype toggle,
            remote r, short ir_arg0, short ir_arg1,
            int count, boolean raw) {
        try {
            switch (cmd) {

                case list_commands:
                    System.err.println("A command consists of two integers, the first between 0 and " + r.max_device + " (\"device\"), the second between 0 and " + r.max_command + " (\"command\").");
                    break;
                case print_ccf:
                    if (ir_arg0 == -1 || ir_arg1 == -1) {
                        throw new NullPointerException("ccf without parameters senseless");
                    }
                    print(filename,
                            r.ccf_string(ir_arg0, ir_arg1, toggle, raw) + "\n");
                    break;
                case print_info:
                    print(filename,
                            "Remote (two-parameter): " + r.name() + "; Vendor: " + r.vendor() + "; Device: " + r.device_name() + "; Toggle: " + r.has_toggle() + "; Freq.: " + r.carrier_frequency() + "\nDevices: 0 -- " + r.max_device + "\nCommands: 0 -- " + r.max_command + "\n");
                    break;
                case print_xml:
                    if (ir_arg0 == -1) {
                        harcutils.printDOM(filename, r.dom(toggle, raw),
                                doctype_systemid);
                    } else {
                        harcutils.printDOM(filename, r.dom(ir_arg0, ir_arg1, toggle, raw),
                                doctype_systemid);
                    }
                    break;
                case print_rem:
                    throw new NoSuchMethodException("Command rem not implemented for two parameter remotes.");
                //break;
                case send_globalcache:
                    globalcache gc = new globalcache(hostname, verbose);
                    gc.send_ir(r.ccf_string(ir_arg0, ir_arg1, toggle, true),
                            module, connector, count);
                    break;
                case send_irtrans_udp:
                     {
                        // Toggle codes not implemented, except for RC5
                        irtrans it = new irtrans(hostname, verbose);
                        for (int i = 1; i <= count; i++) {
                            it.send_ir(r.ccf_string(ir_arg0, ir_arg1, toggle, false),
                                    connector, i != 1);
                        }
                    }
                    break;
                case send_irtrans_http:
                case send_irtrans_ascii:
                case send_ezcontrol_http:
                    System.err.println("Command not implemented for two parameter remotes.");
                    break;
                default:
                    throw new NoSuchMethodException("No such command");
            }
        } catch (NoSuchMethodException e) {
            // User asking for not implemented command
            System.err.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Could not write file: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception");
        } catch (IOException e) {
            System.err.println("IO exception");
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
        }
    }

    // right now, do not do this more general than we need,
    // i.e. intertechno support. House letters are compatible with X10 though.
    // device numbers are 1-based.
    private static void process_args(int cmd, String filename, String hostname,
            int module, int connector,
            boolean verbose,
            toggletype toggle,
            remote r,
            char house,
            short device,
            String command,
            int count, boolean raw) {
        if (debug > 0) {
            System.err.println("This is the three-parameter process_args.\n" + "house = " + house + ", device = " + device + ", command = " + command);
        }

        try {
            switch (cmd) {

                case list_commands:
                    // A to P are hard coded...
                    System.err.println("A command consists of three parameters, the first a letter between 'A'  and 'P, the second an integer between 1 and " + r.max_device + " (\"device\"), the third a string from the command set.");
                    print(filename, r.commands_string("\n") + "\n");
                    break;
                case print_ccf:
                    if (device < 1 || house == '0' || command == null) {
                        throw new NullPointerException("No sensible parameters for ccf");
                    }
                    print(filename,
                            r.ccf_string(house, device, command, toggle, raw) + "\n");
                    break;
                case print_info:
                    print(filename,
                            "Remote (three-parameter): " + r.name() + "; Vendor: " + r.vendor() + "; Device: " + r.device_name() + "; Toggle: " + r.has_toggle() + "; Freq.: " + r.carrier_frequency() + "\nHouses: 'A' -- 'P'" // am I cheating...
                            + "\nDevices: 1 -- " + r.max_device + "\nCommands: { " + r.commands_string(", ") + " }\n");
                    break;
                case print_xml:
                    if (house == '0') // print everything
                    {
                        harcutils.printDOM(filename, r.dom(toggle, false), doctype_systemid);
                    } else if (device == -1) // all codes for the selected house
                    {
                        harcutils.printDOM(filename, r.dom_house(house, toggle, false), doctype_systemid);
                    } else if (command == null) // all codes for select house and device
                    {
                        harcutils.printDOM(filename, r.dom(house, device, toggle, false), doctype_systemid);
                    } else {
                        harcutils.printDOM(filename,
                                r.dom(house, device, command, toggle, false), doctype_systemid);
                    }
                    break;
                case print_rem:
                    if (house != '0' && device == -1 && command == null) {
                        print(filename, r.rem_string_ccf(house, toggle, false));
                    } else {
                        throw new NoSuchMethodException("Command rem requires a house code, but no device nor command.");
                    }
                    break;
                case send_globalcache:
                    globalcache gc = new globalcache(hostname, verbose);
                    gc.send_ir(r.ccf_string(house, device, command, toggle, true),
                            module, connector, count);
                    break;
                case send_irtrans_udp:
                     {
                        // Toggle codes not implemented, except for RC5
                        // repeats not implemented, since presently not needed
                        irtrans it = new irtrans(hostname, verbose);
                        it.send_ir(r.ccf_string(house, device,
                                command, toggle, false),
                                connector, false);
                    }
                    break;
                case send_irtrans_ascii:
                    // Assume command has been flashed with rem file generated
                    // by this program.
                    // repeats not implemented, since presently not needed
                    irtrans it = new irtrans(hostname, verbose);
                    for (int i = 1; i <= count; i++) {
                        it.send_flashed_command(r.name() + "_" + house,
                                device + command, connector, false);
                    }
                    break;
                case send_irtrans_http:
                    String url = irtrans.make_url(hostname, r.name() + "_" + house,
                            device + command, connector);
                    if (verbose) {
                        System.out.println("Getting URL " + url);
                    }
                    try {
                        (new URL(url)).getContent();

                    } catch (java.net.MalformedURLException e) {
                        System.err.println(e.getMessage());
                    } catch (java.io.IOException e) {
                        System.err.println(e.getMessage());
                    }
                    break;
                case send_ezcontrol_http:
                    try {
                        url = ezcontrol.make_url(hostname, r.name(), "" + house,
                                device, command);
                        if (verbose) {
                            System.out.println("Getting URL " + url);
                        }
                        (new URL(url)).getContent();
                    } catch (non_existing_command_exception e) {
                        System.err.println(e.getMessage());
                    } catch (java.net.MalformedURLException e) {
                        System.err.println(e.getMessage());
                    } catch (java.io.IOException e) {
                        System.err.println(e.getMessage());
                    }
                    break;
                default:
                    throw new NoSuchMethodException("No such command");
            }
        } catch (NoSuchMethodException e) {
            // User asking for not implemented command
            System.err.println(e.getMessage());
        } catch (FileNotFoundException e) {
            System.err.println("Could not write file: " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println("Unknown host exception");
        } catch (IOException e) {
            System.err.println("IO exception");
        } catch (NullPointerException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        process_args(args);
    }
}
