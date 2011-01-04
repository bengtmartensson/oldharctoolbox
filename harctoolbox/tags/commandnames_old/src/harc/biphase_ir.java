/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */

package harc;

public abstract class biphase_ir extends ir_code {
    protected biphase_ir_sequence intro_sequence;
    protected biphase_ir_sequence repeat_sequence;
    public biphase_ir(int frequency_code, int halfperiod_duration,
		      int intro_leadout_duration, int repeat_leadout_duration,
		      int intro[], int repeat[],
		      String command_name) {
	super(frequency_code, command_name);
	intro_sequence = new biphase_ir_sequence(halfperiod_duration, intro_leadout_duration, intro);
	repeat_sequence = new biphase_ir_sequence(halfperiod_duration, repeat_leadout_duration, repeat);
    }

    public biphase_ir(int freq_code, int halfperiod_duration,
		      int intro_leadout_duration, int repeat_leadout_duration,
		      String command_name) {
	super(freq_code, command_name);
	intro_sequence = new biphase_ir_sequence(halfperiod_duration,
						 intro_leadout_duration);
	repeat_sequence = new biphase_ir_sequence(halfperiod_duration,
						  repeat_leadout_duration);
    }

    public biphase_ir() {
	super();
     }

    protected int[] get_intro_array() {
	return intro_sequence.int_array();
    }

    public int[] get_repeat_array() {
	return repeat_sequence.int_array();
    }
}
