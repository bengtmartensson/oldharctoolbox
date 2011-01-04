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

// TODO: This functionallity is not presently available from the GUI.
// the globalcache code should not reside here.

// Take care of toggling state here
package org.harctoolbox;

import java.util.*;
//import java.io.*;

public class protocol {

    // States for toggles, one per protocol
    private static Hashtable<String, Integer> toggle_state = new Hashtable<String, Integer>();

    public static ir_code encode(String protocol_name, short deviceno,
            short subdevice, short cmdno, toggletype toggle, boolean verbose) {
        int itoggle = 0;
        if (toggle == toggletype.do_toggle) {
            itoggle = toggle_state.containsKey(protocol_name) ? 1 - toggle_state.get(protocol_name) : 0;
            toggle_state.put(protocol_name, new Integer(itoggle));
        }
        ir_code ir =
            protocol_name.equals("raw_ccf") ? new raw_ir()
            : new protocol_parser(protocol_name, deviceno, subdevice, cmdno, itoggle);
        
        if (!ir.is_valid()) {
            if (verbose)
                System.err.println(protocol_name.equals("")
                        ? "No protocol."
                        : ("Protocol " + protocol_name + " not implemented."));
            ir = null;
        }
        return ir;
    }

    // FIXME: throw exception (?) if protocol name not found
    public static boolean has_toggle(String protocol_name) {
        return protocol_parser.has_toggle(protocol_name);
    }

    public static boolean has_subdevice(String protocol_name) {
        return protocol_parser.has_subdevice(protocol_name);
    }

    public static boolean subdevice_optional(String protocol_name) {
        return protocol_parser.subdevice_optional(protocol_name);
    }

    /**
     *
     * @return Array of strings describing names of implemented protocols.
     */
    public static String[] get_protocols() {
        return harcutils.get_basenames(harcprops.get_instance().get_protocolsdir(), harcutils.protocolfile_extension);
    }

    private static void usage(int returncode) {
        System.err.println("Usage:\n" + "protocol [<options>] <protocol_name> <device> [<subdevice>] <commandno|min:max>" + "\nwhere options=-l,-d,-g <gc_hostname>,-t 0|1,-c <connector>,-v,-m <module>,-w <milliseconds>");
        System.exit(returncode);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }

    public static void main(String[] args) {
        short device = 0;
        int min_command;
        int max_command;
        String protocol_name;
        short subdevice = -1;
        boolean decodeir = false;
        boolean verbose = false;
        boolean list = false;
        String gc_hostname = null;
        int gc_module = 2;
        int gc_connector = 1;
        toggletype toggle = toggletype.toggle_0;
        int wait_ms = 0;
        int arg_i = 0;
        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose = true;
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    decodeir = true;
                } else if (args[arg_i].equals("-l")) {
                    arg_i++;
                    list = true;
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    String t = args[arg_i++];
                    toggle = t.equals("0") ? toggletype.toggle_0
                            : t.equals("1") ? toggletype.toggle_1
                            : toggletype.valueOf(t);
                } else if (args[arg_i].equals("-g")) {
                    arg_i++;
                    gc_hostname = args[arg_i++];
                } else if (args[arg_i].equals("-m")) {
                    arg_i++;
                    gc_module = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    gc_connector = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-w")) {
                    arg_i++;
                    wait_ms = Integer.parseInt(args[arg_i++]);
                } else {
                    usage();
                }
            }

            if (list) {
                harcutils.printtable("Available IR protocols:", get_protocols());
                System.exit(harcutils.exit_success);
            }

            protocol_name = args[arg_i++];
            device = Short.parseShort(args[arg_i++]);
            if (args.length - arg_i > 1) {
                subdevice = Short.parseShort(args[arg_i++]);
            }
            String c = args[arg_i++];
            int n = c.indexOf(":");
            if (n == -1) {
                min_command = Integer.parseInt(c);
                max_command = min_command;
            } else {
                min_command = Integer.parseInt(c.substring(0, n));
                max_command = Integer.parseInt(c.substring(n + 1));
                if (max_command < min_command) {
                    System.err.println("max_command must be >= min_command");
                    System.exit(harcutils.exit_semantic_usage_error);
                }
            }

            globalcache gc = (gc_hostname != null)
                    ? new globalcache(gc_hostname, verbose) : null;

            for (int command = min_command; command <= max_command; command++) {
                ir_code ir = encode(protocol_name, device, subdevice,
                        (short) command, toggle, verbose);
                if (ir == null) {
                    System.exit(1); // FIXME
                }
                //if (verbose)
                //    System.out.println(command);

                if (decodeir) {
                    try {
                        com.hifiremote.decodeir.DecodeIR dec = new com.hifiremote.decodeir.DecodeIR(ir.raw_ccf_array());
                        com.hifiremote.decodeir.DecodeIR.DecodedSignal[] out = dec.getDecodedSignals();
                        if (out.length == 0)
                            System.out.println("No decodings from DecodeIR.");
                        for (int i = 0; i < out.length; i++) {
                            System.out.println(out[i]);
                        }
                    } catch (UnsatisfiedLinkError e) {
                        System.err.println("Did not find DecodeIR");
                        System.exit(harcutils.exit_dynamic_link_error);
                    }
                }

                if (gc != null) {
                    try {
                        gc.send_ir(ir, gc_module, gc_connector, 1);
                    } catch (java.net.UnknownHostException e) {
                        System.err.println("Unknown host " + gc_hostname);
                        System.exit(7);
                    } catch (java.io.IOException e) {
                        System.err.println("Sending to globalcache failed.");
                        System.exit(8);
                    } catch (InterruptedException e) {
                        System.err.println("Sending to globalcache interrupted.");
                        System.exit(8);
                    }
                } else
                    System.out.println(ir.raw_ccf_string());

                if (command < max_command && wait_ms > 0) {
                    try {
                        if (verbose) {
                            System.err.print("Waiting for " + wait_ms + "ms...");
                        }
                        Thread.sleep(wait_ms);
                        System.err.println();
                    } catch (java.lang.InterruptedException e) {
                        System.err.println("Interrupted...");
                        System.exit(0);
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            //System.err.println("ArrayIndexOutOfBoundsException");
            usage();
        } catch (NumberFormatException e) {
            System.err.println("NumberFormatException");
            usage();
        }

    }
}
