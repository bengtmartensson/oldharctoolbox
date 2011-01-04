/**
 *
 * @version 0.01
 * @author Bengt Martensson
 */
package harc;

public class command {

    private int cmd;
    private short cmdno;
    private String name;
    private String transmit;
    private String prefix;
    private String suffix;
    private int response_lines;
    private String response_ending;
    private int type;
    private String protocol_name;
    private short deviceno;
    private short subdevice;
    private boolean toggle;
    private String remotename;
    private String expected_response;
    private int delay_between_reps;
    private String[] arguments;

    @Override
    public String toString() {
        String response = "";
        switch (type) {
            case commandset.ir:
                response = "type = ir, name = " + ir_code.command_name(cmd) + ", remotename = " + remotename + ", protocol_name = " + protocol_name + ", deviceno = " + deviceno + ", subdevice = " + subdevice + ", cmdno = " + cmdno + ", " + (toggle ? "toggle" : "no toggle");
                ;
                break;
            case commandset.tcp:
            case commandset.udp:
            case commandset.web_api:
            case commandset.serial:
                response = "type = " + commandset.toString(type) + ", name = " + ir_code.command_name(cmd) + ", transmitstring = '" + get_transmitstring(false) + "', expected_response = '" + expected_response + "'" + ", response_lines = " + response_lines + ", response_ending = " + response_ending + ", delay_between_reps = " + delay_between_reps;
                for (int i = 0; i < arguments.length; i++) {
                    response = response + ", arguments[" + i + "]=" + arguments[i];
                }
                response = response + ".";
                break;
            default:
                response = commandset.toString(type) + ", " + ir_code.command_name(cmd);
                break;
        }
        return response;
    }

    public int get_delay_between_reps() {
        return delay_between_reps;
    }

    public int getcmd() {
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

    public ir_code get_ir_code() {
        return get_ir_code(0);
    }

    public ir_code get_ir_code(int itoggle) {
        ir_code ir = protocol.encode(protocol_name, deviceno, subdevice, cmdno, itoggle);
        //String cmdname = ir_code.command_name(cmdno);

        return ir;
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
        commandset_entry c = cmdset.getentry(index);
        this.cmd = c.getcmd();
        this.cmdno = c.getcmdno();
        this.name = c.getname();
        this.transmit = c.gettransmit();
        this.response_lines = c.get_response_lines();
        this.response_ending = c.get_response_ending();
        this.expected_response = c.get_expected_response();
        this.arguments = c.get_arguments();
        this.type = cmdset.gettype();
        this.remotename = cmdset.getremotename();
        this.protocol_name = cmdset.getprotocol();
        this.deviceno = cmdset.getdeviceno();
        this.subdevice = cmdset.getsubdevice();
        this.toggle = cmdset.gettoggle();
        this.prefix = cmdset.getprefix();
        this.suffix = cmdset.getsuffix();
        this.delay_between_reps = cmdset.get_delay_between_reps();
    }
}
