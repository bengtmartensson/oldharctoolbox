package harc;

/**
 *
 */
public enum commandtype_t {

    /** Infrared command */
    ir,

    /** RF command with 433 MHz frequency (or US equivalents, 315?) */
    rf433,

    /** RF command with 868 MHz frequency (or US equivalents, 315?) */
    rf868,

    /** Invoking the browser */
    www,

    /** WEB API commands */
    web_api,

    /** Commands talking to TCP sockets */
    tcp,

    /** Commands talking to UDP sockets */
    udp,
    serial,
    bluetooth,

    /** Just on or off */
    on_off,

    /** "IP" commands, for now ping and WOL (wake-up-on-lan) */
    ip,

    /** Special commands, requiring some software support */
    special,

    /** Any of the types, exept for invalid */
    any,

    /** Denotes invalid selection */
    invalid;

    public static boolean is_valid(String s) {
        try {
            return commandtype_t.valueOf(s) != invalid;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static String valid_types(char sep) {
        commandtype_t[] vals = commandtype_t.values();
        String res = vals[0].toString();
        for (int i = 1; i < vals.length - 2; i++) {
            res = res + sep + vals[i];
        }
        return res;
    }
}
