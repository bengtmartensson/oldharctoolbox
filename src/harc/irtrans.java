/*
Copyright (C) 2009 Bengt Martensson.

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

package harc;

import java.io.*;
import java.net.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class irtrans {

    private String irtrans_host;
    
    public final static int irtrans_port = 21000;
    public final static String default_irtrans_host = "irtrans";//"192.168.0.31";

    /** IR LEDs on the IRTrans */
    public enum led_t {
        intern,
        extern,
        both, // synonym all
        led_0, // synonym default
        led_1,
        led_2,
        led_3,
        led_4,
        led_5,
        led_6,
        led_7,
        led_8;

        public static String led_char(led_t l) {
            return "l" + (
                      l == intern ? "i"
                    : l == extern ? "e"
                    : l == both ? "b"
                    : l.toString().substring(4));
        }

        public static led_t parse(String s) {
            try {
            return led_t.valueOf(s);
            } catch (IllegalArgumentException e) {
                try {
                    return led_t.valueOf("led_" + s);
                } catch (IllegalArgumentException ee) {
                    return intern;
                }
            }
        }
    }

    // so many char name[20] in itrans code (19?)
    private final static int max_name_length = 21;
    private final static String send_flashed_command_ack = "**00018 RESULT OK";

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

    // TODO: shorten the timeout if host does not respond
    private String send_command(String cmd)
            throws UnknownHostException, IOException, InterruptedException {
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to Irtrans (tcp ascii)");

        boolean success = true;
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
            outToServer = new PrintStream(sock.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

            outToServer.print("ASCI");
            delay();
            outToServer.print(cmd);
            while (!inFromServer.ready())
                Thread.sleep(20);
            result = inFromServer.readLine();
        } finally {
            if (outToServer != null)
                outToServer.close();
            if (inFromServer != null)
                inFromServer.close();
            if (sock != null)
                sock.close();
        }
        if (verbose)
            System.err.println(result);
        return result;
    }

    public String get_version() throws UnknownHostException, IOException, InterruptedException {
        return send_command("Aver");
    }

    public String[] get_table(String str)
            throws InterruptedException, IOException, InterruptedException {
        if (verbose)
            System.err.println("Sending command `" + str + "0' to Irtrans");

        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));

        String[] remotes = null;
        try {
            outToServer.print("ASCI");
            delay();
            int index = 0;
            int no_remotes = 99999;
            while (index < no_remotes - 1) {
                Thread.sleep(100);
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
        } finally {
            outToServer.close();
            inFromServer.close();
            sock.close();
        }
        return remotes;
    }

    public String[] get_remotes() throws InterruptedException, IOException {
        return get_table("Agetremotes ");
    }

    public String[] get_commands(String remote) throws InterruptedException, IOException {
        return get_table("Agetcommands " + remote + ",");
    }

    public void listen(String configfilename) throws IOException, SAXParseException, SAXException {
        Document doc = harcutils.open_xmlfile(configfilename);

        Element map = doc.getDocumentElement();
        NodeList events = map.getElementsByTagName("event");

        if (verbose)
            System.err.println("Listening to Irtrans");

        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String result = "";

        try {
            sock = new Socket(InetAddress.getByName(irtrans_host), irtrans_port);
            outToServer = new PrintStream(sock.getOutputStream());
            inFromServer = new BufferedReader(new InputStreamReader(sock.getInputStream()));
        } catch (UnknownHostException e) {
            System.err.println("Host " + irtrans_host + " does not resolve.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + irtrans_host);
            System.exit(1);
        }

        outToServer.print("ASCI");

        boolean quit = false;
        do {
            try {
                result = inFromServer.readLine();
            } catch (IOException e) {
                System.err.println("Couldn't read from " + irtrans_host);
                System.exit(1);
            }
            String[] x = result.split(" ")[2].split(",");
            String remote = x[0];
            String commandname = x[1];

            if (verbose) {
                System.err.print("Received >" + remote + "<,>" + commandname + "<");
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
                            for (act = the_action.getFirstChild(); act.getNodeType() != Node.ELEMENT_NODE; act = act.getNextSibling())
                                ;
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

    private void delay() throws InterruptedException {
        Thread.sleep(10);
    }

    /*
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
            System.err.println(result);
        }
    }
*/
    private boolean send_command_udp(String cmd) throws UnknownHostException,IOException {
        boolean success = false;
        if (verbose)
            System.err.println("Sending command `" + cmd + "' to Irtrans over UDP");

        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            InetAddress addr = InetAddress.getByName(irtrans_host);
            byte[] buf = cmd.getBytes("US-ASCII");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, irtrans_port);
            sock.send(dp);
            success = true;
        } finally {
            sock.close();
        }
        return success;
    }

    public static String make_url(String hostname, String remote,
            command_t command, String led) {
        return make_url(hostname, remote, command, led_t.parse(led));
    }

    public static String make_url(String hostname, String remote,
            command_t command, led_t led) {
        return "http://" + (hostname != null ? hostname : default_irtrans_host)
                + "/send.htm?remote=" + remote + "&command=" + command + "&led=" + led_t.led_char(led);
    }

    public boolean send_flashed_command(String remote, command_t cmd, led_t connector, int count)
            throws UnknownHostException, IOException, InterruptedException {
        return send_flashed_command(remote, cmd.toString(), connector, count);
    }

    public boolean send_flashed_command(String remote, command_t cmd, String connector, int count)
            throws UnknownHostException, IOException, InterruptedException {
        return send_flashed_command(remote, cmd, led_t.valueOf(connector), count);
    }

    public boolean send_flashed_command(String remote, String command, led_t connector, int count)
            throws UnknownHostException, IOException, InterruptedException {
        boolean success = send_flashed_command(remote, command, connector, false);
        for (int i = 1; i < count; i++)
            success = success && send_flashed_command(remote, command, connector, true);
        
        return success;
    }

    public boolean send_flashed_command(String remote, String command, led_t led, boolean repeat)
            throws UnknownHostException, IOException, InterruptedException {
        return send_command("Asnd" + (repeat ? "r" : "") + " " + remote + "," + command + "," + led_t.led_char(led)).equals(send_flashed_command_ack);
    }

    public boolean send_flashed_command(String remote, String command)
            throws UnknownHostException, IOException, InterruptedException {
        return send_flashed_command(remote, command, led_t.intern, false);
    }

    // Synonym for interactive use
    public String send_ir(String remote, String command)
            throws UnknownHostException, IOException, InterruptedException {
        return send_flashed_command(remote, command) ? "" : null;
    }

    public boolean send_flashed_command_udp(String remote, String command,
            led_t led, int no_sends) throws UnknownHostException, IOException {
        boolean success = true;
        for (int c = 0; c < no_sends; c++)
            success = success && send_flashed_command_udp(remote, command, led, c > 0);

        return success;
    }

     public boolean send_flashed_command_udp(String remote, String command,
            led_t led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_command_udp("snd" + (repeat ? "r" : "") + " " + remote + "," + command + "," + led_t.led_char(led));
    }

    public boolean send_flashed_command_udp(String remote, String command)
            throws UnknownHostException, IOException {
        return send_flashed_command_udp(remote, command, led_t.intern, false);
    }

    public boolean send_ir(String ccf_string, led_t led, int count)
            throws UnknownHostException, IOException {
        boolean success = true;
        for (int c = 0; c < count; c++)
            success = success && send_ir(ccf_string, led, c > 0);
        return success;
    }

    public boolean send_ir(String ccf_string, led_t led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_command_udp((repeat ? "sndccfr " : "sndccf ") + ccf_string + "," + led_t.led_char(led));
    }

    public boolean send_ir(ir_code code, led_t led, boolean repeat)
            throws UnknownHostException, IOException {
        return send_ir(code.ccf_string(), led, repeat);
    }

    public boolean send_ir(ir_code code)
            throws UnknownHostException, IOException {
        return send_ir(code, led_t.intern, false);
    }

    public boolean send_ir(ir_code code, led_t led)
            throws UnknownHostException, IOException {
        return send_ir(code, led, false);
    }

    public boolean send_ir(ir_code code, led_t led, int count)
            throws UnknownHostException, IOException {
        boolean success = true;
        for (int c = 0; c < count; c++) {
            success = success && send_ir(code, led, c > 0);
        }
        return success;
    }

    public boolean send_ir(ir_code code, String led, int count)
            throws UnknownHostException, IOException {
        return send_ir(code, led_t.valueOf(led), count);
    }

    private static void usage(int exitstatus) {
        System.err.println("Usage:");
        System.err.println("\tirtrans [-v][-h <hostname>] -r [<remotename>]");
        System.err.println("\tirtrans [-v][-h <hostname>] listenfile");
        System.exit(exitstatus);
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

        try {
            irtrans irt = new irtrans(irtrans_host, verbose);
            if (verbose)
                System.out.println(irt.get_version());

            if (args.length > optarg && args[optarg].equals("-r")) {
                if (args.length == optarg + 1) {
                    String[] remotes = irt.get_remotes();
                    for (int i = 0; i < remotes.length; i++) {
                        System.err.println(remotes[i]);
                    }
                } else {
                    String remote = args[optarg + 1];
                    String[] commands = irt.get_commands(remote);
                    for (int i = 0; i < commands.length; i++) {
                        System.err.println(commands[i]);
                    }
                }
            } else {
                if (args.length > optarg) {
                    configfilename = args[optarg];
                }

                irt.listen(configfilename);
            }
        } catch (Exception e) {
            usage(harcutils.exit_usage_error);
        //System.err.println(e.getMessage());
        }
    }
}
