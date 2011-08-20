package org.harctoolbox;

import java.util.Vector;

/**
 *
 * @author bengt
 */
public class device_group {

    private String id;
    private String name;
    private String zone;
    private Vector<String> devices;

    device_group(String id, String name, String zone, Vector<String> devices) {
        this.id = id;
        this.name = name;
        this.zone = zone;
        this.devices = devices;
    }

    public Vector<String> get_devices() {
        return devices;
    }

    public String get_name() {
        return name;
    }
}
