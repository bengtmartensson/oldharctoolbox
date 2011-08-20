package org.harctoolbox;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import wol.WakeUpUtil;
import wol.configuration.EthernetAddress;
import wol.configuration.IllegalEthernetAddressException;

/**
 *
 * @author bengt
 */

// Uses /etc/ethers hardcoded. This is of course not really portable,
// but I do not care enough...

public class ethers {
    
    private static HashMap<String, String> table = null;
    
    public static void init(String filename) throws FileNotFoundException, IOException {
        table = new HashMap<String, String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));

        for (String line = r.readLine(); line != null; line = r.readLine()) {
            if (!line.startsWith("#")) {
                String[] str = line.split("[\\s]+");
                if (str.length == 2) {
                    String mac = str[0];
                    String hostname = str[1];
                    table.put(hostname, mac);
                }
            }
        }
    }

    public static void init() throws FileNotFoundException, IOException {
        init("/etc/ethers");
    }
    
    public static String get_mac(String hostname) {
        if (table == null)
            try {
                init();
            } catch (IOException ex) {
                System.err.println("error reading ethers file.");
            }
        
        return table.get(hostname);
    }

    public static void main(String[] args) {
        if (args.length == 1) {
            System.out.println(get_mac(args[0]));
        } else if (args.length == 2) {
            String mac = get_mac(args[1]);
            System.out.println("Sending a WOL to " + mac);
            try {
                WakeUpUtil.wakeup(new EthernetAddress(mac));
            } catch (IOException ex) {
                System.err.println(ex.getMessage());
            } catch (IllegalEthernetAddressException ex) {
                System.err.println(ex.getMessage());
            }
        } else
            System.err.println("Usage:\n\tethers [-w] host");
    }
}
