package harc;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author bengt
 */
public class input {

    public static class querycommand {

        private command_t cmd;
        private String val;
        private mediatype type;

        public querycommand(command_t cmd, String val, mediatype type) {
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
        
        public mediatype get_type() {
            return type;
        }
    }

    public static class zone {

        private String name;
        private Hashtable<mediatype, command_t> selectcommands;
        private Hashtable<mediatype, querycommand> querycommands;

        public zone(String name, Hashtable<mediatype, command_t> selectcommands,
                Hashtable<mediatype, querycommand> querycommands) {
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
          connectiontype  type;
          String hardware;
          int number;
          String version;
          String remark;
          HashSet <String> deviceref;

          public connector(connectiontype type, String hardware, int number, String version, String remark, HashSet<String> deviceref) {
              this.type = type;
              this.hardware = hardware;
              this.number = number;
              this.version = version;
              this.remark = remark;
              this.deviceref = deviceref;
          }

          public connectiontype get_type() {
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
    private Hashtable<mediatype, command_t> selectcommands;
    private Hashtable<mediatype, querycommand> querycommands;
    private Hashtable<String, zone> zones;
    private Vector<connector> connectors;
    private HashSet<String> devicesrc;
    private HashSet<String> internalsrc;
    private HashSet<String> externalsrc;

    public input(String name,
            String myname,
            boolean audio,
            boolean video,
            Hashtable<mediatype, command_t> selectcommands,
            Hashtable<mediatype, querycommand> querycommands,
            Hashtable<String, zone> zones,
            Vector<connector> connectors,
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

    public Vector<String> get_zone_names() {
        Vector<String> v = new Vector<String>();
        for (zone z : zones.values())
            v.add(z.name);
        return v;
    }

    public HashSet<connectiontype> get_connection_types() {
        HashSet<connectiontype> hs = new HashSet<connectiontype>();
        for (connector c : connectors) {
            hs.add(c.get_type());
        }
        return hs;

    }

    //public Vector<String> get_deviceref() {
    //    return deviceref;
    //}

    public Vector<connector> get_connectors() {
        return connectors;
    }

    public HashSet<String> get_sources(String zonename) {
        HashSet<String> v = new HashSet<String>();
        if (zonename != null && zones.get(zonename) == null)
            return null;

        for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
            connector c = el.nextElement();

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

    public boolean connects_to(String device, connectiontype type) {
        if (internalsrc.contains(device) || externalsrc.contains(device))
            return true;
        for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements();) {
            connector c = el.nextElement();
            if (c.get_deviceref().contains(device) && c.type.is_ok(type))
                return true;
        }
            return false;
    }

    public boolean connects_to(String device) {
        return is_connected_device(device) || internalsrc.contains(device) || externalsrc.contains(device);
    }

    private boolean is_connected_device(String device) {
        for (Enumeration<connector> el = connectors.elements(); el.hasMoreElements(); ) {
            connector c = el.nextElement();
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

    public command_t get_select_command(String zonename, mediatype type) {
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

    public querycommand get_query_command(String zonename, mediatype type) {
        Hashtable<mediatype, querycommand> table;
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
