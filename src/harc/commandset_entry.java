/**
 *
 * @version 0.01
 * @author Bengt Martensson
 */
package harc;

public class commandset_entry {

    private command_t cmd;
    private short cmdno;
    private String name;
    private String transmit;
    private int response_lines;
    private String response_ending;
    private String expected_response;
    private String[] arguments;
    private String ccf_toggle_0;
    private String ccf_toggle_1;

    public String toString(commandset cmdset, boolean verbose) {
        String response = "";
        switch (cmdset.get_type()) {
            case ir:
                response = "Infrared: " + cmd + ", " + cmdno;
                if (verbose)
                    response = response + ", " + cmdset.get_remotename() + ", " + cmdset.get_protocol() + ", " + cmdset.get_deviceno() + ", " + cmdset.get_subdevice() + (cmdset.get_toggle() ? ", toggle, " : "");
                ;
                break;
            case web_api:
            case serial:
            case tcp:
            case udp:
                response = cmdset.get_type() + ": " + cmd + ", '" + transmit + "'";
                break;
            default:
                response = cmdset.get_type() + ": " + cmd;
                break;
        }
        return response;
    }

    public String toString(commandtype_t type) {
        String response = "";
        switch (type) {
            case ir:
                response = "Infrared: " + cmd + ", " + cmdno;
                ;
                break;
            case web_api:
            case serial:
                response = "Serial: " + cmd + ", '" + transmit + "'";
                break;
            case tcp:
            case udp:
            default:
                response = type + ", " + cmd;
                break;
        }
        return response;
    }

    /*public String toString(String type) {
    return toString(commandset.toInt(type));
    }*/
    public command_t get_cmd() {
        return cmd;
    }

    public short get_cmdno() {
        return cmdno;
    }

    public String get_name() {
        return name;
    }

    public String get_transmit() {
        return transmit;
    }

    public int get_response_lines() {
        return response_lines;
    }

    public String get_response_ending() {
        return response_ending;
    }

    public String get_expected_response() {
        return expected_response;
    }

    public String[] get_arguments() {
        return arguments;
    }

    public String get_ccf_toggle_0() {
        return ccf_toggle_0;
    }

    public String get_ccf_toggle_1() {
        return ccf_toggle_1;
    }

    private void setup(command_t cmd, short cmdno, String name, String transmit,
            int response_lines, String response_ending,
            String expected_response,
            String[] arguments,
            String ccf_toggle_0, String ccf_toggle_1) {
        this.cmd = cmd;
        this.cmdno = cmdno;
        this.name = name;
        this.transmit = transmit;
        this.response_lines = response_lines;
        this.response_ending = response_ending;
        this.expected_response = expected_response;
        this.arguments = arguments;
        this.ccf_toggle_0 = ccf_toggle_0;
        this.ccf_toggle_1 = ccf_toggle_1;
    }

    public commandset_entry(String cmdname, String cmdno, String name,
            String transmit, String response_lines,
            String response_end,
            String expected_response, String[] arguments,
            String ccf_toggle_0, String ccf_toggle_1) {
        short cmdnumber =
                cmdno.equals("")
                ? -1
                : ((cmdno.length() > 2 && cmdno.startsWith("0x")) ? Short.parseShort(cmdno.substring(2), 16)
                : Short.parseShort(cmdno));

        setup(command_t.parse(cmdname), cmdnumber, name, transmit,
                response_lines.equals("") ? 0 : Integer.parseInt(response_lines),
                response_end, expected_response, arguments, ccf_toggle_0, ccf_toggle_1);
    }
}
