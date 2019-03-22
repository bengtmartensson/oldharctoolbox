/*
Copyright (C) 2010-2011 Bengt Martensson.

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

package org.harctoolbox.oldharctoolbox;

import org.gnu.readline.ReadlineCompleter;

/**
 *
 * @author bengt
 */
public class jython_rl_completer implements ReadlineCompleter {

    private home hm = null;
    private String[] devices = null;
    //private String[] selecting_devices = null;
    private String[] device_commands = null;
    //private String[] src_devices = null;
    private int devices_p = 0;
    //private int selecting_devices_p = 0;
    private int device_commands_p = 0;
    //private int src_devices_p = 0;
    private boolean is_device = false;
    //private boolean select_mode = false;
    //private boolean devicecommand_mode = false;

    private int no_words = 0;
    private boolean between_words = false;
    private String[] tokens = null;
    private String devicename = null;
    //private String dst_device = null;

    private final static int no_commandsets = 5;

    private rl_commands[] commands;
    private String[] commands4device;// Macros with first arg called 'device'

    private class rl_commands {
        public String[] cmds = null;
        public int ptr;
        private int no_args;

        public void set(String[] cmds, int no_args) {
            this.cmds = cmds;
            this.no_args = no_args;
            ptr = 0;
        }

        public rl_commands() {
            no_args = -1;
            ptr = 0;
        }

        public String paramlist() {
            return no_args < 0 ? "" : no_args == 0 ? "()" : "(";
        }
    }

    public void set_commands(String[] stdcommands, String[] func0, String[] func1,
            String[] func2, String[] func2plus, String[] funcs_devices) {
        commands[0].set(stdcommands, -1);
        commands[1].set(func0, 0);
        commands[2].set(func1, 1);
        commands[3].set(func2, 2);
        commands[4].set(func2plus, 3);
        commands4device = funcs_devices;
    }

    public jython_rl_completer(/*String[] commands,*/ home hm) {
        commands = new rl_commands[no_commandsets];
        for (int i = 0; i < no_commandsets; i++)
            commands[i] = new rl_commands();
        this.hm = hm;
        devices = hm.get_devices();
        //selecting_devices = hm.get_selecting_devices();
    }

    private String first_token_completer(String in, int s) {
        for (int c = 0; c < 5; c++) {
            for (int i = commands[c].ptr; i < commands[c].cmds.length; i++) {
                if (commands[c].cmds[i].startsWith(in)) {
                    commands[c].ptr = i + 1;
                    return commands[c].cmds[i] + commands[c].paramlist();
                }
            }
            commands[c].ptr = commands[c].cmds.length;
        }
        return null;
    }

    private String device_completer(String prefix, String in, int s) {
        //if (s == 0)
        //   System.out.println("***" + prefix + ">>>" + in + ">>>>" + s);
        for (int i = devices_p; i < devices.length; i++)
            if (devices[i].startsWith(in)) {
                devices_p = i + 1;
                return prefix + devices[i] + "'";
            }
        devices_p = devices.length;

        return null;
    }

    /*private String selecting_device_completer(String prefix, String in, int s) {
        for (int i = selecting_devices_p; i < selecting_devices.length; i++)
            if (selecting_devices[i].startsWith(in)) {
                selecting_devices_p = i + 1;
                return prefix + " " + selecting_devices[i];
            }
        selecting_devices_p = selecting_devices.length;

        return null;
    }*/

    /*private String src_device_completer(String prefix, String in, int s) {
        //if (s == 0)
          //  System.out.println("***" + prefix + ">>>" + in );
        for (int i = src_devices_p; i < src_devices.length; i++)
            if (src_devices[i].startsWith(in)) {
                src_devices_p = i + 1;
                return prefix + " " + src_devices[i];
            }
        src_devices_p = src_devices.length;

        return null;
    }*/

    private String device_command_completer(String devicename, String in, int s) {
        if (device_commands == null)
            return null;

        for (int i = device_commands_p; i < device_commands.length; i++)
            if (device_commands[i].startsWith(in)) {
                device_commands_p = i + 1;
                return "harcmacros.device_command('" + devicename + "', '" + device_commands[i] + "')";
            }
        return null;
    }

    private boolean is_command_contained(String cmd, String[] cmds) {
        for (int i = 0; i < cmds.length; i++)
            if (cmd.equals(cmds[i]))
                return true;

        return false;
    }

    private boolean is_command4device(String cmd) {
        return is_command_contained(cmd, commands4device);
    }

    private boolean is_command_onearg(String cmd) {
        return is_command_contained(cmd, commands[2].cmds);
    }

    private boolean is_command_leasttwoargs(String cmd) {
        return is_command_contained(cmd, commands[3].cmds) || is_command_contained(cmd, commands[4].cmds);
    }

    @Override
    public String completer(String in, int s) {
        //boolean select_mode;
        //boolean devicecommand_mode;

        in = in.replaceFirst("^[ \t]+", "");

        if (s == 0) {
            for (int i = 0; i < 5; i++)
                commands[i].ptr = 0;

            devices_p = 0;
            //selecting_devices_p = 0;
            device_commands_p = 0;
            //src_devices_p = 0;
            device_commands = null;
            devicename = null;
            between_words = in.endsWith(" ") || in.endsWith("(") || in.endsWith("'");
            //devicecommand_mode = in.startsWith("harcmacros.device_command(");
            //select_mode = in.startsWith("--select ");
            tokens = in.split("[ \t(),']+");
            is_device = tokens.length > 1 && hm.has_device(tokens[1]);
            //dst_device = (select_mode && tokens.length > 1 && hm.has_device(tokens[1]))
            //        ? hm.expand_alias(tokens[1]) : null;
            //if (dst_device != null) {
            //    src_devices = hm.get_sources(dst_device, null);
            //} else
            if (is_device) {
                devicename = hm.expand_alias(tokens[1]);
                device_commands = hm.get_commands(devicename);
            }

        }

        if (tokens.length <= 1 && !between_words) {
            // Find a command, macro, or device.
            return first_token_completer(in, s);
        //} else if (tokens.length == 1 && between_words && s == 0 && engine.has_macro(tokens[0])) {
            // Macro, deliver its documentation (only once).
        //    return in + " # " + engine.describe_macro(tokens[0]);

        }


        if (is_command4device(tokens[0])) {
            if ((tokens.length == 1 && between_words) || (tokens.length == 2 && !between_words)) {
                // Find a device.
                return device_completer(tokens[0] + "('", tokens.length > 1 ? tokens[1] : "", s);
            }
            if (is_device) {
                if (is_command_onearg(tokens[0])) {
                    // Close the argument list
                    //System.err.println("yup" + s + in + tokens[0] + "('" + tokens[1] + "')" );
                    return s == 0 ? (tokens[0] + "('" + tokens[1] + "')") : null;
                }
                if (tokens[0].equals("harcmacros.device_command"))
                    return device_command_completer(devicename, tokens.length > 2 ? tokens[2] : "", s);
                if (is_command_leasttwoargs(tokens[0]) && tokens.length == 2) {
                    return s == 0 ? (tokens[0] + "('" + tokens[1] + "', ") : null;
                }

            }
        //} else if (select_mode) {
        //    if ((tokens.length == 1 && between_words) || (tokens.length == 2 && !between_words)) {
        //        // Find a selecting device.
        //        return selecting_device_completer(tokens[0], tokens.length > 1 ? tokens[1] : "", s);
        //    } else if ((tokens.length == 2 && between_words) || tokens.length == 3) {
        //        // Find a src device for the already selected device.
        //        return src_device_completer(tokens[0] + " " + dst_device, tokens.length > 2 ? tokens[2] : "", s);
        //    }
        }

        return null;
    }
}
