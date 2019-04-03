/*
Copyright (C) 2009-2011, 2019 Bengt Martensson.

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

import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.XmlUtils;
import org.harctoolbox.irp.IrpParseException;
import org.harctoolbox.irp.IrpUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class starts the program in GUI mode, interactive command line mode, port listening mode,
 * or noninteractive mode, depending on the command line parameters given.
 */
final public class Main {

    private final static String bell = "\007";
    //private final static String formfeed = "\014";
    private final static int socketno_default = 9999;
    private final static int python_socketno_default = 9998;
    private final static int NO_VALUE = -9999;
    private static volatile boolean spawn_new_socketthreads = false;
    private static boolean readline_go_on = true; // FIXME
    //private static volatile boolean go_on;
    // Experimental
    private static int no_threads = 0;
    private static Main the_instance = null;
    private static final String helptext =
            "\tharctoolbox --version|--help\n" + "\tharctoolbox [OPTIONS] [-P] [-g|-r|-l [<portnumber>]]\n" + "\tharctoolbox [OPTIONS] -P <pythoncommand>\n" + "\tharctoolbox [OPTIONS] <device_instance> [<command> [<argument(s)>]]\n" + "\tharctoolbox [OPTIONS] -s <device_instance> <src_device_instance>\n" + "where OPTIONS=-A,-V,-M,-C <charset>,-h <filename>,-t " + CommandType_t.valid_types('|') + ",-T 0|1,-# <count>,-v,-d <debugcode>," + "-a <aliasfile>, -p <propsfile>, -w <tasksfile>, -z <zone>,-c <connectiontype>.";
    private static final String readline_help = "Usage: one of\n\t--<command> [<argument(s)>]\n\t<macro>\n\t<device_instance> <command> [<argument(s)>]\n\t--select <device_instance> <src_device_instance>";
    private static Main instance;
    private static Props properties;
    private final static Logger logger = Logger.getLogger(Main.class.getName());

    private static void usage(int exitstatus) {
        HarcUtils.doExit(exitstatus, "Usage: one of" + IrCoreUtils.LINE_SEPARATOR + helptext);
    }

    private static void usage() {
        usage(IrpUtils.EXIT_USAGE_ERROR);
    }

    private static String formatdate(Calendar c) {
        return c != null ? String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) : "";
    }

    public static Main getInstance() {
        return instance;
    }

    public static Props getProperties() {
        return instance.properties;
    }

    /**
     * This method, dependent on the arguments,
     * starts the program either in GUI mode, in interactive commandline mode,
     * in listening mode (listening on a TCP port), or in noninteractive mode.
     *
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        String homefilename = null;
        //String macrofilename = null;
        String aliasfilename = null;
        String propsfilename = null;
        String tasksfilename = null;
        boolean gui_mode = false;
        boolean readline_mode = false;
        boolean daemon_mode = false;
        boolean no_execute = false;
        boolean select_mode = false;
        boolean smart_memory = false;
        boolean use_python = false;
        int count = 1;
        int debug = NO_VALUE;
        boolean verbose = false;
        int socketno = -1;
        CommandType_t type = CommandType_t.any;
        ToggleType toggle = ToggleType.dont_care;
        MediaType the_mediatype = MediaType.audio_video;
        String charset = "iso-8859-1";
        String zone = null;
        ConnectionType connection_type = null;
        String[] noninteractive_args;
        int arg_i = 0;

        try {
            while (arg_i < args.length && (args[arg_i].length() > 0) && args[arg_i].charAt(0) == '-') {

                if (args[arg_i].equals("--help")) {
                    usage(IrpUtils.EXIT_SUCCESS);
                }
                if (args[arg_i].equals("--version")) {
                    //System.out.println("JVM: "+ System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
                    HarcUtils.doExit(IrpUtils.EXIT_SUCCESS, Version.versionString + IrCoreUtils.LINE_SEPARATOR + Version.licenseString);
                }
                switch (args[arg_i]) {
                    case "-#":
                        arg_i++;
                        count = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-A":
                        arg_i++;
                        the_mediatype = MediaType.audio_only;
                        break;
                    case "-C":
                        arg_i++;
                        charset = args[arg_i++];
                        if (!Charset.isSupported(charset))
                            HarcUtils.doExit(IrpUtils.EXIT_USAGE_ERROR, "Unsupported charset " + charset + ", aborting.");
                        break;
                    case "-M":
                        arg_i++;
                        smart_memory = true;
                        break;
                    case "-P":
                        arg_i++;
                        use_python = true;
                        break;
                    case "-V":
                        arg_i++;
                        the_mediatype = MediaType.video_only;
                        break;
                    case "-T":
                        arg_i++;
                        toggle = ToggleType.decode_toggle(args[arg_i++]);
                        break;
                    case "-a":
                        arg_i++;
                        aliasfilename = args[arg_i++];
                        break;
                    case "-c":
                        arg_i++;
                        connection_type = ConnectionType.parse(args[arg_i++]);
                        break;
                    case "-d":
                        arg_i++;
                        debug = Integer.parseInt(args[arg_i++]);
                        break;
                    case "-g":
                        arg_i++;
                        gui_mode = true;
                        break;
                    case "-h":
                        arg_i++;
                        homefilename = args[arg_i++];
                        break;
                    case "-l":
                        arg_i++;
                        daemon_mode = true;
                        if (arg_i < args.length && args[arg_i].charAt(0) != '-')
                            socketno = Integer.parseInt(args[arg_i++]);
                        //} else if (args[arg_i].equals("-m")) {
                        //    arg_i++;
                        //    macrofilename = args[arg_i++];
                        break;
                    case "-n":
                        arg_i++;
                        no_execute = true;
                        break;
                    case "-p":
                        arg_i++;
                        propsfilename = args[arg_i++];
                        break;
                    case "-s":
                        arg_i++;
                        select_mode = true;
                        break;
                    case "-r":
                        arg_i++;
                        readline_mode = true;
                        break;
                    case "-t":
                        arg_i++;
                        String typename = args[arg_i++];
                        if (!CommandType_t.is_valid(typename)) {
                            usage();
                        }   type = CommandType_t.valueOf(typename);
                        break;
                    case "-v":
                        arg_i++;
                        verbose = true;
                        break;
                    case "-w":
                        arg_i++;
                        tasksfilename = args[arg_i++];
                        break;
                    case "-z":
                        arg_i++;
                        zone = args[arg_i++];
                        // Just an idiot check
                        if (zone.startsWith("-"))
                            usage();
                        break;
                    default:
                        usage();
                        break;
                }
            }

            // To allow for double clicking a jar archieve...
            if (args.length == arg_i && !readline_mode && !daemon_mode)
                gui_mode = true;

        } catch (ArrayIndexOutOfBoundsException e) {
            if (debug != 0)
                System.err.println("ArrayIndexOutOfBoundsException");

            usage();
        } catch (NumberFormatException e) {
            if (debug != 0)
                System.err.println("NumberFormatException");

            usage();
        }

        String appHome = findApplicationHome(homefilename);
        properties = new Props(propsfilename, appHome);
        //HarcProps.initialize(propsfilename);
        //properties.set_propsfilename(propsfilename);
        if (debug != NO_VALUE || ! gui_mode)
            properties.setDebug(debug);
        if (verbose || ! gui_mode)
            properties.setVerbose(verbose);

        try {
            ProtocolDataBase.initialize(properties.getIrpProtocolsPath());
        } catch (IOException | IrpParseException ex) {
            HarcUtils.doExit(IrpUtils.EXIT_CONFIG_READ_ERROR, ex.getMessage());
        }

        if (gui_mode) {
            gui_execute(homefilename);
        } else {
            noninteractive_args = new String[args.length - arg_i];
            System.arraycopy(args, arg_i, noninteractive_args, 0, args.length - arg_i);
            if ((args.length != arg_i) && (gui_mode || readline_mode || daemon_mode))
                System.err.println("Warning: extra arguments ignored: " + String.join(", ", noninteractive_args));

            instance = new Main(homefilename, //propsfilename,
                    aliasfilename,
                    no_execute, select_mode, smart_memory, count, type, toggle,
                    the_mediatype, zone, connection_type, charset);

            int status = IrpUtils.EXIT_SUCCESS;
            if (readline_mode) {
                if (tasksfilename != null)
                    instance.do_tasks(tasksfilename);
                try {
                    if (use_python)
                        status = instance.interactive_jython_execute();
                    else
                        status = instance.readline_execute();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted: " + e.getMessage());
                }
            } else if (daemon_mode) {
                if (tasksfilename != null)
                    instance.do_tasks(tasksfilename);
                // start threads for tcp and udp.
                if (socketno == -1)
                    socketno = use_python ? python_socketno_default : socketno_default;
                instance.udp_execute(socketno, use_python, true);
                status = instance.socket_execute(socketno, use_python);
            } else if (use_python)
                status = instance.jython_noninteractive_execute(noninteractive_args);
            else
                status = instance.noninteractive_execute(noninteractive_args);

            // Do not save properties, these are not really useful outside of the GUI
            HarcUtils.doExit(status);
        }
    }

    private static String findApplicationHome(String appHome) {
        String applicationHome = appHome != null ? appHome : System.getenv("HARCTOOLBOXHOME");
        try {
            if (applicationHome == null) {
                URL url = Main.class.getProtectionDomain().getCodeSource().getLocation();
                File dir = new File(url.toURI()).getParentFile();
                applicationHome = (dir.getName().equals("target") || dir.getName().equals("dist"))
                        ? dir.getParent() : dir.getPath();
            }
        } catch (URISyntaxException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            HarcUtils.doExit(IrpUtils.EXIT_FATAL_PROGRAM_FAILURE);
        }
        if (applicationHome != null && !applicationHome.endsWith(File.separator))
            applicationHome += File.separator;

        return applicationHome;
    }

// GUI does not expand aliases.
    private static void gui_execute(String homefilename/*, String propsfilename*/) {
        // Setup properites
        /*if (propsfilename != null)
        harcprops.initialize(propsfilename);
        else
        harcprops.initialize();*/

        if (homefilename != null)
            Main.getProperties().setHomeConf(homefilename);
        else
            homefilename = Main.getProperties().getHomeConf();

        /*if (macrofilename != null)
        harcprops.get_instance().set_macrofilename(macrofilename);
        else
        macrofilename = harcprops.get_instance().get_macrofilename();

        if (browser != null)
        harcprops.get_instance().set_browser(browser);
        */
        //else
        //    browser = harcprops.get_instance().get_browser();

        //System.err.println("Invoking GUI ...");
        //final int dbg = debug;
        //final boolean vrbs = verbose;
        final String hmnam = homefilename;
        //final String macronam = macrofilename;
        //final String brwsr = browser;
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new GuiMain(hmnam/*, macronam, vrbs, dbg, brwsr*/).setVisible(true);
            } catch (IOException | SAXException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        });
    }

    private static String join(String[] stuff, int start_index) {
        if (stuff == null || stuff.length < start_index + 1)
            return null;

        StringBuilder result = new StringBuilder(stuff[start_index]);
        for (int i = start_index + 1; i < stuff.length; i++)
            result = result.append(" ").append(stuff[i]);

        return result.toString();
    }

    @SuppressWarnings("empty-statement")
    private static int no_lines(String s) {
        LineNumberReader lnr = new LineNumberReader(new StringReader(s.trim()));
        try {
            while (lnr.readLine() != null)
                ;
        } catch (IOException ex) {
        }
        return lnr.getLineNumber();
    }

    //String homefilename = null;
    //String macrofilename = null;
    //String browser = null;
    //String propsfilename = null;
    private String aliasfilename = null;
    private ResultFormatter formatter = null;
    //debugargs db = null;
    //private boolean no_execute = false;
    private boolean select_mode = false;
    //int debug = 0;
    //boolean verbose = false;
    private Home hm = null;
    //macro_engine engine = null;
    private HashMap< String, Calendar> timertable = new HashMap<>(16);
    //jython_engine jython = null;
    private CommandAlias alias_expander = null;
    private CommandType_t type = CommandType_t.any;
    private MediaType the_mediatype = MediaType.audio_video;
    private ConnectionType connection_type = ConnectionType.any;
    private String charset = null;
    private String zone = null;
    private int count = 1;
    private ToggleType toggle = ToggleType.dont_care;
    private boolean smart_memory = false;

    private Main(String homefilename, /*String macrofilename, String browser,*/
            /*String propsfilename,*/ String aliasfilename, //int debug, boolean verbose,
            boolean no_execute, boolean select_mode, boolean smart_memory,
            int count, CommandType_t type, ToggleType toggle,
            MediaType the_mediatype, String zone, ConnectionType connection_type, String charset) {

        if (the_instance != null) {
            System.err.println("This class can be instantiated only once!!");
            return;
        }
        the_instance = this;

        //this.homefilename = homefilename;
        //this.browser = browser;
        //this.propsfilename = propsfilename;
        //this.macrofilename = macrofilename;
        this.aliasfilename = aliasfilename;
        //this.debug = debug;
        //this.verbose = verbose;
        //this.no_execute = no_execute;
        this.select_mode = select_mode;
        this.smart_memory = smart_memory;
        this.count = count;
        this.type = type;
        this.toggle = toggle;
        this.the_mediatype = the_mediatype;
        this.zone = zone;
        this.connection_type = connection_type;
        this.charset = charset;

        // Setup properites
        /*if (propsfilename != null)
            harcprops.initialize(propsfilename);
        else
            harcprops.initialize();*/

        if (homefilename != null)
            Main.getProperties().setHomeConf(homefilename);
        else
            homefilename = Main.getProperties().getHomeConf();

        /*if (macrofilename != null)
            harcprops.get_instance().set_macrofilename(macrofilename);
        else
            this.macrofilename = harcprops.get_instance().get_macrofilename();

        if (browser != null)
            harcprops.get_instance().set_browser(browser);
        else
            this.browser = harcprops.get_instance().get_browser();
*/
        if (aliasfilename != null)
            Main.getProperties().setAliasfilename(aliasfilename);
        else
            this.aliasfilename = Main.getProperties().getAliasfilename();

        this.alias_expander = new CommandAlias(this.aliasfilename);
        //db = new debugargs(debug);
        formatter = new ResultFormatter();

        try {
            hm = new Home(homefilename/*, this.verbose, this.debug*/);
        } catch (IOException e) {
            HarcUtils.doExit(IrpUtils.EXIT_CONFIG_READ_ERROR, e.getMessage());
        } catch (SAXException e) {
            HarcUtils.doExit(IrpUtils.EXIT_XML_ERROR, e.getMessage());
        }
        /*try {
            engine = new macro_engine(this.macrofilename, hm, this.debug);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            engine = null;
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
            engine = null;
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            engine = null;
        }*/
    }

    private void do_tasks(String tasksfilename) {
        Document doc;
        try {
            doc = XmlUtils.openXmlFile(new File(tasksfilename));
        } catch (IOException | SAXException ex) {
            System.err.println(ex.getMessage());
            return;
        }
        NodeList tasks = doc.getElementsByTagName("task");
        //System.err.println(tasks.getLength());
        for (int i = 0; i < tasks.getLength(); i++)
            do_task((Element)tasks.item(i));
    }

    private void do_task(Element task) {
        String name = task.getAttribute("name");
        Element time = (Element)task.getElementsByTagName("time").item(0);

        NodeList commandlines = task.getElementsByTagName("commandline");
        NodeList nl = time.getChildNodes();// getElementsByTagName("period");
        int i;
        Element el = null;
        for (i = 0; i < nl.getLength() && el == null; i++)
            if (nl.item(i).getNodeType() == Node.ELEMENT_NODE)
                el = (Element) nl.item(i);
        if (el == null)
            return;

        if (el.getNodeName().equals("period")) {
            Thread thr = new periodic_thread(evaluate_time(el), name, evaluate_cmds(commandlines));
            thr.start();
        } else {
            Thread thr = new nonperiodic_thread(el, name, evaluate_cmds(commandlines));
            thr.start();
        }
    }

    private String[] evaluate_cmds(NodeList nl) {
        String[] s = new String[nl.getLength()];
        for (int i = 0; i < nl.getLength(); i++)
            s[i] = nl.item(i).getTextContent();
        return s;
    }

    private int evaluate_time(Element e) {
        try {
        return  ((Integer.parseInt(e.getAttribute("days"))*24
                + Integer.parseInt(e.getAttribute("hours")))*60
                + Integer.parseInt(e.getAttribute("minutes")))*60
                + Integer.parseInt(e.getAttribute("seconds"));
        } catch (NumberFormatException ex) {
            System.err.println("Warning, Time not parseable, task ignored.");
            System.err.println(ex.getMessage());
            return -1;
        }
    }

    // VERY experimental
    private int udp_execute(int socketno, boolean use_python, boolean create_thread) {
        System.err.println("Trying to listen to UDP socket " + socketno + (use_python ? " (Using Python)" : ""));
        JythonEngine jython = use_python ? new JythonEngine(hm, false) : null;
        int success = IrpUtils.EXIT_SUCCESS;
        if (create_thread)
            new udp_thread(socketno, jython).start();
        else
            success = udp_work(socketno, jython);
        return success;
    }

    private int udp_work(int socketno, JythonEngine jython) {
        byte buf[] = new byte[1000];
        String commandline;
        boolean go_on = true;
        try {
            while (go_on) {
                try (DatagramSocket sock = new java.net.DatagramSocket(socketno)) {
                    DatagramPacket pack = new DatagramPacket(buf, buf.length);
                    sock.receive(pack);
                    commandline = (new String(pack.getData(), 0, pack.getLength())).trim();
                    InetAddress addr = pack.getAddress();
                    int port = pack.getPort();
                    System.out.println("Got >" + commandline + "< from " + addr.getHostName() + ":" + port);
                    if (commandline.isEmpty() || commandline.equals("--quit"))
                        go_on = false;
                    else {
                        String result = jython != null ? jython.eval(commandline) : process_line(commandline, false);
                        System.out.println(result != null ? "OK: " + result : "ERROR");
                    }
                    pack = new DatagramPacket(buf, 0, addr, port);
                    sock.send(pack);
                    //sock.disconnect();
                }
            }

       } catch (BindException e) {
            System.err.println("Got Bindexception: " + e.getMessage());
            //e.printStackTrace();
            return IrpUtils.EXIT_IO_ERROR;
       } catch (SocketException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
            return IrpUtils.EXIT_IO_ERROR;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
            return IrpUtils.EXIT_IO_ERROR;
        }
        return IrpUtils.EXIT_SUCCESS;
    }

    // This is very experimental and unfinished stuff. It does not give any sensible
    // information back to the client (most important for get_* commands).
    private int socket_execute(int socketno, boolean use_python) {
        System.err.println("Trying to listen to TCP socket " + socketno + (use_python ? " (Using Python)" : "")
                + ", Output encoding: " + charset);
        ServerSocket srv_sock;
        try {
            srv_sock = new java.net.ServerSocket(socketno);
        } catch (IOException e) {
            System.out.println("Could not listen on port " + socketno);
            return IrpUtils.EXIT_IO_ERROR;
        }

        spawn_new_socketthreads = true;
        try {
            while (spawn_new_socketthreads) {
                Socket s = srv_sock.accept();
                if (spawn_new_socketthreads)
                    new socket_thread(s, use_python).start();
            }
        } catch (IOException e) {
            System.out.println("Could not listen on port " + socketno);
            return IrpUtils.EXIT_IO_ERROR;
        }

        try {
            srv_sock.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return IrpUtils.EXIT_IO_ERROR;
        }

        return IrpUtils.EXIT_SUCCESS;
    }

//    private Calendar get_timer_next(String name) {
//        return timertable != null ? timertable.get(name) : null;
//    }

//    private String get_timer_next_as_string(String name) {
//        return timertable != null ? formatdate(timertable.get(name)) : null;
//    }

    //public Enumeration<String> get_timer_names_e() {
    //    return timertable.keys();
    //}

    /*public String[] get_timer_names() {
        Enumeration<String> e = timertable.keys();
        String[] arr = new String[timertable.size()];
        for (int i = 0; e.hasMoreElements(); i++)
            arr[i++] = e.nextElement();
        return arr;
    }*/

    private int interactive_jython_execute() /*throws InterruptedException*/ {
        JythonEngine interactive_jython = new JythonEngine(hm, true);
        interactive_jython.interact();
        return IrpUtils.EXIT_SUCCESS;
    }

    // Recognizes aliases, however, readline does not know them
    private int readline_execute() throws InterruptedException {
        // FIXME: should not die from control-c
        /*try {
        hm = new home(homefilename, verbose, debug, browser);
        engine = new macro_engine(macrofilename, hm, debug);
        } catch (IOException e) {
        System.err.println(e.getMessage());
        return IrpUtils.exit_config_read_error;
        } catch (SAXParseException e) {
        System.err.println(e.getMessage());
        return IrpUtils.exit_xml_error;
        }
         */

        if (DebugArgs.dbg_decode_args()) {
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

        File history;

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    Readline.writeHistoryFile(Main.getProperties().getRlHistoryfile());
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                Readline.cleanup();
                try {
                    SocketStorage.dispose_sockets(true);
                } catch (IOException e) {
                    //e.printStackTrace();
                }
                if (DebugArgs.dbg_misc())
                    System.err.println("*************** This is Readline shutdown **********");
            }
        });

        try {
            Readline.load(ReadlineLibrary.GnuReadline);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: GNU readline not found.");
        }

        Readline.initReadline(Main.getProperties().getAppname());

        history = new File(Main.getProperties().getRlHistoryfile());

        // I become funny errors (UnsupportedEncodingException below) if historyfile
        // does not exist. Therefore create it if not there already.
        try {
            history.createNewFile();
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (history.exists())
                Readline.readHistoryFile(Main.getProperties().getRlHistoryfile());
        } catch (EOFException | UnsupportedEncodingException e) {
            System.err.println("Could not read rl history " + e.getMessage());
        }

        try {
            Readline.setWordBreakCharacters(""/*" \t;"*/);
        } catch (UnsupportedEncodingException enc) {
            // FIXME
            System.err.println(enc.getMessage() + "Could not set word break characters");
            System.err.println("Try touching " + Main.getProperties().getRlHistoryfile());
            return IrpUtils.EXIT_THIS_CANNOT_HAPPEN;
        }

        Readline.setCompleter(new RlCompleter(cl_commands, /*engine,*/ hm));

        // FIXME
        boolean use_readline_thread = false;
        try {
            while (readline_go_on) {
                try {
                    if (use_readline_thread) {
                        Thread thr = new readline_thread();
                        thr.start();
                        thr.join();
                    } else {
                        String line = Readline.readline(Main.getProperties().getRlPrompt(), false);
                        if (line != null && !line.isEmpty()) {
                            line = line.trim();
                            int history_size = Readline.getHistorySize();
                            if (history_size < 1 || !Readline.getHistoryLine(Readline.getHistorySize()-1).equals(line))
                                Readline.addToHistory(line);
                            process_line(line, true);
                        }
                    //readline_go_on = result != null;
                    }
                //System.err.println(thr.isInterrupted());
                //if (Thread.interrupted())
                //    System.err.println("interrupted******rrrrrrrrrrrrrrr");
                } catch (InterruptedException e) {
                    System.err.println("interrupted" + e.getMessage());
                } catch (EOFException e) {
                    // User pressed EOF and want to quit
                    readline_go_on = false;
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        // NOTE: Readline.writeHistoryFile and Readline.cleanup is done in
        // the shutdown hook; do not add it here too.

        return IrpUtils.EXIT_SUCCESS;
    }

    /**
     *
     * @param line
     * @return  null for failure, non-null for success (possibly "").
     */
    private String process_line(String line, boolean verbose) throws EOFException {
        if (line == null)
            return null;

        String result = "";

        String[] arguments = line.split("[ \t]+");
        if (arguments[0].equals("--select")) {
            // TODO: currently no support for connectiontype
            if (arguments.length < 3) {
                if (verbose)
                    System.out.println(bell + "***--select takes at least two arguments***.");
            } else {
                if (DebugArgs.dbg_decode_args()) {
                    System.out.println("Select mode: devname = " + arguments[1] + ", src_device = " + arguments[2] + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
                }

                try {
                    result = null;
                    boolean ok = hm.select(arguments[1], arguments[2], type,
                            zone, the_mediatype, connection_type);
                    if (ok)
                        result = "";
                } catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }
            }
        } else if (line.startsWith("--")) {
            // Decode commands
            switch (arguments[0]) {
                case "--quit":
                case "--exit":
                    //result = null;
                    throw new EOFException("As you requested");
                case "--version":
                    if (verbose)
                        System.out.println(Version.versionString);
                    result = Version.versionString;
                    break;
                case "--help":
                    if (verbose)
                        System.out.println(readline_help);
                    result = readline_help;
                    break;
                case "--license":
                    if (verbose)
                        System.out.println(Version.licenseString);
                    result = Version.licenseString;
                    break;
                case "--verbose":
                    /*boolean v = true;
                    try {
                    v = Integer.parseInt(arguments[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                    System.out.println("+++ Argument missing, assuming 1.");
                    } catch (NumberFormatException e) {
                    System.out.println("+++ Parse error, assuming 1.");
                    }*/
                    //hm.set_verbosity(true);
                    properties.setVerbose(true);
                    result = "Verbosity set";
                    break;
                case "--debug":
                    int v = 0;
                    try {
                        v = Integer.parseInt(arguments[1]);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("+++ Argument missing, assuming 0.");
                    } catch (NumberFormatException e) {
                        System.out.println("+++ Parse error, assuming 0.");
                    }   //hm.set_debug(v);
                    //engine.set_debug(v);
                    properties.setDebug(v);
                    result = "debug set to " + v;
                    break;
                case "--zone":
                    zone = (arguments.length > 1) ? arguments[1] : null;
                    if (DebugArgs.dbg_decode_args())
                        System.out.println("%%% Zone is now " + zone);
                    break;
                case "--audio-video":
                    the_mediatype = MediaType.audio_video;
                    break;
                case "--audio-only":
                    the_mediatype = MediaType.audio_only;
                    break;
                case "--video-only":
                    the_mediatype = MediaType.video_only;
                    break;
                default:
                    if (verbose)
                        System.out.println(bell + "*** Unrecognized command ***");
                    result = null;
                    break;
            }
        }/* else if (engine != null && engine.has_macro(arguments[0])) {
            // NO-FIXME: not implemented: macro arguments,
            // FIXME (possibly): no_execute
            if (db.decode_args())
                System.err.println("%%% Now executing macro `" + arguments[0] + "'");
            try {
                result = engine.eval_macro(arguments[0], null, 0, false);

                if (result == null) {
                    if (verbose)
                        System.out.println(bell + "*** macro failed ***");
                } else if (!result.isEmpty()) {
                    if (verbose)
                        System.out.println(formatter.format(result));
                }
            } catch (non_existing_command_exception e) {
                // This cannot happen
                } catch (InterruptedException e) {
                System.out.println(bell + "*** Interrupted ***");
            }
        }*/ else if (hm.has_device(arguments[0])) {
            // TODO: not implemented: type, count, toggle, smart_memory
            if (DebugArgs.dbg_decode_args())
                System.out.println("%%% Trying to execute `" + line + "'");

            String cmd_name = arguments[0];
            if (arguments.length < 2) {
                if (verbose)
                    System.out.println(bell + "*** command missing ***");
            } else {
                command_t cmd = alias_expander.canonicalize(arguments[1]);
                String[] aux_args = new String[arguments.length - 2];

                if (aux_args.length < hm.get_arguments(arguments[0], cmd, CommandType_t.any)) {
                    System.err.println(bell + "*** Too few arguments ***");
                } else {
                    //if (aux_args.length > hm.get_arguments(arguments[0], cmd, commandtype_t.any))
                    //    System.err.println(bell + "*** Excess arguments ignored ***");

                    for (int i = 0; i < arguments.length - 2; i++) {
                        aux_args[i] = arguments[i + 2];
                        if (DebugArgs.dbg_decode_args()) {
                            System.err.println("Aux arg[" + i + "] = " + aux_args[i]);
                        }
                    }
                    // Heuristic: If exactly one argument is needed,
                    // and several given, tuck the given ones together.
                    // (e.g. for selecting TV channels with names containing spaces.)
                    if (hm.get_arguments(arguments[0], cmd, CommandType_t.any) == 1) {
                        if (DebugArgs.dbg_decode_args())
                            System.err.println("Concatenating arguments");
                        aux_args[0] = join(arguments, 2);
                    }

                    try {
                        if (cmd == command_t.invalid) {
                            if (verbose)
                                System.err.println("Command `" + arguments[1] + "' does not exist.");
                            result = null;
                        } else
                            result = hm.do_command(arguments[0], cmd, aux_args, type, count, toggle, smart_memory);
                        if (result == null) {
                            if (verbose)
                                System.out.println(bell + "*** Failure ***");
                        } else if (!result.isEmpty()) {
                            if (verbose)
                                System.out.println(formatter.format(result));
                        }
                    } catch (InterruptedException e) {
                        System.out.println(bell + "*** Interrupted ***");
                    }
                }
            }
        } else {
            if (verbose)
                System.out.println(bell + "*** Neither macro nor device with name `" + arguments[0] + "'***.");
            result = null;
        }
        return result;
    }

    private int jython_noninteractive_execute(String[] noninteractive_args) {
        JythonEngine jython = new JythonEngine(hm, false);
        for (String noninteractive_arg : noninteractive_args)
            jython.exec(noninteractive_arg);

        return 0;
    }

    /**
     *
     * @param noninteractive_args
     * @return Status to be used as exitstatus
     */
    private int noninteractive_execute(String[] noninteractive_args) {
        String first_arg = noninteractive_args[0];
        if (select_mode) {
            if (noninteractive_args.length < 2) {
                System.err.println("Source device missing");
                return IrpUtils.EXIT_USAGE_ERROR;
            }
            String dst_device = noninteractive_args[0];
            String src_device = noninteractive_args[1];
            System.err.println("select mode ...");
            if (DebugArgs.dbg_decode_args()) {
                System.err.println("Select mode: devname = " + dst_device + ", src_device = " + src_device + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
            }
            if (src_device.equals("?")) {
                if (hm.has_device(dst_device)) {
                    HarcUtils.printtable("Valid inputs for " + dst_device + (zone != null ? (" in zone " + zone) : "") + ":",
                            hm.get_sources(dst_device, zone));
                } else {
                    HarcUtils.doExit(IrpUtils.EXIT_IO_ERROR, "No such device `" + dst_device + "'");
                }
            } else {
                try {
                    hm.select(dst_device, src_device, type, zone, the_mediatype, connection_type);
                } catch (InterruptedException e) {
                    HarcUtils.doExit(IrpUtils.EXIT_INTERRUPTED, e.getMessage());
                }
            }

        }/* else if (first_arg.equals("?")) {
            // List macros
            harcutils.printtable("Available macros: ", engine.get_macros(true));
        }*/ else if (first_arg.equals("?")) {
            // List devices
            HarcUtils.printtable("Available devices:", hm.get_devices());
        }/* else if (engine != null && engine.has_macro(first_arg)) {
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
                } else
                    ;
            } catch (non_existing_command_exception e) {
                System.err.println(e.getMessage());
                System.exit(IrpUtils.exit_nonexisting_command);
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }*/ else if (hm.has_device(first_arg)) {
            try {
                if ((noninteractive_args.length < 2) || noninteractive_args[1].equals("?")) {
                    // No command given, list possible
                    if (DebugArgs.dbg_decode_args()) {
                        System.out.println("Try to list possible device commands");
                    }
                    HarcUtils.printtable("Valid commands for " + first_arg + " of type " + type + ":",
                            hm.get_commands(first_arg, type));
                } else {
                    // Command expected
                    if (DebugArgs.dbg_decode_args()) {
                        System.out.println("Try to execute as device command");
                    }

                    String cmd_name = noninteractive_args[1];
                    command_t cmd = alias_expander.canonicalize(cmd_name);
                    String output = null;
                    if (cmd == command_t.invalid) {
                        System.err.println("Command `" + cmd_name + "' is invalid.");
                    } else {
                        int no_args = noninteractive_args.length - 2;
                        String[] aux_args = new String[no_args];
                        for (int i = 0; i < no_args; i++) {
                            aux_args[i] = noninteractive_args[i + 2];
                            if (DebugArgs.dbg_decode_args())
                                System.err.println("Aux arg[" + i + "] = " + aux_args[i]);
                        }

                        output = hm.do_command(first_arg, cmd, aux_args, type, count, toggle, smart_memory);
                    }
                    if (output == null) {
                        System.out.println("** Failure **");
                        return IrpUtils.EXIT_FATAL_PROGRAM_FAILURE;
                    } else if (!output.isEmpty()) {
                        System.out.println(formatter.format(output));
                    }
                }
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("No device `" + first_arg + "' known, issue `harctoolbox ?' for a list of known devices.");
            return IrpUtils.EXIT_IO_ERROR;
        }
        try {
            SocketStorage.dispose_sockets(true);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return IrpUtils.EXIT_SUCCESS;
    }
    /*private void do_periodic_task(int secs, String name, String[] cmds) {
    if (secs <= 0)
    return;
    System.err.println(secs);
    System.err.println(name);
    for (int i = 0; i < cmds.length; i++) {
    System.err.print(cmds[i] + ": ");
    try {
    String result = process_line(cmds[i], false);
    } catch (EOFException ex) {
    ex.printStackTrace();
    }
    }
    }*/

    private class periodic_thread extends Thread {
        int secs;
        String name;
        String[] cmds;

        periodic_thread(int secs, String name, String[] cmds) {
            super(name);
            this.secs = secs;
            //this.name = name;
            this.cmds = cmds;
        }

        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run() {
            while (true) {
                long starttime = System.currentTimeMillis();
                for (String cmd : cmds) {
                    System.out.print(cmd + ": ");
                    try {
                        String result = process_line(cmd, false);
                    } catch (EOFException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }

                }
                long towait = 1000L * secs - (System.currentTimeMillis() - starttime);
                if (towait > 10) {
                    try {
                        Thread.sleep(towait);
                    } catch (InterruptedException ex) {
                        System.err.println("interrupted");
                    }
                }
            }
        }
    }

    private class nonperiodic_thread extends Thread {
        Element ele;
        String name;
        String[] cmds;

        nonperiodic_thread(Element ele, String name, String[] cmds) {
            super(name);
            this.ele = ele;
            this.name = name;
            this.cmds = cmds;
        }

        private void fix_cal(Calendar cal) {
            if (cal != null) {
                Calendar now = new GregorianCalendar();
                //System.out.println(cal.getTime() + "*****" + now.getTime() + now.before(cal) + now.after(cal));
                if (now.after(cal)) {
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    //System.out.println("Fixing: " + cal.getTime() + "*****" + now.getTime() + now.before(cal) + now.after(cal));
                }
            }
        }

        private Element get_weekday(String weekday, NodeList nl) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element e = (Element) nl.item(i);
                if (e.getAttribute("day").equalsIgnoreCase(weekday))
                    return e;
            }
            return null;
        }

        private Calendar evaluate_time_weekday(Element e, Calendar calendar) {
            if (e == null)
                return null;
            Calendar cal = (Calendar) calendar.clone();
            NodeList nl = e.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    Calendar c = evaluate_time((Element) nl.item(i));
                    cal.set(Calendar.HOUR_OF_DAY, c.get(GregorianCalendar.HOUR_OF_DAY));
                    cal.set(Calendar.MINUTE, c.get(GregorianCalendar.MINUTE));
                    cal.set(Calendar.SECOND, c.get(GregorianCalendar.SECOND));
                    return cal;
                }
            }
            return null;
        }

        private Calendar evaluate_time(Element e) {
            Calendar cal = new GregorianCalendar();
            if (e.getTagName().equals("absolute-time")) {
                try {
                    cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(e.getAttribute("hour")));
                    cal.set(Calendar.MINUTE, Integer.parseInt(e.getAttribute("minute")));
                    cal.set(Calendar.SECOND, Integer.parseInt(e.getAttribute("second")));
                } catch (NumberFormatException expt) {
                    logger.severe(expt.getMessage());
                }

                fix_cal(cal);

            } else if (e.getTagName().equals("sunset") || e.getTagName().equals("sunrise")) {
                try {
                    double latitude = Double.parseDouble(e.getAttribute("latitude"));
                    double longitude = Double.parseDouble(e.getAttribute("longitude"));
                    double degrees = Double.parseDouble(e.getAttribute("degrees"));
                    String timezone = e.getAttribute("tz");
                    TimeZone tz = timezone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
                    Calendar c;
                    if (e.getTagName().equals("sunrise")) {
                        c = SunriseSunsetCalculator.getSunrise(latitude, longitude, tz, cal, degrees);
                        if (cal.after(c)) {
                            cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
                            c = SunriseSunsetCalculator.getSunrise(latitude, longitude, tz, cal, degrees);
                        }
                    } else {
                        c = SunriseSunsetCalculator.getSunset(latitude, longitude, tz, cal, degrees);
                        if (cal.after(c)) {
                            cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
                            c = SunriseSunsetCalculator.getSunset(latitude, longitude, tz, cal, degrees);
                        }
                    }
                    cal = c;
                } catch (NumberFormatException expt) {
                    logger.severe(expt.getMessage());
                }
            } else if (e.getTagName().equals("sunrise")) {
                // FIXME
                cal.set(Calendar.HOUR_OF_DAY, 6);
                cal.set(Calendar.MINUTE, 42);
                cal.set(Calendar.SECOND, 0);
                fix_cal(cal);
            } else if (e.getTagName().equals("last-of")) {
                cal.set(2000, 1, 1);
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Calendar c = evaluate_time((Element) nl.item(i));
                        if (cal.before(c))
                            cal = c;
                    }
                }
            } else if (e.getTagName().equals("first-of")) {
                cal.set(2999, 1, 1);
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        Calendar c = evaluate_time((Element) nl.item(i));
                        if (cal.after(c))
                            cal = c;
                    }
                }
            } else if (e.getTagName().equals("weekdays")) {
                NodeList nl = e.getElementsByTagName("weekday");
                Element today = get_weekday(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US), nl);
                //System.err.println(cal.getTime());
                Calendar todays_candidate = evaluate_time_weekday(today, cal);
                //System.err.println(cal.getTime() + " " + todays_candidate.getTime() + "  " + cal.before(todays_candidate));
                if (todays_candidate != null && cal.before(todays_candidate)) {
                    cal = evaluate_time_weekday(today, cal);
                } else {
                    Element next_day = null;
                    for (int i = 1; next_day == null && i < 7; i++) {
                        cal.add(Calendar.DAY_OF_WEEK, 1);
                        next_day = get_weekday(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US), nl);
                    }
                    cal = next_day != null ? evaluate_time_weekday(next_day, cal) : null;
                }
            } else {
                System.err.println("silly tag: " + e.getTagName());
                cal = null;
            }
            return cal;
        }

        @Override
        @SuppressWarnings("SleepWhileInLoop")
        public void run() {
            while (true) {
                //System.out.println("\"" + name + "\" ");
                Calendar next = evaluate_time(ele);
                the_instance.timertable.put(name, next); // FIXME?
                long towait = next.getTimeInMillis() - System.currentTimeMillis();
                if (towait > 10) {
                    long hours = towait / 3600000L;
                    long minutes = (towait - hours * 3600000L) / 60000L;
                    // TODO: This should, at least optionally, to a logfile instead.
                    System.out.println("Now is " + formatdate(new GregorianCalendar())
                            + ", preparing to sleep until " + formatdate(next)
                            + ", which is for " + String.format("%d:%02d", hours, minutes) + ".");
                    try {
                        Thread.sleep(towait);
                    } catch (InterruptedException ex) {
                        System.err.println("interrupted");
                    }
                }
                for (String cmd : cmds) {
                    // TODO: This should, at least optionally, to a logfile instead.
                    System.out.print("Thread \"" + name + "\" woke up, trying to execute " + cmd + ": ");
                    try {
                        String result = process_line(cmd, false);
                    } catch (EOFException ex) {
                        logger.severe(ex.getMessage());
                    }
                }
                // Prevent from triggering again immediately.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    System.err.println("interrupted");
                }
            }
        }
    }

    private class udp_thread extends Thread {

        private final int socket_no;
        private final JythonEngine jython;

        udp_thread(int socket_no, JythonEngine jython) {
            super("udp_thread");
            this.socket_no = socket_no;
            this.jython = jython;
        }

        @Override
        public void run() {
            udp_work(socket_no, jython);
        }
    }

    // Each socket_thread has its own jython engine, operating on the same home.
    private class socket_thread extends Thread {

        private Socket sock = null;
        private JythonEngine jython = null;
        private final String clientname;


        socket_thread(Socket sock, boolean use_python) {
            super("socket_thread");
            Main.no_threads++;
            this.sock = sock;
            clientname = sock.getInetAddress().getHostName();
            jython = use_python ? new JythonEngine(hm, false) : null;
        }

        @Override
        public void run() {

            boolean kill_prog  = false;
            boolean restart_prog = false;
            //go_on = true;
            try (PrintStream out = new PrintStream(sock.getOutputStream(), false, charset);
                    BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream(), charset))) {
                //out.println("HARC server @ your service!");
                //out.println("HARC server für dich!");

                boolean go_on = true;

                while (go_on) {
                    String commandline = in.readLine();
                    System.err.println("[" + clientname + "," + Main.no_threads + "]"
                            + (commandline != null ? (">" + commandline + "<") : "EOF"));
                    if (null == commandline) {
                        go_on = false;
                        out.println("BYE");
                    } else switch (commandline) {
                        case "--quit":
                            go_on = false;
                            out.println("BYE");
                            break;
                        case "--die":
                            // Kills the program
                            go_on = false;
                            spawn_new_socketthreads = false;
                            kill_prog = true;
                            out.println("BYE");
                            System.err.println("die received");
                            break;
                        case "--restart":
                            // Same as --die but with different exit code
                            go_on = false;
                            spawn_new_socketthreads = false;
                            kill_prog = true;
                            restart_prog = true;
                            out.println("BYE FOR NOW");
                            System.err.println("restart received");
                            break;
                        case "--exit":
                            // Lets the threads finish
                            go_on = false;
                            spawn_new_socketthreads = false;
                            out.println("BYE");
                            System.err.println("exit received");
                            break;
                        case "--foo":
                            System.err.println("foo received");
                            out.println("bar!");
                            break;
                        default:
                            // process this line
                            String result = jython == null ? process_line(commandline, false) : jython.eval(commandline);
                            out.println(result != null ? "OK: " + no_lines(result) + "\n" + result : "ERROR");
                            System.err.println("--|" + (result == null ? "" : result.length() < 70 ? result : (result.substring(0, 69) + "...")) + "|--");
                            break;
                    }
                }
            } catch (IOException e) {
                System.err.println("IOException caught in sockettread.run(): " + e.getMessage());
            } finally {
                try {
                    sock.close();
                    SocketStorage.dispose_sockets(true);
                    Main.no_threads--;
                    if (kill_prog) {
                        HarcUtils.doExit(restart_prog ? IrpUtils.EXIT_RESTART : IrpUtils.EXIT_SUCCESS);
                    }
                } catch (IOException ex) {
                    System.err.println("IOException when closing " + ex.getMessage());
                }
            }
        }
    }
    private class readline_thread extends Thread {

        readline_thread() {
            super("readline_thread");
        }

        @Override
        public void run() {
            try {
                String line = Readline.readline(Main.getProperties().getRlPrompt());
                //Thread.sleep(100);
                String result = process_line(line, true);
                //Thread.sleep(100);
                Main.readline_go_on = result != null;
                //if (isInterrupted())
                //    System.err.println("rrrrrrrrrrrrrrr");
                //if (interrupted())
                //    System.err.println("interrupted******rrrrrrrrrrrrrrr");
            } catch (EOFException e) {
                System.err.println("EOF " + e.getMessage());
                Main.readline_go_on = false;
            } catch (IOException e) {
                System.err.println("io " + e.toString() + e.getMessage());
                Main.readline_go_on = false;
                //} catch (InterruptedException e) {
                //    System.err.println("aaaaaaaaaaaarrg");
            }
        }
    }
}
