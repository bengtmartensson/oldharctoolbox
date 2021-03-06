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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public final class SocketStorage {

    private static boolean enable = true;
    private static boolean debug = false;
    private static HashMap<String, socket_stat> sockettable = new HashMap<String, socket_stat>(16);
    private final static Logger logger = Logger.getLogger(SocketStorage.class.getName());

    public static void enable_storage(boolean en) {
        enable = en;
    }

    public static void debug_enable(boolean d) {
        debug = d;
    }

    private static void debug(String s) {
        if (debug || DebugArgs.dbg_socket_storage())
            System.err.println(s);
    }


    private static String canonicalize(String host) throws UnknownHostException {
        return InetAddress.getByName(host).getHostAddress();
    }

    public static Socket getsocket(String hostname, int portno)
            throws UnknownHostException, IOException {
        return getsocket(hostname, portno, true);
    }

    public static Socket getsocket(String hostname, int portno, boolean unique)
            throws UnknownHostException, IOException {
        if (!enable)
            return new Socket(hostname, portno);

        addr_portno addr = new addr_portno(hostname, portno);
        debug("$$ Requesting socket to " + addr);
        Socket sock = null;
        if (sockettable.containsKey(addr.toString())) {
            socket_stat s = sockettable.get(addr.toString());
            if (s.checked_out) {
                debug("Already checked out, " + (unique ? "reject" : "creating new"));
                try {
                    sock = unique ? null : new Socket(hostname, portno);
                } catch (java.net.ConnectException e) {
                    System.err.println("]]]host " + hostname + " refused connect");
                    sock = null;
                }
            } else {
                debug("Already known, available, checking out");
                if (s.sock.isClosed())
                    s.sock = new Socket(hostname, portno);

                sock = s.sock;
                s.checked_out = true;
            }
        } else {
            debug("Unknown, available, creating new and checking out");
            try {
                sock = new Socket(hostname, portno);
                sockettable.put(addr.toString(), new socket_stat(sock, true));
                debug("succeeded!");
            } catch (java.net.ConnectException e) {
                System.err.println("]]]host " + hostname + " refused connect");
            }
        }
        return sock;
    }

    public static void returnsocket(Socket sock, boolean dispose) throws IOException {
        if (sock == null) {
            debug("Trying to return null socket");
            return;
        }

        if (!enable) {
            sock.close();
            return;
        }

        addr_portno addr = new addr_portno(sock);
        debug("Returning " + addr);
        if (sockettable.containsKey(addr.toString())) {
            debug("in table");
            socket_stat s = sockettable.get(addr.toString());
            if (s.sock.equals(sock)) {
                debug("known socket");
                if (dispose) {
                    debug("junking");
                    sock.close();
                    sockettable.remove(addr.toString());
                } else {
                    debug("checking in");
                    s.checked_out = false;
                }
            } else {
                debug("unknown socket, closing");
                sock.close();
            }
        } else {
            debug("unknown socket, closing");
            sock.close();
        }
        debug("Now there are " + sockettable.size() + " sockets in the table.");
    }

    public static void dispose_sockets(boolean brutal) throws IOException {
        debug("dispose_socket called");
        //for (Enumeration e = sockettable.keys(); e.hasMoreElements();) {
        //    String addr = (String) e.nextElement();
        for (String addr : sockettable.keySet()) {
            Socket skt = sockettable.get(addr).sock;
            if (brutal || !sockettable.get(addr).checked_out) {
                if (!skt.isClosed())
                    skt.close();
                sockettable.remove(addr);
                break;
            }
        }
    }

    public static void main(String[] args) {
        debug_enable(true);
        try {
            String host = args[0];
            int port = Integer.parseInt(args[1]);
            boolean unique = Boolean.parseBoolean(args[2]);

            Socket sock1;
            Socket sock2;
            Socket sock3;
            Socket sock4;

            sock1 = getsocket(host, port, unique);
            sock2 = getsocket(host, port, unique);
            sock3 = getsocket(host, port, unique);
            Thread.sleep(2000);

            returnsocket(sock1, false);
            sock4 = getsocket(host, port, unique);
            returnsocket(sock2, false);
            returnsocket(sock3, false);
            returnsocket(sock4, false);

            sock1 = getsocket(host, port, unique);
            returnsocket(sock1, true);
            sock1 = getsocket(host, port, unique);
            returnsocket(sock1, false);
            Thread.sleep(1000);
            sock1 = getsocket(host, port, unique);

            dispose_sockets(true);
            Thread.sleep(1000);
            sock1 = getsocket(host, port, unique);
        } catch (InterruptedException | IOException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }
    private static class addr_portno {
        private String hostname = null;
        private int portno = 0;

        addr_portno(String addr, int p) throws UnknownHostException {
            hostname = canonicalize(addr);
            portno = p;
        }
        addr_portno(Socket sock) {
            hostname = sock.getInetAddress().getHostAddress();
            portno = sock.getPort();
        }

        @Override
        public String toString() {
            return hostname + ":" + portno;
        }
    }
    private static class socket_stat {
        public Socket sock = null;
        public boolean checked_out = false;

        socket_stat(Socket s, boolean state) {
            sock = s;
            checked_out = state;
        }

        @Override
        public String toString() {
            return sock.toString() + (checked_out ? " (out)" : " (in)");
        }
    }
}
