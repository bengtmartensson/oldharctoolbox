package harc;

import java.io.*;
import org.gnu.readline.*;

/**
 *
 * @author bengt
 */
public class Main {

    private static void usage(int exitstatus) {
        System.err.println("Usage: one of");
        System.err.println(helptext);
        System.exit(exitstatus);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }
    private static final String helptext =
            "\tharc [OPTIONS] [-g|-r]\n"
            + "\tharc [OPTIONS] <macro>\n"
            + "\tharc [OPTIONS] <device_instance> <command> [<argument(s)>]\n"
            + "\tharc [OPTIONS] -s <device_instance> <src_device_instance>\n"
            + "where OPTIONS=-A,-V,-M,-h <filename>,-t " + commandset.valid_types('|')
            + ",-m <macrofilename>,-T 0|1,-# <count>,-v,-d <debugcode>,"
            + "-b <browserpath>, -p <propsfile>, -z <zone>,-c <connection_type>.";
    private static final String readline_help = "Usage: one of\n\t--<command> [<argument(s)>]\n\t<macro>\n\t<device_instance> <command> [<argument(s)>]\n\t--select <device_instance> <src_device_instance>";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String homefilename = null;
        String macrofilename = null;
        String browser = null;
        String propsfilename = null;
        boolean gui_mode = false;
        boolean readline_mode = false;
        boolean no_execute = false;
        boolean select_mode = false;
        boolean smart_memory = false;
        int count = 1;
        int debug = 0;
        int verbose = 0;
        int type = commandset.any;
        toggletype toggle = toggletype.no_toggle;
        mediatype the_mediatype = mediatype.audio_video;
        String zone = null;
        String connection_type = null;

        String first_arg = null;
        String src_device = null;
        int no_arguments = 0;
        home hm = null;
        macro_engine engine = null;
        debugargs db = null;

        int arg_i = 0;

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("--help")) {
                    System.out.println(helptext);
                    System.exit(harcutils.exit_success);
                }
                if (args[arg_i].equals("--version")) {
                    System.out.println(harcutils.version_string);
                    System.out.println(harcutils.license_string);
                    System.exit(harcutils.exit_success);
                }
                if (args[arg_i].equals("-#")) {
                    arg_i++;
                    count = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-A")) {
                    arg_i++;
                    the_mediatype = mediatype.audio_only;
                } else if (args[arg_i].equals("-M")) {
                    arg_i++;
                    smart_memory = true;
                } else if (args[arg_i].equals("-V")) {
                    arg_i++;
                    the_mediatype = mediatype.video_only;
                } else if (args[arg_i].equals("-T")) {
                    arg_i++;
                    toggle = harcutils.decode_toggle(args[arg_i++]);
                } else if (args[arg_i].equals("-b")) {
                    arg_i++;
                    browser = args[arg_i++];
                } else if (args[arg_i].equals("-c")) {
                    arg_i++;
                    connection_type = args[arg_i++];
                } else if (args[arg_i].equals("-d")) {
                    arg_i++;
                    debug = Integer.parseInt(args[arg_i++]);
                } else if (args[arg_i].equals("-g")) {
                    arg_i++;
                    gui_mode = true;
                } else if (args[arg_i].equals("-h")) {
                    arg_i++;
                    homefilename = args[arg_i++];
                } else if (args[arg_i].equals("-m")) {
                    arg_i++;
                    macrofilename = args[arg_i++];
                } else if (args[arg_i].equals("-n")) {
                    arg_i++;
                    no_execute = true;
                } else if (args[arg_i].equals("-p")) {
                    arg_i++;
                    propsfilename = args[arg_i++];
                } else if (args[arg_i].equals("-s")) {
                    arg_i++;
                    select_mode = true;
                } else if (args[arg_i].equals("-r")) {
                    arg_i++;
                    readline_mode = true;
                } else if (args[arg_i].equals("-t")) {
                    arg_i++;
                    String typename = args[arg_i++];
                    if (!commandset.valid(typename)) {
                        usage();
                    }
                    type = commandset.toInt(typename);
                } else if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose++;
                } else if (args[arg_i].equals("-z")) {
                    arg_i++;
                    zone = args[arg_i++];
                } else {
                    usage();
                }
            }
            db = new debugargs(debug);

            if (!gui_mode && !readline_mode) {
                first_arg = args[arg_i];
            }

            if (select_mode) {
                src_device = args[arg_i + 1];
            }

            no_arguments = args.length - arg_i;
        //aux_args = new String[no_arguments];
        //System.arraycopy(args, arg_i, aux_args, 0, no_arguments);
        } catch (ArrayIndexOutOfBoundsException e) {
            if (db.decode_args()) {
                System.err.println("ArrayIndexOutOfBoundsException");
            }
            usage();
        } catch (NumberFormatException e) {
            if (db.decode_args()) {
                System.err.println("NumberFormatException");
            }
            usage();
        }

        // Setup properites
        if (propsfilename != null)
            harcprops.initialize(propsfilename);
        else
            harcprops.initialize();

        if (homefilename != null)
            harcprops.get_instance().set_home_file(homefilename);
        else
            homefilename = harcprops.get_instance().get_home_file();

        if (macrofilename != null)
            harcprops.get_instance().set_macro_file(macrofilename);
        else
            macrofilename = harcprops.get_instance().get_macro_file();

        if (browser != null)
            harcprops.get_instance().set_browser(browser);
        else
            browser = harcprops.get_instance().get_browser();

        resultformatter formatter = new resultformatter();

        if (gui_mode) {
            System.err.println("Invoking GUI ...");
            final int dbg = debug;
            final int vrbs = verbose;
            final String hmnam = homefilename;
            final String macronam = macrofilename;
            final String brwsr = browser;
            java.awt.EventQueue.invokeLater(new Runnable() {
                public void run() {
                    new gui_main(hmnam, macronam, vrbs, dbg, brwsr).setVisible(true);
                }
            });

        } else if (readline_mode) {
            try {
                hm = new home(homefilename, verbose, debug, browser);
                engine = new macro_engine(macrofilename, hm, debug);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(harcutils.exit_config_read_error);
            }

            if (no_arguments > 0)
                System.err.println("Warning: non-option arguments ignored.");

            if (db.decode_args()) {
                System.err.println("Entering readline mode");
            }

            String[] cl_commands = new String[]{
                "--help",
                "--version",
                "--select",
                "--quit",
                "--exit",
                "--license",
                "--verbose",
                "--debug",
                "--zone",
                "--audio-video",
                "--audio-only",
                "--video-only"
            };

            File history = null;

            Runtime.getRuntime().addShutdownHook(new Thread() {

                public void run_() {
                    //Readline.writeHistoryFile(history.getName());
                    Readline.cleanup();
                }
            });

            try {
                Readline.load(ReadlineLibrary.GnuReadline);
            } catch (UnsatisfiedLinkError e) {
                System.err.println("Warning: GNU readline not found.");
            }

            Readline.initReadline(harcprops.get_instance().get_appname());

            history = new File(harcprops.get_instance().get_rl_historyfile());
            try {
                if (history.exists())
                    Readline.readHistoryFile(history.getName());
            } catch (Exception e) {
                System.err.println("Could not read rl history " + e.getMessage());
            }

            try {
                Readline.setWordBreakCharacters(""/*" \t;"*/);
            } catch (UnsupportedEncodingException enc) {
                // FIXME
                System.err.println(enc.getMessage() + "Could not set word break characters");
                System.err.println("Try touching " + harcprops.get_instance().get_rl_historyfile());
                System.exit(harcutils.exit_this_cannot_happen);
            }

            Readline.setCompleter(new rl_completer(cl_commands, engine, hm));

            boolean go_on = true;
            while (go_on) {
                String line = null;
                try {
                    line = Readline.readline(harcprops.get_instance().get_rl_prompt());
                } catch (EOFException e) {
                    System.err.println(e.getMessage());
                    go_on = false;
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }

                if (line != null) {
                    String[] arguments = line.split("[ \t]+");
                    if (arguments[0].equals("--select")) {
                        // FIXME: no support connectiontype
                        if (arguments.length < 3) {
                            System.out.println("\007***--select takes at least two arguments***.");
                        } else {
                            if (db.decode_args()) {
                                System.out.println("Select mode: devname = " + arguments[1] + ", src_device = " + arguments[2] + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
                            }

                            try {
                                hm.select(arguments[1], arguments[2], type,
                                        zone, the_mediatype, connection_type);
                            } catch (InterruptedException e) {
                                System.err.println(e.getMessage());
                            //System.exit(harcutils.exit_interrupted);
                            }
                        }
                    } else if (line.startsWith("--")) {
                        // Decode commands
                        if (arguments[0].equals("--quit") || arguments[0].equals("--exit")) {
                            go_on = false;
                        } else if (arguments[0].equals("--version")) {
                            System.out.println(harcutils.version_string);
                        } else if (arguments[0].equals("--help")) {
                            System.out.println(readline_help);
                        } else if (arguments[0].equals("--license")) {
                            System.out.println(harcutils.license_string);
                        } else if (arguments[0].equals("--verbose")) {
                            int v = 1;
                            try {
                                v = Integer.parseInt(arguments[1]);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println("+++ Argument missing, assuming 1.");
                            } catch (NumberFormatException e) {
                                System.out.println("+++ Parse error, assuming 1.");
                            }
                            hm.set_verbosity(v);
                        } else if (arguments[0].equals("--debug")) {
                            int v = 0;
                            try {
                                v = Integer.parseInt(arguments[1]);
                            } catch (ArrayIndexOutOfBoundsException e) {
                                System.out.println("+++ Argument missing, assuming 0.");
                            } catch (NumberFormatException e) {
                                System.out.println("+++ Parse error, assuming 0.");
                            }
                            hm.set_debug(v);
                            engine.set_debug(v);
                        } else if (arguments[0].equals("--zone")) {
                            zone = (arguments.length > 1) ? arguments[1] : null;
                            if (db.decode_args())
                                System.out.println("%%% Zone is now " + zone);
                        } else if (arguments[0].equals("--audio-video")) {
                            the_mediatype = mediatype.audio_video;
                        } else if (arguments[0].equals("--audio-only")) {
                            the_mediatype = mediatype.audio_only;
                        } else if (arguments[0].equals("--video-only")) {
                            the_mediatype = mediatype.video_only;
                        } else {
                            System.out.println("\007*** Unrecognized command ***");//FIXME
                        }
                    } else if (engine.has_macro(arguments[0])) {
                        // FIXME: not implemented: macro arguments, no_execute
                        if (db.decode_args())
                            System.err.println("%%% Now executing macro `" + arguments[0] + "'");
                        try {
                            String out = engine.eval_macro(arguments[0], null, 0, false);

                            if (out == null) {
                                System.out.println("\007*** macro failed ***");
                            } else if (!out.equals("")) {
                                System.out.println(formatter.format(out));
                            }
                        } catch (non_existing_command_exception e) {
                            // This cannot happen
                        } catch (InterruptedException e) {
                            System.out.println("\007*** Interrupted ***");
                        }
                    } else if (hm.has_device(arguments[0])) {
                        // FIXME: not implemented: type, count, toggle, smart_memory
                        if (db.decode_args())
                            System.out.println("%%% Trying to execute `" + line + "'");

                        String cmd_name = arguments[0];
                        if (arguments.length < 2) {
                            System.out.println("\007*** command missing ***");
                        } else {
                            int cmd = ir_code.decode_command(arguments[1]);
                            String[] aux_args = new String[arguments.length - 2];
                            for (int i = 0; i < arguments.length - 2; i++) {
                                aux_args[i] = arguments[i + 2];
                                if (db.decode_args()) {
                                    System.err.println("Aux arg[" + i + "] = " + aux_args[i]);
                                }
                            }

                            try {
                                String output = hm.do_command(arguments[0], cmd, aux_args, type, count, toggle, smart_memory);
                                if (output == null) {
                                    System.out.println("\007*** Failure ***");
                                } else if (!output.equals("")) {
                                    System.out.println(formatter.format(output));
                                }
                            } catch (InterruptedException e) {
                                System.out.println("\007*** Interrupted ***");
                            }
                        }
                    } else {
                        System.out.println("\007*** Neither macro nor device with name `" + arguments[0] + "'***.");
                    }
                }
            }

            try {
                Readline.writeHistoryFile(history.getName());
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
            Readline.cleanup();

        } else {
            // Noninteractive mode
            try {
                hm = new home(homefilename, verbose, debug, browser);
                if (!select_mode) {
                    engine = new macro_engine(macrofilename, hm, debug);
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(harcutils.exit_config_read_error);
            }

            if (select_mode) {
                System.err.println("select mode ...");
                if (db.decode_args()) {
                    System.err.println("Select mode: devname = " + first_arg + ", src_device = " + src_device + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
                }
                if (src_device.equals("?")) {
                    if (hm.has_device(first_arg)) {
                        harcutils.printtable("Valid inputs for " + first_arg + (zone != null ? (" in zone " + zone) : "") + ":",
                                hm.get_sources(first_arg, zone));
                    } else {
                        System.err.println("No such device `" + first_arg + "'");
                        System.exit(harcutils.exit_nonexisting_device);
                    }
                } else {
                    try {
                        hm.select(first_arg, src_device, type, zone, the_mediatype, connection_type);
                    } catch (InterruptedException e) {
                        System.err.println(e.getMessage());
                        System.exit(harcutils.exit_interrupted);
                    }
                }

            } else if (first_arg.equals("?")) {
                // List macros
                harcutils.printtable("Available macros: ", engine.get_macros(true));
            } else if (first_arg.equals("??")) {
                // List devices
                harcutils.printtable("Available devices:", hm.get_devices());
            } else if (engine.has_macro(first_arg)) {
                // Macro execution
                if (db.decode_args()) {
                    System.err.println("Trying to execute macro...");
                }
                try {
                    String out = engine.eval_macro(first_arg, null, 0, no_execute);
                    if (out == null) {
                        System.out.println("** Fail **");
                    } else if (!out.equals("")) {
                        System.out.println(formatter.format(out));
                    } else /* Nothing */;
                } catch (non_existing_command_exception e) {
                    System.err.println(e.getMessage());
                    System.exit(harcutils.exit_nonexisting_command);
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            } else if (hm.has_device(first_arg)) {
                try {
                    if ((args.length < arg_i + 2) || args[arg_i + 1].equals("?")) {
                        // No command given, list possible
                        if (db.decode_args()) {
                            System.out.println("Try to list possible device commands");
                        }
                        harcutils.printtable("Valid commands for " + first_arg + " of type " + commandset.toString(type) + ":",
                                hm.get_commands(first_arg, type));
                    } else {
                        // Command expected
                        if (db.decode_args()) {
                            System.out.println("Try to execute as device command");
                        }

                        String cmd_name = args[arg_i + 1];
                        int cmd = ir_code.decode_command(cmd_name);
                        if (cmd == commandnames.cmd_invalid) {
                            cmd = ir_code.decode_command("cmd_" + cmd_name);
                            if (cmd != commandnames.cmd_invalid) {
                                System.err.println("Warning: prepeding `cmd_' to command `"
                                        + cmd_name + "'");
                            }
                        }
                        int no_args = args.length - (arg_i + 2);
                        String[] aux_args = new String[no_args];
                        for (int i = 0; i < no_args; i++) {
                            aux_args[i] = args[arg_i + 2];
                            if (db.decode_args()) {
                                System.err.println("Aux arg[" + i + "] = " + args[arg_i + 2]);
                            }
                        }

                        String output = hm.do_command(first_arg, cmd, aux_args, type, count, toggle, smart_memory);
                        if (output == null) {
                            System.out.println("** Failure **");
                            System.exit(harcutils.exit_fatal_program_failure);
                        } else if (!output.equals("")) {
                            System.out.println(formatter.format(output));
                        }
                    }
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            } else {
                System.err.println("`" + first_arg + "' is neither macro nor device");
                System.exit(harcutils.exit_nonexisting_device);
            }
        }
        try {
            harcprops.get_instance().save();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_config_write_error);
        }

        if (!gui_mode) {
            System.err.println("Program exited normally.");
            System.exit(harcutils.exit_success);
        }
    }
}
