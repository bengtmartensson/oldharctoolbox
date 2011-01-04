/**
 *
 * @version 0.01
 * @author Bengt Martensson
 */

package harc;

public class commandset_entry {
    private int cmd;
    private short cmdno;
    private String name;
    private String transmit;
    private int response_lines;
    private String response_ending;
    private String expected_response;
    private String[] arguments;

    public String toString(commandset cmdset, boolean verbose) {
	String response = "";
	switch (cmdset.gettype()) {
	case commandset.ir:
	    response = "Infrared: " 
		+ ir_code.command_name(cmd)
		+ ", " + cmdno;
	    if (verbose)
		response = response
		    + ", " + cmdset.getremotename()
		    + ", " + cmdset.getprotocol()
		    + ", "+ cmdset.getdeviceno()
		    + ", "+ cmdset.getsubdevice()
		    + (cmdset.gettoggle() ? ", toggle, " : "")
		    ;
	    ;
	    break;
	case commandset.web_api:
	case commandset.serial:
	case commandset.tcp:
	case commandset.udp:
	    response = commandset.toString(cmdset.gettype()) + ": "
		+ ir_code.command_name(cmd) + ", '" + transmit + "'";
	    break;
	default:
	    response = commandset.toString(cmdset.gettype()) + ": " 
		+ ir_code.command_name(cmd);
	    break;
	}
	return response;
    }

    public String toString(int type) {
	String response = "";
	switch (type) {
	case commandset.ir:
	    response = "Infrared: " 
		+ ir_code.command_name(cmd)
		+ ", " + cmdno;
	    ;
	    break;
	case commandset.web_api:
	case commandset.serial:
	    response = "Serial: "
		+ ir_code.command_name(cmd) + ", '"
		+ transmit + "'";
	    break;
	case commandset.tcp:
	case commandset.udp:
	default:
	    response = commandset.toString(type) + ", " + ir_code.command_name(cmd);
	    break;
	}
	return response;
    }

    public String toString(String type) {
	return toString(commandset.toInt(type));
    }

    public int getcmd() {
	return cmd;
    }

    public short getcmdno() {
	return cmdno;
    }

    public String getname() {
	return name;
    }

    public String gettransmit() {
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

    private void setup(int cmd, short cmdno, String name, String transmit,
		       int response_lines, String response_ending, 
		       String expected_response,
		       String[] arguments) {
	this.cmd = cmd;
	this.cmdno = cmdno;
	this.name = name;
	this.transmit = transmit;
	this.response_lines = response_lines;
	this.response_ending = response_ending;
	this.expected_response = expected_response;
	this.arguments = arguments;
    }

    public commandset_entry(String cmdname, String cmdno, String name,
			    String transmit, String response_lines,
			    String response_end,
			    String expected_response, String[] arguments) {
	short cmdnumber = 
	    cmdno.equals("") ? -1 : 
	    (cmdno.length() > 2 && cmdno.charAt(0) == '0' && cmdno.charAt(1) == 'x') ? Short.parseShort(cmdno.substring(2),16) :
	    Short.parseShort(cmdno);
	setup(ir_code.decode_command(cmdname), cmdnumber, name, transmit,
	      response_lines.equals("") ? 0 : Integer.parseInt(response_lines),
	      response_end, expected_response, arguments);
    }
}
