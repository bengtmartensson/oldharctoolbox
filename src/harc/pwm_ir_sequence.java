/**
 *
 * @deprecated
 */

package harc;

public class pwm_ir_sequence extends modulated_ir_sequence {
    private pulse_pair code0;
    private pulse_pair code1;
    private pulse_pair interlude;
    protected static final int is_interlude = 2;

    public pwm_ir_sequence(pulse_pair code0, pulse_pair code1,
			   pulse_pair leadin, pulse_pair leadout,
			   pulse_pair leadbetween, pulse_pair interlude, int data[]) {
	super(data, leadin, leadout, leadbetween);
	this.code0 = code0;
	this.code1 = code1;
	this.interlude = interlude;
    }

    public pwm_ir_sequence(pulse_pair code0, pulse_pair code1, int data[]) {
	this(code0, code1, new pulse_pair(), new pulse_pair(), new pulse_pair(), new pulse_pair(), data);
    }

    public pwm_ir_sequence(pulse_pair code0, pulse_pair code1) {
	this(code0, code1, new pulse_pair(), new pulse_pair(), new pulse_pair(), new pulse_pair(), new int[0]);
    }
    
    public pwm_ir_sequence(pulse_pair code0, pulse_pair code1, pulse_pair interlude) {
	this(code0, code1, new pulse_pair(), new pulse_pair(), new pulse_pair(), interlude, new int[0]);
    }
    
    public void appendinterlude() {
	appenddata(new int[]{is_interlude});
    }

    public boolean nonempty() {
	return (data.length > 0) || ! leadin.is_zero() || ! leadout.is_zero() || leadbetween.is_zero();
    }

    public int[] int_array() {
	int len = (repeat_leadin ? count : 1) * (leadin.is_zero() ? 0 : 2) 
	    + 2*count*data.length
	    + (count-1)*(leadbetween.is_zero() ? 0 : 2)
	    + (leadout.is_zero() ? 0 : 2);
	int [] result = new int[len];
	int i = 0;
	for (int cnt = 1; cnt <= count; cnt++) {
	    if ((cnt == 1 || repeat_leadin) && ! leadin.is_zero()) {
		result[i++] = leadin.x();
		result[i++] = leadin.y();
	    }
	    for (int j = 0; j < data.length; j++) {
		pulse_pair pp = data[j] == is_interlude ? interlude : data[j] == 1 ? code1 : code0;
		result[i++] = pp.x();
		result[i++] = pp.y();
	    }
 	    if (cnt < count) {
		if (! leadbetween.is_zero()) {
		    result[i++] = leadbetween.x();
		    result[i++] = leadbetween.y();
		}
	    } else {
		if (! leadout.is_zero()) {
		    result[i++] = leadout.x();
		    result[i++] = leadout.y();
		}
	    }
	}
	
	return result;
    }
};
