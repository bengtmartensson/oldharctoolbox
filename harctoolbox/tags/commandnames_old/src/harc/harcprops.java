package harc;

/**
 *
 */

// FIXME:
// This code has the problem that if a property is not found, null is returned
// instead of a sensible default, or a warning.
// Rewrite the get* set* function to call a helper function, possibly with default value.

import java.util.Properties;
import java.io.*;

public class harcprops {

    private Properties props;
    private String filename;
    private final static boolean use_xml = true;
    private boolean need_save;

    private String appendable(String env) {
        String str = System.getenv(env);
        return str == null ? "" : str.endsWith(File.separator) ? str : (str + File.separator);
    }

    private void setup_defaults() {
        String harc_home = appendable("HARC_HOME");
        String home = appendable("HOME");

        props.setProperty("home_conf", harc_home + "config/home.xml");
        props.setProperty("dtd_dir", harc_home + "dtds");
        props.setProperty("devices_dir", harc_home + "devices");
        props.setProperty("macro_file", harc_home + "config/mymacs.xml");
        props.setProperty("browser", "firefox");
        props.setProperty("rl_historyfile", home + ".harc.rl");
        props.setProperty("appname", "harc");
        props.setProperty("rl_prompt", "harc> ");
        props.setProperty("helpfile" , harc_home + "docs/harchelp.html");
        props.setProperty("resultformat", "[%2$tY-%2$tm-%2$td %2$tk:%2$tM:%2$tS] >%1$s<");
        props.setProperty("commandformat", "harc>%1$s");
    }

    public harcprops(String filename) {
        this.filename = filename;
        need_save = false;
        props = new Properties();
        FileInputStream f;
        try {
            f = new FileInputStream(filename);
            if (use_xml)
                props.loadFromXML(f);
            else
                props.load(f);
        } catch (FileNotFoundException e) {
            System.err.println("Property File " + filename + " not found, using builtin defaults.");
            f = null;
            setup_defaults();
            need_save = true;
        } catch (IOException e) {
            System.err.println("Property File " + filename + " could not be read, using builtin defaults.");
            f = null;
            setup_defaults();
            need_save = true;
        }

    }

    public void save() throws IOException,FileNotFoundException {
        if (!need_save)
            return;
        
        FileOutputStream f = null;

        f = new FileOutputStream(filename);

        if (use_xml) {
            props.storeToXML(f, "Harc Properties, feel free to hand edit if desired");
        } else {
            props.store(f, "Harc Properties, feel free to hand edit if desired");
        }
        need_save = false;
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

    public String get_home_file() {
        return props.getProperty("home_conf");
    }

    public void set_home_file(String s) {
        props.setProperty("home_conf", s);
        need_save = true;
    }

    public String get_dtd_dir() {
        return props.getProperty("dtd_dir");
    }

    public void set_dtd_dir(String s) {
        props.setProperty("dtd_dir", s);
        need_save = true;
    }

    public String get_devices_dir() {
        return props.getProperty("devices_dir");
    }

    public void set_gevices_dir(String s) {
        props.setProperty("devices_dir", s);
        need_save = true;
    }

    public String get_macro_file() {
        return props.getProperty("macro_file");
    }

    public void set_macro_file(String s) {
        props.setProperty("macro_file", s);
        need_save = true;
    }

    public String get_browser() {
        return props.getProperty("browser");
    }

    public void set_browser(String s) {
        props.setProperty("browser", s);
        need_save = true;
    }

    public String get_rl_historyfile() {
        return props.getProperty("rl_historyfile");
    }

    public String get_appname() {
        return props.getProperty("appname");
    }

    public String get_rl_prompt() {
        return props.getProperty("rl_prompt");
    }

    public String get_helpfile() {
        return props.getProperty("helpfile");
    }

    public String get_resultformat() {
        return props.getProperty("resultformat");
    }

    public String get_commandformat() {
        return props.getProperty("commandformat");
    }

   private static harcprops instance = null;

    public static void initialize(String filename) {
        if (instance == null)
            instance = new harcprops(filename);
    }

    public static void initialize() {
        initialize("harc.properties.xml");
    }

    public static void finish() throws IOException {
        instance.save();
    }

    public static harcprops get_instance() {
        initialize();
        return instance;
    }

    public static void main(String[] args) {
        String filename = args.length > 0 ? args[0] : "harc.properties";
        harcprops p = new harcprops(filename);
        p.list();
        try {
            p.save();
        } catch (IOException e) {
        }
    }
}
