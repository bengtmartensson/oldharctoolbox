/*
Copyright (C) 2009-2011 Bengt Martensson.

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

import java.io.File;
import java.io.IOException;
import org.harctoolbox.irp.IrpUtils;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 *
 */
public class jython_engine {

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
                System.err.println("Exception from Python " + e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else
            return false;
    }

    // The correctness of the return value is unclear to me.
    // At least it takes True to "true", False to "false", None to null.
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
            //s = po.asString();
        } catch (Exception e) {
            //System.err.println("Buggers");
            s = Boolean.toString(po.__nonzero__());
        }
        return s;
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

    // TODO: should probably be more general
    public String[] get_argumentless_macros() {
        return getstuff("_harcfuncs_0_args_ok()", null);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        home hm = null;
        try {
            hm = new home(harcprops.get_instance().get_homefilename());
        } catch (SAXParseException ex) {
            System.err.println("XML problem in home file");
        } catch (SAXException ex) {
            System.err.println("XML problem in home file");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        if (hm == null)
            System.exit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        jython_engine jython = null;
        try {
            jython = new jython_engine(hm, args.length == 0);
        } catch (Exception e) {
            System.err.println("Could not create jython engine.");
            System.err.println(e.getMessage());
            System.exit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        }

        if (args.length > 0)
            if (args[0].equals("-a"))
                HarcUtils.printtable("Macros with no or all defaulted arguments are:", jython.get_argumentless_macros());
            else
                for (int i = 0; i < args.length; i++) {
                    jython.exec(args[i]);
                }
        else
           jython.interact();
    }
}
