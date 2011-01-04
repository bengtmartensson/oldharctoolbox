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

/**
 * Exception thrown when a not existing (within an ir_code or remote) is requested.
 */
public class non_existing_command_exception extends Exception {

    public non_existing_command_exception(String command) {
        super("Command " + command + " not found");
    }

    public non_existing_command_exception(String command, String device) {
        super("Command " + command + " not implemented in " + device);
    }

    public non_existing_command_exception(command_t command, String remote) {
        super("Command " + command + " not implemented in " + remote);
    }

    public non_existing_command_exception(command_t command) {
        super("Command " + command + " not implemented");
    }

    public non_existing_command_exception() {
        super();
    }
}
