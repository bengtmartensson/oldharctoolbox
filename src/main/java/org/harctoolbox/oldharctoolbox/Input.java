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
public class Input {

    public static class querycommand {

        private command_t cmd;
        private String val;
        private MediaType type;

        public querycommand(command_t cmd, String val, MediaType type) {
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

    public static class zone {

        private String name;
        private EnumMap<MediaType, command_t> selectcommands;
        private EnumMap<MediaType, querycommand> querycommands;

        public zone(String name, EnumMap<MediaType, command_t> selectcommands,
                EnumMap<MediaType, querycommand> querycommands) {
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

    public static class connector {
          ConnectionType  type;
          String hardware;
          int number;
          String version;
          String remark;
          HashSet<String> deviceref;

          public connector(ConnectionType type, String hardware, int number, String version, String remark, HashSet<String> deviceref) {
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

    private String name;
    private String myname;
    private boolean audio;
    private boolean video;
    private EnumMap<MediaType, command_t> selectcommands;
    private EnumMap<MediaType, querycommand> querycommands;
    private HashMap<String, zone> zones;
    private ArrayList<connector> connectors;
    private HashSet<String> devicesrc;
    private HashSet<String> internalsrc;
    private HashSet<String> externalsrc;

    public Input(String name,
            String myname,
            boolean audio,
            boolean video,
            EnumMap<MediaType, command_t> selectcommands,
            EnumMap<MediaType, querycommand> querycommands,
            HashMap<String, zone> zones,
            ArrayList<connector> connectors,
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
        ArrayList<String> v = new ArrayList<String>();
        for (zone z : zones.values())
            v.add(z.name);
        return v;
    }

    public HashSet<ConnectionType> get_connection_types() {
        HashSet<ConnectionType> hs = new HashSet<ConnectionType>();
        for (connector c : connectors) {
            hs.add(c.get_type());
        }
        return hs;

    }

    //public Vector<String> get_deviceref() {
    //    return deviceref;
    //}

    public ArrayList<connector> get_connectors() {
        return connectors;
    }

    public HashSet<String> get_sources(String zonename) {
        HashSet<String> v = new HashSet<String>();
        if (zonename != null && zones.get(zonename) == null)
            return null;

        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
        //    connector c = el.nextElement();
        for (connector c : connectors) {
            HashSet<String> s = c.get_deviceref();
            for (String ss : s) {
                v.add(ss);
            }
            for (String e : internalsrc) {
                v.add(e);
            }
            for (String e : externalsrc) {
                v.add(e);
            }
        }
        return v;
    }

    public boolean connects_to(String device, ConnectionType type) {
        if (internalsrc.contains(device) || externalsrc.contains(device))
            return true;
        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
        //    connector c = el.nextElement();
        for (connector c : connectors) {
            if (c.get_deviceref().contains(device) && c.type.is_ok(type))
                return true;
        }
        return false;
    }

    public boolean connects_to(String device) {
        return is_connected_device(device) || internalsrc.contains(device) || externalsrc.contains(device);
    }

    private boolean is_connected_device(String device) {
        //for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements(); ) {
        //    connector c = el.nextElement();
        for (connector c : connectors) {
            if (c.deviceref.contains(device))
                return true;
        }
        return false;
    }

    public boolean has_separate_av_commands() {
        if (selectcommands.size() > 1)
            return true;
        for (zone z : zones.values())
            if (z.has_separate_av_commands())
                return true;

        return false;
    }

    public command_t get_select_command(String zonename, MediaType type) {
        command_t c;
        if (zonename == null || zonename.isEmpty()) {
            c = selectcommands.get(type);
        } else {
            zone z = zones.get(zonename);
            if (z == null)
                return command_t.invalid;
            c = z.selectcommands.get(type);
        }
        return c != null ? c : command_t.invalid;
    }

    public querycommand get_query_command(String zonename, MediaType type) {
        EnumMap<MediaType, querycommand> table;
        if (zonename == null || zonename.isEmpty()) {
            table = querycommands;
        } else {
            zone z = zones.get(zonename);
            if (z == null)
                return null;
            table = z.querycommands;
        }
        return table == null ? null : table.get(type);
    }
}
