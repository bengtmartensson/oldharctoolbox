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

package org.harctoolbox.oldharctoolbox;

import org.harctoolbox.ircore.IrCoreUtils;

public final class CommandSet {

    private CommandType_t type;
    private String protocol;
    private short deviceno;
    private short subdevice;
    private boolean has_toggle;
    private int portnumber;
    private String additional_parameters;
    private String name;
    private String remotename;
    private String pseudo_power_on;
    private String prefix;
    private String suffix;
    private String open;
    private String close;
    private int delay_between_reps;
    private CommandSetEntry[] entries;
    private String charset;
    private String flavor;

    public int get_no_commands() {
        return entries.length;
    }

    public CommandType_t get_type() {
        return type;
    }

    public boolean is_type_ir() {
        return type == CommandType_t.ir;
    }

    public String get_remotename() {
        return remotename;
    }

    public String get_pseudo_power_on() {
        return pseudo_power_on;
    }

    public String get_protocol() {
        return protocol;
    }

    public boolean get_toggle() {
        return has_toggle;
    }

    public String get_additional_parameters() {
        return additional_parameters;
    }

    public short get_deviceno() {
        return deviceno;
    }

    public short get_subdevice() {
        return subdevice;
    }

    public String get_prefix() {
        return prefix;
    }

    public String get_suffix() {
        return suffix;
    }

    public int get_delay_between_reps() {
        return delay_between_reps;
    }

    public String get_charset() {
        return charset;
    }

    public String get_flavor() {
        return flavor;
    }

    public CommandSetEntry get_entry(int index) {
        return entries[index];
    }

    public int get_portnumber() {
        return portnumber;
    }

    public String get_open() {
        return open;
    }

    public String get_close() {
        return close;
    }

    private static int safe_parse_portnumber(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
        }

        return (int) IrCoreUtils.INVALID;
    }

    public String get_info() {
        StringBuilder s = new StringBuilder(
                "*** Commandset\n" +
                "   type = " + type + "\n" +
                "   protocol = " + protocol + "\n" +
                "   toggle = " + has_toggle + "\n" +
                "   deviceno = " + deviceno + "\n" +
                "   subdevice = " + subdevice + "\n" +
                "   additional_parameters = " + additional_parameters + "\n" +
                "   remotename = " + remotename + "\n" +
                "   prefix = " + prefix + "\n" +
                "   suffix = " + suffix + "\n" +
                "   pseudo_power_on = " + pseudo_power_on + "\n" +
                "   # commands = " + entries.length);
        for (CommandSetEntry entrie : entries)
            s.append("\n").append(entrie.toString(this, true));

        return s.toString();
    }

    public Command get_command(command_t cmd, CommandType_t type) {
        if (type == this.type || type == CommandType_t.any) {
            for (int i = 0; i < entries.length; i++) {
                if (entries[i].get_cmd() == cmd) {
                    return new Command(this, i);
                }
            }
        }
        return null;
    }

    public Command get_command(String cmdname, CommandType_t type) {
        return get_command(command_t.parse(cmdname), type);
    }

    public Command[] get_commands() {
        Command[] result = new Command[entries.length];
        for (int i = 0; i < entries.length; i++)
            result[i] = new Command(this, i);

        return result;
    }

    public CommandSet(CommandSetEntry[] commands, CommandType_t type, String protocol,
            short deviceno, short subdevice, boolean has_toggle, String additional_parameters, String name,
            String remotename, String pseudo_power_on, String prefix,
            String suffix, int delay_between_reps, String open, String close, int portnumber, String charset, String flavor) {
        this.entries = commands;
        this.type = type;
        this.deviceno = deviceno;
        this.subdevice = subdevice;
        this.protocol = protocol;
        this.has_toggle = has_toggle;
        this.additional_parameters = additional_parameters;
        this.name = name;
        this.remotename = remotename;
        this.pseudo_power_on = pseudo_power_on;
        this.prefix = prefix;
        this.suffix = suffix;
        this.delay_between_reps = delay_between_reps;
        this.open = open;
        this.close = close;
        this.portnumber = portnumber;
        this.charset = charset;
        this.flavor = flavor;
    }

    public CommandSet(CommandSetEntry[] commands, String type,
            String protocol, String deviceno, String subdevice, String toggle,
            String additional_parameters, String name, String remotename, String pseudo_power_on,
            String prefix, String suffix, String delay_between_reps,
            String open, String close, String portnumber, String charset, String flavor) {
        this(commands, CommandType_t.valueOf(type), protocol,
                deviceno.isEmpty() ? -1 : Short.parseShort(deviceno),
                subdevice.isEmpty() ? -1 : Short.parseShort(subdevice),
                toggle.equals("yes"), additional_parameters, name, remotename, pseudo_power_on,
                prefix, suffix, Integer.parseInt(delay_between_reps),
                open, close, safe_parse_portnumber(portnumber), charset, flavor);
    }
}
