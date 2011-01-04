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

package org.harctoolbox;

import java.io.*;

/**
 *
 */
public class extension_filter implements FilenameFilter {

    protected String extension;

    public extension_filter(String extension) {
        this.extension = extension;
    }

    public boolean accept(File directory, String name) {
        return name.toLowerCase().endsWith(extension.toLowerCase());
    }

    public static void main(String args[]) {
        if (args.length != 1) {
            System.err.println("usage: java Filter <pattern list>");
            return;
        }

        extension_filter nf = new extension_filter(args[0]);
        File dir = new File(".");
        String[] strs = dir.list(nf);
        for (int i = 0; i < strs.length; i++) {
            System.out.println(strs[i]);
        }
    }
}
