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
import java.util.HashSet;

/**
 *
 */
public final class Input {

    private final String name;
    private final String myname;
    private final boolean audio;
    private final boolean video;
    private final EnumMap<MediaType, command_t> selectcommands;
    private final EnumMap<MediaType, QueryCommand> querycommands;
    private final HashMap<String, Zone> zones;
    private final ArrayList<Connector> connectors;
    //private HashSet<String> devicesrc;
    private final HashSet<String> internalsrc;
    private final HashSet<String> externalsrc;

    public Input(String name,
            String myname,
            boolean audio,
            boolean video,
            EnumMap<MediaType, command_t> selectcommands,
            EnumMap<MediaType, QueryCommand> querycommands,
            HashMap<String, Zone> zones,
            ArrayList<Connector> connectors,
            HashSet<String> internalsrc,
            HashSet<String> externalsrc) {

        this.name = name;
        this.myname = myname;
        this.audio = audio;
        this.video = video;
        this.selectcommands = selectcommands;
        this.querycommands = querycommands;
        this.zones = zones;
        this.connectors = connectors;
        this.internalsrc = internalsrc;
        this.externalsrc = externalsrc;
    }

    public String get_name() {
        return name;
    }

    public ArrayList<String> get_zone_names() {
        ArrayList<String> v = new ArrayList<>(4);
        zones.values().forEach((z) -> {
            v.add(z.name);
        });
        return v;
    }

    public HashSet<ConnectionType> get_connection_types() {
        HashSet<ConnectionType> hs = new HashSet<>(8);
        connectors.forEach((c) -> {
            hs.add(c.get_type());
        });
        return hs;

    }

    //public Vector<String> get_deviceref() {
    //    return deviceref;
    //}

    public ArrayList<Connector> get_connectors() {
        return connectors;
    }

    public HashSet<String> get_sources(String zonename) {
        HashSet<String> v = new HashSet<>(8);
        if (zonename != null && zones.get(zonename) == null)
            return null;

        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
        //    connector c = el.nextElement();
        for (Connector c : connectors) {
            HashSet<String> s = c.get_deviceref();
            s.forEach((ss) -> {
                v.add(ss);
            });
            internalsrc.forEach((e) -> {
                v.add(e);
            });
            externalsrc.forEach((e) -> {
                v.add(e);
            });
        }
        return v;
    }

    public boolean connects_to(String device, ConnectionType type) {
        if (internalsrc.contains(device) || externalsrc.contains(device))
            return true;
        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
        //    connector c = el.nextElement();
        return connectors.stream().anyMatch((c) -> (c.get_deviceref().contains(device) && c.type.is_ok(type)));
    }

    public boolean connects_to(String device) {
        return is_connected_device(device) || internalsrc.contains(device) || externalsrc.contains(device);
    }

    private boolean is_connected_device(String device) {
        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements(); ) {
        //    connector c = el.nextElement();

        return connectors.stream().anyMatch((c) -> (c.deviceref.contains(device)));
    }

    public boolean has_separate_av_commands() {
        if (selectcommands.size() > 1)
            return true;
        return zones.values().stream().anyMatch((z) -> (z.has_separate_av_commands()));
    }

    public command_t get_select_command(String zonename, MediaType type) {
        command_t c;
        if (zonename == null || zonename.isEmpty()) {
            c = selectcommands.get(type);
        } else {
            Zone z = zones.get(zonename);
            if (z == null)
                return command_t.invalid;
            c = z.selectcommands.get(type);
        }
        return c != null ? c : command_t.invalid;
    }

    public QueryCommand get_query_command(String zonename, MediaType type) {
        EnumMap<MediaType, QueryCommand> table;
        if (zonename == null || zonename.isEmpty()) {
            table = querycommands;
        } else {
            Zone z = zones.get(zonename);
            if (z == null)
                return null;
            table = z.querycommands;
        }
        return table == null ? null : table.get(type);
    }

    public static class QueryCommand {

        private final command_t cmd;
        private final String val;
        private final MediaType type;

        public QueryCommand(command_t cmd, String val, MediaType type) {
            this.cmd = cmd;
            this.val = val;
            this.type = type;
        }

        public command_t get_command() {
            return cmd;
        }

        public String get_response() {
            return val;
        }

        public MediaType get_type() {
            return type;
        }
    }

    public static class Zone {

        private final String name;
        private final EnumMap<MediaType, command_t> selectcommands;
        private final EnumMap<MediaType, QueryCommand> querycommands;

        public Zone(String name, EnumMap<MediaType, command_t> selectcommands,
                EnumMap<MediaType, QueryCommand> querycommands) {
            this.name = name;
            this.selectcommands = selectcommands;
            this.querycommands = querycommands;
        }

        public String get_name() {
            return name;
        }

        public boolean has_separate_av_commands() {
            return selectcommands.size() > 1;
        }
    }

    public static class Connector {
        private final ConnectionType  type;
        private final String hardware;
        private final int number;
        private final String version;
        private final String remark;
        private final HashSet<String> deviceref;

        public Connector(ConnectionType type, String hardware, int number, String version, String remark, HashSet<String> deviceref) {
            this.type = type;
            this.hardware = hardware;
            this.number = number;
            this.version = version;
            this.remark = remark;
            this.deviceref = deviceref;
        }

        public ConnectionType get_type() {
            return type;
        }

        public String get_hardware() {
            return hardware;
        }

        public int get_number() {
            return number;
        }

        public String get_remark() {
            return remark;
        }

        public HashSet<String> get_deviceref() {
            return deviceref;
        }
    }
}
