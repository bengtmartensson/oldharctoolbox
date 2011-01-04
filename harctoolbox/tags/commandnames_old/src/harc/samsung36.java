/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class samsung36 extends pwm_ir {

    // Device=32.0
    // define E=1
    // Function=0..255
    // Frequency=38000
    // One=498,-1498
    // Zero=498,-498
    // Form=;4488,-4492,d:8,s:8,498,-4498,e:4,f:8,-68,~f:8,498,-59154
    // Note: we use function/command with the other bit direction than in .irp.
    public samsung36(int device, int command) {
        super(0x006d, new pulse_pair(0x0013, 0x0013),
                new pulse_pair(0x0013, 0x0039), new pulse_pair(0x0013, 0x00ab));
        repeat_sequence.setleadin(new pulse_pair(0x00ab, 0x00aa));
        repeat_sequence.setdata(array_lsb(device, 8));
        repeat_sequence.appenddata(array_lsb(0, 8));
        repeat_sequence.appendinterlude();
        repeat_sequence.appenddata(array_lsb(7, 4));
        repeat_sequence.appenddata(array_msb(command, 8));
        repeat_sequence.appenddata(invert_bitarray(array_msb(command, 8)));
        repeat_sequence.setleadout(new pulse_pair(0x0013, 0x0900));
    }

    public static void main(String[] args) {
        int device = Integer.parseInt(args[0]);
        int subdevice = Integer.parseInt(args[1]);
        int command = Integer.parseInt(args[2]);
        denon_k d = new denon_k(device, subdevice, command);
        System.out.println(d.ccf_string());
    }
}
