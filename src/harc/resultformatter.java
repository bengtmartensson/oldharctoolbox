package harc;

import java.util.*;

/**
 *
 */
public class resultformatter {

    private String format;

    public resultformatter(String format) {
        this.format = format;
    }

    public resultformatter() {
        this(harcprops.get_instance().get_resultformat());
    }

    public String format(String str) {
        GregorianCalendar c = new GregorianCalendar();
        String s = "";
        try {
            s = String.format(format, str, c);
        } catch (/*UnknownFormatConversion*/Exception e) {
            System.err.println("Erroneous format string `" + format + "'.");
        }
        return s;
    }

    // Just for testing...
    public static void main(String[] args) {
        resultformatter formatter = new resultformatter(args[0]);
        System.out.println(formatter.format(args[1]));
    }
}
