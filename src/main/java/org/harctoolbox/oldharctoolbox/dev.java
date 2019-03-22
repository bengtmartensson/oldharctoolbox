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

public class dev {

    private String name;
    private String id;
    private String canonical;
    private String model;
    private String clazz;
    private String firmware;
    private int pin;
    private String description;
    private String notes;
    private String defaultzone;
    private HashMap<String, String> attributes;
    private String powered_through;
    private ArrayList<gateway_port> gateway_ports;
    private HashMap<String, input> inputs;

    public dev(String name,
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
            ArrayList<gateway_port> gateway_ports,
            HashMap<String, input> inputs) {
        this.name = name;
        this.id = id;
        this.canonical = canonical;
        this.model = model;
        this.clazz = clazz;
        this.firmware = firmware;
        this.pin = pin;
        this.defaultzone = defaultzone;
        this.description = description;
        this.notes = notes;
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
        HashSet<String> hs = new HashSet<String>();
        for (input inp : inputs.values()) {
            ArrayList<String> v = inp.get_zone_names();
            for (String s : v)
                hs.add(s);
        }
        return hs;
    }

    public HashSet<String> get_sources(String zone) {
        HashSet<String> hs = new HashSet<String>();
        for (input inp : inputs.values()) {
            HashSet<String> v = inp.get_sources(zone);
            if (v != null)
                for (String s : v)
                    hs.add(s);
        }
        return hs;
    }

    public boolean has_separate_av_commands() {
        for (input in : inputs.values())
            if (in.has_separate_av_commands())
                return true;

        return false;
    }

    public input find_input(String srcdevice, connectiontype type) {
        for (input i : inputs.values())
            if (i.connects_to(srcdevice, type))
                return i;
        return null;
    }

    public HashSet<connectiontype> get_connection_types(String srcdevice) {
        HashSet<connectiontype> hs = new HashSet<connectiontype>();
        for (input inp : inputs.values()) {
            if (inp.get_sources(null).contains(srcdevice)) {
                HashSet<connectiontype> ct = inp.get_connection_types();
                for (connectiontype s : ct) {
                    hs.add(s);
                }
            }
        }
        return hs;
    }

    public String get_canonical_name() {
        return canonical.isEmpty() ? id : canonical;
    }

    public String get_powered_through() {
        return powered_through;
    }

    public ArrayList<gateway_port> get_gateway_ports() {
        return gateway_ports;
    }

    public boolean has_commandtype(commandtype_t type) {
        if (type == commandtype_t.any)
            return ! gateway_ports.isEmpty();

        for (gateway_port g : gateway_ports)
            if (g.get_connectortype() == type)
                return true;

        return false;
    }

    public HashMap<String, input> get_inputs() {
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
