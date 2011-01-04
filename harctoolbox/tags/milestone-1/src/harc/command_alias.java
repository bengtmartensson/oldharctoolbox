package harc;

/**
 *
 */

import java.util.*;
import java.io.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class command_alias {

    private Hashtable<String, command_t> aliastable = null;
    private boolean is_valid = false;

    public command_alias(String filename) {
        if (filename == null)
            return;

        aliastable = new Hashtable<String, command_t>();
        Document doc = null;
        try {
            doc = harcutils.open_xmlfile(filename);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            is_valid = false;
            return;
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
            is_valid = false;
            return;
        }
        NodeList cmds = doc.getElementsByTagName("command");
        for (int i = 0; i < cmds.getLength(); i++) {
            Element e = (Element) cmds.item(i);
            String name = e.getAttribute("name");
            if (!name.isEmpty())
                aliastable.put(name, command_t.valueOf(e.getAttribute("id")));
        }

        NodeList synonyms = doc.getElementsByTagName("synonym");
        for (int i = 0; i < synonyms.getLength(); i++) {
            Element e = (Element) synonyms.item(i);
            String name = e.getAttribute("name");
            aliastable.put(name, command_t.valueOf(((Element)e.getParentNode()).getAttribute("id")));
        }
        is_valid = true;
    }

    public command_t expand(String alias) {
        return is_valid 
                ? (aliastable.containsKey(alias) ? aliastable.get(alias) : command_t.invalid)
                : command_t.invalid;
    }

    public command_t canonicalize(String alias) {
        return command_t.is_valid(alias) ? command_t.valueOf(alias) : is_valid ? expand(alias) : command_t.invalid;
    }

    public static void main(String[] args) {
        command_alias ca = new command_alias(args[0]);
        System.out.println(ca.canonicalize(args[1]));
    }
}
