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

import java.util.ArrayList;


/**
 *
 * @author bengt
 */
public class device_group {

    private String id;
    private String name;
    private String zone;
    private ArrayList<String> devices;

    device_group(String id, String name, String zone, ArrayList<String> devices) {
        this.id = id;
        this.name = name;
        this.zone = zone;
        this.devices = devices;
    }

    public ArrayList<String> get_devices() {
        return devices;
    }

    public String get_name() {
        return name;
    }
}
