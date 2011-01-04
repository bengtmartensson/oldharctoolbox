/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public abstract class modulated_ir_sequence extends ir_sequence {

    protected int data[];
    protected pulse_pair leadin;
    protected pulse_pair leadout;
    protected pulse_pair leadbetween;
    protected int count = 1;
    protected boolean repeat_leadin = false;

    public modulated_ir_sequence() {
        data = new int[0];
        leadin = new pulse_pair();
        leadout = new pulse_pair();
        leadbetween = new pulse_pair();
    }

    public modulated_ir_sequence(int data[],
            pulse_pair leadin, pulse_pair leadout,
            pulse_pair leadbetween) {
        this.data = data;
        this.leadin = leadin;
        this.leadout = leadout;
        this.leadbetween = leadbetween;
    }

    @Override
    public boolean nonempty() {
        return data.length > 0;
    }

    public void setdata(int data[]) {
        this.data = data;
    }

    public void setdata(int index, int x) {
        data[index] = x;
    }

    public int getdata(int index) {
        return data[index];
    }

    public int getdatalength() {
        return data.length;
    }

    public int get_repetitiondelay() {
        return leadbetween.y();
    }

    public int length() {
        return count * data.length + (leadin.is_zero() ? 0 : 1) + (count - 1) * (leadbetween.is_zero() ? 0 : 1) + (leadout.is_zero() ? 0 : 1);
    }

    public void setleadout(pulse_pair leadout) {
        this.leadout = leadout;
    }

    public void setleadin(pulse_pair leadin) {
        this.leadin = leadin;
    }

    public void setleadbetween(pulse_pair leadbetween) {
        this.leadbetween = leadbetween;
    }

    public void appenddata(int data[]) {
        int newdata[] = new int[this.data.length + data.length];
        for (int i = 0; i < this.data.length; i++) {
            newdata[i] = this.data[i];
        }
        for (int i = 0; i < data.length; i++) {
            newdata[this.data.length + i] = data[i];
        }
        this.data = newdata;
    }

    public int get_parity(int lower, int upper) {
        int sum = 0;
        for (int i = lower; i <= upper; i++) {
            sum += data[i];
        }
        return sum % 2;
    }

    @Override
    public String toString() {
        return "";
    }

    public void setcount(int c, boolean repeat) {
        count = c;
        this.repeat_leadin = repeat;
    }

    public boolean has_leadin() {
        return !leadin.is_zero();
    }
};
