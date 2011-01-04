/*
Copyright (C) 2009 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox;

import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * This class consists of a collection of useful static constants and functions.
 */
public class harcutils {

    public final static String license_string
            = "Copyright (C) 2009, 2010 Bengt Martensson.\n\n"
            + "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.\n\n"
            + "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.\n\n"
            + "You should have received a copy of the GNU General Public License along with this program. If not, see http://www.gnu.org/licenses/.";

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
    public final static int exit_dynamic_link_error = 13;
    public final static int exit_this_cannot_happen = 14;
    public final static int exit_restart = 99; // An invoking script is supposed to restart the program

    public final static int main_version = 0;
    public final static int sub_version = 5;
    public final static int subminor_version = 1;
    public final static String version_string = "Harc version " + main_version
            + "." + sub_version + "." + subminor_version;

    public final static String devicefile_extension = ".xml";
    public final static String protocolfile_extension = ".xml";

    public final static int portnumber_invalid = -1;

    public static int safe_parse_portnumber(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
        }

        return portnumber_invalid;
    }

    public static final int ping_timeout = 2000; // milliseconds

    public static Document open_xmlfile(File file) throws IOException, SAXParseException, SAXException {
        final String fname = file.getCanonicalPath();
        if (debugargs.dbg_open_files())
            System.err.println("Opening XML-File " + fname);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(true);
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(true);
        Document docu = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.ErrorHandler() {

                public void error(SAXParseException exception) throws SAXParseException {
                    //System.err.println("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage());
                    throw new SAXParseException("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage(), "", fname, exception.getLineNumber(), 0);
                }

                public void fatalError(SAXParseException exception) throws SAXParseException {
                    //System.err.println("Parse Fatal Error: " + exception.getMessage() + exception.getLineNumber());
                    throw new SAXParseException("Parse Error in file " + fname + ", line " + exception.getLineNumber() + ": " + exception.getMessage(), "", fname, exception.getLineNumber(), 0);
                }

                public void warning(SAXParseException exception) {
                    System.err.println("Parse Warning: " + exception.getMessage() + exception.getLineNumber());
                }
            });
            docu = builder.parse(file);
        } catch (ParserConfigurationException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            //System.err.println(e.getMessage());
            throw e;
            //System.exit(exit_xml_error);
        // } catch (IOException e) {
        // 	    System.err.println(e.getMessage());
        // 	    System.exit(421);
        }
        return docu;
    }

    public static Document open_xmlfile(String filename) throws IOException, SAXParseException, SAXException {
        return open_xmlfile(new File(filename));
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

    public static void printDOM(OutputStream ostr, Document doc, String doctype_systemid) {
        try {
            Transformer tr = TransformerFactory.newInstance().newTransformer();
            tr.setOutputProperty(OutputKeys.INDENT, "yes");
            tr.setOutputProperty(OutputKeys.METHOD, "xml");
            if (doctype_systemid != null)
                tr.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype_systemid);
            tr.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            //if (filename != null) {
                //FileOutputStream fos = new FileOutputStream(filename);
                tr.transform(new DOMSource(doc), new StreamResult(ostr));
                //System.err.println("File " + filename + " written.");
            //} else {
            //    tr.transform(new DOMSource(doc), new StreamResult(System.out));
            
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
    
    public static void printDOM(String filename, Document doc, String doctype_systemid)
            throws FileNotFoundException {
        printDOM(filename != null ? new FileOutputStream(filename) : System.out,
                doc, doctype_systemid);
        System.err.println("File " + filename + " written.");
    }

    public static void printDOM(String filename, Document doc)
            throws FileNotFoundException {
        printDOM(filename != null ? new FileOutputStream(filename) : System.out,
                doc, null);
        System.err.println("File " + filename + " written.");
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
        if (array == null)
            return null;
        if (array.length == 0)
            return array;
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

    public static String[] nonnulls(String[] array) {
        if (array == null)
            return null;
        if (array.length == 0)
            return array;

        int n = 0;
        for (int i = 0; i < array.length; i++)
            if (array[i] != null && !array[i].equals(""))
                n++;

        String[] result = new String[n];
        int m = 0;
        for (int i = 0; i < array.length; i++)
            if (array[i] != null && !array[i].equals(""))
                result[m++] = array[i];

        return result;
    }

    public static String join(String[] stuff, char separator, int start_index) {
        if (stuff == null || stuff.length < start_index + 1)
            return null;

        String result = stuff[start_index];
        for (int i = start_index + 1; i < stuff.length; i++)
            result = result + separator + stuff[i];

        return result;
    }

    public static String join(String[] stuff, int start_index) {
        return join(stuff, ' ', start_index);
    }

    public static String join(String[] stuff) {
        return join(stuff, 0);
    }
    
    public static String[] get_basenames(String dirname, String extension) {
        File dir = new File(dirname);
        if (!dir.isDirectory())
            return null;

        String[] files = dir.list(new extension_filter(extension));
        String[] result = new String[files.length];
        for (int i =0; i < files.length; i++)
            result[i] = files[i].substring(0, files[i].lastIndexOf(extension));
        return result;//String[] {"sjkdfld"};
    }

    @SuppressWarnings("empty-statement")
    public static int no_lines(String s) {
        LineNumberReader lnr = new LineNumberReader(new StringReader(s.trim()));
        try {
            while (lnr.readLine() != null)
                ;
        } catch (IOException ex) {
            ;
        }
        return lnr.getLineNumber();
    }

    public static void main(String[] args) {
        System.out.println(no_lines("foobar"));
        System.out.println(no_lines("\n foobar\n"));
        System.out.println(no_lines("foo\r\nbar"));
        System.out.println(no_lines("foo\nbar"));
        System.out.println(no_lines("foo\rbar"));
    }
}
