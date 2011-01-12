package org.harctoolbox;

public enum connectiontype {

    analog,
    s_video,
    yuv,
    spdif, // No not distinguish between optical and coax here
    hdmi,
    cvbs,
    rgb,
    denon_link,
    external_analog,// 6 or 8 analog RCA plugs
    other,
    invalid,
    any;

    public static connectiontype parse(String s) {
        connectiontype ct = invalid;
        try {
            ct = valueOf(s);
        } catch (IllegalArgumentException e) {
        }
        return ct;
    }

    boolean is_ok(connectiontype requested) {
        return requested == null || requested == any || equals(requested);
    }
}
