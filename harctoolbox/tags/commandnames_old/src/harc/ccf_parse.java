/**
 *
 * @version 0.01 
 * @author Bengt Martensson
 */
package harc;

public class ccf_parse extends raw_ir {

    public ccf_parse(String ccf_code[], String command) {
        super(Integer.parseInt(ccf_code[1], 16), command);

        int i = 0;
        if (Integer.parseInt(ccf_code[i++], 16) != 0) {
            System.err.println("Can only handle ccf codes of type 0");
            System.exit(1);
        }
        int freq_code = Integer.parseInt(ccf_code[i++], 16);
        int intro_length = Integer.parseInt(ccf_code[i++], 16);
        int intro[] = new int[2 * intro_length];
        int repeat_length = Integer.parseInt(ccf_code[i++], 16);
        int repeat[] = new int[2 * repeat_length];

        for (int j = 0; j < 2 * intro_length; j++) {
            intro[j] = Integer.parseInt(ccf_code[i++], 16);
        }

        set_intro_sequence(intro);

        for (int j = 0; j < 2 * repeat_length; j++) {
            repeat[j] = Integer.parseInt(ccf_code[i++], 16);
        }

        set_repeat_sequence(repeat);
    }

    public ccf_parse(String ccf_code[]) {
        this(ccf_code, "unknown");
    }

    public ccf_parse(String code, String command) {
        this(code.split("[ \n\t]"), command);
    }

    public ccf_parse(String code) {
        this(code.split("[ \n\t]"), "unknown");
    }

    public static void main(String args[]) {
        remote.process_args("ccf_parse", args);
    }
}
