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

import java.util.HashMap;

/**
 *
 */
public class Port {
    private int number;
    private int baud;
    private HashMap<command_t, CommandMapping> commandmappings;
    public Port(int number, int baud, HashMap<command_t, CommandMapping> commandmappings) {
        this.number = number;
        this.baud = baud;
        this.commandmappings = commandmappings;
    }

    public int get_number() {
        return number;
    }

    public CommandMapping get_commandmapping(command_t cmd) {
        return commandmappings.get(cmd);
    }
}
