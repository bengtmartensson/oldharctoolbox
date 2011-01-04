/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class biphase_ir_sequence extends modulated_ir_sequence {

    private int halfperiod_duration;
    private int leadout_duration;

    public biphase_ir_sequence(int halfperiod_duration, int leadout_duration,
            int data[],
            pulse_pair leadin, pulse_pair leadout) {
        this.halfperiod_duration = halfperiod_duration;
        this.data = data;
        this.leadin = leadin;
        this.leadout = leadout;
        this.leadout_duration = leadout_duration;
    }

    public biphase_ir_sequence(int halfperiod_duration, int leadout_duration, int data[]) {
        this(halfperiod_duration, leadout_duration, data,
                new pulse_pair(), new pulse_pair());
    }

    public biphase_ir_sequence(int halfperiod_duration, int leadout_duration) {
        this(halfperiod_duration, leadout_duration, new int[0],
                new pulse_pair(), new pulse_pair());
        data = new int[0];
    }

    @Override
    public int length() {
        int len = 0;
        int last;
        if (leadin.is_zero()) {
            last = 1;
            len = 0;
        } else {
            len = 3;
            last = 0;
        }

        if (data.length > 0) {
            for (int i = 0; i < data.length; i++) {
                len += last == data[i] ? 2 : 1;
                last = data[i];
            }
            if (last == 1) {
                len += 1;
            }

            len += 1;
        }
        if (!leadout.is_zero()) {
            len += 2;
        }

        return len / 2;
    }

    @Override
    public void setleadin(pulse_pair leadin) {
        this.leadin = leadin;
    }

    @Override
    public int[] int_array() {
        int result[] = new int[2 * length()];
        int i = 0;
        int last;
        if (leadin.is_zero()) {
            last = 1;
        } else {
            result[i++] = leadin.x();
            result[i++] = leadin.y();
            result[i++] = halfperiod_duration;
            last = 0;
        }

        if (data.length > 0) {
            //data[0] == 1 always!
            //s += ir_code.ccf_integer(halfperiod_duration) + " ";
            for (int j = 0; j < data.length; j++) {
                if (last == data[j]) {
                    result[i++] = halfperiod_duration;
                    result[i++] = halfperiod_duration;
                } else {
                    result[i++] = 2 * halfperiod_duration;
                }
                last = data[j];
            }
            if (last == 1) {
                result[i++] = halfperiod_duration;
            }

            result[i++] = leadout_duration > 0 ? leadout_duration : halfperiod_duration;
        }
        if (!leadout.is_zero()) {
            result[i++] = leadout.x();
            result[i++] = leadout.y();
        }
        if (i != result.length) {
            System.err.println("Internal error");
            System.exit(1);
        }

        return result;
    }
};
