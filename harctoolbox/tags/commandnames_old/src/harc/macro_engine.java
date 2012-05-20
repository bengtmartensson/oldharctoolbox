/**
 * An implementation of my own macro engine.
 */

// TODO: macro arguments.
package harc;

import java.io.*;
import org.w3c.dom.*;

public class macro_engine {

    private home the_home;
    private Document doc = null;
    private Element root = null;
    private NodeList macros = null;
    private debugargs db = null;

    private String indentation(int level) {
        String res = "";
        for (int i = 0; i < level; i++) {
            res = res + "   ";
        }
        return res;
    }

    private void debug(boolean doit, String message, int level, boolean terminate_line) {
        if (doit) {
            System.err.print(indentation(level) + message + (terminate_line ? "\n" : ""));
        }
    }

    private void debug(boolean doit, String message, int level) {
        debug(doit, message, level, true);
    }

    public void set_debug(int arg) {
        db.set_state(arg);
    }

    public macro_engine(String filename, home the_home, int debug) throws IOException {
        this.the_home = the_home;
        db = new debugargs(debug);
        doc = harcutils.open_xmlfile(filename);
        root = doc.getDocumentElement();
        macros = root.getElementsByTagName("macro");
    }

    public macro_engine(home hm) throws IOException {
        this(harcprops.get_instance().get_macro_file(), hm, 0);
    }
    
    private String[] get_macros(NodeList macros_nl, boolean include_description) {
        int len = 0;
        for (int i = 0; i < macros_nl.getLength(); i++) {
            if (!((Element) macros_nl.item(i)).getAttribute("visibility").equals("private"))
                len++;
        }

        String[] result = new String[len];
        int pos = 0;
        for (int i = 0; i < macros_nl.getLength(); i++) {
            Element mac = (Element) macros_nl.item(i);
            if (!mac.getAttribute("visibility").equals("private")) {
                result[pos] = mac.getAttribute("name");
                if (include_description) {
                    NodeList nl = mac.getElementsByTagName("description");
                    if (nl != null && nl.item(0) != null) {
                        result[pos] = result[pos] + ":\t" + nl.item(0).getFirstChild().getNodeValue();
                    }
                }
                pos++;
            }
        }
        return result;
    }

    /**
     *
     * @param include_description If true, description will be added to the name
     * @return an array of non-private macro names.
     */
    public String[] get_macros(boolean include_description) {
        return get_macros(macros, include_description);
    }

    public String[] get_macros(String parentfolder) {
        NodeList folders = root.getElementsByTagName("macros");
        for (int i = 0; i < folders.getLength(); i++) {
            Element el = (Element) folders.item(i);
             if (el.getAttribute("name").equals(parentfolder))
                 return get_macros(el.getElementsByTagName("macro"), false);
        }
        return null;
    }

    private String[] get_folders(Element parent) {
        NodeList nl = parent.getChildNodes();
        int len = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nl.item(i);
                if (el.getTagName().equals("macros") && !el.getAttribute("visibility").equals("private"))
                    len++;
            }
        }

        String[] result = new String[len];
        int pos = 0;

        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) nl.item(i);
                if (el.getTagName().equals("macros") && !el.getAttribute("visibility").equals("private"))
                    result[pos++] = el.getAttribute("name");
            }
        }
        return result;
    }

    private String[] get_folders(String parentname, int level, Element e, int e_level) {
        // System.err.println("**" + (e_level > 0 ? "**" : "") + (e_level > 1 ? "**" : "") + (e_level > 2 ? "**" : "") + (e_level > 3 ? "**" : "") + (e_level > 0 ? "**" : "") + e.getAttribute("name") + e_level);
        if (e.getAttribute("name").equals(parentname) && level == e_level)
            return get_folders(e);

        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) children.item(i);
                if (el.getTagName().equals("macros")) {
                    String[] s = get_folders(parentname, level, el, e_level + 1);
                    if (s != null)
                        return s;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param parentname Name of macro folder to be investigated.
     * @param level Depth of sought folder, counting 0 as root.
     * @return Array of strings of contained macro folders.
     */
    public String[] get_folders(String parentname, int level) {
        return get_folders(parentname, level, root, 0);
    }

    public String[] get_folders() {
        return get_folders(root.getAttribute("name"), 0);
    }


    private String eval_command(Element ele, int level, String execute_name,
            boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        NodeList argnodes = ele.getElementsByTagName("argument");
        String[] args = new String[argnodes.getLength()];
        for (int j = 0; j < argnodes.getLength(); j++) {
            args[j] = ((Element) argnodes.item(j)).getTextContent();
        }
        String command = ele.getAttribute("command");
        String dev = ele.getAttribute("device");
        int count = Integer.parseInt(ele.getAttribute("count"));
        toggletype toggle = harcutils.decode_toggle(ele.getAttribute("toggle"));
        debug(db.trace_commands(), "command: " + dev + " " + command + " " + count + " toggle: " + toggle + ", #args = " + args.length, level);
        int cmd = ir_code.decode_command(command);
        if (cmd == commandnames.cmd_invalid) {
            throw new non_existing_command_exception(command);
        }
        String output;
        //	try {
        output = the_home.do_command(dev, cmd, args, commandset.any, count, toggle, false);
// 	} catch (non_existing_command_exception e) {
// 	    throw new non_existing_command_exception("Device " + dev + " has no such command: " + command);
// 	}
        return output;
    }

    private String eval_select_src(Element ele, int level, String execute_name,
            boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        String dev = ele.getAttribute("device");
        String src = ele.getAttribute("src");
        String zone = ele.getAttribute("zone");
        mediatype the_mediatype = mediatype.valueOf(ele.getAttribute("mediatype"));
        String connectiontype = ele.getAttribute("connectiontype");
        if (connectiontype.equals("")) {
            connectiontype = null;
        }
        debug(db.trace_commands(), "select-src: " + dev + " " + src + (zone.equals("") ? "" : (" zone " + zone)) + ", " + the_mediatype + "; " + connectiontype, level);
        the_home.select(dev, src, commandset.any, zone, the_mediatype, connectiontype);
        return "";
    }

    private String eval_delay(Element ele, int level, String execute_name,
            boolean no_execute) {
        int del = Integer.parseInt(ele.getAttribute("duration"));
        debug(db.misc(), "delay " + del + "ms...", level, false);
        try {
            Thread.currentThread().sleep(del);
        } catch (InterruptedException e) {
        }
        debug(db.misc(), "done", 0);
        return "";
    }

    private String eval_message(Element ele, int level, String execute_name,
            boolean no_execute) {
        debug(db.misc(), "message: ", level, false);
        System.out.println(ele.getFirstChild().getNodeValue());
        return ele.getFirstChild().getNodeValue();
    }

    private String eval_code(Element ele, int level, String execute_name,
            boolean no_execute) {
        String cmd = ele.getFirstChild().getNodeValue();
        boolean wait = true;
        debug(db.misc(), "code: " + cmd + (wait ? " (wait)" : " (nowait)"), level, true);

        try {
            java.lang.Process proc = java.lang.Runtime.getRuntime().exec(cmd);
            if (wait) {
                proc.waitFor();
            }
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
        return "";
    }

    private String eval_cond(Element ele, int level, String execute_name,
            boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        debug(db.conds(), "Wow, got a cond", level);
        String output = null;
        NodeList clauses = ele.getElementsByTagName("clause");
        boolean found = false;
        int clause_no = 0;
        for (int j = 0; j < clauses.getLength() && !found; j++) {
            int indx = -1;
            Element clause = (Element) clauses.item(j);
            NodeList clausechildren = clause.getChildNodes();
            do {
                indx++;
            } while (clausechildren.item(indx).getNodeType() != Node.ELEMENT_NODE);
            Element the_test = (Element) clausechildren.item(indx);
            output = eval_element(the_test, level + 1, "yy", no_execute);

            if (output != null) {
                found = true;
                for (int k = indx + 1; k < clausechildren.getLength(); k++) {
                    if (clausechildren.item(k).getNodeType() == Node.ELEMENT_NODE) {
                        output = eval_element((Element) clausechildren.item(k), level + 1, execute_name, no_execute);
                    }
                }
            }
        }
        return output;
    }

    private String eval_macrocall(Element ele, int level, String execute_name,
            boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        String name = ele.getAttribute("macro");
        if (db.nested_macros()) {
            System.err.println(indentation(level) + "Macrocall: " + name);
        }
        String output = eval_macro(name, /*FIXME should take new arguments */ null, level + 1, no_execute);
        return output;
    }

    private String eval_element(Element ele, int level, String execute_name,
            boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        String output = null;
        if (ele.getTagName().equals("command")) {
            output = eval_command(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("select-src")) {
            output = eval_select_src(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("delay")) {
            output = eval_delay(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("message")) {
            output = eval_message(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("code")) {
            output = eval_code(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("macrocall")) {
            output = eval_macrocall(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("match")) {
            output = eval_match(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("description")) {
            debug(db.misc(), "description: " + ele.getFirstChild().getNodeValue(), level);
        } else if (ele.getTagName().equals("cond")) {
            output = eval_cond(ele, level, "xx", no_execute);
        } else if (ele.getTagName().equals("true")) {
            output = "";
        } else if (ele.getTagName().equals("false")) {
            output = null;
        } else {
            debug(db.misc(), "Unknown element (ignored): " + ele.getTagName(), level);
        }
        return output;
    }

     private Element find_macro(String macro_name) {
        Element the_macro = null;
        for (int i = 0; i < macros.getLength() && the_macro == null; i++)
            if (((Element) macros.item(i)).getAttribute("name").equals(macro_name))
                the_macro = (Element) macros.item(i);

        return the_macro;
    }

    private String get_description(Element e) {
        if (e == null)
            return null;
        
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                if (el.getTagName().equals("description"))
                    return el.getTextContent();
            }
        }
        return null;
    }

    public String describe_macro(String macro_name) {
        return get_description(find_macro(macro_name));
    }

    public String describe_folder(String folder_name) {
        NodeList folders = root.getElementsByTagName("macros");
        for (int i = 0; i < folders.getLength(); i++) {
            Element el = (Element) folders.item(i);
            if (el.getAttribute("name").equals(folder_name))
                return get_description(el);
        }
        return null;
    }

    public String eval_macro(String macro_name, String[] arguments, int level, boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        Element the_macro = find_macro(macro_name);
        if (the_macro == null)
            throw new non_existing_command_exception(macro_name);

        return eval_all_children(the_macro, arguments, level, macro_name, no_execute);
    }

    public boolean has_macro(String candidate) {
        return find_macro(candidate) != null;
    }

    private String eval_all_children(Element the_macro, String[] arguments, int level,
            String execute_name, boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        debug(db.execute(), "Entering `" + execute_name + "'", level);
        String output = null;

        // Found the macro, now execute the actions
        NodeList actions = the_macro.getChildNodes();
        for (int i = 0; i < actions.getLength(); i++) {
            if (actions.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element ele = (Element) actions.item(i);
                output = eval_element(ele, level, "xx", no_execute);
            }
        }
        debug(db.execute(), "Exiting `" + execute_name + "'", level);
        return output;
    }

    private String eval_match(Element the_test, int level, String eval_name, boolean no_execute)
            throws non_existing_command_exception, InterruptedException {
        NodeList nl = the_test.getElementsByTagName("command");
        String output;
        Element ele = (Element) nl.item(0);

        NodeList argnodes = ele.getElementsByTagName("argument");
        String[] args = new String[argnodes.getLength()];
        for (int j = 0; j < argnodes.getLength(); j++) {
            args[j] = ((Element) argnodes.item(j)).getTextContent();
        }
        String command = ele.getAttribute("command");
        String dev = ele.getAttribute("device");
        int count = Integer.parseInt(ele.getAttribute("count"));
        toggletype toggle = harcutils.decode_toggle(ele.getAttribute("toggle"));
        debug(db.trace_commands(), "(within evaluate_test) command: " + dev + " " + command + " " + count + " toggle: " + toggle + ", #args = " + args.length, level);
        int cmd = ir_code.decode_command(command);
        if (cmd == commandnames.cmd_invalid) {
            throw new non_existing_command_exception(command);
        }

        output = the_home.do_command(dev, cmd, args, commandset.any, count, toggle, false);

        String regexp = the_test.getAttribute("regexp");
        boolean ok = output != null && output.matches(regexp);
        debug(db.conds(),
                "match: command output = \"" + output + "\", pattern = \"" + regexp + "\", " + (ok ? "match" : "no match"), level);
        return ok ? "" : null;
    }

    private static void usage(int exitcode) {
        System.err.println("Usage:\n" + "macro_engine [<options>] <macroname> [<arguments>]*\n" + "where options=-d arg,-m macrofile (mandatory),-h homefile (mandatory),-v,-n,-# count");
        System.exit(exitcode);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }

    public static void main(String args[]) {
        String macro_filename = harcprops.get_instance().get_macro_file();
        String home_filename = harcprops.get_instance().get_home_file();
        int debug = 0;
        int verbose = 0;
        int count = 1;
        boolean no_execute = false;
        boolean folder_mode = false;
        //String zone = null;
        String macroname = null;
        String[] arguments = null;
        home the_home = null;
        macro_engine engine = null;

        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose++;
                } else if (args[arg_i].equals("-n")) {
                    arg_i++;
                    no_execute = true;
                } else if (args[arg_i].equals("-h")) {
                    arg_i++;
                    home_filename = args[arg_i++];
                } else if (args[arg_i].equals("-f")) {
                    arg_i++;
                    folder_mode = true;
                } else if (args[arg_i].equals("-m")) {
                    arg_i++;
                    macro_filename = args[arg_i++];
                } else if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                //} else if (args[arg_i].equals("-z")) {
                //arg_i++;
                //zone = args[arg_i++];
                } else {
                    usage();
                }
            }
            macroname = args[arg_i++];
            int no_arguments = args.length - arg_i;

            arguments = new String[no_arguments];
            System.arraycopy(args, arg_i, arguments, 0, no_arguments);
        } catch (ArrayIndexOutOfBoundsException e) {
            //if (debug_decode_args(debug))
            //System.err.println("ArrayIndexOutOfBoundsException");
            usage();
        } catch (NumberFormatException e) {
            //if (debug_decode_args(debug))
            //System.err.println("NumberFormatException");
            usage();
        }

        try {
            the_home = new home(home_filename, verbose, 0, null);
        } catch (IOException e) {
            System.err.println("Cannot open home file " + home_filename);
            System.exit(harcutils.exit_config_read_error);
        }

        try {
            engine = new macro_engine(macro_filename, the_home, debug);
        } catch (IOException e) {
            System.err.println("Cannot open macro file " + macro_filename);
            System.exit(harcutils.exit_config_read_error);
        }

        if (folder_mode) {
            harcutils.printtable("Top level folders:", engine.get_folders());
            System.out.println();
            harcutils.printtable("Content of " + macroname + " folder:",
                    engine.get_folders(macroname, Integer.parseInt(arguments[0])));
            System.exit(harcutils.exit_success);
        }

        try {
            if (macroname.equals("?")) {
                harcutils.printtable("Available macros:", engine.get_macros(true));
            } else {
                String out = engine.eval_macro(macroname, arguments, 0, no_execute);
                if (out == null) {
                    System.out.println("** Fail **");
                } else if (!out.equals("")) {
                    System.out.println("Result: \"" + out + "\"");
                } else /* Nothing */;
            }
        } catch (non_existing_command_exception e) {
            System.err.println(e.getMessage());//"Macro \"" + macroname + "\" not found.");
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }
    }
}