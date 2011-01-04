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

// In this class, toggletype should not be used, a toggle is just any parameter
// holding the value 0 or 1 given to us.
package harc;

import java.io.*;
import java.util.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class protocol_parser extends raw_ir {

    private final static int no_value = -1;
    public final static short no_subdevice = -1;
    private final static short no_device = -1;
    private final static short no_command = -1;

    private static class digested_protocol {

        public String name = null;
        public boolean valid = false;
        public int frequency = 0;
        public Hashtable<String, Integer> timings;
        public Hashtable<String, pulse_pair> pairs;
        public Hashtable<String, Integer> parameters;
        public Hashtable<String, Element> parameter_defaults;
        private Document doc = null;
        public Element intro_element = null;
        public Element repeat_element = null;
        public jp1protocoldata jp1data;// = new jp1protocoldata();

        public digested_protocol(String name) {
            this.name = name;
            String filename = harcprops.get_instance().get_protocolsdir() + File.separator + name + harcutils.protocolfile_extension;
            try {
                doc = harcutils.open_xmlfile(filename);
                if (doc == null)
                    return;
            } catch (IOException e) {
                System.err.println(e.getMessage());
                return;
            } catch (SAXParseException e) {
                System.err.println(e.getMessage());
                return;
            } catch (SAXException e) {
                System.err.println(e.getMessage());
                return;
            }

            frequency = Integer.parseInt(((Element) doc.getElementsByTagName("frequency").item(0)).getAttribute("value"));

            // Read in the timings
            timings = new Hashtable<String, Integer>();
            NodeList timingslist = doc.getElementsByTagName("timing");
            for (int i = 0; i < timingslist.getLength(); i++) {
                Element timing = (Element) timingslist.item(i);
                timings.put(timing.getAttribute("id"), Integer.parseInt(timing.getAttribute("cycles")));
            }

            // Read in the pulses
            pairs = new Hashtable<String, pulse_pair>();
            NodeList pairslist = doc.getElementsByTagName("pair");
            for (int i = 0; i < pairslist.getLength(); i++) {
                Element pair = (Element) pairslist.item(i);
                NodeList children = pair.getChildNodes();
                int k = 0;
                Node n = children.item(0);
                while (n.getNodeType() != Node.ELEMENT_NODE) {
                    n = children.item(k++);
                }
                Element first = (Element) n;

                n = children.item(k++);
                while (n.getNodeType() != Node.ELEMENT_NODE) {
                    n = children.item(k++);
                }
                Element second = (Element) n;
                pulse_pair pp = new pulse_pair(timings.get(first.getAttribute("timing")),
                        timings.get(second.getAttribute("timing")),
                        first.getTagName().equals("zero"));
                pairs.put(pair.getAttribute("id"), pp);
            }

            // Read in the parameters
            parameters = new Hashtable<String, Integer>();
            parameter_defaults = new Hashtable<String, Element>();
            NodeList parameterlist = doc.getElementsByTagName("parameter");
            for (int i = 0; i < parameterlist.getLength(); i++) {
                Element param = (Element) parameterlist.item(i);
                String param_name = param.getAttribute("id");
                parameters.put(param_name, 0);

                // Treat defaults
                NodeList nl = param.getElementsByTagName("default");
                if (nl.getLength() > 0)
                    parameter_defaults.put(param_name, (Element) nl.item(0));

            //if (param_name.equals("toggle"))
            //    has_toggle = true;
            }

            Element e = (Element) doc.getElementsByTagName("intro").item(0);
            if (e.getChildNodes().getLength() > 0)
                intro_element = e;
            e = (Element) doc.getElementsByTagName("repeat").item(0);
            if (e.getChildNodes().getLength() > 0)
                repeat_element = e;

            valid = true;
            //digested_protocols.put(name, this);

            try {
                NodeList nl = doc.getElementsByTagName("jp1data");
                //if (nl.getLength() > 0) {
                Element jp1dat = (Element) (nl.item(0));
                Element prot = (Element) (jp1dat.getElementsByTagName("protocol").item(0));
                //System.err.println(jp1dat.getElementsByTagName("protocol").getLength());
                String /*jp1data.*/protocol_name = prot.getAttribute("name");
                int protocol_number = -1;
                try {
                    /*jp1data.*/protocol_number = Integer.parseInt(prot.getAttribute("number").replaceAll(" +", ""), 16);
                } catch(NumberFormatException ex) {
                    System.err.println("Warning: cannot parse jp1 protocol number in file " + filename);
                }
                Element tohex = (Element) jp1dat.getElementsByTagName("tohex").item(0);
                jp1protocoldata.tohex_function function;
                try {
                    /*jp1data.*/function = jp1protocoldata.tohex_function.valueOf(tohex.getAttribute("function"));
                } catch (IllegalArgumentException ex) {
                    System.err.println("Nonsensical tohex function, selecting identity instead.");
                    //System.err.println(ex.getMessage());
                    function = jp1protocoldata.tohex_function.id;
                }
                int /*jp1data.*/bits = Integer.parseInt(tohex.getAttribute("bits"));

                jp1data = new jp1protocoldata(protocol_name, function, bits, protocol_number);

                NodeList codes = jp1dat.getElementsByTagName("urc-code");
                for (int i = 0; i < codes.getLength(); i++) {
                    Element code = (Element) codes.item(i);
                    String cpu = code.getAttribute("cpu");
                    String data = code.getTextContent();
                    jp1data.add_code(cpu, data);
                }
                //}
            } catch (NullPointerException ex) {
                System.err.println(e);
                System.err.println("JP1 protocol info could not be retrieved");
            }
        }
    }

    private static Hashtable<String, digested_protocol> digested_protocols = new Hashtable<String, digested_protocol>();
    public Hashtable<String, Integer> parameters;
    private String protocol_name;
    private short deviceno;
    private short subdevice = no_value;
    private short cmdno;

    //private boolean always_append_repeat = false;
    private boolean valid = false;
    private digested_protocol the_protocol = null;
    private int[][] intro;
    private int[][] repeat;

    private static digested_protocol digested_protocol_factory(String protocol_name) {
        if (digested_protocols.containsKey(protocol_name)) {
            return digested_protocols.get(protocol_name);
        } else {
            digested_protocol p = new digested_protocol(protocol_name);
            digested_protocols.put(protocol_name, p);
            return p;
        }
    }

    public static boolean has_toggle(String name) {
        return digested_protocol_factory(name).parameters.containsKey("toggle");
    }

    public static boolean has_subdevice(String name) {
        return digested_protocol_factory(name).parameters.containsKey("subdevice");
    }

    public static boolean subdevice_optional(String name) {
        return digested_protocol_factory(name).parameter_defaults.containsKey("subdevice");
    }

    public static jp1protocoldata get_jp1data(String name) {
        return (name == null || name.isEmpty()) ? null : digested_protocol_factory(name).jp1data;
    }

    public protocol_parser(String protocol_name, short deviceno, short subdevice, short cmdno, int itoggle) {
        super();
        this.protocol_name = protocol_name;
        this.deviceno = deviceno;
        this.subdevice = subdevice;
        this.cmdno = cmdno;

        the_protocol = digested_protocol_factory(protocol_name);

        if (!the_protocol.valid || deviceno < 0)
            return;

        set_frequency(the_protocol.frequency);

        // Assign actual values to the parameters
        // first copy then down...
        parameters = new Hashtable<String, Integer>();
        for (Enumeration<String> e = the_protocol.parameters.keys(); e.hasMoreElements();) {
            parameters.put(e.nextElement(), no_value);
        }

        // then assing according to parametes given
        parameters.put("deviceno", new Integer(deviceno));
        parameters.put("subdevice", new Integer(subdevice));
        parameters.put("cmdno", new Integer(cmdno));
        parameters.put("toggle", new Integer(itoggle));

        // then assign defaults to those parametes who has not received a value
        for (Enumeration<String> e = parameters.keys(); e.hasMoreElements();) {
            String param_name = e.nextElement();
            if (parameters.get(param_name) == no_value)
                parameters.put(param_name, evaluate_number(the_protocol.parameter_defaults.get(param_name)));
        }

        // Process the intro sequence
        intro = process_sequence(the_protocol.intro_element);

        // process the repeat sequence
        repeat = process_sequence(the_protocol.repeat_element);

        set_intro_sequence(canonicalize(intro));
        set_repeat_sequence(canonicalize(repeat));

        valid = true;
    }

    //public boolean get_always_append_repeat() {
    //    return always_append_repeat;
    //}
    @Override
    public boolean is_valid() {
        return valid;
    }

    private int[] canonicalize(int[][] arr) {
        if (arr == null || arr.length == 0)
            return new int[0];

        int[] work = new int[arr.length];
        int work_i = 0;
        int arr_i = 0;
        // Ignore leading zeros
        while (arr[arr_i][1] == 0) {
            arr_i++;
        }

        int cycles_elapsed = 0;
        int stored = 0;

        for (int i = arr_i; i < arr.length - 1; i++) {
            cycles_elapsed += arr[i][0];
            if (arr[i][1] == arr[i + 1][1]) {
                stored += arr[i][0];
            } else {
                work[work_i++] = stored + arr[i][0];
                stored = 0;
            //cycles_elapsed += stored + arr[i][0];
            }
        }
        if (work_i % 2 == 1)
            work_i++;

        if (arr[arr.length - 1][0] < 0) { // Padding case
            work[work_i - 1] += -arr[arr.length - 1][0] - cycles_elapsed;
        } else
            work[work_i - 1] += stored + arr[arr.length - 1][0];

        int[] result = new int[work_i];
        for (int i = 0; i < work_i; i++) {
            result[i] = work[i];
        }

        return result;
    }

    private int[][] append(int[][] first, int[][] second) {
        int[][] result = new int[first.length + second.length][2];
        for (int i = 0; i < first.length; i++) {
            result[i][0] = first[i][0];
            result[i][1] = first[i][1];
        }
        for (int i = first.length; i < result.length; i++) {
            result[i][0] = second[i - first.length][0];
            result[i][1] = second[i - first.length][1];
        }
        return result;
    }

    private int[][] expand_sequence_element(Element e) {
        if (e == null)
            return null;

        int[][] result = null;
        String type = e.getNodeName();
        //System.out.println(type);
        if (type.equals("one") || type.equals("zero") || type.equals("zeropadding")) {
            result = new int[1][2];
            int time = the_protocol.timings.get(e.getAttribute("timing"));
            result[0][0] = type.equals("zeropadding") ? -time : time;
            result[0][1] = type.equals("one") ? 1 : 0;
        //System.out.println(type + "\t" + result[0][0] + "\t" + result[0][1]);
        } else if (type.equals("coded_number")) {
            boolean is_msb = e.getAttribute("direction").equals("msb");
            pulse_pair zero_encoding = the_protocol.pairs.get(e.getAttribute("zero"));
            pulse_pair one_encoding = the_protocol.pairs.get(e.getAttribute("one"));
            pulse_pair two_encoding = the_protocol.pairs.get(e.getAttribute("two"));
            pulse_pair three_encoding = the_protocol.pairs.get(e.getAttribute("three"));
            int bits = Integer.parseInt(e.getAttribute("bits"));
            result = coded_number(is_msb, zero_encoding, one_encoding, two_encoding, three_encoding,
                    bits, get_first_child_element(e));
        }
        return result;
    }

    private static int backwards(int bits, int n) {
        return Integer.reverse(n) >> (32-bits);
    }

    private int[][] coded_number(pulse_pair zero, pulse_pair one, pulse_pair two, pulse_pair three,
            int bits, int n) {
        assert (n % 2 == 0);
        int result[][] = new int[bits][2];
        for (int i = 0; i < bits / 2; i++) {
            int this_bit = (n >> 2 * i) & 3;
            //System.err.println(n + "\t" + i + "\t" + (3 << 2*i) + "\t" + this_bit);
            pulse_pair this_pair =
                    this_bit == 0 ? zero
                    : this_bit == 1 ? one
                    : this_bit == 2 ? two
                    : three;
            result[2 * i][0] = this_pair.x();
            result[2 * i][1] = this_pair.first_value();
            result[2 * i + 1][0] = this_pair.y();
            result[2 * i + 1][1] = this_pair.second_value();
        }
        return result;
    }

    private int[][] coded_number(pulse_pair zero, pulse_pair one, int bits, int n) {
        int result[][] = new int[bits * 2][2];
        //System.err.println("" + n + "\t" + bits);
        for (int i = 0; i < bits; i++) {
            int this_bit = n & (1 << i);
            pulse_pair this_pair = this_bit == 0 ? zero : one;
            result[2 * i][0] = this_pair.x();
            result[2 * i][1] = this_pair.first_value();
            result[2 * i + 1][0] = this_pair.y();
            result[2 * i + 1][1] = this_pair.second_value();
        }
        return result;
    }

    private int[][] coded_number(boolean is_msb,
            pulse_pair zero, pulse_pair one, pulse_pair two, pulse_pair three,
            int bits, Element e) {
        int n = evaluate_number(e);
        int arg = is_msb ? backwards(bits, n) : n;
        return three == null ? coded_number(zero, one, bits, arg)
                : is_msb ? coded_number(zero, two, one, three, bits, arg)
                : coded_number(zero, one, two, three, bits, arg);
    }

    private int[][] process_sequence(/*String tagname*/Element seq_element) {
        if (seq_element == null)
            return null;

        //int arrayindex = 0;
        int[][] result = new int[0][2];
        //NodeList nl = doc.getElementsByTagName(tagname);
        //if (nl.getLength() == 0)
        //    return null;

        //Element seq_element = (Element) nl.item(0);
        NodeList nl = seq_element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result = append(result, expand_sequence_element((Element) nl.item(i)));
            }
        }
        return result;
    }

    // For debugging
    protected void dump() {
        System.out.println("Timings:");
        for (Enumeration<String> e = the_protocol.timings.keys(); e.hasMoreElements();) {
            String k = e.nextElement();
            System.out.println(k + "\t" + the_protocol.timings.get(k));
        }

        System.out.println("Pulse pairs:");
        for (Enumeration<String> e = the_protocol.pairs.keys(); e.hasMoreElements();) {
            String k = e.nextElement();
            System.out.println(k + "\t" + the_protocol.pairs.get(k));
        }

        System.out.println("Parameters:");
        for (Enumeration<String> e = parameters.keys(); e.hasMoreElements();) {
            String k = e.nextElement();
            System.out.println(k + "\t" + parameters.get(k));
        }

        System.out.println("intro");
        for (int i = 0; i < intro.length; i++) {
            System.out.println(intro[i][0] + "\t" + intro[i][1]);
        }

        System.out.println("repeat");
        for (int i = 0; i < repeat.length; i++) {
            System.out.println(repeat[i][0] + "\t" + repeat[i][1]);
        }
    }

    public protocol_parser(String protocol_name, short deviceno, short subdevice, short cmdno) {
        this(protocol_name, deviceno, subdevice, cmdno, 0);
    }

    public protocol_parser(String protocol_name, short deviceno, short cmdno) {
        this(protocol_name, deviceno, no_subdevice, cmdno);
    }

    public protocol_parser(String protocol_name, short deviceno, short cmdno, int itoggle) {
        this(protocol_name, deviceno, no_subdevice, cmdno, itoggle);
    }

    // This is really a crippled version that can not generate codes
    public protocol_parser(String protocol_name) {
        this(protocol_name, no_device, no_subdevice, no_command, 0);
    }

    private static Element get_first_child_element(Element e) {
        NodeList nl = e.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                return (Element) nl.item(i);
        }

        return null;
    }

    private static int complement(int bits, int n) {
        return (1 << bits) - 1 - n;
    }

    private static int evaluate_mask(int x, int mask, int shifts) {
        return shifts > 0 ? ((x & mask) << shifts) : ((x >> -shifts) & mask);
    }

    private int evaluate_xor(NodeList nl) {
        int result = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                result ^= evaluate_number((Element) nl.item(i));
            }
        }
        return result;
    }

    private int evaluate_number(Element e) {
        if (e == null)
            return 0;
        String type = e.getTagName();
        return type.equals("parameterref") ? parameters.get(e.getAttribute("parameter"))
                : type.equals("complement") ? complement(Integer.parseInt(e.getAttribute("bits")), evaluate_number(get_first_child_element(e)))
                : type.equals("constant") ? Integer.parseInt(e.getAttribute("value"))
                : type.equals("mask") ? evaluate_mask(evaluate_number(get_first_child_element(e)), Integer.parseInt(e.getAttribute("mask")), Integer.parseInt(e.getAttribute("shift")))
                : type.equals("xor") ? evaluate_xor(e.getChildNodes())
                : evaluate_number(get_first_child_element(e));
    }

    @Override
    public String cooked_ccf_string() {
        Element cooked_element = (Element) the_protocol.doc.getElementsByTagName("cooked-ccf-string").item(0);
        if (cooked_element == null)
            return null;

        String format = cooked_element.getAttribute("format");
        int[] arguments = new int[]{0, 0, 0, 0};
        int p = 0;
        NodeList arglist = cooked_element.getChildNodes();
        for (int i = 0; i < arglist.getLength(); i++) {
            if (arglist.item(i).getNodeType() == Node.ELEMENT_NODE) {
                arguments[p++] = evaluate_number((Element) arglist.item(i));
            }
        }

        return String.format(format, arguments[0], arguments[1], arguments[2], arguments[3]);
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("protocol_parser [-t toggle] protocol_name deviceno [subdevice] commandno");
        System.exit(harcutils.exit_usage_error);
    }

    public static void main(String args[]) {
        String name = null;
        short deviceno = 0;
        short subdevice = no_subdevice;
        short cmdno = 0;
        int itoggle = 0;
        int args_i = 0;
        try {
            if (args[0].equals("-t")) {
                itoggle = Integer.parseInt(args[1]);
                args_i += 2;
            }
            name = args[args_i++];
            deviceno = Short.parseShort(args[args_i++]);
            if (args.length - args_i > 1) {
                subdevice = Short.parseShort(args[args_i++]);
                cmdno = Short.parseShort(args[args_i++]);
            } else
                cmdno = Short.parseShort(args[args_i++]);

        } catch (NumberFormatException e) {
            System.err.println(e.getMessage());
            usage();
        } catch (ArrayIndexOutOfBoundsException e) {
            usage();
        }
        protocol_parser pp = new protocol_parser(name, deviceno, subdevice, cmdno, itoggle);
        if (!pp.is_valid()) {
            System.err.println("protocol parser failed");
            System.exit(harcutils.exit_config_read_error);
        }

        System.out.println(pp.cooked_ccf_string());
        System.out.println(pp.ccf_string());
    }
}
