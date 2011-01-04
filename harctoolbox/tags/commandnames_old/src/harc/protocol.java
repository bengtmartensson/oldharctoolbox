/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

// TODO: the globalcache code should not reside here.

package harc;

public class protocol {

    public static ir_code encode(String protocol_name, short deviceno, short subdevice, short cmdno, int toggle) {
        ir_code ir = null;
        
        if (protocol_name.equals("rc5")) {
            ir = new rc5((short) deviceno, (short) cmdno, toggle);
        } else if (protocol_name.equals("dbox2_old")) {
            ir = new dbox2_old((short) cmdno);
        } /*else if (protocol_name.equals("sony12")) {
            ir = new sony12(deviceno, cmdno);
        } */else if (protocol_name.equals("samsung36")) {
            ir = new samsung36(deviceno, cmdno);
        } else if (protocol_name.equals("denon")) {
            ir = new denon(deviceno, cmdno);
        } else if (protocol_name.equals("denon_k")) {
            ir = new denon_k(deviceno, subdevice, cmdno);
        } /*else if (protocol_name.equals("nokia")) {
            ir = new nokia(deviceno, subdevice, cmdno);
        } else if (protocol_name.equals("panasonic")) {
            ir = new panasonic(deviceno, subdevice, cmdno);
        //} else if (protocol_name.equals("panasonic2")) {
        //ir = new panasonic2(deviceno, subdevice, cmdno);
        } */ else if (protocol_name.equals("nec1")) {
            if (subdevice == -1) {
                ir = new nec1(deviceno, cmdno);
            } else {
                ir = new nec1(deviceno, subdevice, cmdno);
            }
        } /* else if (protocol_name.equals("pioneer")) {
            if (subdevice == -1) {
                ir = new pioneer(deviceno, cmdno);
            } else {
                ir = new nec1(deviceno, subdevice, cmdno);
            }
        } else if (protocol_name.equals("nubert_ir")) {
            ir = new nubert_ir(deviceno, cmdno);
        } */ else {
            System.err.println("Protocol " + protocol_name + " not implemented.");
            ir = null;
        }
        return ir;
    }

    /**
     *
     * @return Array of strings describing names of implemented protocols.
     */
    public static String[] get_protocols() {
        return new String[] {
                    "rc5",
                    "dbox2_old",
                    "samsung36",
                    "denon",
                    "denon_k",
                    "nec1"
                };
    }

    private static void usage() {
	System.err.println("Usage:\n"
			   + "protocol [<options>] <protocol_name> <device> [<subdevice>] <commandno|min:max>"
			   + "\nwhere options=-d,-g <gc_hostname>,-t 0|1,-c <connector>,-v,-m <module>,-w <milliseconds>");
	System.exit(1);
    }

    public static void main(String[] args) {
        short device = 0;
        int min_command;
        int max_command;
        String protocol_name;
        short subdevice = -1;
        boolean decodeir = false;
        boolean verbose = false;
        String gc_hostname = null;
        int gc_module = 2;
        int gc_connector = 1;
        int toggle = 0;
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
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    toggle = Integer.parseInt(args[arg_i++]);
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
                    System.exit(2);
                }
            }

            globalcache gc = (gc_hostname != null)
                    ? new globalcache(gc_hostname, verbose) : null;

            for (int command = min_command; command <= max_command; command++) {
                ir_code ir = encode(protocol_name, device, subdevice,
                        (short) command, toggle);
                if (ir == null) {
                    System.exit(1);
                }
                if (verbose) {
                    System.out.println(command);
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
                    }
                } else
                /* if (decodeir) {
                try {
                com.hifiremote.decodeir.DecodeIRCaller dec = new com.hifiremote.decodeir.DecodeIRCaller();
                dec.setupCCF(ir.raw_ccf_array());
                while (dec.decode())
                System.out.println(dec.result_str());
                } catch (UnsatisfiedLinkError e) {
                System.err.println("Did not find DecodeIR");
                System.exit(3);
                }
                } else */
                System.out.println(ir.raw_ccf_string());

                if (command < max_command && wait_ms > 0) {
                    try {
                        if (verbose) {
                            System.err.print("Waiting for " + wait_ms + "ms...");
                        }
                        Thread.currentThread().sleep(wait_ms);
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
