/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class nec1 extends pwm_ir {

    public nec1(int device, int subdevice, int command) {
        super(0x006d, new pulse_pair(0x0015, 0x0015),
                new pulse_pair(0x0015, 0x0040), "command_name");
        device_no = (short) device;
        command_no = (short) command;
        intro_sequence.setleadin(new pulse_pair(0x0157, 0x00ac));
        intro_sequence.setdata(array_lsb(device, 8));
        intro_sequence.appenddata(array_lsb(subdevice, 8));
        intro_sequence.appenddata(array_lsb(command, 8));
        intro_sequence.appenddata(invert_bitarray(array_lsb(command, 8)));
        intro_sequence.setleadout(new pulse_pair(0x0015, 0x0689));
        repeat_sequence.setleadin(new pulse_pair(0x0157, 0x0056));
        repeat_sequence.setleadout(new pulse_pair(0x0015, 0x0e94));
    }

    public nec1(int device, int command) {
        this(device, 255 - device, command);
    }

    public static void main(String[] args) {
        int device = Integer.parseInt(args[0]);
        int command = Integer.parseInt(args[1]);
        nec1 n = new nec1(device, command);
        System.out.println(n.ccf_string());
    }
}
