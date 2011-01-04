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
import org.xml.sax.*;
import org.gnu.readline.*;
import org.w3c.dom.*;
import java.util.*;
import com.luckycatlabs.sunrisesunset.SunriseSunset;

/**
 * This class starts the program in GUI mode, interactive command line mode, port listening mode,
 * or noninteractive mode, depending on the command line parameters given.
 */
public class Main {

    private final static String bell = "\007";
    private final static String formfeed = "\014";
    private final static int socketno_default = 9999;
    private final static int python_socketno_default = 9998;
    String homefilename = null;
    String macrofilename = null;
    String browser = null;
    String propsfilename = null;
    String aliasfilename = null;
    resultformatter formatter = null;
    debugargs db = null;
    boolean no_execute = false;
    boolean select_mode = false;
    int debug = 0;
    boolean verbose = false;
    home hm = null;
    macro_engine engine = null;
    Hashtable< String, GregorianCalendar> timertable = new Hashtable<String, GregorianCalendar>();
    //jython_engine jython = null;
    command_alias alias_expander = null;
    commandtype_t type = commandtype_t.any;
    mediatype the_mediatype = mediatype.audio_video;
    String connection_type = null;
    String zone = null;
    int count = 1;
    toggletype toggle = toggletype.no_toggle;
    boolean smart_memory = false;
    static boolean spawn_new_socketthreads = false;
    private static boolean readline_go_on = true; // FIXME

    private static Main the_instance = null;
    public static Main get_instance() {
        return the_instance;
    }

    private static void usage(int exitstatus) {
        System.err.println("Usage: one of");
        System.err.println(helptext);
        System.exit(exitstatus);
    }

    private static void usage() {
        usage(harcutils.exit_usage_error);
    }
    private static final String helptext =
            "\tharc --version|--help\n" + "\tharc [OPTIONS] [-g|-r|-l [<portnumber>]]\n" + "\tharc [OPTIONS] <macro>\n" + "\tharc [OPTIONS] <device_instance> <command> [<argument(s)>]\n" + "\tharc [OPTIONS] -s <device_instance> <src_device_instance>\n" + "where OPTIONS=-A,-V,-M,-P,-h <filename>,-t " + commandtype_t.valid_types('|') + ",-m <macrofilename>,-T 0|1,-# <count>,-v,-d <debugcode>," + "-a <aliasfile>, -b <browserpath>, -p <propsfile>, -w <tasksfile>, -z <zone>,-c <connection_type>.";
    private static final String readline_help = "Usage: one of\n\t--<command> [<argument(s)>]\n\t<macro>\n\t<device_instance> <command> [<argument(s)>]\n\t--select <device_instance> <src_device_instance>";

    private static String formatdate(Calendar c) {
            return c != null ? String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)) : "";
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
        String macrofilename = null;
        String aliasfilename = null;
        String browser = null;
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
        int debug = 0;
        boolean verbose = false;
        int socketno = -1;
        commandtype_t type = commandtype_t.any;
        toggletype toggle = toggletype.no_toggle;
        mediatype the_mediatype = mediatype.audio_video;
        String zone = null;
        String connection_type = null;
        String[] noninteractive_args;
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
                } else if (args[arg_i].equals("-P")) {
                    arg_i++;
                    use_python = true;
                 } else if (args[arg_i].equals("-V")) {
                    arg_i++;
                    the_mediatype = mediatype.video_only;
                } else if (args[arg_i].equals("-T")) {
                    arg_i++;
                    toggle = toggletype.decode_toggle(args[arg_i++]);
                } else if (args[arg_i].equals("-a")) {
                    arg_i++;
                    aliasfilename = args[arg_i++];
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
                } else if (args[arg_i].equals("-l")) {
                    arg_i++;
                    daemon_mode = true;
                    if (arg_i < args.length && args[arg_i].charAt(0) != '-')
                        socketno = Integer.parseInt(args[arg_i++]);
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
                    if (!commandtype_t.is_valid(typename)) {
                        usage();
                    }
                    type = commandtype_t.valueOf(typename);
                } else if (args[arg_i].equals("-v")) {
                    arg_i++;
                    verbose = true;
                } else if (args[arg_i].equals("-w")) {
                    arg_i++;
                    tasksfilename = args[arg_i++];
                } else if (args[arg_i].equals("-z")) {
                    arg_i++;
                    zone = args[arg_i++];
                    // Just an idiot check
                    if (zone.startsWith("-"))
                        usage();
                } else {
                    usage();
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

        if (gui_mode) {
            gui_execute(homefilename, macrofilename, browser, propsfilename, debug, verbose);
        } else {
            noninteractive_args = new String[args.length - arg_i];
            System.arraycopy(args, arg_i, noninteractive_args, 0, args.length - arg_i);
            if ((args.length != arg_i) && (gui_mode || readline_mode || daemon_mode))
                System.err.println("Warning: extra arguments ignored: " + harcutils.join(noninteractive_args));

            Main m = new Main(homefilename, macrofilename, browser, propsfilename,
                    aliasfilename, debug, verbose,
                    no_execute, select_mode, smart_memory, count, type, toggle,
                    the_mediatype, zone, connection_type);
            if (m == null)
                System.exit(harcutils.exit_fatal_program_failure);

            int status = harcutils.exit_success;
            if (readline_mode) {
                if (tasksfilename != null)
                    m.do_tasks(tasksfilename);
                try {
                    if (use_python)
                        status = m.interactive_jython_execute();
                    else
                        status = m.readline_execute();
                } catch (InterruptedException e) {
                    System.err.println("Interrupted: " + e.getMessage());
                }
            } else if (daemon_mode) {
                if (tasksfilename != null)
                    m.do_tasks(tasksfilename);
                // start threads for tcp and udp.
                if (socketno == -1)
                    socketno = use_python ? python_socketno_default : socketno_default;
                m.udp_execute(socketno, use_python, true);
                status = m.socket_execute(socketno, use_python);
            } else
                if (use_python)
                    status = m.jython_noninteractive_execute(noninteractive_args);
                else
                    status = m.noninteractive_execute(noninteractive_args);

            m.shutdown();

            if (status == harcutils.exit_success)
                System.err.println("Program exited normally.");
            System.exit(status);
        }
    }

    public Main(String homefilename, String macrofilename, String browser,
            String propsfilename, String aliasfilename, int debug, boolean verbose,
            boolean no_execute, boolean select_mode, boolean smart_memory,
            int count, commandtype_t type, toggletype toggle,
            mediatype the_mediatype, String zone, String connection_type) {

        if (the_instance != null) {
            System.err.println("This class can be instantiated only once!!");
            return;
        }
        the_instance = this;

        this.homefilename = homefilename;
        this.browser = browser;
        this.propsfilename = propsfilename;
        this.macrofilename = macrofilename;
        this.aliasfilename = aliasfilename;
        this.debug = debug;
        this.verbose = verbose;
        this.no_execute = no_execute;
        this.select_mode = select_mode;
        this.smart_memory = smart_memory;
        this.count = count;
        this.type = type;
        this.toggle = toggle;
        this.the_mediatype = the_mediatype;
        this.zone = zone;
        this.connection_type = connection_type;

        // Setup properites
        if (propsfilename != null)
            harcprops.initialize(propsfilename);
        else
            harcprops.initialize();

        if (homefilename != null)
            harcprops.get_instance().set_homefilename(homefilename);
        else
            this.homefilename = harcprops.get_instance().get_homefilename();

        if (macrofilename != null)
            harcprops.get_instance().set_macrofilename(macrofilename);
        else
            this.macrofilename = harcprops.get_instance().get_macrofilename();

        if (browser != null)
            harcprops.get_instance().set_browser(browser);
        else
            this.browser = harcprops.get_instance().get_browser();

        if (aliasfilename != null)
            harcprops.get_instance().set_aliasfilename(aliasfilename);
        else
            this.aliasfilename = harcprops.get_instance().get_aliasfilename();

        this.alias_expander = new command_alias(this.aliasfilename);
        db = new debugargs(debug);
        formatter = new resultformatter();

        try {
            hm = new home(this.homefilename, this.verbose, this.debug, this.browser);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_config_read_error);
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_xml_error);
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_xml_error);
        }
        try {
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
        }
    }

    private void shutdown() {
        try {
            harcprops.get_instance().save();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_config_write_error);
        }
    }

    // GUI does not expand aliases.
    private static void gui_execute(String homefilename, String macrofilename,
            String browser, String propsfilename, int debug, boolean verbose) {
        // Setup properites
        if (propsfilename != null)
            harcprops.initialize(propsfilename);
        else
            harcprops.initialize();

        if (homefilename != null)
            harcprops.get_instance().set_homefilename(homefilename);
        else
            homefilename = harcprops.get_instance().get_homefilename();

        if (macrofilename != null)
            harcprops.get_instance().set_macrofilename(macrofilename);
        else
            macrofilename = harcprops.get_instance().get_macrofilename();

        if (browser != null)
            harcprops.get_instance().set_browser(browser);
        else
            browser = harcprops.get_instance().get_browser();

        //System.err.println("Invoking GUI ...");
        final int dbg = debug;
        final boolean vrbs = verbose;
        final String hmnam = homefilename;
        final String macronam = macrofilename;
        final String brwsr = browser;
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new gui_main(hmnam, macronam, vrbs, dbg, brwsr).setVisible(true);
            }
        });
    }

    private void do_tasks(String tasksfilename) {
        Document doc = null;
        try {
            doc = harcutils.open_xmlfile(tasksfilename);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            return;
        } catch (SAXParseException ex) {
            System.err.println(ex.getMessage());
            return;
        } catch (SAXException ex) {
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
            s[i] = ((Element)nl.item(i)).getTextContent();
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

        public periodic_thread(int secs, String name, String[] cmds) {
            super(name);
            this.secs = secs;
            this.name = name;
            this.cmds = cmds;
        }

        @Override
        public void run() {
            while (true) {
                long starttime = System.currentTimeMillis();
                for (int i = 0; i < cmds.length; i++) {
                    System.out.print(cmds[i] + ": ");
                    try {
                        String result = process_line(cmds[i], false);
                    } catch (EOFException ex) {
                        ex.printStackTrace();
                    }
                }
                long towait = 1000 * secs - (System.currentTimeMillis() - starttime);
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

        public nonperiodic_thread(Element ele, String name, String[] cmds) {
            super(name);
            this.ele = ele;
            this.name = name;
            this.cmds = cmds;
        }

        private void fix_cal(GregorianCalendar cal) {
            if (cal != null) {
                GregorianCalendar now = new GregorianCalendar();
                //System.out.println(cal.getTime() + "*****" + now.getTime() + now.before(cal) + now.after(cal));
                if (now.after(cal)) {
                    cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
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

        private GregorianCalendar evaluate_time_weekday(Element e, GregorianCalendar calendar) {
            if (e == null)
                return null;
            GregorianCalendar cal = (GregorianCalendar) calendar.clone();
            NodeList nl = e.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                    GregorianCalendar c = evaluate_time((Element) nl.item(i));
                    cal.set(GregorianCalendar.HOUR_OF_DAY, c.get(GregorianCalendar.HOUR_OF_DAY));
                    cal.set(GregorianCalendar.MINUTE, c.get(GregorianCalendar.MINUTE));
                    cal.set(GregorianCalendar.SECOND, c.get(GregorianCalendar.SECOND));
                    return cal;
                }
            }
            return null;
        }

        private GregorianCalendar evaluate_time(Element e) {
            GregorianCalendar cal = new GregorianCalendar();
            if (e.getTagName().equals("absolute-time")) {
                try {
                    cal.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(e.getAttribute("hour")));
                    cal.set(GregorianCalendar.MINUTE, Integer.parseInt(e.getAttribute("minute")));
                    cal.set(GregorianCalendar.SECOND, Integer.parseInt(e.getAttribute("second")));
                } catch (NumberFormatException expt) {
                    expt.printStackTrace();
                }

                fix_cal(cal);

            } else if (e.getTagName().equals("sunset") || e.getTagName().equals("sunrise")) {
                try {
                    double latitude = Double.parseDouble(e.getAttribute("latitude"));
                    double longitude = Double.parseDouble(e.getAttribute("longitude"));
                    double degrees = Double.parseDouble(e.getAttribute("degrees"));
                    String timezone = e.getAttribute("tz");
                    TimeZone tz = timezone.isEmpty() ? TimeZone.getDefault() : TimeZone.getTimeZone(timezone);
                    GregorianCalendar c;
                    if (e.getTagName().equals("sunrise")) {
                        c = SunriseSunset.sunrise(latitude, longitude, tz, cal, degrees);
                        if (cal.after(c)) {
                            cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
                            c = SunriseSunset.sunrise(latitude, longitude, tz, cal, degrees);
                        }
                    } else {
                        c = SunriseSunset.sunset(latitude, longitude, tz, cal, degrees);
                        if (cal.after(c)) {
                            cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
                            c = SunriseSunset.sunset(latitude, longitude, tz, cal, degrees);
                        }
                    }
                    cal = c;
                } catch (NumberFormatException expt) {
                    expt.printStackTrace();
                }
            } else if (e.getTagName().equals("sunrise")) {
                // FIXME
                cal.set(GregorianCalendar.HOUR_OF_DAY, 6);
                cal.set(GregorianCalendar.MINUTE, 42);
                cal.set(GregorianCalendar.SECOND, 0);
                fix_cal(cal);
            } else if (e.getTagName().equals("last-of")) {
                cal.set(2000, 1, 1);
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        GregorianCalendar c = evaluate_time((Element) nl.item(i));
                        if (cal.before(c))
                            cal = c;
                    }
                }
            } else if (e.getTagName().equals("first-of")) {
                cal.set(2999, 1, 1);
                NodeList nl = e.getChildNodes();
                for (int i = 0; i < nl.getLength(); i++) {
                    if (nl.item(i).getNodeType() == Node.ELEMENT_NODE) {
                        GregorianCalendar c = evaluate_time((Element) nl.item(i));
                        if (cal.after(c))
                            cal = c;
                    }
                }
            } else if (e.getTagName().equals("weekdays")) {
               NodeList nl = e.getElementsByTagName("weekday");
                Element today = get_weekday(cal.getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SHORT, Locale.US), nl);
                //System.err.println(cal.getTime());
                GregorianCalendar todays_candidate = evaluate_time_weekday(today, cal);
                //System.err.println(cal.getTime() + " " + todays_candidate.getTime() + "  " + cal.before(todays_candidate));
                if (todays_candidate != null && cal.before(todays_candidate)) {
                    cal = evaluate_time_weekday(today, cal);
                } else {
                    Element next_day = null;
                    for (int i = 1; next_day == null && i < 7; i++) {
                        cal.add(GregorianCalendar.DAY_OF_WEEK, 1);
                        next_day = get_weekday(cal.getDisplayName(GregorianCalendar.DAY_OF_WEEK, GregorianCalendar.SHORT, Locale.US), nl);
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
        public void run() {
            while (true) {
                //System.out.println("\"" + name + "\" ");
                GregorianCalendar next = evaluate_time(ele);
                Main.get_instance().timertable.put(name, next); // FIXME?
                long towait = next.getTimeInMillis() - System.currentTimeMillis();
                if (towait > 10) {
                long hours = towait / (long) 3600000;
                    long minutes = (towait - hours * 3600000) / (long) 60000;
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
                for (int i = 0; i < cmds.length; i++) {
                    // TODO: This should, at least optionally, to a logfile instead.
                    System.out.print("Thread \"" + name + "\" woke up, trying to execute "+ cmds[i] + ": ");
                    try {
                        String result = process_line(cmds[i], false);
                    } catch (EOFException ex) {
                        ex.printStackTrace();
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

    // VERY experimental
    private int udp_execute(int socketno, boolean use_python, boolean create_thread) {
        System.err.println("Trying to listen to UDP socket " + socketno + (use_python ? " (Using Python)" : ""));
        jython_engine jython = use_python ? new jython_engine(hm, false) : null;
        int success = harcutils.exit_success;
        if (create_thread)
            new udp_thread(socketno, jython).start();
        else
            success = udp_work(socketno, jython);
        return success;
    }

    private int udp_work(int socketno, jython_engine jython) {
        byte buf[] = new byte[1000];
        String commandline;
        boolean go_on = true;
        try {
            while (go_on) {
                DatagramSocket sock = new java.net.DatagramSocket(socketno);
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
                sock.close();
            }

        } catch (SocketException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return harcutils.exit_ioerror;
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            return harcutils.exit_ioerror;
        }
        return harcutils.exit_success;
    }

    private class udp_thread extends Thread {

        private int socket_no;
        private jython_engine jython;

        public udp_thread(int socket_no, jython_engine jython) {
            super("udp_thread");
            this.socket_no = socket_no;
            this.jython = jython;
        }

        @Override
        public void run() {
            udp_work(socket_no, jython);
        }
    }

    // This is very experimental and unfinished stuff. It does not give any sensible
    // information back to the client (most important for get_* commands).
    private int socket_execute(int socketno, boolean use_python) {
        System.err.println("Trying to listen to TCP socket " + socketno + (use_python ? " (Using Python)" : ""));
        ServerSocket srv_sock = null;
        try {
            srv_sock = new java.net.ServerSocket(socketno);
        } catch (IOException e) {
            System.out.println("Could not listen on port " + socketno);
            return harcutils.exit_ioerror;
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
            return harcutils.exit_ioerror;
        }

        try {
            srv_sock.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            return harcutils.exit_ioerror;
        }

        return harcutils.exit_success;
    }

    private class socket_thread extends Thread {

        private Socket sock = null;
        private jython_engine jython = null;

        public socket_thread(Socket sock, boolean use_python) {
            super("socket_thread");
            this.sock = sock;
            jython = use_python ? new jython_engine(hm, false) : null;
        }

        @Override
        public void run() {

            PrintStream out = null;
            BufferedReader in = null;
            try {
                out = new PrintStream(sock.getOutputStream());
                in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                out.println("HARC server @ your service!");
                boolean go_on = true;
                while (go_on) {
                    String commandline = in.readLine();
                    System.err.println(">>>" + (commandline));
                    if (commandline == null) {
                        go_on = false;
                        out.println("BYE");
                    } else if (commandline.equals("--die")) {
                        go_on = false;
                        spawn_new_socketthreads = false;
                        out.println("BYE");
                        System.err.println("die received");
                    } else if (commandline.equals("--quit")) {
                        go_on = false;
                        spawn_new_socketthreads = true;
                        out.println("BYE");
                        System.err.println("quit received");
                    } else {
                        String result = jython == null ? process_line(commandline, false) : jython.eval(commandline);
                        out.println(result != null ? "OK: " + result : "ERROR");
                    }
                }
                out.close();
                in.close();
                sock.close();
                socket_storage.dispose_sockets(true);
                if (!spawn_new_socketthreads) {
                    System.exit(harcutils.exit_ioerror);// ??
                }
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
    }

    public GregorianCalendar get_timer_next(String name) {
        return timertable != null ? timertable.get(name) : null;
    }
    
    public String get_timer_next_as_string(String name) {
        return timertable != null ? formatdate(timertable.get(name)) : null;
    }

    //public Enumeration<String> get_timer_names_e() {
    //    return timertable.keys();
    //}
    
    public String[] get_timer_names() {
        Enumeration<String> e = timertable.keys();
        String[] arr = new String[timertable.size()];
        for (int i = 0; e.hasMoreElements(); i++)
            arr[i++] = e.nextElement();
        return arr;
    }

    private int interactive_jython_execute() /*throws InterruptedException*/ {
        jython_engine interactive_jython = new jython_engine(hm, true);
        interactive_jython.interact();
        return harcutils.exit_success;
    }

    // Recognizes aliases, however, readline does not know them
    private int readline_execute() throws InterruptedException {
        // FIXME: should not die from control-c
        /*try {
        hm = new home(homefilename, verbose, debug, browser);
        engine = new macro_engine(macrofilename, hm, debug);
        } catch (IOException e) {
        System.err.println(e.getMessage());
        return harcutils.exit_config_read_error;
        } catch (SAXParseException e) {
        System.err.println(e.getMessage());
        return harcutils.exit_xml_error;
        }
         */

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

            public void run() {
                try {
                    Readline.writeHistoryFile(harcprops.get_instance().get_rl_historyfile());
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                Readline.cleanup();
                try {
                    socket_storage.dispose_sockets(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (db.misc())
                    System.err.println("*************** This is Readline shutdown **********");
            }
        });

        try {
            Readline.load(ReadlineLibrary.GnuReadline);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Warning: GNU readline not found.");
        }

        Readline.initReadline(harcprops.get_instance().get_appname());

        history = new File(harcprops.get_instance().get_rl_historyfile());

        // I become funny errors (UnsupportedEncodingException below) if historyfile
        // does not exist. Therefore create it if not there already.
        try {
            history.createNewFile();
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (history.exists())
                Readline.readHistoryFile(harcprops.get_instance().get_rl_historyfile());
        } catch (Exception e) {
            System.err.println("Could not read rl history " + e.getMessage());
        }

        try {
            Readline.setWordBreakCharacters(""/*" \t;"*/);
        } catch (UnsupportedEncodingException enc) {
            // FIXME
            System.err.println(enc.getMessage() + "Could not set word break characters");
            System.err.println("Try touching " + harcprops.get_instance().get_rl_historyfile());
            return harcutils.exit_this_cannot_happen;
        }

        Readline.setCompleter(new rl_completer(cl_commands, engine, hm));

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
                        String line = Readline.readline(harcprops.get_instance().get_rl_prompt(), false);
                        if (line != null && !line.isEmpty()) {
                            line = line.trim();
                            if (!Readline.getHistoryLine(Readline.getHistorySize()-1).equals(line))
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

        return harcutils.exit_success;
    }

    private class readline_thread extends Thread {

        public readline_thread() {
            super("readline_thread");
        }

        @Override
        public void run() {
            try {
                String line = Readline.readline(harcprops.get_instance().get_rl_prompt());
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
                if (db.decode_args()) {
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
                //System.exit(harcutils.exit_interrupted);
                }
            }
        } else if (line.startsWith("--")) {
            // Decode commands
            if (arguments[0].equals("--quit") || arguments[0].equals("--exit")) {
                result = null;
                throw new EOFException("As you requested");
            } else if (arguments[0].equals("--version")) {
                if (verbose)
                    System.out.println(harcutils.version_string);
                result = harcutils.version_string;
            } else if (arguments[0].equals("--help")) {
                if (verbose)
                    System.out.println(readline_help);
                result = readline_help;
            } else if (arguments[0].equals("--license")) {
                if (verbose)
                    System.out.println(harcutils.license_string);
                result = harcutils.license_string;
            } else if (arguments[0].equals("--verbose")) {
                /*boolean v = true;
                try {
                v = Integer.parseInt(arguments[1]);
                } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("+++ Argument missing, assuming 1.");
                } catch (NumberFormatException e) {
                System.out.println("+++ Parse error, assuming 1.");
                }*/
                hm.set_verbosity(true);
                result = "Verbosity set";
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
                result = "debug set to " + v;
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
                if (verbose)
                    System.out.println(bell + "*** Unrecognized command ***");
                result = null;
            }
        } else if (engine != null && engine.has_macro(arguments[0])) {
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
        } else if (hm.has_device(arguments[0])) {
            // TODO: not implemented: type, count, toggle, smart_memory
            if (db.decode_args())
                System.out.println("%%% Trying to execute `" + line + "'");

            String cmd_name = arguments[0];
            if (arguments.length < 2) {
                if (verbose)
                    System.out.println(bell + "*** command missing ***");
            } else {
                command_t cmd = alias_expander.canonicalize(arguments[1]);
                String[] aux_args = new String[arguments.length - 2];

                if (aux_args.length < hm.get_arguments(arguments[0], cmd, commandtype_t.any)) {
                    System.err.println(bell + "*** Too few arguments ***");
                } else {
                    //if (aux_args.length > hm.get_arguments(arguments[0], cmd, commandtype_t.any))
                    //    System.err.println(bell + "*** Excess arguments ignored ***");

                    for (int i = 0; i < arguments.length - 2; i++) {
                        aux_args[i] = arguments[i + 2];
                        if (db.decode_args()) {
                            System.err.println("Aux arg[" + i + "] = " + aux_args[i]);
                        }
                    }
                    // Heuristic: If exactly one argument is needed,
                    // and several given, tuck the given ones together.
                    // (e.g. for selecting TV channels with names containing spaces.)
                    if (hm.get_arguments(arguments[0], cmd, commandtype_t.any) == 1) {
                        if (db.decode_args())
                            System.err.println("Concatenating arguments");
                        aux_args[0] = harcutils.join(arguments, 2);
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
        jython_engine jython = new jython_engine(hm, false);
        for (int i = 0; i < noninteractive_args.length; i++)
            jython.exec(noninteractive_args[i]);

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
                return harcutils.exit_usage_error;
            }
            String dst_device = noninteractive_args[0];
            String src_device = noninteractive_args[1];
            System.err.println("select mode ...");
            if (db.decode_args()) {
                System.err.println("Select mode: devname = " + dst_device + ", src_device = " + src_device + " (connection_type = " + (connection_type == null ? "any" : connection_type) + ").");
            }
            if (src_device.equals("?")) {
                if (hm.has_device(dst_device)) {
                    harcutils.printtable("Valid inputs for " + dst_device + (zone != null ? (" in zone " + zone) : "") + ":",
                            hm.get_sources(dst_device, zone));
                } else {
                    System.err.println("No such device `" + dst_device + "'");
                    System.exit(harcutils.exit_nonexisting_device);
                }
            } else {
                try {
                    hm.select(dst_device, src_device, type, zone, the_mediatype, connection_type);
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
        } else if (engine != null && engine.has_macro(first_arg)) {
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
                if ((noninteractive_args.length < 2) || noninteractive_args[1].equals("?")) {
                    // No command given, list possible
                    if (db.decode_args()) {
                        System.out.println("Try to list possible device commands");
                    }
                    harcutils.printtable("Valid commands for " + first_arg + " of type " + type + ":",
                            hm.get_commands(first_arg, type));
                } else {
                    // Command expected
                    if (db.decode_args()) {
                        System.out.println("Try to execute as device command");
                    }

                    String cmd_name = noninteractive_args[1];
                    command_t cmd = alias_expander.canonicalize(cmd_name);
                    String output = null;
                    if (cmd == command_t.invalid) {
                        System.err.println("Command `" + cmd_name + "' does not exist.");
                    } else {
                        int no_args = noninteractive_args.length - 2;
                        String[] aux_args = new String[no_args];
                        for (int i = 0; i < no_args; i++) {
                            aux_args[i] = noninteractive_args[i + 2];
                            if (db.decode_args())
                                System.err.println("Aux arg[" + i + "] = " + aux_args[i]);
                        }

                        output = hm.do_command(first_arg, cmd, aux_args, type, count, toggle, smart_memory);
                    }
                    if (output == null) {
                        System.out.println("** Failure **");
                        return harcutils.exit_fatal_program_failure;
                    } else if (!output.equals("")) {
                        System.out.println(formatter.format(output));
                    }
                }
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        } else {
            System.err.println("`" + first_arg + "' is neither macro nor device");
            return harcutils.exit_nonexisting_device;
        }
        try {
            socket_storage.dispose_sockets(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return harcutils.exit_success;
    }
}
