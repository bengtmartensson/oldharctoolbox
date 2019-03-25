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

/**
 *
 */
public final class UserPrefs {
    private boolean verbose = false;
    private int debug = 0;
     // Can be annoying with unwanted and unexpected browsers popping up
    private boolean use_www_for_commands = false;
    // Browser is in properties
    //private String browser = "firefox";

    private String propsfilename;

    private static final UserPrefs the_instance = new UserPrefs();

    public static UserPrefs get_instance() {
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
