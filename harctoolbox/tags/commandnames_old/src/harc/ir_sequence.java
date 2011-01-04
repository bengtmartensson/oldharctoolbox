/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

package harc;

public class ir_sequence {
    private pulse_pair[] raw_data;

    public int length () {
	return raw_data.length;
    }

    public boolean nonempty() {
	return raw_data.length > 0;
    }

    public void append(pulse_pair tail[]) {
	pulse_pair new_data[] = new pulse_pair[raw_data.length + tail.length];
	for (int i = 0; i < raw_data.length; i++)
	    new_data[i] = raw_data[i];
	for (int i = 0; i < tail.length; i++)
	    new_data[raw_data.length + i] = tail[i];
	raw_data = new_data;
    }

    public int[] int_array() {
	int result[] = new int[2*raw_data.length];
	for (int i = 0; i < raw_data.length; i++) {
	    result[2*i] = raw_data[i].x();
	    result[2*i+1] = raw_data[i].y();
	}
	return result;
    }

    public ir_sequence() {
	raw_data = new pulse_pair[0];
    }

    public ir_sequence(pulse_pair data[]) {
	raw_data = data;
    }

    public void setdata(pulse_pair data[]) {
	System.out.println("ir_sequence::setdata 1");
	raw_data = data;
    }

    public void setdata(int index, pulse_pair x) {
	raw_data[index] = x;
    }

    public void appenddata(pulse_pair data[]) {
	pulse_pair newdata[] = new pulse_pair[raw_data.length + data.length];
	for (int i = 0; i < raw_data.length; i++)
	    newdata[i] = raw_data[i];
	for (int i = 0; i < data.length; i++)
	    newdata[raw_data.length + i] = data[i];
	raw_data = newdata;
    }
};
