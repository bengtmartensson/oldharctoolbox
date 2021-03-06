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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

/**
 *
 * @author bengt
 */
public final class Gateway {

    private final String hostname;
    private final ArrayList<GatewayPort> gateway_ports;
    private final EnumMap<CommandType_t, HashMap<Integer, Port>> ports_table;
    private final HashMap<command_t, CommandMapping> commandmappings;
    private final String clazz;
    private final String model;
    private final String interfaze;
    private final String deviceclass;
    private final String firmware;
    private boolean www = false;
    private boolean web_api = false;
    private int web_api_portnumber = 80;
    private final String name;
    private final String id;
    private final int timeout;

    public Gateway(String hostname,
            ArrayList<GatewayPort> gateway_ports,
            EnumMap<CommandType_t, HashMap<Integer, Port>> ports_table,
            HashMap<command_t, CommandMapping> commandmappings,
            String clazz,
            String model,
            String interfaze,
            String deviceclass,
            String firmware,
            boolean www,
            boolean web_api,
            int web_api_portnumber,
            String name,
            String id,
            int timeout) {
        this.hostname = hostname;
        this.gateway_ports = gateway_ports;
        this.ports_table = ports_table;
        this.commandmappings = commandmappings;
        this.clazz = clazz;
        this.model = model;
        this.interfaze = interfaze;
        this.deviceclass = deviceclass;
        this.firmware = firmware;
        this.www = www;
        this.web_api = web_api;
        this.web_api_portnumber = web_api_portnumber;
        this.name = name;
        this.id = id;
        this.timeout = timeout;
    }

    public String get_hostname() {
        return hostname;
    }

    public int get_timeout() {
        return timeout;
    }

    public CommandMapping get_commandmapping(command_t cmd) {
        return commandmappings.get(cmd);
    }

    public String get_class() {
        return clazz;
    }

    public String get_id() {
        return id;
    }

    public EnumMap<CommandType_t, HashMap<Integer, Port>> get_ports_table() {
        return ports_table;
    }

    public ArrayList<GatewayPort> get_gateway_ports() {
        return gateway_ports;
    }

    public String get_model() {
        return model;
    }

    public String get_interface() {
        return interfaze;
    }

    public Port get_port(CommandType_t type, int n) {
        HashMap<Integer, Port> table = ports_table.get(type);
        return table != null ? table.get(n) : null;
    }
}
