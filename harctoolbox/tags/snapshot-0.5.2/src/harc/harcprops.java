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

    private void update(String key, String value) {
        if (props.getProperty(key) == null) {
            props.setProperty(key, value);
            need_save = true;
        }
    }

    private void setup_defaults() {
        String harc_home = appendable("HARC_HOME");
        String home = appendable("HOME");

        update("home_conf",	harc_home + "config/home.xml");
        update("dtddir",	harc_home + "dtds");
        update("devicesdir",	harc_home + "devices");
        update("protocolsdir",	harc_home + "protocols");
        update("buttons_remotesdir", harc_home + "button_remotes");
        update("exportdir",	harc_home + "exports");
        update("aliasfilename",	harc_home + "src/harc/commandnames.xml");
        update("macrofilename",	harc_home + "config/macros.xml");
        update("browser",	"firefox");
        update("rl_historyfile", home + ".harc.rl");
        update("appname",	"harc");
        update("rl_prompt",	"harc> ");
        update("helpfilename" , harc_home + "docs/harchelp.html");
        update("resultformat",	"[%2$tY-%2$tm-%2$td %2$tk:%2$tM:%2$tS] >%1$s<");
        update("commandformat", "harc>%1$s");
        update("remotemaster_home", "/home/bengt/harc/jp1/remotemaster-1.89");
        update("rmdu_button_rules", harc_home + "config/button_rules.xml");
        // recognized are: "GnuReadline", "Editline", "Getline", "PureJava"
        //update("python.console.readlinelib", "GnuReadline");
        update("pythonlibdir", harc_home + "pythonlib");
        update("python.home", "/usr/local/jython");
        update("harcmacros", harc_home + "pythonlib" + File.separator + "harcinit.py");
    }

    public Properties get_props() {
        return props;
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
        setup_defaults();
    }

    public void save(String filename) throws IOException,FileNotFoundException {
        if (!need_save && filename.equals(this.filename))
            return;
        
        FileOutputStream f = new FileOutputStream(filename);

        if (use_xml) {
            props.storeToXML(f, "Harc Properties, feel free to hand edit if desired");
        } else {
            props.store(f, "Harc Properties, feel free to hand edit if desired");
        }
        need_save = false;
    }

    public void save() throws IOException {
        save(filename);
    }

    // For debugging
    private void list() {
        props.list(System.err);
    }

    public String get_homefilename() {
        return props.getProperty("home_conf");
    }

    public void set_homefilename(String s) {
        props.setProperty("home_conf", s);
        need_save = true;
    }

    public String get_dtddir() {
        return props.getProperty("dtddir");
    }

    public void set_dtddir(String s) {
        props.setProperty("dtddir", s);
        need_save = true;
    }

    public String get_devicesdir() {
        return props.getProperty("devicesdir");
    }

    public String get_protocolsdir() {
        return props.getProperty("protocolsdir");
    }

    public String get_buttons_remotesdir() {
        return props.getProperty("buttons_remotesdir");
    }

   public void set_devicesdir(String s) {
        props.setProperty("devicesdir", s);
        need_save = true;
    }

    public String get_macrofilename() {
        return props.getProperty("macrofilename");
    }

    public String get_aliasfilename() {
        return props.getProperty("aliasfilename");
    }

    public void set_aliasfilename(String s) {
        props.setProperty("aliasfilename", s);
        need_save = true;
    }

    public String get_exportdir() {
        return props.getProperty("exportdir");
    }

    public void set_exportdir(String dir) {
        props.setProperty("exportdir", dir);
        need_save = true;
    }

    public void set_macrofilename(String s) {
        props.setProperty("macrofilename", s);
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

    public String get_helpfilename() {
        return props.getProperty("helpfilename");
    }

    public String get_resultformat() {
        return props.getProperty("resultformat");
    }

    public String get_commandformat() {
        return props.getProperty("commandformat");
    }

    public String get_rmdu_button_rules() {
        return props.getProperty("rmdu_button_rules");
    }

    public void set_rmdu_button_rules(String s) {
        props.setProperty("rmdu_button_rules", s);
        need_save = true;
    }

    public String get_remotemaster_home() {
        return props.getProperty("remotemaster_home");
    }

    public void set_remotemaster_home(String s) {
        props.setProperty("remotemaster_home", s);
        need_save = true;
    }
    
    public String get_pythonlibdir() {
        return props.getProperty("pythonlibdir");
    }

    public void set_pythonlibdir(String s) {
        props.setProperty("pythonlibdir", s);
        need_save = true;
    }

    public String get_harcmacros() {
        return props.getProperty("harcmacros");
    }

    public void set_harcmacros(String s) {
        props.setProperty("harcmacros", s);
        need_save = true;
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
        String filename = args.length > 0 ? args[0] : "harcprops.xml";
        harcprops p = new harcprops(filename);
        p.list();
        try {
            p.save();
        } catch (IOException e) {
        }
    }
}
