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

public class CommandSetEntry {

    private command_t cmd;
    private short cmdno;
    private String name;
    private String transmit;
    private int response_lines;
    private String response_ending;
    private String expected_response;
    private String remark;
    private String[] arguments;
    private String ccf_toggle_0;
    private String ccf_toggle_1;

    @Override
    public String toString() {
        return "commandset_entry: " + cmd + ", " + cmdno + ", '" + transmit + "'";
    }

    public String toString(CommandSet cmdset, boolean verbose) {
        StringBuilder response = new StringBuilder(128);
        switch (cmdset.get_type()) {
            case ir:
                response.append("Infrared: ").append(cmd).append(", ").append(cmdno);
                if (verbose)
                    response.append(", ").append(cmdset.get_remotename()).append(", ").append(cmdset.get_protocol())
                            .append(", ").append(cmdset.get_deviceno()).append(", ").append(cmdset.get_subdevice())
                            .append(cmdset.get_toggle() ? ", toggle, " : "");
                break;
            case web_api:
            case serial:
            case tcp:
            case udp:
                response.append(cmdset.get_type()).append(": ").append(cmd).append(", '").append(transmit).append("'");
                break;
            default:
                response.append(cmdset.get_type()).append(": ").append(cmd);
                break;
        }
        return response.toString();
    }

    public String toString(CommandType_t type) {
        String response;
        switch (type) {
            case ir:
                response = "Infrared: " + cmd + ", " + cmdno;
                ;
                break;
            case web_api:
            case serial:
                response = "Serial: " + cmd + ", '" + transmit + "'";
                break;
            case tcp:
            case udp:
            default:
                response = type + ", " + cmd;
                break;
        }
        return response;
    }

    public command_t get_cmd() {
        return cmd;
    }

    public short get_cmdno() {
        return cmdno;
    }

    public String get_name() {
        return name;
    }

    public String get_transmit() {
        return transmit;
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

    public String get_remark() {
        return remark;
    }

    public String[] get_arguments() {
        return arguments;
    }

    public String get_ccf_toggle_0() {
        return ccf_toggle_0;
    }

    public String get_ccf_toggle_1() {
        return ccf_toggle_1;
    }

    private void setup(command_t cmd, short cmdno, String name, String transmit,
            int response_lines, String response_ending,
            String expected_response, String remark,
            String[] arguments,
            String ccf_toggle_0, String ccf_toggle_1) {
        this.cmd = cmd;
        this.cmdno = cmdno;
        this.name = name;
        this.transmit = transmit;
        this.response_lines = response_lines;
        this.response_ending = response_ending;
        this.expected_response = expected_response;
        this.remark = remark;
        this.arguments = arguments;
        this.ccf_toggle_0 = ccf_toggle_0;
        this.ccf_toggle_1 = ccf_toggle_1;
    }

    public CommandSetEntry(String cmdname, String cmdno, String name,
            String transmit, String response_lines,
            String response_end,
            String expected_response, String remark, String[] arguments,
            String ccf_toggle_0, String ccf_toggle_1) {
        short cmdnumber =
                cmdno.isEmpty()
                ? -1
                : ((cmdno.length() > 2 && cmdno.startsWith("0x")) ? Short.parseShort(cmdno.substring(2), 16)
                : Short.parseShort(cmdno));

        setup(command_t.parse(cmdname), cmdnumber, name, transmit,
                response_lines.isEmpty() ? 0 : Integer.parseInt(response_lines),
                response_end, expected_response, remark, arguments, ccf_toggle_0, ccf_toggle_1);
    }
}
