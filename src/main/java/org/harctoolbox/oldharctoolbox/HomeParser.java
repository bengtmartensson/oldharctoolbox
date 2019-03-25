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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import org.harctoolbox.ircore.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class HomeParser {
    private LinkedHashMap<String, Dev> device_table = new LinkedHashMap<>(64);
    private HashMap<String, String> alias_table = new HashMap<>(64);
    private LinkedHashMap<String, DeviceGroup> device_groups_table = new LinkedHashMap<>(16);
    private HashMap<String, Gateway> gateway_table = new HashMap<>(16);

    private HashMap<String, GatewayPort> gateway_port_by_id = new HashMap<>(16);

    private final Document document;

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

    public HomeParser(org.w3c.dom.Document document) {
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
            Dev d = parse_device(el);
            device_table.put(d.get_id(), d);
        }
    }

    public LinkedHashMap<String, Dev> get_device_table() {
        return device_table;
    }

    private Dev parse_device(Element element) {
        String description = "";
        String notes = "";
        String powered_through = "";
        String defaultzone = "";
        HashMap <String, String> attributes = new HashMap <>(16);
        ArrayList<GatewayPort> gateway_ports = new ArrayList<>(16);
        HashMap<String, Input> inputs = new HashMap<>(16);

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
                        GatewayPort gwp = parse_gateway_port(nodeElement);
                        gateway_ports.add(gwp);
                    }
                    if (nodeElement.getTagName().equals("gateway-port-ref")) {
                        String g = nodeElement.getAttribute("gateway-port");
                        GatewayPort gwp = gateway_port_by_id.get(g);
                        gateway_ports.add(gwp);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }
        Dev d = new Dev(element.getAttribute("name"),
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
            GatewayPort gwp = parse_gateway_port(el);
            String id = gwp.get_id();
            if (!id.isEmpty())
                gateway_port_by_id.put(id, gwp);
        }
    }

    private void print_gateway_port_by_id() {
        //for (Enumeration e = gateway_port_by_id.keys(); e.hasMoreElements(); ) {
        //    String k = (String)e.nextElement();
        gateway_port_by_id.keySet().forEach((k) -> {
            GatewayPort gwp = gateway_port_by_id.get(k);
            System.out.println(k + "\t" + gwp.get_gateway() + "\t" + gwp.get_connectortype() + gwp.get_connectorno());
        });
    }

    public HashMap<String, GatewayPort> get_gateway_port_by_id() {
        return gateway_port_by_id;
    }

    public HashMap<String, String> get_alias_table() {
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

    private void init_device_groups() {
        device_groups_table.clear();
        NodeList nl = document.getElementsByTagName("device-group");
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            NodeList dnl = el.getElementsByTagName("deviceref");
            ArrayList<String> devs = new ArrayList<>(16);
            for (int j = 0; j < dnl.getLength(); j++) {
                Element dev = (Element) dnl.item(j);
                devs.add(dev.getAttribute("device"));
            }
            DeviceGroup dg = new DeviceGroup(el.getAttribute("id"),
                    el.getAttribute("name"),
                    el.getAttribute("zone"), devs);
            device_groups_table.put(el.getAttribute("name"), dg);
        }
    }

//    private void print_device_groups() {
//        //for (Enumeration e = device_groups_table.keys(); e.hasMoreElements();) {
//        //    String id = (String) e.nextElement();
//        device_groups_table.keySet().stream().map((id) -> {
//            System.out.println(id);
//            return id;
//        }).map((id) -> device_groups_table.get(id)).forEachOrdered((DeviceGroup dg) -> {
//            //for (Enumeration f = dg.get_devices().elements(); f.hasMoreElements();) {
//            //    String dev = (String) f.nextElement();
//            dg.get_devices().forEach((dev) -> {
//                System.out.println("\t" + dev);
//            });
//        });
//    }

    public LinkedHashMap<String, DeviceGroup> get_device_groups_table() {
        return device_groups_table;
    }

    private void init_gateways() {
        gateway_table.clear();
        NodeList nl = document.getElementsByTagName("gateway");
        for (int i = 0; i < nl.getLength(); i++) {
            //String hostname = "";
            HashMap<command_t, CommandMapping> commandmappings = new HashMap<>(16);
            EnumMap<CommandType_t, HashMap<Integer, Port>> ports_table = new EnumMap<>(CommandType_t.class);
            ArrayList<GatewayPort> gateway_ports = new ArrayList<>(16);
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
                        GatewayPort gwp = parse_gateway_port(nodeElement);
                        gateway_ports.add(gwp);
                    }
                    if (nodeElement.getTagName().equals("gateway-port-ref")) {
                        String gp = nodeElement.getAttribute("gateway-port");
                        GatewayPort gwp = gateway_port_by_id.get(gp);
                        gateway_ports.add(gwp);
                    }
                    //if (nodeElement.getTagName().equals("hostname")) {
                    //    hostname = nodeElement.getAttribute("ipname");
                    //}
                    if (nodeElement.getTagName().equals("ports")) {
                        //Vector<port> ports = parse_ports(nodeElement);
                        CommandType_t type = CommandType_t.valueOf(nodeElement.getAttribute("type"));
                        HashMap<Integer, Port> port_table = new HashMap<>(16);
                        NodeList portlist = nodeElement.getElementsByTagName("port");
                        for (int k = 0; k < portlist.getLength(); k++) {
                            Element el = (Element) portlist.item(k);
                            HashMap<command_t, CommandMapping> cmdmaps = new HashMap<>(64);
                            //commandmappings cm = null;
                            NodeList cmdmapsnodes = el.getElementsByTagName("commandmappings");
                            if (cmdmapsnodes.getLength() > 0)
                                cmdmaps = parse_commandmappings((Element) cmdmapsnodes.item(0));
                            Port p = new Port(parse_int(el.getAttribute("number"), -1),
                                    parse_int(el.getAttribute("baud"), -1), cmdmaps);
                            port_table.put(p.get_number(), p);
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
            Gateway gw = new Gateway(gw_el.getAttribute("hostname"),
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

    public HashMap<String, Gateway> get_gateway_table() {
        return gateway_table;
    }

    HashMap<String, Input> parse_inputs(Element element) {
        HashMap<String, Input> inputs = new HashMap<>(8);
        NodeList nl = element.getElementsByTagName("input");
        for (int i = 0; i < nl.getLength(); i++) {
            Input inp = parse_input((Element)nl.item(i));
            inputs.put(inp.get_name(), inp);
        }
        return inputs;
    }

    Input parse_input(Element element) {
        ArrayList<String> devicerefs = new ArrayList<>(32);
        //HashSet<String> connectiontypes = new HashSet<String>();
        HashSet<String> internalsrc = new HashSet<>(8);
        HashSet<String> externalsrc = new HashSet<>(8);
        EnumMap<MediaType, command_t> selectcommands = new EnumMap<>(MediaType.class);
        EnumMap<MediaType, Input.QueryCommand> querycommands = new EnumMap<>(MediaType.class);
        HashMap<String, Input.Zone> zones = new HashMap<>(4);
        ArrayList<Input.Connector> connectors = new ArrayList<>(8);

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
                       Input.Connector c = parse_connector(nodeElement);
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
                        Input.QueryCommand qc = parse_querycommand(nodeElement);
                        querycommands.put(qc.get_type(), qc);
                    }
                    if (nodeElement.getTagName().equals("zone")) {
                        Input.Zone z = parse_zone(nodeElement);
                        zones.put(z.get_name(), z);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }

        Input inp = new Input(element.getAttribute("name"),
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

    EnumMap<MediaType, command_t> parse_selectcommand(Element el) {
        EnumMap<MediaType, command_t> cmd = new EnumMap<>(MediaType.class);
        String av = el.getAttribute("audio_video");
            cmd.put(MediaType.audio_video, command_t.parse(av));
        String a = el.getAttribute("audio_only");
        if (!a.isEmpty())
            cmd.put(MediaType.audio_only, command_t.parse(a));
        String v = el.getAttribute("video_only");
        if (!v.isEmpty())
            cmd.put(MediaType.video_only, command_t.parse(v));
        return cmd;
    }

    Input.Connector parse_connector(Element el) {
        HashSet<String> deviceref = new HashSet<>(8);
        NodeList nl = el.getElementsByTagName("deviceref");
        for (int i = 0; i < nl.getLength(); i++)
            deviceref.add(((Element) nl.item(i)).getAttribute("device"));
        String n_string = el.getAttribute("number");
        int number = -1;
        try {
            number = Integer.parseInt(n_string);
        } catch (NumberFormatException e) {
        }
        return new Input.Connector(ConnectionType.parse(el.getAttribute("connectiontype")),
                el.getAttribute("hardware"),
                number,
                el.getAttribute("version"),
                el.getAttribute("remark"),
                deviceref);

    }

    Input.Zone parse_zone(Element element) {
        EnumMap<MediaType, command_t> selectcommands = new EnumMap<>(MediaType.class);
        EnumMap<MediaType, Input.QueryCommand> querycommands = new EnumMap<>(MediaType.class);
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
                        Input.QueryCommand qc = parse_querycommand(nodeElement);
                        //new input.querycommand(command_t.parse(nodeElement.getAttribute("cmd")),nodeElement.getAttribute("val"));
                        querycommands.put(qc.get_type(), qc);
                    }
                    break;
                case org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE:
                    break;
            }
        }
        return new Input.Zone(element.getAttribute("name"), selectcommands, querycommands);
    }

    Input.QueryCommand parse_querycommand(Element element) {
        MediaType type = MediaType.valueOf(element.getAttribute("mediatype"));
        return new Input.QueryCommand(command_t.parse(element.getAttribute("cmd")),
                                element.getAttribute("val"), type);
    }

    HashMap<command_t, CommandMapping> parse_commandmappings(org.w3c.dom.Element element) {
        HashMap<command_t, CommandMapping> cmdmappings = new HashMap<>(16);
        org.w3c.dom.NodeList nodes = element.getElementsByTagName("commandmapping");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            CommandMapping cmdmap = parse_commandmapping(el);
            command_t cmd = command_t.parse(el.getAttribute("cmd"));
            cmdmappings.put(cmd, cmdmap);
        }
        return cmdmappings;
    }

    private CommandMapping parse_commandmapping(Element element) {
        Short devno = -1;
        try {
            devno = Short.parseShort(element.getAttribute("deviceno"));
        } catch (NumberFormatException e) {
        }
        //int devno = devno_str.isEmpty() ? -1 : Integer.parseInt(devno_str);
        return new CommandMapping(command_t.parse(element.getAttribute("cmd")),
                element.getAttribute("house"),
                devno,
                command_t.parse(element.getAttribute("commandname")),
                element.getAttribute("remotename"));
    }

    GatewayPort parse_gateway_port(Element element) {
        return new GatewayPort(element.getAttribute("id"),
                element.getAttribute("gateway"),
                parse_int(element.getAttribute("connectorno"), -1),
                CommandType_t.valueOf(element.getAttribute("connectortype")),
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
            dom = XmlUtils.openXmlFile(new File(filename));
        } catch (IOException | SAXException ex) {

        }
        HomeParser p = new HomeParser(dom);
        //p.print_aliases();
        //p.print_device_groups();
        //p.print_gateways();
        p.print_gateway_port_by_id();
    }

}
