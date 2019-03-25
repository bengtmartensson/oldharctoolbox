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
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author bengt
 */

/** For the device in the home (the name device was already taken...) */

public final class Dev {

    //private final String name;
    private final String id;
    private final String canonical;
    //private final String model;
    private final String clazz;
    //private final String firmware;
    private final int pin;
    //private final String description;
    //private final String notes;
    private final String defaultzone;
    private final HashMap<String, String> attributes;
    private final String powered_through;
    private final ArrayList<GatewayPort> gateway_ports;
    private final HashMap<String, Input> inputs;

    public Dev(String name,
            String id,
            String canonical,
            String model,
            String clazz,
            String firmware,
            int pin,
            String defaultzone,
            String description,
            String notes,
            HashMap<String, String> attributes,
            String powered_through,
            ArrayList<GatewayPort> gateway_ports,
            HashMap<String, Input> inputs) {
        //this.name = name;
        this.id = id;
        this.canonical = canonical;
        //this.model = model;
        this.clazz = clazz;
        //this.firmware = firmware;
        this.pin = pin;
        this.defaultzone = defaultzone;
        //this.description = description;
        //this.notes = notes;
        this.attributes = attributes;
        this.powered_through = powered_through;
        this.gateway_ports = gateway_ports;
        this.inputs = inputs;
    }

    public String get_id() {
        return id;
    }

    public String get_class() {
        return clazz;
    }

    public HashSet<String> get_zone_names() {
        HashSet<String> hs = new HashSet<>(4);
        inputs.values().stream().map((inp) -> inp.get_zone_names()).forEachOrdered((v) -> {
            v.forEach((s) -> {
                hs.add(s);
            });
        });
        return hs;
    }

    public HashSet<String> get_sources(String zone) {
        HashSet<String> hs = new HashSet<>(8);
        inputs.values().stream().map((inp) -> inp.get_sources(zone)).filter((v) -> (v != null)).forEachOrdered((v) -> {
            v.forEach((s) -> {
                hs.add(s);
            });
        });
        return hs;
    }

    public boolean has_separate_av_commands() {
        return inputs.values().stream().anyMatch((in) -> (in.has_separate_av_commands()));
    }

    public Input find_input(String srcdevice, ConnectionType type) {
        for (Input i : inputs.values())
            if (i.connects_to(srcdevice, type))
                return i;
        return null;
    }

    public HashSet<ConnectionType> get_connection_types(String srcdevice) {
        HashSet<ConnectionType> hs = new HashSet<>(8);
        inputs.values().stream().filter((inp) -> (inp.get_sources(null).contains(srcdevice))).map((inp) -> inp.get_connection_types()).forEachOrdered((ct) -> {
            ct.forEach((s) -> {
                hs.add(s);
            });
        });
        return hs;
    }

    public String get_canonical_name() {
        return canonical.isEmpty() ? id : canonical;
    }

    public String get_powered_through() {
        return powered_through;
    }

    public ArrayList<GatewayPort> get_gateway_ports() {
        return gateway_ports;
    }

    public boolean has_commandtype(CommandType_t type) {
        if (type == CommandType_t.any)
            return ! gateway_ports.isEmpty();

        return gateway_ports.stream().anyMatch((g) -> (g.get_connectortype() == type));
    }

    public HashMap<String, Input> get_inputs() {
        return inputs;
    }

    public String get_defaultzone() {
        return defaultzone;
    }

    public int get_pin() {
        return pin;
    }

    public HashMap<String, String> get_attributes() {
        return attributes;
    }
}
