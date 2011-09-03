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

package org.harctoolbox;

public class commandmapping {
    // Command to be mapped
    private command_t cmd;
    private String house;
    private short deviceno;
    private command_t new_cmd;
    private String remotename;

    public commandmapping(command_t cmd, String house, short deviceno, command_t new_cmd, String remotename) {
        this.cmd = cmd;
        this.house = house;
        this.deviceno = deviceno;
        this.new_cmd = new_cmd;
        this.remotename = remotename;
    }

    public command_t get_cmd() {
        return cmd;
    }

    public String get_house() {
        return house;
    }

    public short get_deviceno() {
        return deviceno;
    }

    public command_t get_new_cmd() {
        return new_cmd;
    }

    public String get_remotename() {
        return remotename;
    }

}
