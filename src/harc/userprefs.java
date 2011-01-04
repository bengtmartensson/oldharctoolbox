package harc;

/**
 *
 * @author bengt
 */
public class userprefs {
    private boolean verbose = false;
    private int debug = 0;
     // Can be annoying with unwanted and unexpected browsers popping up
    private boolean use_www_for_commands = false;
    // Browser is in properties
    //private String browser = "firefox";

    private String propsfilename;
    
    private static userprefs the_instance = new userprefs();
    
    public static userprefs get_instance() {
        return the_instance;
    }
    
    public String get_propsfilename() {
       return propsfilename;
    }
    
    public int get_debug() {
        return debug;
    }
    
    public boolean get_verbose() {
        return verbose;
    }

    public boolean get_use_www_for_commands() {
        return use_www_for_commands;
    }

    public void set_propsfilename(String propsfilename) {
	this.propsfilename = propsfilename;
    }

    public void set_debug(int debug) {
	this.debug = debug;
    }

    public void set_verbose(boolean verbose) {
	this.verbose = verbose;
    }

    public void set_use_www_for_commands(boolean u) {
	this.use_www_for_commands = u;
    }
}
