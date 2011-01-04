/**
 *
 */
package harc;

import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

public class harcutils {

    public final static String license_string
            = "Copyright (C) 2009 Bengt Martensson. All rights reserved.\n"
            + "It is planned (but not finally decided) to release this software under the GPL. "
            + "However, the present version is unreleased and proprietary, and may not be distributed.";

    public final static int exit_success = 0;
    public final static int exit_usage_error = 1;
    public final static int exit_semantic_usage_error = 2;

    public final static int exit_fatal_program_failure = 3;
    public final static int exit_internal_failure = 4;
    public final static int exit_config_read_error = 5;
    public final static int exit_config_write_error = 6;
    public final static int exit_ioerror = 7;
    public final static int exit_no_bock = 8;
    public final static int exit_nonexisting_device = 9;
    public final static int exit_nonexisting_command = 10;
    public final static int exit_interrupted = 11;
    public final static int exit_xml_error = 12;
    public final static int exit_this_cannot_happen = 13;

    public final static int main_version = 0;
    public final static int sub_version = 0;
    public final static int subminor_version = 1;
    public final static String version_string = "Harc version " + main_version
            + "." + sub_version + "." + subminor_version;

    public final static String devicefile_extension = ".xml";

    //public final static int audio_video = 0;
    //public final static int video_only = 1;
    //public final static int audio_only = 2;


    /*
     public static int encode_mediatype(String str) {
        return str.equals("audio_only") ? audio_only
                : str.equals("video_only") ? video_only
                : audio_video;
    }
     */

    /**
     * Do not generate toggle codes.
     */
    //public static final int no_toggle = -1;
    /**
     * Generate toggle codes
     */
    //public static final int do_toggle = 2;
    /**
     * Generate the toggle code with toggle = 0.
     */
    //public static final int toggle_0 = 0;
    /**
     * Generate the toggle code with toggle = 1.
     */
    //public static final int toggle_1 = 1;

    public static toggletype decode_toggle(String t) {
        return
                t.equals("yes") ? toggletype.do_toggle
                : t.equals("0") ? toggletype.toggle_0
                : t.equals("1") ? toggletype.toggle_1
                : toggletype.no_toggle;
    }

    public static String format_toggle(toggletype toggle) {
        return
                toggle == toggletype.toggle_0 ? "0"
                : toggle == toggletype.toggle_1 ? "1"
                : toggle == toggletype.do_toggle ? "yes" : "no";
    }

  public static final int ping_timeout = 2000; // milliseconds

    public static Document open_xmlfile(String filename) throws IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(false);
        Document docu = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            docu = builder.parse(filename);
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            System.exit(exit_xml_error);
        // } catch (IOException e) {
        // 	    System.err.println(e.getMessage());
        // 	    System.exit(421);
        }
        return docu;
    }

    public static Document newDocument() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(false);
        Document doc = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            doc = builder.newDocument();
        } catch (ParserConfigurationException e) {
        }
        return doc;
    }

    // FIXME: should use Printstream or such
    public static void printDOM(String filename, Document doc,
            String doctype_systemid)
            throws FileNotFoundException {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype_systemid);
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            if (filename != null) {
                FileOutputStream fos = new FileOutputStream(filename);
                tr.transform(new DOMSource(doc), new StreamResult(fos));
                System.err.println("File " + filename + " written.");
            } else {
                tr.transform(new DOMSource(doc), new StreamResult(System.out));
            }
        } catch (TransformerConfigurationException e) {
        } catch (TransformerException e) {
        }
    }

    public static void printtable(String title, String[] arr, PrintStream str) {
        if (arr != null) {
            str.println(title);
            str.println();
            for (int i = 0; i < arr.length; i++) {
                str.println(arr[i]);
            }
        }
    }

    public static void printtable(String title, String[] arr) {
        printtable(title, arr, System.out);
    }

    // This is a naive implementation, however good enough for the present case.
    public static String[] sort_unique(String[] array) {
        java.util.Arrays.sort(array);
        int n = 0;
        for (int i = 0; i < array.length; i++)
            if (i == 0 || !array[i].equals(array[i-1]))
                n++;

        String[] result = new String[n];
        int pos = 0;
        for (int i = 0; i < array.length; i++)
            if (i == 0 || !array[i].equals(array[i-1]))
                result[pos++] = array[i];

        return result;
    }
}
