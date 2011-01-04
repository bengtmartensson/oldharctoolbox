/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class denon extends pwm_ir {

    // protocol={38k,264}<1,-3|1,-7>(D:5,F:8,0:2,1,-165,D:5,~F:8,3:2,1,-165)+
    public denon(int device, int command) {
        super(0x006d, new pulse_pair(0x000a, 0x001e),
                new pulse_pair(0x000a, 0x0046),
                new pulse_pair(0x000a, 0x0677));
        repeat_sequence.setdata(array_lsb(device, 5));
        repeat_sequence.appenddata(array_lsb(command, 8));
        repeat_sequence.appenddata(array_lsb(0, 2));
        repeat_sequence.appendinterlude();
        repeat_sequence.appenddata(array_lsb(device, 5));
        repeat_sequence.appenddata(invert_bitarray(array_lsb(command, 8)));
        repeat_sequence.appenddata(array_lsb(3, 2));
        repeat_sequence.setleadout(new pulse_pair(0x000a, 0x0677));
    }

    public static void main(String[] args) {
        int device = Integer.parseInt(args[0]);
        int command = Integer.parseInt(args[1]);
        denon d = new denon(device, command);
        System.out.println(d.ccf_string());
    }
}
