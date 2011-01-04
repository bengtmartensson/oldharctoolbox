/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

package harc;

/**
 * Command not existing within an ir_code or remote.
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
