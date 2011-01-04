/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class denon_k extends pwm_ir {

    // protocol={37k,432}<1,-1|1,-3>(8,-4,84:8,50:8,0:4,D:4,S:4,F:12,((D*16)^S^(F*16)^((F::4)%8)):8,1,-173)+
    public denon_k(int device, int subdevice, int command) {
        super(0x0070, new pulse_pair(0x0010, 0x0010),
                new pulse_pair(0x0010, 0x0030), "");
        repeat_sequence.setleadin(new pulse_pair(0x0080, 0x0040));
        repeat_sequence.setdata(array_lsb(84, 8));
        repeat_sequence.appenddata(array_lsb(50, 8));
        repeat_sequence.appenddata(array_lsb(0, 4));
        repeat_sequence.appenddata(array_lsb(device, 4));
        repeat_sequence.appenddata(array_lsb(subdevice, 4));
        repeat_sequence.appenddata(array_lsb(command, 12));
        //System.err.println(device*16);
        //System.err.println(subdevice + 16*(command%16));
        //System.err.println(command/16);
        // See page 6.
        int chksum = (device * 16) ^ (subdevice + 16 * (command % 16)) ^ (command / 16);
        //System.err.println(chksum);
        repeat_sequence.appenddata(array_lsb(chksum, 8));
        repeat_sequence.setleadout(new pulse_pair(0x0010, 0x0acd));
    }

    public static void main(String[] args) {
        int device = Integer.parseInt(args[0]);
        int subdevice = Integer.parseInt(args[1]);
        int command = Integer.parseInt(args[2]);
        denon_k d = new denon_k(device, subdevice, command);
        System.out.println(d.ccf_string());
    }
}
