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

package org.harctoolbox;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import java.util.HashMap;

public class amx_beacon {
    public final static String broadcast_ip = "239.255.250.250";
    public final static int broadcast_port = 9131;

    public static HashMap<InetAddress, result>gadgets = new HashMap<InetAddress, result>();

    public static void print_gadgets() {
        for (InetAddress addr : gadgets.keySet()) {
            System.err.println(gadgets.get(addr));
        }
    }
    
    public static void reset() {
        gadgets.clear();
    }

    public static class result {
        public InetAddress addr;
        public int port;
        public HashMap<String,String> table;
        public result(InetAddress addr, int port, HashMap<String, String>table) {
            this.addr = addr;
            this.port = port;
            this.table = table;
        }
        @Override
        public String toString() {
            String res = "";
            for (String s : table.keySet()) {
                res = res + "\n" + "table[" + s + "] = " + table.get(s);
            }
            return "IP = " + addr.getHostName() + "\n"
                    + "port = " + port
                    + res;
        }
    }
    
    public static boolean listen(boolean verbose, int count, int timeout) {
        if (verbose)
            System.err.println("listening to beacon...");

        for (int c = 0; c < count; c++) {
            byte buf[] = new byte[1000];
            try {
                InetAddress group = java.net.InetAddress.getByName(broadcast_ip);
                MulticastSocket sock = new MulticastSocket(broadcast_port/*, addr*/);
                sock.joinGroup(group);
                sock.setSoTimeout(timeout);
                DatagramPacket pack = new DatagramPacket(buf, buf.length);
                //System.err.println("listening...");
                sock.receive(pack);
                //System.err.println("got something...");
                String payload = (new String(pack.getData(), 0, pack.getLength())).trim();
                InetAddress a = pack.getAddress();
                int port = pack.getPort();
                //System.out.println("Got |" + payload + "| from " + a.getHostName() + ":" + port);
                HashMap<String, String> t = new HashMap<String, String>();
                if (payload.startsWith("AMXB<"))
                    payload = payload.substring(5, payload.length() - 1);
                //System.err.println(payload);
                String[] pairs = payload.split("><");
                HashMap<String, String> table = new HashMap<String, String>(pairs.length);
                for (int i = 0; i < pairs.length; i++) {
                    String[] x = pairs[i].split("=");
                    table.put(x[0], x[1]);
                }

                result r = new result(a, port, table);
                gadgets.put(a, r);
                //System.err.println(r);

            } catch (IOException ex) {
                System.err.println(ex.getMessage());
                return false;
            } 
        }
        return true;
    }

    public static result get_key(String key, String value) {
        for (result r : gadgets.values())
            if (r.table.containsKey(key) && r.table.get(key).equals(value))
                return r;

        return null;
    }

    public static result listen_for(String key, String value, int timeout) {
        result r = null;
        while (r == null) {
            r = get_key(key, value);
            if (r == null) {
                boolean got_response = listen(false, 1, timeout);
                if (!got_response)
                    return null;
            }
        }
        return r;
    }

    public static void main(String args[]) {
        //listen(true, 4);
        //print_gadgets();
        result r = listen_for("-Make", args[0], Integer.parseInt(args[1]));
        System.err.println(r);
    }
}
