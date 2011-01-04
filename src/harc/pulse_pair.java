/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

package harc;

public class pulse_pair {
    private int first;
    private int second;

    public pulse_pair(int x, int y) {
	first = x;
	second = y;
    }
    public pulse_pair() {
	first = 0;
	second = 0;
    }

    public boolean is_zero() {
	return (first == 0) && (second == 0); 
    }

    public int x() {
	return first;
    }

    public int y() {
	return second;
    }

    public String toString() {
	return Integer.toString(first) + " " + Integer.toString(second);
    }

    public String ccf_string(double unit) {
	return ir_code.ccf_integer((int)((double)first * unit)) + " " + ir_code.ccf_integer((int)((double)second * unit));
    }
}
