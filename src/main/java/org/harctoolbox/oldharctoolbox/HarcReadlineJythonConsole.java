// Copyright (c) Corporation for National Research Initiatives
package org.harctoolbox.oldharctoolbox;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyObject;
import org.python.core.PySystemState;

/**
 * Uses: <a href="http://java-readline.sourceforge.net">Java Readline</a>
 *
 * Based on CPython-1.5.2's code module
 *
 */
public final class HarcReadlineJythonConsole extends org.python.util.InteractiveConsole {

    public static String getDefaultBanner() {
        //return String.format("%s, Jython %s on %s", harcutils.version_string, PySystemState.version, PySystemState.platform);
        return String.format("%s, Jython %s", Version.versionString, PySystemState.version);
    }

    //public String filename;
    private String history_pathname;
    private JythonRlCompleter completer = null;

    public HarcReadlineJythonConsole() {
        this(null, CONSOLE_FILENAME, null);
    }

    public HarcReadlineJythonConsole(PyObject locals) {
        this(locals, CONSOLE_FILENAME, null);
    }

    public HarcReadlineJythonConsole(PyObject locals, String filename, ReadlineCompleter completer) {
        super(locals, filename, true);

        history_pathname = Main.getProperties().getRlHistoryfile() + ".python";

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                try {
                    Readline.writeHistoryFile(history_pathname);
                } catch (IOException e) {
                    System.err.println(e.getMessage());
                }
                Readline.cleanup();
                //if (db.misc())
                //    System.err.println("*************** This is Readline shutdown **********");
            }
        });

        String backingLib = PySystemState.registry.getProperty("python.console.readlinelib",
                                                               "GnuReadline");
        try {
            Readline.load(ReadlineLibrary.byName(backingLib));
        } catch(RuntimeException e) {
            System.err.println("Warning: Could not load Readline backing lib " + backingLib);
            // Silently ignore errors during load of the native library.
            // Will use a pure java fallback.
        }
        Readline.initReadline("harc_jython");

        File history = new File(history_pathname);

        try {
            history.createNewFile();
        } catch (IOException e) {
            System.err.println(e);
        }
        try {
            if (history.exists())
                Readline.readHistoryFile(history_pathname);
        } catch (EOFException | UnsupportedEncodingException e) {
            System.err.println("Could not read rl history " + e.getMessage());
        }

        try {
            Readline.setWordBreakCharacters(""/* \t;"*/);
        } catch (UnsupportedEncodingException enc) {
            // FIXME
            System.err.println(enc.getMessage() + "Could not set word break characters");
            System.err.println("Try touching " + history_pathname);
            //return harcutils.exit_this_cannot_happen;
        }

        Readline.setCompleter(completer);
        //try {
            // Force rebind of tab to insert a tab instead of complete
        //    Readline.parseAndBind("tab: tab-insert");
        //}
        //catch (UnsupportedOperationException uoe) {
        // parseAndBind not supported by this readline
        //}

        //systemState.ps1 = new PyString("[pharc> "); // FIXME
        //systemState.ps2 = new PyString("....... ");
    }

    @Override
    public void interact() {
        interact(getDefaultBanner(), null);
    }

    @Override
    public void interact(String banner, PyObject file) {
        if(banner != null) {
            write(banner);
            write("\n");
        }
        // Dummy exec in order to speed up response on first command
        exec("2");
        // System.err.println("interp2");
        boolean more = false;
        while(true) {
            PyObject prompt = more ? systemState.ps2 : systemState.ps1;
            String line;
            try {
        	if (file == null)
        	    line = raw_input(prompt);
        	else
        	    line = raw_input(prompt, file);
            } catch(PyException exc) {
                if(!exc.match(Py.EOFError))
                    throw exc;
                write("\n");
                break;
            }
            if (!more && line.startsWith("!")) {
                try {
                    Process proc = Runtime.getRuntime().exec(line.substring(1));
                    BufferedReader out = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                    BufferedReader err = new BufferedReader(new InputStreamReader(proc.getErrorStream()));
                    String l;
                    while ((l = out.readLine()) != null)
                        System.out.println(l);
                    while ((l = err.readLine()) != null)
                        System.out.println(l);
                } catch (IOException ex) {
                    //ex.printStackTrace();
                    System.err.println(ex.getMessage());
                }
            } else
                more = push(line);
        }
    }

    /**
     * Write a prompt and read a line.
     *
     * The returned line does not include the trailing newline. When the user
     * enters the EOF key sequence, EOFError is raised.
     *
     * This subclass implements the functionality using JavaReadline.
     * @param prompt
     * @return
     */

    @Override
    public String raw_input(PyObject prompt) {
        try {
            String line = Readline.readline(prompt == null ? "" : prompt.toString(), false);
            if (line != null && !line.isEmpty()) {
                line = line.replaceFirst("[ \t]+$", "");
                int history_size = Readline.getHistorySize();
                if (history_size < 1 || !Readline.getHistoryLine(Readline.getHistorySize()-1).equals(line))
                    Readline.addToHistory(line);

                //  There must be a cleaner way of doing this...
                if (line.startsWith("quit()"))
                    throw new PyException(Py.EOFError);
            }
            return (line == null ? "" : line);
        } catch(EOFException eofe) {
            throw new PyException(Py.EOFError);
        } catch(IOException ioe) {
            throw new PyException(Py.IOError);
        }
    }
}
