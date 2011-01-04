package harc;

import org.gnu.readline.*;

/**
 *
 * @author bengt
 */
public class rl_completer implements ReadlineCompleter {

    private macro_engine engine = null;
    private home hm = null;
    private String[] commands = null;
    private String[] macros = null;
    private String[] devices = null;
    private String[] selecting_devices = null;
    private String[] device_commands = null;
    private String[] src_devices = null;
    private int commands_p = 0;
    private int macro_p = 0;
    private int devices_p = 0;
    private int selecting_devices_p = 0;
    private int device_commands_p = 0;
    private int src_devices_p = 0;
    private boolean is_device = false;
    private boolean select_mode = false;

    private int no_words = 0;
    private boolean between_words = false;
    private String[] tokens = null;
    private String devicename = null;
    String dst_device = null;

    public rl_completer(String[] commands, macro_engine engine, home hm) {
        this.engine = engine;
        this.hm = hm;
        this.commands = commands;
        macros = engine.get_macros(false);
        devices = hm.get_devices();
        selecting_devices = hm.get_selecting_devices();
    }

    private String first_token_completer(String in, int s) {
        for (int i = commands_p; i < commands.length; i++)
            if (commands[i].startsWith(in)) {
                commands_p = i + 1;
                return commands[i];
            }
        commands_p = commands.length;

        for (int i = macro_p; i < macros.length; i++)
            if (macros[i].startsWith(in)) {
                macro_p = i + 1;
                return macros[i]
                        + (in.endsWith(" ") ? ("# " + engine.describe_macro(macros[i])) : "");
            }
        macro_p = macros.length;

        for (int i = devices_p; i < devices.length; i++)
            if (devices[i].startsWith(in)) {
                devices_p = i + 1;
                return devices[i];
            }
        devices_p = devices.length;

        return null;
    }

    private String device_completer(String prefix, String in, int s) {
        //if (s == 0)
          //  System.out.println("***" + prefix + ">>>" + in );
        for (int i = devices_p; i < devices.length; i++)
            if (devices[i].startsWith(in)) {
                devices_p = i + 1;
                return prefix + " " + devices[i];
            }
        devices_p = devices.length;

        return null;
    }

    private String selecting_device_completer(String prefix, String in, int s) {
        for (int i = selecting_devices_p; i < selecting_devices.length; i++)
            if (selecting_devices[i].startsWith(in)) {
                selecting_devices_p = i + 1;
                return prefix + " " + selecting_devices[i];
            }
        selecting_devices_p = selecting_devices.length;

        return null;
    }

    private String src_device_completer(String prefix, String in, int s) {
        //if (s == 0)
          //  System.out.println("***" + prefix + ">>>" + in );
        for (int i = src_devices_p; i < src_devices.length; i++)
            if (src_devices[i].startsWith(in)) {
                src_devices_p = i + 1;
                return prefix + " " + src_devices[i];
            }
        src_devices_p = src_devices.length;

        return null;
    }

    private String device_command_completer(String devicename, String in, int s) {
        if (device_commands == null)
            return null;
        
        for (int i = device_commands_p; i < device_commands.length; i++)
            if (device_commands[i].startsWith(in)) {
                device_commands_p = i + 1;
                return devicename + " " + device_commands[i];
            }
        return null;
    }

    public String completer(String in, int s) {
        
        in = in.replaceFirst("^[ \t]+", "");

        if (s == 0) {
            commands_p = 0;
            macro_p = 0;
            devices_p = 0;
            selecting_devices_p = 0;
            device_commands_p = 0;
            src_devices_p = 0;
            device_commands = null;
            devicename = null;
            between_words = in.endsWith(" ");
            select_mode = in.startsWith("--select ");
            tokens = in.split("[ \t]+");
            is_device = tokens.length > 0 && hm.has_device(tokens[0]);
            dst_device = (select_mode && tokens.length > 1 && hm.has_device(tokens[1]))
                    ? hm.expand_alias(tokens[1]) : null;
            if (dst_device != null) {
                src_devices = hm.get_sources(dst_device, null);
            } else if (is_device) {
                devicename = hm.expand_alias(tokens[0]);
                device_commands = hm.get_commands(devicename);
            }

        }

        if (tokens.length <= 1 && !between_words) {
            // Find a command, macro, or device.
            return first_token_completer(in, s);
        } else if (tokens.length == 1 && between_words && s == 0 && engine.has_macro(tokens[0])) {
            // Macro, deliver its documentation (only once).
            return in + " # " + engine.describe_macro(tokens[0]);
        } else if (select_mode) {
            if ((tokens.length == 1 && between_words) || (tokens.length == 2 && !between_words)) {
                // Find a selecting device.
                return selecting_device_completer(tokens[0], tokens.length > 1 ? tokens[1] : "", s);
            } else if ((tokens.length == 2 && between_words) || tokens.length == 3) {
                // Find a src device for the already selected device.
                return src_device_completer(tokens[0] + " " + dst_device, tokens.length > 2 ? tokens[2] : "", s);
            }
        } else if (is_device && ((tokens.length == 1 && between_words) || (tokens.length > 1))) {
            // Find a command for an already selected device.
            return device_command_completer(devicename, tokens.length > 1 ? tokens[1] : "", s);
        }

        return null;
    }
}
