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

/*
 * Importing a rmdu file to xml format.
 * This file is deliberately written not to use the RemoteMaster api.
 *
 * This is just a rather dumb file conversion.
 */

package org.harctoolbox;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;

/**
 *
 */
public class rmdu_import {

    private static class fcn {
        public String name;
        public short hex;
        public String notes;

        public fcn() {
        }

        @Override
        public String toString() {
            return "Function: " + name + Integer.toHexString(hex) + (notes != null && !notes.isEmpty() ? notes : "");
        }
    }

    private static String description;
    private static String devicetype;
    private static String setupcode;
    private static String protocol_name;
    private static String protocol_number;
    private static String protocolparms;
    private static String fixeddata;
    private static String notes;
    private static Vector<fcn> functions = new Vector<fcn>(100);

    private static String rest_of_line(StreamTokenizer st) throws IOException {
        String result = "";
        st.nextToken();
        while (st.ttype != StreamTokenizer.TT_EOL) {
            result = result + (result.isEmpty() ? "" : " ") + (st.ttype == StreamTokenizer.TT_WORD ? st.sval : (char) st.ttype);
            st.nextToken();
        }
        return result;
    }

    // just catch normal exceptions and return false...
    public static boolean convert(String rmdu_file, String outfilename) {
        Reader r;
        try {
            r = new BufferedReader(new InputStreamReader(new FileInputStream(rmdu_file)));
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
            return false;
        }

        StreamTokenizer st = new StreamTokenizer(r);
        st.resetSyntax();
        st.commentChar('#');
        st.eolIsSignificant(true);
        st.lowerCaseMode(false);
        st.wordChars('a', 'z');
        st.wordChars('A', 'Z');
        st.wordChars('0', '9');
        st.wordChars('-', '-');
        st.wordChars('_', '_');
        st.whitespaceChars(' ', ' ');
        st.whitespaceChars('=', '=');
        st.whitespaceChars(13, 13);// \r
        int token;
        try {
            while (st.ttype != StreamTokenizer.TT_EOF) {
                st.nextToken();
                int type = st.ttype;
                if (type == StreamTokenizer.TT_WORD) {
                    if (st.sval.equalsIgnoreCase("description"))
                        description = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("devicetype"))
                        devicetype = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("setupcode"))
                        setupcode = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("fixeddata"))
                        fixeddata = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("notes"))
                        notes = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("protocol")) {
                        st.nextToken();
                        if (st.ttype == '.') {
                            st.nextToken();
                            protocol_name = rest_of_line(st);
                        } else {
                            st.pushBack();
                            protocol_number = rest_of_line(st);
                        }
                    } else if (st.sval.equalsIgnoreCase("protocolparms"))
                        protocolparms = rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("button"))
                        rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("function")) {
                        st.nextToken();
                        st.nextToken();
                        int index = Integer.parseInt(st.sval);
                        if (index > functions.size()) {
                            System.err.println("Functions are not ordered, cannot process");
                            return false;
                        }
                        if (index == functions.size()) {
                            fcn f = new fcn();
                            functions.add(index, f);
                        }
                        st.nextToken();
                        st.nextToken();
                        fcn f = functions.elementAt(index);
                        String key = st.sval;
                        String argument = rest_of_line(st);
                        if (key.equalsIgnoreCase("name"))
                            f.name = argument;
                        else if (key.equalsIgnoreCase("hex")) {
                            Integer.parseInt(argument, 16);
                            f.hex = Short.parseShort(argument, 16);
                        } else if (key.equalsIgnoreCase("notes"))
                            f.notes = argument;
                        //System.out.println(f);
                    } else if (st.sval.equalsIgnoreCase("remote"))
                        rest_of_line(st);
                    else if (st.sval.equalsIgnoreCase("deviceindex"))
                        rest_of_line(st);
                    else
                        System.out.println("Unknown word: " + st.sval);
                } else if (type == StreamTokenizer.TT_NUMBER)
                    System.out.println("Number: " + st.nval);
                else if (type == StreamTokenizer.TT_EOL)
                    System.out.println("EOL");
                else if (type == StreamTokenizer.TT_EOF)
                    ;
                    //System.out.println("EOF");
                else
                    System.out.println("Unknown type " + type);
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        jp1protocoldata jp1prot = protocol_parser.get_jp1data(protocol_name.toLowerCase());

        String[] protocol_parameters = protocolparms.split(" ");

        short device_no = -1;
        short subdevice_no = -1;
        try {
            device_no = Short.parseShort(protocol_parameters[0]);
            subdevice_no = Short.parseShort(protocol_parameters[1]);
        } catch (NumberFormatException e) {
        }

        Document doc = harcutils.newDocument();
        
        
        Element root = doc.createElement("device");
        root.appendChild(doc.createComment(notes));
        String desc = description == null ? "" : description;
        root.setAttribute("id", desc.replaceAll(" ", "_").toLowerCase());
        root.setAttribute("name", desc);
        root.setAttribute("type", device_type.parse(devicetype).toString());
        root.setAttribute("model", desc.replaceFirst("^[^ ]+ ", ""));
        doc.appendChild(root);

        Element jp1data = doc.createElement("jp1data");
        root.appendChild(jp1data);
        Element setup = doc.createElement("setupcode");
        setup.setAttribute("value", setupcode);
        jp1data.appendChild(setup);
        if (fixeddata != null && !fixeddata.isEmpty()) {
            Element fixeddata_el = doc.createElement("fixed_data");
            fixeddata_el.setAttribute("data", fixeddata);
            jp1data.appendChild(fixeddata_el);
        }

        Element commandset = doc.createElement("commandset");
        commandset.setAttribute("type", "ir");
        commandset.setAttribute("protocol", protocol_name.toLowerCase());
        commandset.setAttribute("remotename", description.replaceAll(" ", "_").toLowerCase());
        commandset.setAttribute("deviceno", Short.toString(device_no));
        if (subdevice_no != -1)
            commandset.setAttribute("subdevice", Short.toString(subdevice_no));
        root.appendChild(commandset);

        for (int i = 0; i < functions.size(); i++) {
            fcn f = functions.get(i);
            Element command_el = doc.createElement("command");
            //System.out.println(f.name);
            String cmdref = f.name.toLowerCase();
            if (!command_t.is_valid(cmdref))
                cmdref = "cmd_" + cmdref;
            if (!command_t.is_valid(cmdref)) {commandset.setAttribute("deviceno", Short.toString(device_no));
                System.err.println("Warning: function name `" + f.name + "' does not match a valid command name.");
                cmdref = "cmd_invalid";
            }

            command_el.setAttribute("cmdref", cmdref);
            command_el.setAttribute("name", f.name);
            if (f.notes != null && !f.notes.isEmpty())
                command_el.setAttribute("remark", f.notes);
            command_el.setAttribute("cmdno", Short.toString(jp1prot.hex2obc(f.hex)));
            commandset.appendChild(command_el);
        }

        try {
            harcutils.printDOM(outfilename, doc, harcprops.get_instance().get_dtddir() + File.separator + "devices.dtd");
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
            return false;
        }
        return true;
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("rmdu_import -o <outputfile> <rmdu-file>");
    }

    public static void main(String[] args) {
        if (args.length >= 3)
            convert(args[2], args[1]);
        else
            usage();
    }
}
