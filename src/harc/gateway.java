package harc;

import java.util.Hashtable;
import java.util.Vector;

/**
 *
 * @author bengt
 */
public class gateway {

    private String hostname;
    private Vector<gateway_port> gateway_ports;
    private Hashtable<commandtype_t, Hashtable<Integer, port>> ports_table;
    private Hashtable<command_t, commandmapping> commandmappings;
    private String clazz;
    private String model;
    private String interfaze;
    private String deviceclass;
    private String firmware;
    private boolean www = false;
    private boolean web_api = false;
    private int web_api_portnumber = 80;
    private String name;
    private String id;
    private int timeout;

    public gateway(String hostname,
            Vector<gateway_port> gateway_ports,
            Hashtable<commandtype_t, Hashtable<Integer, port>> ports_table,
            Hashtable<command_t, commandmapping> commandmappings,
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

    public commandmapping get_commandmapping(command_t cmd) {
        return commandmappings.get(cmd);
    }

    public String get_class() {
        return clazz;
    }

    public String get_id() {
        return id;
    }

    public Hashtable<commandtype_t, Hashtable<Integer, port>> get_ports_table() {
        return ports_table;
    }

    public Vector<gateway_port> get_gateway_ports() {
        return gateway_ports;
    }

    public String get_model() {
        return model;
    }

    public String get_interface() {
        return interfaze;
    }

    public port get_port(commandtype_t type, int n) {
        Hashtable<Integer, port> table = ports_table.get(type);
        return table != null ? table.get(n) : null;
    }
}
