package harc;

import java.io.File;
import java.io.IOException;
import org.python.core.PyException;
//import org.python.core.PyInteger;
import org.python.core.PyObject;
//import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;
//import org.python.util.ReadlineConsole;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 * @author bengt
 */
public class jython_engine {

    private String jython_helpstring = "Useful commands are, e.g., <TODO>";
    private PythonInterpreter python;
    private int debug;
    private home hm;
    private jython_rl_completer completer;

    private String[] getstuff(String cmd, String prefix) {
        String[] s = null;
        try {
            s = (String[]) python.eval(cmd).__tojava__(Class.forName("[Ljava.lang.String;"));
        } catch (ClassNotFoundException ex) {
            // This cannot happen
        }
        if (prefix != null)
            for (int i = 0; i < s.length; i++) {
                s[i] = prefix + s[i];
            }
        return s;
    }

    public jython_engine(home hm, boolean interactive) throws PyException {
        this.hm = hm;
        if (interactive)
            completer = new jython_rl_completer(hm);
        PythonInterpreter.initialize(System.getProperties(), harcprops.get_instance().get_props(), new String[0]);
        python = interactive ?
            new harc_readline_jythonconsole(null, org.python.util.InteractiveConsole.CONSOLE_FILENAME, completer) :
            new PythonInterpreter();
        python.set("hm", hm);
        python.exec("import sys");

        String harcinit = harcprops.get_instance().get_harcmacros();
        python.exec("sys.path.append('" + harcprops.get_instance().get_pythonlibdir() + "')");
        try {
            if ((new File(harcinit)).exists())
                python.execfile(harcinit);
            else
                System.err.println("Python init file " + harcinit + " does not exist");

            if (interactive) {
                String[] stdcommands = getstuff("_stdcommands()", null);
                String[] funcs_0 = getstuff("_harcfuncs_n_args(0)", "harcmacros.");
                String[] funcs_1 = getstuff("_harcfuncs_n_args(1)", "harcmacros.");
                String[] funcs_2 = getstuff("_harcfuncs_n_args(2)", "harcmacros.");
                String[] funcs_2plus = getstuff("_harcfuncs_gt2_args()", "harcmacros.");
                String[] funcs_device = getstuff("_harcfuncs_device()", "harcmacros.");

                completer.set_commands(stdcommands, funcs_0, funcs_1, funcs_2, funcs_2plus, funcs_device);
            }
        } catch (PyException e) {
            System.err.println("Initializing Jython failed, probably error in " + harcinit + ".");
            //System.err.println(e.traceback.toString());
            //e.printStackTrace();
            throw (e);
        }
    }

    public boolean exec(String macro) {
        if (python !=  null) {
            try {
                python.exec(macro);
            } catch (Exception e) {
                System.err.println("fffffffffff " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else
            return false;
    }

    public String eval(String macro) {
        PyObject po = null;
        try {
            po = python.eval(macro);
        } catch (PyException pye) {
            System.err.println("Python error: ");
            pye.printStackTrace();
            return null;
        }
        String s;
        try {
            s = po.asStringOrNull();
        } catch (Exception e) {
            // FIXME
            //System.err.println("Buggers");
            //e.printStackTrace();
            s = po.__not__().asInt() == 0 ? "" : null; //??!!
        }

        return s;//po.asString();
    }

    public void set(String name, int value) {
        python.set(name, value);
    }

    public String get(String name) {
        return python.get(name).toString();
    }

    public void interact() {
        if (python.getClass() == harc_readline_jythonconsole.class)
            ((harc_readline_jythonconsole) python).interact();
        else
            System.err.println("Not interactive, returning...");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        home hm = null;
        try {
            hm = new home();
        } catch (SAXParseException ex) {
            System.err.println("XML problem in home file");
        } catch (SAXException ex) {
            System.err.println("XML problem in home file");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        if (hm == null)
            System.exit(harcutils.exit_config_read_error);
        jython_engine jython = null;
        try {
            jython = new jython_engine(hm, args.length == 0);
        } catch (Exception e) {
            System.err.println("Could not create jython engine.");
            System.err.println(e.getMessage());
            System.exit(harcutils.exit_config_read_error);
        }

        if (args.length > 0)
            for (int i = 0; i < args.length; i++) {
                jython.exec(args[i]);
            }
        else
            jython.interact();
    }
}
