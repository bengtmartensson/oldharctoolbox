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

// TODO: This functionallity is not presently available from the GUI.
// the globalcache code should not reside here.

// Take care of toggling state here
package org.harctoolbox.oldharctoolbox;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.Protocol;

public final class ProtocolDataBase {

    private static IrpDatabase irpDatabase = null;
    private static HashMap<String, Protocol> protocols = null;

    private final static short invalid_parameter = -1;

    public static void initialize(String irp_database) throws IOException, IrpParseException {
        irpDatabase = new IrpDatabase(irp_database);
        irpDatabase.expand();
        protocols = new HashMap<>(8);
    }

    public static IrSignal encode(String protocol_name, short deviceno,
            short subdevice, short cmdno, ToggleType toggle, String params, boolean verbose) throws IrpException {
        //int itoggle = 0;
        //if (toggle == toggletype.do_toggle) {
        //    itoggle = toggle_state.containsKey(protocol_name) ? 1 - toggle_state.get(protocol_name) : 0;
        //    toggle_state.put(protocol_name, new Integer(itoggle));
        //}
        IrSignal ir =
            protocol_name.equals("raw_ccf") ? null // new raw_ir()
            : /*new*/ protocol_parser(protocol_name, deviceno, subdevice, cmdno, /*i*/toggle, params);

        /*if (!ir.is_valid()) {
            if (verbose)
                System.err.println(protocol_name.equals("")
                        ? "No protocol."
                        : ("Protocol " + protocol_name + " not implemented."));
            ir = null;
        }*/
        return ir;
    }

    private static HashMap<String, Long> parameters(short deviceno, short subdevice, short cmdno, ToggleType toggle, String extra_params) {
        HashMap<String, Long>params = new LinkedHashMap<>(4);
        if (deviceno != invalid_parameter)
            params.put("D", (long) deviceno);
        if (subdevice != invalid_parameter)
            params.put("S", (long) subdevice);
        if (cmdno != invalid_parameter)
            params.put("F", (long) cmdno);
        if (toggle != ToggleType.dont_care)
            params.put("T", (long) ToggleType.toInt(toggle));
        if (extra_params != null && !extra_params.trim().isEmpty()) {
            String[] str = extra_params.trim().split("[ \t]+");
            for (String s : str) {
                String[] q = s.split("=");
                if (q.length == 2)
                    params.put(q[0], IrCoreUtils.parseLong(q[1]));
            }
        }
        return params;
    }

    private static IrSignal protocol_parser(String protocol_name, short deviceno, short subdevice, short cmdno, ToggleType /*i*/toggle, String extra_params) throws IrpException {
        Protocol protocol = irpDatabase.getNamedProtocol(protocol_name);
        if (protocol == null)
            return null;

        HashMap<String, Long> params = parameters(deviceno, subdevice, cmdno, toggle, extra_params);
        IrSignal irSignal = protocol.toIrSignal(params);
        return irSignal;
    }

    private static boolean hasParameter(String protocol_name, String paramName) throws IrpException {
        return irpDatabase.getNamedProtocol(protocol_name).hasParameter(paramName);
    }

    public static boolean has_toggle(String protocol_name) throws IrpException {
        return hasParameter(protocol_name, "T");
    }

    public static boolean has_subdevice(String protocol_name) throws IrpException {
                return hasParameter(protocol_name, "S");
    }

    public static boolean subdevice_optional(String protocol_name) throws IrpException {
        return irpDatabase.getNamedProtocol(protocol_name).hasParameterDefault("S");
    }

    public static String get_IRP(String protocol_name) {
        return irpDatabase.getIrp(protocol_name);
    }

    /**
     *
     * @return List of strings describing names of implemented protocols.
     */
    public static List<String> get_protocols() {
        //return harcutils.get_basenames(harcprops.get_instance().get_protocolsdir(), harcutils.protocolfile_extension);
        return irpDatabase.getNames();//.toArray(new String[0]);
    }
/*
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
            try {
                initialize();
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            } catch (IncompatibleArgumentException ex) {
                System.err.println(ex.getMessage());
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
                IrSignal ir = null;
                try {
                    ir = encode(protocol_name, device, subdevice,
                   (short) command, toggle, null/* additional_parameters * /, verbose);
                } catch (IrpMasterException ex) {
                    System.err.println(ex.getMessage());
                } catch (RecognitionException ex) {
                    System.err.println(ex.getMessage());
                }
                if (ir == null) {
                    System.exit(1); // FIXME
                }
                //if (verbose)
                //    System.out.println(command);

                // FIXME
                if (decodeir) {
                    try {
                        //com.hifiremote.decodeir.DecodeIR dec = new com.hifiremote.decodeir.DecodeIR(ir.raw_ccf_array());
                        //com.hifiremote.decodeir.DecodeIR.DecodedSignal[] out = dec.getDecodedSignals();
                        DecodeIR.DecodedSignal[] out = null;


                        DecodeIR.loadLibrary();
                        //System.err.println("++++++++++++++++" + DecodeIR.getVersion());
                        out = DecodeIR.decode(ir.ccfString());
                        if (out == null || out.length == 0)
                            System.out.println("No decodings from DecodeIR.");
                        for (int i = 0; i < out.length; i++) {
                            System.out.println(out[i]);
                        }
                    } catch (IrpMasterException ex) {
                        System.err.println(ex.getMessage());
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
                    try {
                    System.out.println(ir.ccfString());
                } catch (IncompatibleArgumentException ex) {
                    System.err.println(ex.getMessage());
                }

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

    }*/

    private ProtocolDataBase() {
    }
}
