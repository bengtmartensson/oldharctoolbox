package org.harctoolbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;

/**
 *
 * @author bengt
 */

// Ref: http://www.openremote.org/display/docs/Controller+2.0+HTTP-REST-XML

public class openremote_simple_client {
    public static final String multicast_ip = "224.0.1.100";
    public static final int multicast_port = 3333;
    public static final int listen_port = 2346;
    public static final String token = "openremote";

    private final String base_url;

    public openremote_simple_client(String base_url) {
        if (!base_url.endsWith("/"))
            base_url = base_url + "/";
        this.base_url = base_url;
    }

    public openremote_simple_client() {
        this(discover());
    }

    public openremote_simple_client(String host, int portnumber) {
        this("http://" + host + ":" + portnumber + "/controller");
    }

    public String get_baseurl() {
        return base_url;
    }

    /** Returns the base url
     *
     * @return url base as String.
     */
    public static String discover() {
        DatagramSocket sock = null;
        try {
            sock = new DatagramSocket();
            InetAddress addr = null;
            addr = InetAddress.getByName(multicast_ip);
            byte[] buf = token.getBytes("US-ASCII");
            DatagramPacket dp = new DatagramPacket(buf, buf.length, addr, multicast_port);
            sock.send(dp);
        } catch (Exception ex) {
            System.err.println("Exception: " + ex.getMessage());
        } finally {
            sock.close();
        }

        String result = null;
        ServerSocket srv_sock = null;
        Socket s = null;
        try {
            srv_sock = new java.net.ServerSocket(listen_port);
            s = new Socket();
            s = srv_sock.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            result = in.readLine();
        } catch (Exception ex) {
            System.err.println("Exception: " + ex.getMessage());
        } finally {
            try {
                s.close();
                srv_sock.close();
            } catch (IOException ex) {
            }
        }
        return result;
    }

    private int http_request(String short_url, String method) {
        int result = 0;
        try {
            String url_str = base_url + short_url;
            System.err.println(url_str);
            URL url = new URL(url_str);

            HttpURLConnection hu = (HttpURLConnection) url.openConnection();
            //hu.connect();
            hu.setRequestMethod(method);
            String ct = hu.getContentType();
            //System.out.println(ct);
            hu.getContent();
        } catch (java.net.UnknownServiceException ex) {
            //System.err.println("sdsdfd");
        } catch (IOException ex) {
            //Logger.getLogger(openremote_simple_client.class.getName()).log(Level.SEVERE, null, ex);
            System.err.println(ex.getMessage());
        }

        return result;
    }

    public boolean control_command(int control_no, String param) {
        return http_request("/rest/control/" + Integer.toString(control_no) + "/" + param, "POST") == HttpURLConnection.HTTP_OK;
    }

    public boolean get_panels() {
        return http_request("rest/panels", "GET") == HttpURLConnection.HTTP_OK;
    }

    public static void main(String[] args) {
        openremote_simple_client client = new openremote_simple_client();
        System.out.println("Discovered " + client.get_baseurl());
        if (args.length == 0)
            System.exit(harcutils.exit_success);
        int but = Integer.parseInt(args[0]);
        String cmd = args.length >= 2 ? args[1] : "click";
        client.control_command(but, cmd);
        //client.get_panels();
    }
}
