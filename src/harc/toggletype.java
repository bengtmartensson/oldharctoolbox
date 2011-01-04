package harc;

/**
 *
 */
public enum toggletype {

    /**
     * Generate the toggle code with toggle = 0.
     */
    toggle_0,
    /**
     * Generate the toggle code with toggle = 1.
     */
    toggle_1,
    /**
     * Do not generate toggle codes
     */
    no_toggle,
    /**
     * Generate toggle codes
     */
    do_toggle;
    
    public static toggletype flip(toggletype t) {
        return t == toggle_0 ? toggle_1 : toggle_0;
    }

    public static int toInt(toggletype t) {
        return t == toggle_1 ? 1 : 0;
    }

    public static toggletype decode_toggle(String t) {
        return t.equals("yes") ? toggletype.do_toggle
                : t.equals("0") ? toggletype.toggle_0
                : t.equals("1") ? toggletype.toggle_1
                : toggletype.no_toggle;
    }

    public static String format_toggle(toggletype toggle) {
        return toggle == toggletype.toggle_0 ? "0"
                : toggle == toggletype.toggle_1 ? "1"
                : toggle == toggletype.do_toggle ? "yes" : "no";
    }
}
