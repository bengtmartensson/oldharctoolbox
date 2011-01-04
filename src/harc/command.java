/**
 *
 * @version 0.01
 * @author Bengt Martensson
 */
package harc;

public class command {

    private command_t cmd;
    private short cmdno;
    private String name;
    private String transmit;
    private String prefix;
    private String suffix;
    private int response_lines;
    private String response_ending;
    private commandtype_t type;
    private String protocol_name;
    private short deviceno;
    private short subdevice;
    private boolean toggle;
    private String remotename;
    private String expected_response;
    private int delay_between_reps;
    private String[] arguments;
    private String ccf_toggle_0;
    private String ccf_toggle_1;

    @Override
    public String toString() {
        String response = "";
        switch (type) {
            case ir:
                response = "type = ir, name = " + cmd + ", remotename = " + remotename + ", protocol_name = " + protocol_name + ", deviceno = " + deviceno + ", subdevice = " + subdevice + ", cmdno = " + cmdno + ", " + (toggle ? "toggle" : "no toggle");
                ;
                break;
            case tcp:
            case udp:
            case web_api:
            case serial:
                response = "type = " + type + ", name = " + cmd + ", transmitstring = '" + get_transmitstring(false) + "', expected_response = '" + expected_response + "'" + ", response_lines = " + response_lines + ", response_ending = " + response_ending + ", delay_between_reps = " + delay_between_reps;
                for (int i = 0; i < arguments.length; i++) {
                    response = response + ", arguments[" + i + "]=" + arguments[i];
                }
                response = response + ".";
                break;
            default:
                response = type + ", " + cmd;
                break;
        }
        return response;
    }

    public int get_delay_between_reps() {
        return delay_between_reps;
    }

    public command_t getcmd() {
        return cmd;
    }

    public String[] get_arguments() {
        return arguments;
    }

    public boolean gettoggle() {
        return toggle;
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

//    public ir_code get_ir_code() {
//        return get_ir_code(false);
//    }

//    public ir_code get_ir_code(boolean verbose) {
//        return get_ir_code(toggletype.toggle_0, verbose);
//    }

    public ir_code get_ir_code(toggletype toggle, boolean verbose, short devno, short subdev) {
        ir_code ir = protocol.encode(protocol_name, devno, subdev, cmdno, toggle, verbose);
        // Fallback
        String ccf_string = toggle == toggletype.toggle_1 ? ccf_toggle_1 : ccf_toggle_0;
        if (ir == null && ccf_string != null) {
            if (verbose)
                System.err.println("No protocol code available, falling back to raw CCF code");
            ir = new ccf_parse(ccf_string);
        }

        return ir;
    }


     public ir_code get_ir_code(toggletype toggle, boolean verbose) {
        return get_ir_code(toggle, verbose, deviceno, subdevice);
    }



    public String get_remotename() {
        return remotename;
    }

    public String get_transmitstring(boolean expand_escapes) {
        String s = prefix + transmit + suffix;
        if (expand_escapes) // not really very general...
        {
            s = s.replaceAll("\\\\r", "\r").replaceAll("\\\\n", "\n");
        }
        return s;
    }

    public command(commandset cmdset, int index) {
        commandset_entry c = cmdset.get_entry(index);
        this.cmd = c.get_cmd();
        this.cmdno = c.get_cmdno();
        this.name = c.get_name();
        this.transmit = c.get_transmit();
        this.response_lines = c.get_response_lines();
        this.response_ending = c.get_response_ending();
        this.expected_response = c.get_expected_response();
        this.arguments = c.get_arguments();
        this.ccf_toggle_0 = c.get_ccf_toggle_0();
        this.ccf_toggle_1 = c.get_ccf_toggle_1();
        this.type = cmdset.get_type();
        this.remotename = cmdset.get_remotename();
        this.protocol_name = cmdset.get_protocol();
        this.deviceno = cmdset.get_deviceno();
        this.subdevice = cmdset.get_subdevice();
        this.toggle = cmdset.get_toggle();
        this.prefix = cmdset.get_prefix();
        this.suffix = cmdset.get_suffix();
        this.delay_between_reps = cmdset.get_delay_between_reps();
    }
}
