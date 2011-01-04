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

package harc;

import java.io.*;
import org.xml.sax.*;

/**
 * This class exports a device to a text file in the format used by the <a href="http://www.irtrans.com">IRTrans</a> device.
 * Note that the semantics of the file format is not fully documented. Creating rem-files
 * in this way is not condoned by the IRTrans manufacturer.
 *
 * For every distinct remotename occuring in the device description, a separate
 * rem-file (= a "remote") is generated. File names are taken from names of the remotes.
 * See the philips_37pfl9602 device for
 * a device with 4 commandsets, the first two makes up one remote (rem-file), the
 * third one makes up a second. The forth one is ignored, while not being of the
 * type ir.
 */
public class rem_export {

    private rem_remote[] remotes;
    private device the_device;

    private class rem_remote {

        private static final String timing_preamble = "[TIMING]\n ";
        private static final String rc5_timing = "[0][N]0[RC]3[RP]87[FREQ]36[RC5]";
        private static final String rc6_timing = "[0][N]0[RC]3[RP]81[FREQ]36[SB][RS][RC6]";
        private static final String nec1_timing = "[0][N]5[1]8968 4472[2]576 576[3]576 1656[4]8968 2232[5]576 95848[RC]1[RP]0[FREQ]38[SB][RS]";
        public static final String rem_extension = ".rem";

        // It would clearly be doable to allow for several protocols in one remote
        // (several timing patterns in the [TIMING] section).
        // However, there are more interesting things to do :-)
        private String remotename;
        private String protocol_name;
        public boolean valid = false;

        public rem_remote(String remotename) {
            this.remotename = remotename;
            String[] protocols = the_device.get_protocols(remotename);
            if (protocols.length != 1) {
                System.err.println("Error: remote " + remotename
                        + " should have exactly one protocol (actual number = " + protocols.length + ").");
                valid = false;
                return;
            }
            protocol_name = protocols[0];
            valid = true;
        }

        @Override
        public String toString() {
            String cmnds = commands();
            return cmnds != null ? head() + timings() + commands() : null;
        }

        private String head() {
            return "[REMOTE]\n [NAME]" + remotename + "\n\n";
        }

        private String timings() {
            return protocol_name.equals("rc5") ? timing_preamble + rc5_timing + "\n\n"
                    : protocol_name.equals("rc6") ? timing_preamble + rc6_timing + "\n\n"
                    : protocol_name.equals("nec1") ? timing_preamble + nec1_timing + "\n\n"
                    : "";
        }

        private String commands() {
            int no_commands = 0;
            String result = "[COMMANDS]\n";
            commandset[] cmdsets = the_device.get_commandsets(remotename, commandtype_t.ir);
            for (int i = 0; i < cmdsets.length; i++) {
                commandset cs = cmdsets[i];
                short devno = cs.get_deviceno();
                short subdevice = cs.get_subdevice();
                for (int j = 0; j < cs.get_no_commands(); j++) {
                    commandset_entry cse = cs.get_entry(j);
                    short cmdno = cse.get_cmdno();
                    String code =
                            protocol_name.equals("rc5") ? rc5_format(devno, cmdno)
                            : protocol_name.equals("rc6") ? rc6_format(devno, cmdno)
                            : protocol_name.equals("nec1") ? nec1_format(devno, subdevice == -1 ? (short) (255 - devno) : subdevice, cmdno)
                            : ccf_format(devno, subdevice, cmdno, cse.get_ccf_toggle_0());

                    if (code != null && !code.isEmpty()) {
                        result = result + " [" + cse.get_cmd() + "]" + code + "\n";
                        no_commands++;
                    }
                }
            }
            return no_commands > 0 ? result : null;
        }

        private String rc5_format(short devno, short cmdno) {
            return "[T]0[D]1" + ((cmdno & 64) == 0 ? "1" : "0")  + "0"
                    + binary_format(5, devno) + binary_format(6, (short)(cmdno & 63));
        }

        private String rc6_format(short devno, short cmdno) {
            return "[T]0[D]S100000" + binary_format(8, devno) + binary_format(8, cmdno);
        }

        private String nec1_format(short devno, short subdevice, short cmdno) {
            return "[T]0[D]S"
                    + binary_format_lsb(8, devno) + binary_format_lsb(8, subdevice) + binary_format_lsb(8, cmdno)
            + binary_format_lsb(8, (short)(255 - cmdno)) + "320";
        }

        private String ccf_format(short devno, short subdevice, short cmdno, String fallback) {
            ir_code irc = protocol.encode(protocol_name, devno, subdevice, cmdno, toggletype.no_toggle, true);
            if (irc == null)
                return null;

            String ccf = irc.raw_ccf_string();
            if (ccf.equals("0000 0000 0000 0000"))
                ccf = fallback;
            return "[CCF]" + ccf;
        }

        private String binary_format(int bits, short x) {
            if (x >= (1 << bits)) {
                System.err.println("This cannot happen: bits = " + bits + ", x = " + x);
                System.exit(harcutils.exit_this_cannot_happen);
            }

            String result = "";
            for (int i = bits - 1; i >= 0; i--) {
                //System.err.println(i + " " + x + " " + (1 << i) );
                result = result + ((x & (1 << i)) == 0 ? "0" : "1");
            }
            return result;
        }
        
        private String binary_format_lsb(int bits, short x) {
            if (x >= (1 << bits)) {
                System.err.println("This cannot happen: bits = " + bits + ", x = " + x);
                System.exit(harcutils.exit_this_cannot_happen);
            }

            String result = "";
            for (int i = 0; i < bits; i++) {
                //System.err.println(i + " " + x + " " + (1 << i) );
                result = result + ((x & (1 << i)) == 0 ? "0" : "1");
            }
            return result;
        }

        boolean export(String exportdir) {
            String filename = exportdir + File.separatorChar + remotename + rem_extension;
            try {
                String s = toString();
                if (s != null)
                    (new PrintStream(filename)).print(s);
            } catch (FileNotFoundException e) {
                System.err.println(e.getMessage());
                return false;
            }
            return true;
        }
    }

    /**
     *
     * @param dev Device to be exported.
     */
    public rem_export(device dev) {
        the_device = dev;
        String[] remotenames = dev.get_remotenames();
        remotes = new rem_remote[remotenames.length];
        for (int i = 0; i < remotenames.length; i++)
            remotes[i] = new rem_remote(remotenames[i]);
    }

    /**
     * Perform the actual export, in the directory whose name is given in the
     * argument. Exististing files are silently overwritten.
     *
     * @param exportdir File name of the directory where the exports are to be generated.
     * @return Success status
     */
    public boolean export(String exportdir) {
        boolean success = true;
        for (int i = 0; i < remotes.length; i++) {
            if (remotes[i].valid) {
                boolean s = remotes[i].export(exportdir);
                success &= s;
            } else
                success = false;

        }
        return success;
    }

    /**
     * Export every device to the directory given as argument.
     * @param exportdir Name of export directory.
     */

    public static void export_all(String exportdir) {
        String[] devnames = device.get_devices();
        for (int i = 0; i < devnames.length; i++) {
            device dev = null;
            try {
                dev = new device(devnames[i]);
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            (new rem_export(dev)).export(exportdir);
        }
    }

    public static void export(String exportdir, String devicename) {
        try {
            (new rem_export(new device(devicename))).export(exportdir);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Export every device to the default directory.
     */
    public static void export_all() {
        export_all(harcprops.get_instance().get_exportdir());
    }

    public static void create_irdb(String filename, String[] remotes) throws IOException, SAXParseException {
        PrintStream ps = new PrintStream(new java.io.FileOutputStream(filename));
        for (int i = 0; i < remotes.length; i++) {
            ps.println("[DBREMOTE2]" + remotes[i]);
            ps.println(" [IP]000.000.000.000");
            ps.println(" [PORT]0");
            ps.println(" [FLAGS]1");
            ps.println(" [RACTION]");
            ps.println("[END]");
        }
        ps.close();
    }

/*
    public static void t10dummy_setup() throws FileNotFoundException {
        String remotename = "t10dummy";
        String ipaddress = "192.168.001.042";
        PrintStream ps = new PrintStream(new java.io.FileOutputStream(remotename + ".irdb"));

        for (int command = 1; command >= 0; command--) {
            for (int n = 1; n <= 1; n++) {
                String commandname = "cmd_" + n + (command == 1 ? "on" : "off");
                ps.println("[COMACTION_0]");
                ps.println(" [ACREMOTE]t10dummy");
                ps.println(" [ACCOMMAND]" + commandname);
                ps.println(" [ACFLAGS]5");
                ps.println(" [RELAIS] 0");
                ps.println(" [ACLEN]8");
                ps.println(" [IP]" + ipaddress);
                ps.println(" [PORT]7042");
                ps.print(" [ACTION]");
                ps.write((char) (n + command));
                ps.write('\253');
                ps.write('\1');
                ps.write('\253');
                ps.write((char) (n - 1));
                ps.write('\0');
                ps.write((char) command);
                ps.write('\0');
                ps.println();
                ps.println("[END]");

                for (int i = 1; i <= 7; i++) {
                    ps.println("[COMACTION_" + i + "]");
                    ps.println(" [ACREMOTE]t10dummy");
                    ps.println(" [ACCOMMAND]" + commandname);
                    ps.println(" [ACFLAGS]2");
                    ps.println(" [ACLEN]0");
                    ps.println(" [IP]000.000.000.000");
                    ps.println(" [PORT]0");
                    ps.println(" [ACTION]");
                    ps.println("[END]");
                }
            }
        }
        ps.close();
    }*/

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("rem_export -a|devicename");
        System.exit(harcutils.exit_usage_error);
    }

    /**
     * @param args args[0] name of the device to be exported, or "-a" for exporting
     * everything.
     */
    public static void main(String[] args) {
        if (args.length != 1)
            usage();

        if (args[0].equals("-a"))
            export_all();
        else if (args[0].equals("-i"))
            try {
                create_irdb("junk.irdb", new String[]{
                            "analog_8ch_switch",
                            "denon_asd_3n",
                            "denon_avr3808",
                            "mypowerbox",
                            "nokia_dbox2",
                            "nubert",
                            "oppo_dv983",
                            "philips_37pfl9603",
                            "philips_vr1100",
                            "popcorn_hour_a100",
                            "samsung_bdp1400",
                            "sanyo_plv_z2000",
                            "t10dummy",
                            "toshiba_hd-ep30"
                        });
                //t10dummy_setup();
            } catch (Exception e) {
                System.err.println(e);
            }
        else {
            device dev = null;
            try {
                dev = new device(args[0]);
            } catch (IOException e) {
                System.err.println("Could not instantiate device: " + e.getMessage());
            } catch (SAXParseException e) {
                System.err.println("Could not parse device: " + e.getMessage());
            } catch (SAXException e) {
                System.err.println("Could not parse device: " + e.getMessage());
            }
            if (dev != null && dev.is_valid())
                (new rem_export(dev)).export(harcprops.get_instance().get_exportdir());
        }
    }
}
