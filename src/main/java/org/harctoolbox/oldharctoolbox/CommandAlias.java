/*
Copyright (C) 2009-2011 Bengt Martensson.

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

package org.harctoolbox.oldharctoolbox;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import org.harctoolbox.ircore.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */

public final class CommandAlias {

    public static void main(String[] args) {
        CommandAlias ca = new CommandAlias(new File(args[0]));
        System.out.println(ca.canonicalize(args[1]));
    }

    private HashMap<String, command_t> aliastable = null;
    private boolean is_valid = false;

    public CommandAlias(File file) {
        if (file == null)
            return;

        aliastable = new HashMap<>(64);
        Document doc;
        try {
            doc = XmlUtils.openXmlFile(file);
        } catch (IOException | SAXException e) {
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
}
