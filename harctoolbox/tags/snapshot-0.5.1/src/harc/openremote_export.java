package harc;

import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class openremote_export {

    private home hm = null;
    private macro_engine mac = null;
    private Document doc = null;
    private int index_counter = 1;

    private final static String harc_host = "localhost";
    private final static int harc_portno = 9999;
    private final static String openremote_namespace = "http://www.openremote.org";
    private final static String schema_namespace = "http://www.w3.org/2001/XMLSchema-instance";
    private final static String schema_location = "http://www.openremote.org controller-1.0-M3.xsd";
    private final static String controller_filename = "controller.xml";

    private Element socket_events = null;
    private Element buttons = null;

    private void make_harc_event(String label, String command) {
        Element ev = doc.createElement("tcpSocketEvent");
        ev.setAttribute("id", event_id());
        ev.setAttribute("label", label);
        ev.setAttribute("command", command);
        ev.setAttribute("ip", harc_host);
        ev.setAttribute("port", Integer.toString(harc_portno));
        socket_events.appendChild(ev);
    }

    private void make_button() {
        Element btn = doc.createElement("button");
        btn.setAttribute("id", button_id());
        buttons.appendChild(btn);
        Element e = doc.createElement("event");
        e.setTextContent(event_id());
        btn.appendChild(e);
    }

    private String event_id() {
        return Integer.toString(2 * index_counter);
    }

    private String button_id() {
        return Integer.toString(2*index_counter + 1);
    }

    openremote_export(home hm, macro_engine mac) {
        this.hm = hm;
        this.mac = mac;
        doc = harcutils.newDocument();
        Element root = doc.createElement("openremote");
        root.setAttribute("xmlns", openremote_namespace);
        root.setAttribute("xmlns:xsi", schema_namespace);
        root.setAttribute("xsi:schemaLocation", schema_location);
        doc.appendChild(root);
        buttons = doc.createElement("buttons");
        root.appendChild(buttons);
        Element events = doc.createElement("events");
        root.appendChild(events);
        Element ir_events = doc.createElement("irEvents");
        events.appendChild(ir_events);
        Element knx_events = doc.createElement("knxEvents");
        events.appendChild(knx_events);
        Element x10_events = doc.createElement("x10Events");
        events.appendChild(x10_events);
        Element http_events = doc.createElement("httpEvents");
        events.appendChild(http_events);
        socket_events = doc.createElement("socketEvents");
        events.appendChild(socket_events);
        Element telnet_events = doc.createElement("telnetEvents");
        events.appendChild(telnet_events);

        String devices[] = hm.get_devices();
        for (int i = 0; i < devices.length; i++) {
            device dev = hm.get_device(devices[i]);
            if (dev != null) {
                command_t cmds[] = dev.get_commands();
                if (cmds != null) {
                    for (int j = 0; j < cmds.length; j++) {
                        command c = dev.get_command(cmds[j]);
                        if (c != null && c.get_no_arguments() == 0) {
                            make_harc_event(cmds[j].name(), devices[i] + " " + cmds[j].name());
                            make_button();
                            index_counter++;
                        }
                    }
                }
            }
        }

        if (mac != null) {
            String macros[] = mac.get_macros(false);
            for (int i = 0; i < macros.length; i++) {
                make_harc_event(macros[i], macros[i]);
                make_button();
                index_counter++;
            }
        }
    }

    void print(String filename) throws FileNotFoundException {
        harcutils.printDOM(filename, doc);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        home hm = null;
        macro_engine mac = null;
        String filename = controller_filename;
        try {
            hm = new home();
            mac = new macro_engine(hm);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (SAXParseException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        }

        openremote_export exporter = new openremote_export(hm, mac);
        try {
            exporter.print(filename);
        } catch (FileNotFoundException ex) {
            System.err.println("Cannot open file " + filename);
        }
    }
}
