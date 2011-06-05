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

package org.harctoolbox;

import java.io.*;
import java.util.*;
import org.xml.sax.*;

/**
 * This class exports a device to a text file in the format used by <a href="http://www.lirc.org">LIRC</a>.
 * It is influenced by pronto2lirc.py from <a href="mailto:olavi.akerman@gmail.com">Olavi Akerman</a>.
 */
public class lirc_export {

    private lirc_remote[] remotes;
    private device the_device;

    private class lirc_remote {

        public static final String lirc_extension = ".lirc";

        private String remotename;
        private String devicename;
        private String protocol_name;
        public boolean valid = false;

        // TODO: Is exactly one protocol sensible here?
        public lirc_remote(String remotename, String devicename) {
            this.remotename = remotename;
            this.devicename = devicename;
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
            return cmnds != null ? comments() + head() + timings() + commands() + postamble() : null;
        }

        private String comments() {
            return "#\n# "
                    + (remotename.equals(devicename) ? ("Device: " + devicename) : ("Remotename: " + remotename + " for device " + devicename)) + "\n"
                    + "#\n# Created by " + harcutils.version_string
                    + " for " + System.getenv("USER")
                    + " on " + (new Date()) + "\n#\n";
        }

        private String head() {
            return "begin remote\n\tname\t" + remotename + "\n";
        }

        private String timings() {
            int freq = the_device.get_frequency();
            if (freq == -1) {
                commandset[] cmdsets = the_device.get_commandsets(remotename, commandtype_t.ir);
                freq = ccf_parse.get_frequency(cmdsets[0].get_entry(0).get_ccf_toggle_0());
            }
            int gap = the_device.get_gap();
            if (gap == 0) {
                commandset[] cmdsets = the_device.get_commandsets(remotename, commandtype_t.ir);
                gap = ccf_parse.get_gap(cmdsets[0].get_entry(0).get_ccf_toggle_0());
            }
            return "\tflags\tRAW_CODES\n\teps\t30\n\taeps\t100\n\tfrequency\t"
                    + freq + "\n\tgap\t" + gap + "\n";
        }

        private String commands() {
            int no_commands = 0;
            String result = "\t\tbegin raw_codes\n";
            commandset[] cmdsets = the_device.get_commandsets(remotename, commandtype_t.ir);
            for (int i = 0; i < cmdsets.length; i++) {
                commandset cs = cmdsets[i];
                short devno = cs.get_deviceno();
                short subdevice = cs.get_subdevice();
                for (int j = 0; j < cs.get_no_commands(); j++) {
                    commandset_entry cse = cs.get_entry(j);
                    short cmdno = cse.get_cmdno();
                    String code = ccf_format(devno, subdevice, cmdno, cse.get_ccf_toggle_0());

                    if (code != null && !code.isEmpty()) {
                        result = result + "\t\t\tname " + cse.get_cmd() + code + "\n";
                        no_commands++;
                    }
                }
            } result = result + "\t\tend raw_codes\n";
            return no_commands > 0 ? result : null;
        }

        private String postamble() {
            return "end remote\n";
        }

        private String ccf_format(int[] arr, double pulse_time) {
            String result = "";
            for (int i = 4; i < arr.length - 1; i++)
                result = result + (i % 6 == 4 ? "\n\t\t\t\t" : i > 4 ? " " : "") + (int) (pulse_time * arr[i] + 0.5);

            return result;
        }

        private String ccf_format(short devno, short subdevice, short cmdno, String fallback) {
            ir_code irc = protocol.encode(protocol_name, devno, subdevice, cmdno, toggletype.no_toggle, true);
            if (irc == null)
                return null;

            int[] ccf = irc.raw_ccf_array();
            boolean invalid = ccf == null || ccf.length <= 4;
            return ccf_format(invalid ? ir_code.parse_ccf(fallback) : ccf,
                    invalid ? ir_code.get_pulse_time(ir_code.parse_ccf(fallback)[1]) : irc.get_pulse_time());
        }

        private boolean export(String exportdir) throws FileNotFoundException {
            String filename = exportdir + File.separatorChar + remotename + lirc_extension;
            String s = toString();
            if (s != null)
                (new PrintStream(filename)).print(s);
            return true;
        }

        private boolean export(PrintStream str) {
            str.print(toString());
            return true;
        }
    }

    /**
     *
     * @param dev Device to be exported.
     */
    public lirc_export(device dev) {
        the_device = dev;
        String[] remotenames = dev.get_remotenames();
        remotes = new lirc_remote[remotenames.length];
        for (int i = 0; i < remotenames.length; i++)
            remotes[i] = new lirc_remote(remotenames[i], dev.device_name);
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
                boolean s;
                try {
                    s = remotes[i].export(exportdir);
                } catch (FileNotFoundException e) {
                    System.err.println(e);
                    s = false;
                }
                success &= s;
            } else
                success = false;
        }
        return success;
    }

    public boolean export(PrintStream str) {
        boolean success = true;
        for (int i = 0; i < remotes.length; i++) {
            if (remotes[i].valid) {
                boolean s = remotes[i].export(str);
                success &= s;
            } else
                success = false;
        }
        return success;
    }

    public static void export(String exportdir, String devicename) throws IOException, SAXParseException, SAXException {
        (new lirc_export(new device(devicename))).export(exportdir);
    }

    public static void export(String filename, String[] devices) throws FileNotFoundException, IOException, SAXParseException, SAXException {
        PrintStream str = new PrintStream(new FileOutputStream(filename));
        for (int i = 0; i < devices.length; i++) {
            (new lirc_export(new device(devices[i]))).export(str);
        }
        str.close();
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
            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (SAXParseException e) {
                System.err.println(e.getMessage());
            } catch (SAXException e) {
                System.err.println(e.getMessage());
            }
            (new lirc_export(dev)).export(exportdir);
        }
    }

    /**
     * Export every device to the default directory.
     */
    public static void export_all() {
        export_all(harcprops.get_instance().get_exportdir());
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("lirc_export -a|devicename");
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
                (new lirc_export(dev)).export(harcprops.get_instance().get_exportdir());
        }
    }
}