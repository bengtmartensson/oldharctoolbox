package org.harctoolbox;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class home_parser {
    private Hashtable <String, dev> device_table = new Hashtable<String, dev>();
    private Hashtable <String, String> alias_table = new Hashtable <String, String>();
    private Hashtable <String, device_group> device_groups_table = new Hashtable <String, device_group>();
    private Hashtable <String, gateway> gateway_table = new Hashtable<String, gateway>();

    private Hashtable <String, gateway_port> gateway_port_by_id = new Hashtable<String, gateway_port>();

    private org.w3c.dom.Document document;

    private boolean parse_yes_no(String s) {
        return s.equalsIgnoreCase("yes");
    }

    @SuppressWarnings("empty-statement")
    private int parse_int(String s, int default_value) {
        int x = default_value;
        try {
            x = Integer.parseInt(s);
        } catch (NumberFormatException e) {
        }
        return x;
    }

    public home_parser(org.w3c.dom.Document document) {
        this.document = document;
        if (document == null)
            return;

        init_alias_table();
        init_device_groups();
        init_gateway_port_by_id();
        init_gateways();
        init_devices();
    }

    private void init_devices() {
        device_table.clear();
        NodeList nl = document.getElementsByTagName("device");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            dev d = parse_device(el);
            device_table.put(d.get_id(), d);
        }
    }

    public Hashtable <String, dev> get_device_table() {
        return device_table;
    }

    private dev parse_device(Element element) {
        String description = "";
        String notes = "";
        String powered_through = "";
        String defaultzone = "";
        HashMap <String, String> attributes = new HashMap <String, String>();
        Vector<gateway_port> gateway_ports = new Vector<gateway_port>();
        Hashtable<String, input> inputs = new Hashtable<String, input>();

        org.w3c.dom.NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            switch (node.getNodeType()) {
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                    break;
                case org.w3c.dom.Node.ELEMENT_NODE:
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    if (nodeElement.getTagName().equals("description")) {
                        description = nodeElement.getTextContent();
                    }
                    if (nodeElement.getTagName().equals("powered-through")) {
                        powered_through = nodeElement.getAttribute("device");
                    }
                    if (nodeElement.getTagName().equals("notes")) {
                        notes = nodeElement.getTextContent();
                    }
                    if (nodeElement.getTagName().equals("inputs")) {
                        inputs = parse_inputs(nodeElement);
                    }
                    if (nodeElement.getTagName().equals("attribute")) {
                        attributes.put(nodeElement.getAttribute("name"), nodeElement.getAttribute("value"));
                    }
                    if (nodeElement.getTagName().equals("gateway-port")) {
                        gateway_port gwp = parse_gateway_port(nodeElement);
                        gateway_ports.add(gwp);
                    }
                    if (nodeElement.getTagName().equals("gateway-port-ref")) {
                        String g = nodeElement.getAttribute("gateway-port");
                        gateway_port gwp = gateway_port_by_id.get(g);
                        gateway_ports.add(gwp);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }
        dev d = new dev(element.getAttribute("name"),
                element.getAttribute("id"),
                element.getAttribute("canonical"),
                element.getAttribute("model"),
                element.getAttribute("class"),
                element.getAttribute("firmware"),
                parse_int(element.getAttribute("pin"), 0),
                element.getAttribute("defaultzone"),
                description,
                notes,
                attributes,
                powered_through,
                gateway_ports,
                inputs);
        return d;
    }


    private void init_gateway_port_by_id() {
        gateway_port_by_id.clear();
        NodeList nl = document.getElementsByTagName("gateway-port");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            gateway_port gwp = parse_gateway_port(el);
            String id = gwp.get_id();
            if (!id.isEmpty())
                gateway_port_by_id.put(id, gwp);
        }
    }

    private void print_gateway_port_by_id() {
        for (Enumeration e = gateway_port_by_id.keys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            gateway_port gwp = gateway_port_by_id.get(k);
            System.out.println(k + "\t" + gwp.get_gateway() + "\t" + gwp.get_connectortype() + gwp.get_connectorno());
        }
    }

    public Hashtable<String, gateway_port> get_gateway_port_by_id() {
        return gateway_port_by_id;
    }

    public Hashtable <String, String> get_alias_table() {
        return alias_table;
    }

    private void init_alias_table() {
        NodeList nl = document.getElementsByTagName("alias");
        alias_table.clear();
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            alias_table.put(el.getAttribute("id"), el.getAttribute("device"));
        }
    }

    private void print_aliases() {
        for (Enumeration e = alias_table.keys(); e.hasMoreElements(); ) {
            String k = (String)e.nextElement();
            System.out.println(k + "\t" + alias_table.get(k));
        }
    }

    private void init_device_groups() {
        device_groups_table.clear();
        NodeList nl = document.getElementsByTagName("device-group");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            NodeList dnl = el.getElementsByTagName("deviceref");
            Vector<String> devs = new Vector<String>();
            for (int j = 0; j < dnl.getLength(); j++) {
                Element dev = (Element) dnl.item(j);
                devs.add(dev.getAttribute("device"));
            }
            device_group dg = new device_group(el.getAttribute("id"),
                    el.getAttribute("name"),
                    el.getAttribute("zone"), devs);
            device_groups_table.put(el.getAttribute("name"), dg);
        }
    }

    private void print_device_groups() {
        for (Enumeration e = device_groups_table.keys(); e.hasMoreElements();) {
            String id = (String) e.nextElement();
            System.out.println(id);
            device_group dg = device_groups_table.get(id);
            for (Enumeration f = dg.get_devices().elements(); f.hasMoreElements();) {
                String dev = (String) f.nextElement();
                System.out.println("\t" + dev);
            }
        }
    }

    public Hashtable <String, device_group> get_device_groups_table() {
        return device_groups_table;
    }

    private void init_gateways() {
        gateway_table.clear();
        NodeList nl = document.getElementsByTagName("gateway");
        for (int i = 0; i < nl.getLength(); i++) {
            //String hostname = "";
            Hashtable<command_t, commandmapping> commandmappings = new Hashtable<command_t, commandmapping>();
            Hashtable<commandtype_t, Hashtable<Integer, port>> ports_table = new Hashtable<commandtype_t, Hashtable<Integer, port>>();
            Vector<gateway_port> gateway_ports = new Vector<gateway_port>();
            Element gw_el = (Element) nl.item(i);
            NodeList nodes = gw_el.getChildNodes();
            for (int j = 0; j < nodes.getLength(); j++) {
                Node node = nodes.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    if (nodeElement.getTagName().equals("commandmappings")) {
                        commandmappings = parse_commandmappings(nodeElement);
                    }
                    if (nodeElement.getTagName().equals("gateway-port")) {
                        gateway_port gwp = parse_gateway_port(nodeElement);
                        gateway_ports.add(gwp);
                    }
                    if (nodeElement.getTagName().equals("gateway-port-ref")) {
                        String gp = nodeElement.getAttribute("gateway-port");
                        gateway_port gwp = gateway_port_by_id.get(gp);
                        gateway_ports.add(gwp);
                    }
                    //if (nodeElement.getTagName().equals("hostname")) {
                    //    hostname = nodeElement.getAttribute("ipname");
                    //}
                    if (nodeElement.getTagName().equals("ports")) {
                        //Vector<port> ports = parse_ports(nodeElement);
                        commandtype_t type = commandtype_t.valueOf(nodeElement.getAttribute("type"));
                        Hashtable<Integer, port> port_table = new Hashtable<Integer, port>();
                        NodeList portlist = nodeElement.getElementsByTagName("port");
                        for (int k = 0; k < portlist.getLength(); k++) {
                            Element el = (Element) portlist.item(k);
                            Hashtable<command_t, commandmapping> cmdmaps = new Hashtable<command_t, commandmapping>();
                            //commandmappings cm = null;
                            NodeList cmdmapsnodes = el.getElementsByTagName("commandmappings");
                            if (cmdmapsnodes.getLength() > 0)
                                cmdmaps = parse_commandmappings((Element) cmdmapsnodes.item(0));
                            port p = new port(parse_int(el.getAttribute("number"), -1),
                                    parse_int(el.getAttribute("baud"), -1), cmdmaps);
                            port_table.put(Integer.valueOf(p.get_number()), p);
                        }
                        ports_table.put(type, port_table);
                    }
                }
            }
            int timeout = 4321;
            try {
                Integer.parseInt(gw_el.getAttribute("timeout"));
            } catch (NumberFormatException e) {
            }
            gateway gw = new gateway(gw_el.getAttribute("hostname"),
                    gateway_ports,
                    ports_table,
                    commandmappings,
                    gw_el.getAttribute("class"),
                    gw_el.getAttribute("model"),
                    gw_el.getAttribute("interface"),
                    gw_el.getAttribute("deviceclass"),
                    gw_el.getAttribute("firmware"),
                    parse_yes_no(gw_el.getAttribute("www")),
                    parse_yes_no(gw_el.getAttribute("web_api")),
                    Integer.parseInt(gw_el.getAttribute("web_api_portnumber")),
                    gw_el.getAttribute("name"),
                    gw_el.getAttribute("id"),
                    timeout);
            gateway_table.put(gw_el.getAttribute("id"), gw);

        }
    }

    /*private void print_gateways() {
        for (Enumeration e = gateway_table.keys(); e.hasMoreElements();) {
            String id = (String) e.nextElement();
            gateway gw = gateway_table.get(id);
            System.out.println(id + "\t" + gw.get_hostname() + "\t" + gw.get_commandmappings().isEmpty());
            
        }
    }*/
    
    public Hashtable <String, gateway> get_gateway_table() {
        return gateway_table;
    }

    Hashtable<String, input> parse_inputs(Element element) {
        Hashtable<String, input> inputs = new Hashtable<String, input>();
        NodeList nl = element.getElementsByTagName("input");
        for (int i = 0; i < nl.getLength(); i++) {
            input inp = parse_input((Element)nl.item(i));
            inputs.put(inp.get_name(), inp);
        }
        return inputs;
    }

    input parse_input(Element element) {
        Vector<String> devicerefs = new Vector<String>();
        //HashSet<String> connectiontypes = new HashSet<String>();
        HashSet<String> internalsrc = new HashSet<String>();
        HashSet<String> externalsrc = new HashSet<String>();
        Hashtable<mediatype, command_t> selectcommands = new Hashtable<mediatype, command_t>();
        Hashtable<mediatype, input.querycommand> querycommands = new Hashtable<mediatype, input.querycommand>();
        Hashtable<String, input.zone> zones = new Hashtable<String, input.zone>();
        Vector<input.connector> connectors = new Vector<input.connector>();

        org.w3c.dom.NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            switch (node.getNodeType()) {
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                    break;
                case org.w3c.dom.Node.ELEMENT_NODE:
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    //if (nodeElement.getTagName().equals("deviceref")) {
                    //    devicerefs.add(nodeElement.getAttribute("device"));
                    //}
                    //if (nodeElement.getTagName().equals("connectiontype")) {
                    //    connectiontypes.add(nodeElement.getAttribute("type"));
                    //}
                    if (nodeElement.getTagName().equals("connector")) {
                       input.connector c = parse_connector(nodeElement);
                       connectors.add(c);
                    }
                    if (nodeElement.getTagName().equals("internalsrc")) {
                        internalsrc.add(nodeElement.getAttribute("name"));
                    }
                    if (nodeElement.getTagName().equals("externalsrc")) {
                        externalsrc.add(nodeElement.getAttribute("name"));
                    }
                    if (nodeElement.getTagName().equals("selectcommand")) {
                        selectcommands = parse_selectcommand(nodeElement);
                    }
                    if (nodeElement.getTagName().equals("querycommand")) {
                        input.querycommand qc = parse_querycommand(nodeElement);
                        querycommands.put(qc.get_type(), qc);
                    }
                    if (nodeElement.getTagName().equals("zone")) {
                        input.zone z = parse_zone(nodeElement);
                        zones.put(z.get_name(), z);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }

        input inp = new input(element.getAttribute("name"),
                element.getAttribute("myname"),
                parse_yes_no(element.getAttribute("audio")),
                parse_yes_no(element.getAttribute("video")),
                selectcommands,
                querycommands,
                zones,
                connectors,
                //devicerefs,
                internalsrc,
                externalsrc);
        return inp;
    }
    
    Hashtable<mediatype, command_t> parse_selectcommand(Element el) {
        Hashtable<mediatype, command_t> cmd = new Hashtable<mediatype, command_t>(3);
        String av = el.getAttribute("audio_video");
            cmd.put(mediatype.audio_video, command_t.parse(av));
        String a = el.getAttribute("audio_only");
        if (!a.isEmpty())
            cmd.put(mediatype.audio_only, command_t.parse(a));
        String v = el.getAttribute("video_only");
        if (!v.isEmpty())
            cmd.put(mediatype.video_only, command_t.parse(v));
        return cmd;
    }

    input.connector parse_connector(Element el) {
        HashSet<String> deviceref = new HashSet<String>();
        NodeList nl = el.getElementsByTagName("deviceref");
        for (int i = 0; i < nl.getLength(); i++)
            deviceref.add(((Element) nl.item(i)).getAttribute("device"));
        String n_string = el.getAttribute("number");
        int number = -1;
        try {
            number = Integer.parseInt(n_string);
        } catch (NumberFormatException e) {
        }
        return new input.connector(connectiontype.parse(el.getAttribute("connectiontype")),
                el.getAttribute("hardware"),
                number,
                el.getAttribute("version"),
                el.getAttribute("remark"),
                deviceref);
        
    }

    input.zone parse_zone(Element element) {
        Hashtable<mediatype, command_t> selectcommands = new Hashtable<mediatype, command_t>(3);
        Hashtable<mediatype, input.querycommand> querycommands = new Hashtable<mediatype, input.querycommand>(3);
        org.w3c.dom.NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            org.w3c.dom.Node node = nodes.item(i);
            switch (node.getNodeType()) {
                case org.w3c.dom.Node.CDATA_SECTION_NODE:
                    break;
                case org.w3c.dom.Node.ELEMENT_NODE:
                    org.w3c.dom.Element nodeElement = (org.w3c.dom.Element) node;
                    if (nodeElement.getTagName().equals("selectcommand")) {
                        selectcommands = parse_selectcommand(nodeElement);
                    }
                    if (nodeElement.getTagName().equals("querycommand")) {
                        //mediatype type = mediatype.valueOf(nodeElement.getAttribute("mediatype"));
                        input.querycommand qc = parse_querycommand(nodeElement);
                        //new input.querycommand(command_t.parse(nodeElement.getAttribute("cmd")),nodeElement.getAttribute("val"));
                        querycommands.put(qc.get_type(), qc);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }
        return new input.zone(element.getAttribute("name"), selectcommands, querycommands);
    }

    input.querycommand parse_querycommand(Element element) {
        mediatype type = mediatype.valueOf(element.getAttribute("mediatype"));
        return new input.querycommand(command_t.parse(element.getAttribute("cmd")),
                                element.getAttribute("val"), type);
    }

    Hashtable<command_t, commandmapping> parse_commandmappings(org.w3c.dom.Element element) {
        Hashtable<command_t, commandmapping> cmdmappings = new Hashtable<command_t, commandmapping>();
        org.w3c.dom.NodeList nodes = element.getElementsByTagName("commandmapping");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            commandmapping cmdmap = parse_commandmapping(el);
            command_t cmd = command_t.parse(el.getAttribute("cmd"));
            cmdmappings.put(cmd, cmdmap);
        }
        return cmdmappings;
    }

    private commandmapping parse_commandmapping(Element element) {
        Short devno = -1;
        try {
            devno = Short.parseShort(element.getAttribute("deviceno"));
        } catch (NumberFormatException e) {
        }
        //int devno = devno_str.isEmpty() ? -1 : Integer.parseInt(devno_str);
        return new commandmapping(command_t.parse(element.getAttribute("cmd")),
                element.getAttribute("house"),
                devno,
                command_t.parse(element.getAttribute("commandname")),
                element.getAttribute("remotename"));
    }

    gateway_port parse_gateway_port(Element element) {
        return new gateway_port(element.getAttribute("id"),
                element.getAttribute("gateway"),
                parse_int(element.getAttribute("connectorno"), -1),
                commandtype_t.valueOf(element.getAttribute("connectortype")),
                element.getAttribute("hostname"),
                parse_int(element.getAttribute("portnumber"), -1),
                element.getAttribute("mac"),
                parse_yes_no(element.getAttribute("wol")),
                parse_int(element.getAttribute("timeout"),10000));
    }

    public static void main(String[] args) {
        String filename = args[0];
        org.w3c.dom.Document dom = null;

       try {
            dom = harcutils.open_xmlfile(filename);
        } catch (IOException ex) {
           
        } catch (SAXParseException ex) {
           
        } catch (SAXException ex) {
        }
        home_parser p = new home_parser(dom);
        //p.print_aliases();
        //p.print_device_groups();
        //p.print_gateways();
        p.print_gateway_port_by_id();
    }

}
