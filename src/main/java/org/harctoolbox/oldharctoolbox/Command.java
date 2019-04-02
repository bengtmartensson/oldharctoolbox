/*
Copyright (C) 2009-2011, 2019 Bengt Martensson.

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

import org.harctoolbox.ircore.InvalidArgumentException;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.Pronto;
import org.harctoolbox.irp.IrpException;


// TODO: use a reference to the commandset instead of copying most of its content.
public final class Command {

    private final command_t cmd;
    private final short cmdno;
    private final String name;
    private final String transmit;
    private final String prefix;
    private final String suffix;
    private final int response_lines;
    private final String response_ending;
    private final CommandType_t type;
    private final String protocol_name;
    private final short deviceno;
    private final short subdevice;
    private final boolean toggle;
    private final String additional_parameters;
    private final String remotename;
    private final String remark;
    private final String expected_response;
    private final int delay_between_reps;
    private final String[] arguments;
    private final String ccf_toggle_0;
    private final String ccf_toggle_1;
    private final String charset;
    private final String flavor;
    private final int minsends;

    public Command(CommandSet cmdset, int index) {
        CommandSetEntry c = cmdset.get_entry(index);
        this.cmd = c.get_cmd();
        this.cmdno = c.get_cmdno();
        this.name = c.get_name();
        this.transmit = c.get_transmit();
        this.response_lines = c.get_response_lines();
        this.response_ending = c.get_response_ending();
        this.expected_response = c.get_expected_response();
        this.remark = c.get_remark();
        this.arguments = c.get_arguments();
        this.ccf_toggle_0 = c.get_ccf_toggle_0();
        this.ccf_toggle_1 = c.get_ccf_toggle_1();
        this.type = cmdset.get_type();
        this.remotename = cmdset.get_remotename();
        this.protocol_name = cmdset.get_protocol();
        this.deviceno = cmdset.get_deviceno();
        this.subdevice = cmdset.get_subdevice();
        this.toggle = cmdset.get_toggle();
        this.additional_parameters = cmdset.get_additional_parameters();
        this.prefix = cmdset.get_prefix();
        this.suffix = cmdset.get_suffix();
        this.delay_between_reps = cmdset.get_delay_between_reps();
        this.charset = cmdset.get_charset();
        this.flavor = cmdset.get_flavor();
        this.minsends = cmdset.get_minsends();
    }

    @Override
    public String toString() {
        StringBuilder response = new StringBuilder(132);
        switch (type) {
            case ir:
                response.append("type = ir, name = ").append(cmd).append(", remotename = ").append(remotename).append(", protocol_name = ")
                        .append(protocol_name).append(", deviceno = ").append(deviceno).append(", subdevice = ").append(subdevice).append(", cmdno = ")
                        .append(cmdno).append(", ").append(toggle ? "toggle" : "no toggle").append(additional_parameters.isEmpty() ? "" : (", " + additional_parameters));
                break;
            case tcp:
            case udp:
            case web_api:
            case serial:
                response.append("type = ").append(type).append(", name = ").append(cmd).append(", transmitstring = '")
                        .append(get_transmitstring(false)).append("', expected_response = '").append(expected_response)
                        .append("', response_lines = ").append(response_lines).append(", response_ending = ")
                        .append(response_ending).append(", delay_between_reps = ").append(delay_between_reps);
                for (int i = 0; i < arguments.length; i++) {
                    response.append(", arguments[").append(i).append("]=").append(arguments[i]);
                }
                response.append(".");
                break;
            default:
                response.append(type).append(", ").append(cmd);
                break;
        }
        return response.toString();
    }

    public int get_delay_between_reps() {
        return delay_between_reps;
    }

    public command_t get_cmd() {
        return cmd;
    }

    public String[] get_arguments() {
        return arguments;
    }

    public int get_no_arguments() {
        return arguments.length;
    }

    public boolean get_toggle() {
        return toggle;
    }

    public String get_additional_parameters() {
        return additional_parameters;
    }

    public int get_response_lines() {
        return response_lines;
    }

    public String get_response_ending() {
        return response_ending;
    }

    public String get_expected_response() {
        return expected_response;
    }

    public short get_commandno() {
        return cmdno;
    }

    public String get_remark() {
        return remark;
    }

    public CommandType_t get_commandtype() {
        return type;
    }

    public String get_flavor() {
        return flavor;
    }

    public int get_minsends() {
        return minsends;
    }

//    public ir_code get_ir_code() {
//        return get_ir_code(false);
//    }

//    public ir_code get_ir_code(boolean verbose) {
//        return get_ir_code(toggletype.toggle_0, verbose);
//    }

    public IrSignal get_ir_code(ToggleType toggle, boolean verbose, short devno, short subdev) {
        IrSignal ir = null;
        try {
            ir = ProtocolDataBase.encode(protocol_name, devno, subdev, cmdno, toggle, additional_parameters, verbose);
            // Fallback
            if (ir == null || protocol_name.equals("raw_ccf")) {
                String ccf = toggle == ToggleType.toggle_1 ? ccf_toggle_1 : ccf_toggle_0;
                if (ccf == null) {
                    System.err.println("Neither protocol code nor raw CCF available");
                    return null;
                }
                if (verbose)
                    System.err.println("No protocol code available, falling back to raw CCF code");

                //ir = new ccf_parse(ccf);
                ir = Pronto.parse(ccf);
            }
        } catch (IrpException | Pronto.NonProntoFormatException | InvalidArgumentException ex) {
            System.err.println(ex.getMessage());
        }
        return ir ;
    }

     public IrSignal get_ir_code(ToggleType toggle, boolean verbose) {
        return get_ir_code(toggle, verbose, deviceno, subdevice);
    }

    public String get_remotename() {
        return remotename;
    }

    public String get_charset() {
        return charset;
    }

    public String get_transmitstring(boolean expand_escapes) {
        if (transmit.isEmpty())
            return null;
        String s = prefix + transmit + suffix;
        if (expand_escapes) // not really very general...
        {
            s = s.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
        }
        return s;
    }
}
