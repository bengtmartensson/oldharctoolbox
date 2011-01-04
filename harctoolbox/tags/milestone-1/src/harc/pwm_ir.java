/**
 *
 * @deprecated
 */

package harc;

public abstract class pwm_ir extends ir_code {

    protected pwm_ir_sequence intro_sequence;
    protected pwm_ir_sequence repeat_sequence;
    public static String lirc_flags = "SPACE_ENC";

    public pwm_ir(int freq_code, pulse_pair code0, pulse_pair code1,
            String command_name) {
        super(freq_code, command_name);
        intro_sequence = new pwm_ir_sequence(code0, code1);
        repeat_sequence = new pwm_ir_sequence(code0, code1);
    }

    public pwm_ir(int freq_code, pulse_pair code0, pulse_pair code1,
            pulse_pair interlude) {
        super(freq_code, "");
        intro_sequence = new pwm_ir_sequence(code0, code1, interlude);
        repeat_sequence = new pwm_ir_sequence(code0, code1, interlude);
    }

    public pwm_ir(int frequency_code, pulse_pair code0, pulse_pair code1,
            int intro[], int repeat[], String command_name) {
        super(frequency_code, command_name);
        intro_sequence = new pwm_ir_sequence(code0, code1, intro);
        repeat_sequence = new pwm_ir_sequence(code0, code1, repeat);
    }

    public pwm_ir() {
        super();
    }

    protected int[] get_intro_array() {
        return intro_sequence.int_array();
    }

    public int[] get_repeat_array() {
        return repeat_sequence.int_array();
    }
}
