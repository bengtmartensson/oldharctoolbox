/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

// A LIRC network client.
package harc;

import java.io.*;
import java.net.*;

public class lirc {

    private String lirc_host;
    private final static int lirc_default_port = 8765;
    private int lirc_port;
    public final static String default_lirc_host = "localhost";

    // multiple hardware/transmitters are currently not supported.
    private boolean verbose = true;

    public lirc(String hostname, int port, boolean verbose) {
        lirc_host = (hostname != null) ? hostname : default_lirc_host;
        lirc_port = port;
        this.verbose = verbose;
    }

    public lirc(String hostname, boolean verbose) {
        this(hostname, lirc_default_port, verbose);
    }

    public lirc(String hostname) {
        this(hostname, false);
    }

    public lirc(boolean verbose) {
        this(default_lirc_host, verbose);
    }

    public lirc() {
        this(default_lirc_host, false);
    }

    public void set_verbosity(boolean verbosity) {
        this.verbose = verbosity;
    }
    private final static int P_BEGIN = 0;
    private final static int P_MESSAGE = 1;
    private final static int P_STATUS = 2;
    private final static int P_DATA = 3;
    private final static int P_N = 4;
    private final static int P_DATA_N = 5;
    private final static int P_END = 6;

    private class bad_packet extends Exception {

        public bad_packet() {
            super();
        }

        public bad_packet(String message) {
            super(message);
        }
    }

    private String[] send_command(String packet) throws IOException, UnknownHostException, NoRouteToHostException {
        if (verbose) {
            System.err.println("Sending command `" + packet + "' to Lirc@" + lirc_host);
        }
        Socket sock = null;
        PrintStream outToServer = null;
        BufferedReader inFromServer = null;
        String string = "";

        sock = new Socket(lirc_host, lirc_port);
        outToServer = new PrintStream(sock.getOutputStream());
        inFromServer =
                new BufferedReader(new InputStreamReader(sock.getInputStream()));
        
        outToServer.print(packet + '\n');

        String[] result = null;
        int status = 0;
        try {
            int state = P_BEGIN;
            int n = 0;
            boolean done = false;
            int errno = 0;
            int data_n = -1;

            while (!done) {
                string = inFromServer.readLine();
                //System.out.println("***"+string+"***"+state);
                if (string == null) {
                    done = true;
                }

                switch (state) {
                    case P_BEGIN:
                        if (!string.equals("BEGIN")) {
                            System.err.println("!begin");
                            continue;
                        }
                        state = P_MESSAGE;
                        break;
                    case P_MESSAGE:
                        if (!string.equals(packet)) {
                            state = P_BEGIN;
                            continue;
                        }
                        state = P_STATUS;
                        break;
                    case P_STATUS:
                        if (string.equals("SUCCESS")) {
                            status = 0;
                        } else if (string.equals("END")) {
                            status = 0;
                            done = true;
                        } else if (string.equals("ERROR")) {
                            System.err.println("command failed: " + packet);
                            status = -1;
                        } else {
                            throw new bad_packet();
                        }
                        state = P_DATA;
                        break;
                    case P_DATA:
                        if (string.equals("END")) {
                            done = true;
                            break;
                        } else if (string.equals("DATA")) {
                            state = P_N;
                            break;
                        }
                        throw new bad_packet();
                    case P_N:
                        errno = 0;
                        data_n = Integer.parseInt(string);
                        result = new String[data_n];

                        state = data_n == 0 ? P_END : P_DATA_N;
                        break;
                    case P_DATA_N:
                        if (verbose) {
                            System.out.println(string);
                        }
                        result[n++] = string;
                        if (n == data_n) {
                            state = P_END;
                        }
                        break;
                    case P_END:
                        if (string.equals("END")) {
                            done = true;
                        } else {
                            throw new bad_packet();
                        }
                        break;
                }
            }
        } catch (bad_packet e) {
            System.err.println("bad return packet");
        } catch (IOException e) {
            System.err.println("Couldn't read from " + lirc_host);
            System.exit(1);
        } finally {
            try {
                sock.close();
            } catch (IOException e) {
                System.err.println("Couldn't close socket.");
                System.exit(1);
            }
        }
        if (verbose) {
            System.err.println(status == 0 ? "Lirc command succeded."
                    : "Lirc command failed.");
        }
        if (status != 0 && result != null) {
            System.err.println(result[0]);
        }

        return result;
    }

    public void send_ccf(String ccf, int count) throws IOException, UnknownHostException, NoRouteToHostException {
        send_command("SEND_CCF_ONCE " + (count - 1) + " " + ccf);
    }

    public void send_ccf_repeat(String ccf, int count) throws IOException, UnknownHostException, NoRouteToHostException {
        send_command("SEND_CCF_START " + ccf);
    }

    public String send_ir(String remote, String command) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_ir(remote, command, 1) ? "" : null;
    }

    public boolean send_ir(String remote, String command, int count) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("SEND_ONCE " + remote + " " + command + " " + (count - 1)) == null;
    }

    public boolean send_ir(String remote, command_t cmd, int count) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("SEND_ONCE " + remote + " " + cmd + " " + (count - 1)) == null;
    }

    public void send_ir_repeat(String remote, String command) throws IOException, UnknownHostException, NoRouteToHostException {
        send_command("SEND_START " + remote + " " + command);
    }

    public void stop_ir(String remote, String command) throws IOException, UnknownHostException, NoRouteToHostException {
        send_command("SEND_STOP " + remote + " " + command);
    }

    public String[] get_remotes() throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("LIST");
    }

    public String[] get_ccf_remote() throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("CCF");
    }

    public String[] get_commands(String remote) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("LIST " + remote);
    }

    public String[] get_ccf_remote(String remote) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("CCF " + remote);
    }

    public String get_remote_command(String remote, String command) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("LIST " + remote + " " + command)[0];
    }

    public String get_ccf_remote_command(String remote, String command) throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("CCF " + remote + " " + command)[0];
    }

    public void set_transmitters(int[] trans) throws IOException, UnknownHostException, NoRouteToHostException {
        String s = "SET_TRANSMITTERS";
        for (int i = 0; i < trans.length; i++) {
            s = s + " " + trans[i];
        }
        send_command(s);
    }

    public String get_version() throws IOException, UnknownHostException, NoRouteToHostException {
        return send_command("VERSION")[0];
    }

    public static void main(String[] args) {
        lirc l = new lirc(args[0], args.length > 1 && args[1].equals("-v"));
        try {
            //  	dump_array(l.get_remote());
            //  	dump_array(l.get_remote("rc5_cd"));
            // 	System.out.println(l.get_remote_command("rc5_cd", "stop"));
            // 	System.out.println(l.get_version());
            // 	System.out.println(l.get_ccf_remote_command("panasonic_dvd", "power_toggle"));
            // 	dump_array(l.get_ccf_remote("panasonic_dvd"));
            System.out.println(l.get_version());
            harcutils.printtable("Remotes: ", l.get_remotes());
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to: " + l.lirc_host);
            System.exit(1);
        }
    }
}
