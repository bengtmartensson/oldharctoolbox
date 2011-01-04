package org.harctoolbox;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

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
    private Vector<gateway_port> gateway_ports;
    private Hashtable<String, input> inputs;

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
            Vector<gateway_port> gateway_ports,
            Hashtable<String, input> inputs) {
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
            Vector<String> v = inp.get_zone_names();
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

    public Vector<gateway_port> get_gateway_ports() {
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

    public Hashtable<String, input> get_inputs() {
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
