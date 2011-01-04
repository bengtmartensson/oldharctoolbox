package harc;

import java.util.Hashtable;

/**
 *
 * @author bengt
 */
public class port {
    private int number;
    private int baud;
    private Hashtable<command_t, commandmapping> commandmappings;
    public port(int number, int baud, Hashtable<command_t, commandmapping> commandmappings) {
        this.number = number;
        this.baud = baud;
        this.commandmappings = commandmappings;
    }

    public int get_number() {
        return number;
    }

    public commandmapping get_commandmapping(command_t cmd) {
        return commandmappings.get(cmd);
    }
}
