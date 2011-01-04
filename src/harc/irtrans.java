/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

import java.io.*;
import java.net.*;
//import java.lang.Thread;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;

public class irtrans {

    private String irtrans_host;
    private final static int irtrans_port = 21000;
    public final static String default_irtrans_host = "192.168.1.71";
    public final static int led_internal = 1;
    public final static int led_external = 2;
    public final static int led_both = 3;

    // so many char name[20] in itrans code (19?)
    public final static int max_name_length = 21;
    private final static String send_flashed_command_ack = "**00018 RESULT OK";

    private static String led_char(int led) {
        return led == led_external ? ",le" : led == led_both ? ",lb" : ",li";
    }

    private int decode_connector(String con) {
        return con.equals("1") || con.equals("internal") || con.equals("intern") ? 1 : con.equals("2") || con.equals("external") || con.equals("extern") ? 2 : 3;
    }
    private boolean verbose = true;

    public irtrans(String hostname, boolean verbose) {
        irtrans_host = hostname != null ? hostname : default_irtrans_host;
        this.verbose = verbose;
    }

    public irtrans(String hostname) {
        this(hostname, false);
    }

    public irtrans(boolean verbose) {
        this(default_irtrans_host, verbose);
    }

    public irtrans() {
        this(default_irtrans_host, false);
    }

    public void set_verbosity(boolean verbosity) {
        this.verbose = verbosity;
    }

    // TODO: shorten the timeout
    public String send_command(String cmd)
            throws UnknownHostException, IOException {
        if (verbose) {
            System.err.println("Sending command `" + cmd + "' to Irtrans");
        }
        boolean success = true;
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer =
                new BufferedReader(new InputStreamReader(sock.getInputStream()));

        outToServer.print("ASCI");
        try {
            Thread.currentThread().sleep(10); // Yes, this is necessary
        } catch (InterruptedException e) {
            success = false;
        }
        outToServer.print(cmd);
        result = inFromServer.readLine();
        if (verbose) {
            System.out.println(result);
        }

        sock.close();
        return result;
    }

    public String[] get_table(String str) {
        if (verbose) {
            System.err.println("Sending command `" + str + "0' to Irtrans");
        }
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
            outToServer =
                    new PrintStream(sock.getOutputStream());
            inFromServer =
                    new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + irtrans_host + " does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + irtrans_host);
            System.exit(1);
        }

        String[] remotes = null;
        try {
            outToServer.print("ASCI");
            Thread.currentThread().sleep(10); // Yes, this is necessary
            int index = 0;
            int no_remotes = 99999;
            while (index < no_remotes - 1) {
                Thread.currentThread().sleep(100);
                outToServer.print(str + index + "\r");
                result = inFromServer.readLine(); // hangs here sometimes
                int second_space = result.indexOf(' ', 9);
                if (verbose) {
                    System.err.println(result);
                }
                String[] words = result.substring(second_space + 1, result.length()).split(",");
                index = Integer.parseInt(words[0]);
                no_remotes = Integer.parseInt(words[1]);
                int chunk = Integer.parseInt(words[2]);
                if (remotes == null) {
                    remotes = new String[no_remotes];
                }

                for (int c = 0; c < chunk; c++) {
                    remotes[index + c] = words[3 + c];
                }

                index += chunk;
            }
        } catch (InterruptedException e) {
        } catch (IOException e) {
            System.err.println("Couldn't read from " + irtrans_host);
            System.exit(1);
        }

        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Couldn't close socket.");
            System.exit(1);
        }
        return remotes;
    }

    public String[] get_remotes() {
        return get_table("Agetremotes ");
    }

    public String[] get_commands(String remote) {
        return get_table("Agetcommands " + remote + ",");
    }

    public void listen(String configfilename) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(false);
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            //doc = builder.newDocument();
            doc = builder.parse(configfilename);
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            System.exit(421);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(421);
        }

        Element map = doc.getDocumentElement();
        NodeList events = map.getElementsByTagName("event");

        if (verbose) {
            System.err.println("Listening to Irtrans");
        }
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
            outToServer =
                    new PrintStream(sock.getOutputStream());
            inFromServer =
                    new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + irtrans_host + " does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + irtrans_host);
            System.exit(1);
        }

        //try {
        outToServer.print("ASCI");
        //Thread.currentThread().sleep(10); // Yes, this is necessary
        //outToServer.print(cmd);
        //} catch (InterruptedException e) {
        //}


        boolean quit = false;
        do {
            try {
                result = inFromServer.readLine();
            } catch (IOException e) {
                System.err.println("Couldn't read from " + irtrans_host);
                System.exit(1);
            }
            String res[] = result.split(",");
            String x[] = res[0].split(" ");
            String remote = x[2];
            String commandname = res[1];

            if (verbose) {
                System.err.print("Received " + remote + "," + commandname);
            }

            boolean has_action = false;
            for (int i = 0; i < events.getLength(); i++) {
                Element event = (Element) events.item(i);
                Element ircode = (Element) event.getElementsByTagName("ir-command").item(0);
                String r = ircode.getAttribute("remote");
                String c = ircode.getAttribute("command");
                if (remote.equals(r) && commandname.equals(c)) {
                    if (verbose) {
                        System.err.print(", found action: ");
                    }
                    has_action = true;
                    NodeList actions = event.getElementsByTagName("action");
                    for (int j = 0; j < actions.getLength(); j++) {
                        Node the_action = actions.item(j);
                        if (the_action.getNodeType() == Node.ELEMENT_NODE) {
                            Node act;
                            for (act = the_action.getFirstChild(); act.getNodeType() != Node.ELEMENT_NODE; act = act.getNextSibling());
                            String nodename = act.getNodeName();
                            if (nodename.equals("quit")) {
                                quit = true;
                                if (verbose) {
                                    System.err.println("quit");
                                }
                            } else if (nodename.equals("log-string")) {
                                String payload = act.getFirstChild().getNodeValue();
                                if (verbose) {
                                    System.err.println("log: " + payload);
                                }
                            } else if (nodename.equals("exec")) {
                                boolean wait = ((Element) act).getAttribute("wait").equals("true");
                                Node n = ((Element) act).getElementsByTagName("progname").item(0);
                                String progname = ((Element) n).getAttribute("filename");
                                NodeList args = ((Element) act).getElementsByTagName("argument");


                                if (verbose) {
                                    System.err.print("exec " + (wait ? "(wait)" : "(nowait)") + " \"" + progname);
                                }
                                String[] cmd_array = new String[args.getLength() + 1];
                                cmd_array[0] = progname;
                                for (int m = 0; m < args.getLength(); m++) {
                                    cmd_array[m + 1] = ((Element) args.item(m)).getFirstChild().getNodeValue();
                                    if (verbose) {
                                        System.err.print(" " + cmd_array[m + 1]);
                                    }
                                }
                                if (verbose) {
                                    System.err.println("\"");
                                }

                                try {
                                    java.lang.Process proc = java.lang.Runtime.getRuntime().exec(cmd_array);
                                // TODO: implement wait
                                } catch (java.io.IOException e) {
                                }
                            }
                        }
                    }
                }

            }
            if (verbose && !has_action) {
                System.err.println(", no action found");
            }

        } while (!quit);


        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Couldn't close socket.");
            System.exit(1);
        }
    }
    public final static int command_sendccf = 28;
    public final static int mode = 0;
    public final static int address = 0;
    public final static int timeout = 0;
    public final static int protocol_version = 200;

    public void XXXsend_command(String cmd) {
        if (verbose) {
            System.err.println("Sending command `" + cmd + "' RAW TCP to Irtrans");
        }
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
            outToServer =
                    new PrintStream(sock.getOutputStream(), true);
            inFromServer =
                    new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + irtrans_host + " does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + irtrans_host);
            System.exit(1);
        }

        byte[] b = new byte[272];
        int i = 0;
        b[i++] = command_sendccf;
        b[i++] = 0;

        b[i++] = 0;
        b[i++] = 0;
        b[i++] = 0;
        b[i++] = 0;

        b[i++] = (byte) 0xc8;
        b[i++] = 0;

        b[i++] = 0x00;
        b[i++] = 0x50;

        b[i++] = 0x73;
        b[i++] = 0x00;

        b[i++] = 0x00;
        b[i++] = 0x00;

        b[i++] = 0x01;
        b[i++] = 0x00;

        b[i++] = 0x00;
        b[i++] = 0x00;

        b[i++] = 0x00;
        b[i++] = 0x00;



        //try {
// 	    outToServer.writeByte(command_sendccf);
// 	    outToServer.writeByte(mode);
// 	    outToServer.writeShort(timeout);
// 	    outToServer.writeInt(address);
// 	    outToServer.writeShort(0x5000);
// 	    outToServer.writeShort(0x0073);
// 	    outToServer.writeShort(0x0000);
// 	    outToServer.writeShort(0x0001);
// 	    outToServer.writeShort(0x0000);
// 	    outToServer.writeShort(0x0000);

        outToServer.write(b, 0, 272);
        System.err.println("GGGGGGGGG");

        //} catch (IOException e) {
        //System.err.println("IOexception " + e.getMessage());
        //}

        try {
            result = inFromServer.readLine();
            System.err.println("GGGG000000000GGGGG");
        } catch (IOException e) {
            System.err.println("Couldn't read from " + irtrans_host);
            System.exit(1);
        }

        try {
            sock.close();
        } catch (IOException e) {
            System.err.println("Couldn't close socket.");
            System.exit(1);
        }
        if (verbose) {
            System.out.println(result);
        }
    }

    public boolean send_command_udp(String cmd) /*throws UnknownHostException,IOException*/ {
        boolean success = false;
        if (verbose) {
            System.err.println("Sending command `" + cmd + "' to Irtrans/UDP");
        }
        try {
            DatagramSocket sock = new DatagramSocket();
            InetAddress addr = InetAddress.getByName(irtrans_host);
            byte[] buf = cmd.getBytes("US-ASCII");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, irtrans_port);
            sock.send(dp);
            sock.close();
            success = true;
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return success;
    }

    public static String make_url(String hostname, String remote,
            String command, int led) {
        return make_url(hostname, remote, command,
                (led == 3 ? "both" : led == 2 ? "extern" : "intern"));
    }

    public static String make_url(String hostname, String remote,
            String command, String led) {
        return "http://" + (hostname != null ? hostname : default_irtrans_host) + "/send.htm?remote=" + remote + "&command=" + command + "&led=" + led;
    }
    private java.lang.Process netcat_process;

    private void xsend_command_udp(String command) {
        if (verbose) {
            System.err.println("Sending command `" + command + "' to IRTrans/UDP.");
        }

        String cmd_array[] = new String[4];
        int i = 0;
        cmd_array[i++] = "/usr/bin/netcat";
        cmd_array[i++] = "-u";
        cmd_array[i++] = irtrans_host;
        cmd_array[i++] = (new Integer(irtrans_port)).toString();

        try {
            netcat_process = java.lang.Runtime.getRuntime().exec(cmd_array);
            PrintStream proc_stdin = new PrintStream(netcat_process.getOutputStream());
            proc_stdin.println(command);
            proc_stdin.flush();
            Thread.currentThread().sleep(10); // Yes, this is necessary

            netcat_process.destroy();
            netcat_process.waitFor();
        } catch (java.io.IOException e) {
            System.err.println("Could not exec command `" + cmd_array[0] + " " + cmd_array[1] + " " + cmd_array[2] + " " + cmd_array[3] + "'.");
        } catch (java.lang.InterruptedException e) {
            System.err.println("Interrupted...");
        }
    }

    public boolean send_flashed_command(String remote, int cmd, int connector, int count) throws UnknownHostException, IOException {
        return send_flashed_command(remote, ir_code.command_name(cmd), connector, count);
    }

    public boolean send_flashed_command(String remote, int cmd, String connector, int count) throws UnknownHostException, IOException {
        return send_flashed_command(remote, cmd, decode_connector(connector), count);
    }

    public boolean send_flashed_command(String remote, String command, int connector, int count) throws UnknownHostException, IOException {
        boolean success = send_flashed_command(remote, command, connector, false);
        for (int i = 1; i < count; i++) {
            success = success && send_flashed_command(remote, command, connector, true);
        }
        return success;
    }

    public boolean send_flashed_command(String remote, String command, int led, boolean repeat) throws UnknownHostException, IOException {
        if (command.startsWith("cmd_")) /* only temporary, FIXME */
            command = command.substring(4);
        return send_command("Asnd" + (repeat ? "r" : "") + " " + remote + "," + command + led_char(led)).equals(send_flashed_command_ack);
    }

    public boolean send_flashed_command(String remote, String command) throws UnknownHostException, IOException {
        return send_flashed_command(remote, command, led_internal, false);
    }

    public boolean send_flashed_command_udp(String remote, String command,
            int led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_command_udp("snd" + (repeat ? "r" : "") + " " + remote + "," + command + led_char(led));
    }

    public boolean send_flashed_command_udp(String remote, String command)
            throws UnknownHostException, IOException {
        return send_flashed_command_udp(remote, command, led_internal, false);
    }

    public boolean send_ir(String ccf_string, int led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_command_udp("sndccf" + (repeat ? "r" : "") + " " + ccf_string + led_char(led));
    }

    public boolean send_ir(ir_code code, int led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_ir(code.ccf_string(), led, repeat);
    }

    public boolean send_ir(ir_code code)
            throws UnknownHostException, IOException {
        return send_ir(code, led_internal, false);
    }

    public boolean send_ir(ir_code code, int led)
            throws UnknownHostException, IOException {
        return send_ir(code, led, false);
    }

    public boolean send_ir(ir_code code, int led, int count)
            throws UnknownHostException, IOException {
        boolean success = true;
        for (int c = 0; c < count; c++) {
            success = success && send_ir(code, led, c > 0);
        }
        return success;
    }

    public boolean send_ir(ir_code code, String led, int count)
            throws UnknownHostException, IOException {
        return send_ir(code, decode_connector(led), count);
    }

    public static void main(String args[]) {
        boolean verbose = false;
        String irtrans_host = default_irtrans_host;
        String configfilename = "listen.xml";

        int optarg = 0;
        if (args.length > optarg && args[optarg].equals("-v")) {
            optarg++;
            verbose = true;
        }
        if (args.length > optarg + 1 && args[optarg].equals("-h")) {
            irtrans_host = args[optarg + 1];
            optarg += 2;
        }

        irtrans irt = new irtrans(irtrans_host, verbose);

        if (args.length > optarg && args[optarg].equals("-r")) {
            if (args.length == optarg + 1) {
                String[] remotes = irt.get_remotes();
                for (int i = 0; i < remotes.length; i++) {
                    System.out.println(remotes[i]);
                }
            } else {
                String remote = args[optarg + 1];
                String[] commands = irt.get_commands(remote);
                for (int i = 0; i < commands.length; i++) {
                    System.out.println(commands[i]);
                }
            }
        } else {
            if (args.length > optarg) {
                configfilename = args[optarg];
            }

            irt.listen(configfilename);
        }
    }
}
