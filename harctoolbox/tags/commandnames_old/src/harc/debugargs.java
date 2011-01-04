package harc;

/**
 * This class encapsulates debugging arguments.
 *
 * @author Bengt Martensson
 */
public class debugargs {

    private int state;

    private boolean debug_aux(int n) {
        return ((state >> n) & 1) == 1;
    }

    // debug 1
    public boolean decode_args() {
        return debug_aux(0);
    }

    // debug 2
    public boolean dom() {
        return debug_aux(1);
    }

    // debug 4
    public boolean dispatch() {
        return debug_aux(2);
    }

    // debug 8
    public boolean transmit() {
        return debug_aux(3);
    }

    // debug 16
    public boolean nested_macros() {
        return debug_aux(4);
    }

    // debug 32
    public boolean trace_commands() {
        return debug_aux(5);
    }

    // debug 64
    public boolean misc() {
        return debug_aux(6);
    }

    // debug 128
    public boolean conds() {
        return debug_aux(7);
    }

    // debug 256
    public boolean verbose_execution() {
        return debug_aux(8);
    }

    // debug 512
    public boolean execute() {
        return debug_aux(9);
    }

    public String[] help() {
        String[] str = new String[]{
            "decode_args",
            "dom",
            "dispatch",
            "transmit",
            "nested_macros",
            "trace_commands",
            "misc",
            "conds",
            "verbose_execution",
            "execute"
        };
        return str;
    }

    public debugargs(int arg) {
        state = arg;
    }

    public void set_state(int arg) {
        state = arg;
    }

    public static void main(String[] args) {
        debugargs db = new debugargs(Integer.parseInt(args[0]));
        harcutils.printtable("things:", db.help());
        System.out.println(db.decode_args());
        System.out.println(db.execute());
    }

}
