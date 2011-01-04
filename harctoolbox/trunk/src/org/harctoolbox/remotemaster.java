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
import java.util.*;

/**
 *
 */
public class remotemaster {

    // From remotemaster/DeviceUpdate.java, see also RDF file spec ver 3.
    // page 21.
    public final static String[] genericButtonNames = {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "vol up", "vol down", "mute", "channel up", "channel down",
        "power", "enter", "tv/vcr", "prev ch", "menu", "guide", "up arrow", "down arrow", "left arrow", "right arrow",
        "select", "sleep", "pip on/off", "display", "pip swap", "pip move", "play", "pause", "rewind", "fast fwd",
        "stop", "record", "exit", "surround", "input", "+100", "fav/scan", "device button", "next track", "prev track",
        "shift-left", "shift-right", "pip freeze", "slow", "eject", "slow+", "slow-", "x2", "center", "rear", "phantom1",
        "phantom2", "phantom3", "phantom4", "phantom5", "phantom6", "phantom7", "phantom8", "phantom9", "phantom10",
        "setup", "light", "theater", "macro1", "macro2", "macro3", "macro4", "learn1", "learn2", "learn3", "learn4" // ,
    // "button85", "button86", "button87", "button88", "button89", "button90",
    // "button91", "button92", "button93", "button94", "button95", "button96",
    // "button97", "button98", "button99", "button100", "button101", "button102",
    // "button103", "button104", "button105", "button106", "button107", "button108",
    // "button109", "button110", "button112", "button113", "button114", "button115",
    // "button116", "button117", "button118", "button119", "button120", "button121",
    // "button122", "button123", "button124", "button125", "button126", "button127",
    // "button128", "button129", "button130", "button131", "button132", "button133",
    // "button134", "button135", "button136"
    };

    // Just a rough try..
    public static String jp1_devicetype(device_type type) {
        return
            type == device_type.amplifier ? "Amp" :
            type == device_type.receiver ? "Amp" :
            type == device_type.projector ? "TV" :
            type == device_type.cable ? "Cable" :
            type == device_type.ld ? "Laserdisc" :
            type == device_type.ha ? "Home Auto" :
            type == device_type.tape ? "Tape" :
            type == device_type.misc_audio ? "Misc Audio" :
            type.toString().toUpperCase();
    }

    private static remotemaster the_instance = null;
    private static boolean remotemaster_unavailable = false;

    private static com.hifiremote.jp1.PropertyFile properties = null;
    private static com.hifiremote.jp1.ProtocolManager protocolManager = null;

    static remotemaster get_instance() {
        return the_instance;
    }

    static String get_rdf_dir() {
        boolean ok = init();
        return ok ? properties.getProperty("RDFPath") : null;
    }

    private remotemaster(String digitmaps, String protocols_ini, String propertiespath) {
        if (remotemaster_unavailable || the_instance != null)
            return;
        
        try {
            try {
                com.hifiremote.jp1.DigitMaps.load(new File(digitmaps));
            } catch (java.lang.NoClassDefFoundError e) {
                System.err.println("Warning: Cannot find RemoteMaster jar, some functions will not be available.");
                remotemaster_unavailable = true;
                return;
            }
            protocolManager = com.hifiremote.jp1.ProtocolManager.getProtocolManager();
            properties = new com.hifiremote.jp1.PropertyFile(new File(propertiespath));
            String rdf_dir = properties.getProperty("RDFPath");
            String upgrade_dir = properties.getProperty("UpgradePath");

            // This is used to set up the static member "properties" in JP1Frame
            com.hifiremote.jp1.JP1Frame dummy = new com.hifiremote.jp1.JP1Frame("", properties);
            com.hifiremote.jp1.ProtocolManager.getProtocolManager().load(new File(protocols_ini));
            com.hifiremote.jp1.RemoteManager.getRemoteManager().loadRemotes(properties);
            the_instance = this;
        } catch (Exception e) {// Blame RM for this, not me ;-)
            System.err.println("RemoteMaster initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean init(String digitmaps, String protocols_ini, String propertiespath) {
        new remotemaster(digitmaps, protocols_ini, propertiespath);
        return the_instance != null;
    }

    public static boolean init() {
        return init(".");
    };

    public static boolean init(String directory, String propertiespath) {
        return init(directory + File.separator + "digitmaps.bin",
                directory + File.separator + "protocols.ini", propertiespath);
    }

    public static boolean init(String directory) {
        return init(directory, directory + File.separator + "RemoteMaster.properties");
    }

    public static File basename2rdf_file(String remotename) {
        boolean ok = init();
        return ok ? new File(properties.getProperty("RDFPath") + File.separator + remotename + ".rdf")
                : null;
    }

    public static String get_version() {
        boolean ok = init();
        return ok ? com.hifiremote.jp1.RemoteMaster.version : null;
    }

    public static boolean launch(boolean rmir, String rm_home, String arg) {
        boolean ok = init();
        if (ok) {
            if (rm_home == null)
                rm_home = harcprops.get_instance().get_remotemaster_home();
            String[] args = rmir ? new String[]{"-ir", "-h", rm_home}
                         : ((arg == null || arg.isEmpty()) ? new String[]{"-h", rm_home} : new String[]{"-h", rm_home, arg});
            com.hifiremote.jp1.RemoteMaster.main(args);
            return true;
        } else
            return false;
    }

    public static boolean launch(boolean rmir) {
        return launch(rmir, harcprops.get_instance().get_remotemaster_home(), null);
    }

    public static String[] get_remotenames() {
        boolean ok = init();
        if (!ok)
            return null;
        
        Collection<com.hifiremote.jp1.Remote> remotes = com.hifiremote.jp1.RemoteManager.getRemoteManager().getRemotes();
        String[] result = new String[remotes.size()];
        int i = 0;
        for (com.hifiremote.jp1.Remote r : remotes)
            result[i++] = r.getName();

        return result;
    }

    public static void rdf2button_remote(String rdffilename, String brfilename) throws FileNotFoundException {
        init();
        com.hifiremote.jp1.Remote remote = new com.hifiremote.jp1.Remote(new File(rdffilename));
        String[] states;
        // This is now working as expected :-\
        states = remote.getXShiftEnabled() ? new String[]{"unshifted", "shifted", "xshifted"} : new String[]{"unshifted", "shifted"};
        List<com.hifiremote.jp1.Button>buttons = remote.getButtons();
        button_remote.button[] btns = new button_remote.button[buttons.size()];
        int index = 0;
        for (com.hifiremote.jp1.Button button : buttons)
            btns[index++] = new button_remote.button(button.getName(), button.getKeyCode());
        button_remote br = new button_remote(remote.getName().replaceAll(" ", "_"),
                remote.getName(), remote.getName(), new String[]{rdffilename}, btns, states);
        br.export(brfilename);
    }

    public static void main(String[] args) {
        if (args.length >= 2 && args[0].equals("rm"))
            launch(false, args[1], null);
        else if (args.length >= 2 && args[0].equals("rmir"))
            launch(true, args[1], null);
        else if (args.length == 2) {
            try {
                rdf2button_remote(args[0], args[1]);
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println(get_version());
            harcutils.printtable("Available remotes:", get_remotenames());
        }
    }
}
