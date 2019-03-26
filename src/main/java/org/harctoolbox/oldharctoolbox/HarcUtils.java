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

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class consists of a collection of useful static constants and functions.
 */
final public class HarcUtils {

    public final static String license_string
            = "Copyright (C) 2009, 2010, 2011, 2019 Bengt Martensson.\n\n"
            + "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.";

    public final static String homepage_url = "http://www.harctoolbox.org";

    public final static int main_version = 0;
    public final static int sub_version = 7;
    public final static int subminor_version = 0;
    public final static String version_string = "Harctoolbox version " + main_version
            + "." + sub_version + "." + subminor_version;

    public final static String devicefile_extension = ".xml";

    public static final int ping_timeout = 2000; // milliseconds
    private static final Logger logger = Logger.getLogger(HarcUtils.class.getName());

    public static void printtable(String title, String[] arr, PrintStream str) {
        if (arr != null) {
            str.println(title);
            str.println();
            for (String arr1 : arr) {
                str.println(arr1);
            }
        }
    }

    public static void printtable(String title, String[] arr) {
        printtable(title, arr, System.out);
    }

    // This is a naive implementation, however good enough for the present case.
    public static String[] sort_unique(String[] array) {
        if (array == null)
            return null;
        if (array.length == 0)
            return array;
        java.util.Arrays.sort(array);
        int n = 0;
        for (int i = 0; i < array.length; i++)
            if (i == 0 || !array[i].equals(array[i-1]))
                n++;

        String[] result = new String[n];
        int pos = 0;
        for (int i = 0; i < array.length; i++)
            if (i == 0 || !array[i].equals(array[i-1]))
                result[pos++] = array[i];

        return result;
    }

    public static String[] nonnulls(String[] array) {
        if (array == null)
            return null;
        if (array.length == 0)
            return array;

        int n = 0;
        for (String array1 : array)
            if (array1 != null && !array1.isEmpty())
                n++;

        String[] result = new String[n];
        int m = 0;
        for (String array1 : array)
            if (array1 != null && !array1.isEmpty())
                result[m++] = array1;

        return result;
    }

    public static void browse(URI uri) {
        if (! Desktop.isDesktopSupported()) {
            logger.severe("Desktop not supported");
            return;
        }
        if (uri == null || uri.toString().isEmpty()) {
            logger.severe("No URI.");
            return;
        }
        try {
            //if (HarcProps.get_instance().getVerbose())
                logger.log(Level.INFO, "Browsing URI \"{0}\"", uri.toString());
            Desktop.getDesktop().browse(uri);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Could not start browser using uri \"{0}\".", uri.toString());
        }
    }

    public static void browse(String url) {
        try {
            browse(new URI(url));
        } catch (URISyntaxException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

    private HarcUtils() {
    }
}
