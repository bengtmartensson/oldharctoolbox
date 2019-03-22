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

import com.neuron.app.tonto.ActionIRCode;
import com.neuron.app.tonto.CCF;
import com.neuron.app.tonto.CCFAction;
import com.neuron.app.tonto.CCFButton;
import com.neuron.app.tonto.CCFChild;
import com.neuron.app.tonto.CCFDevice;
import com.neuron.app.tonto.CCFFrame;
import com.neuron.app.tonto.CCFIRCode;
import com.neuron.app.tonto.CCFPanel;
import com.neuron.app.tonto.ProntoModel;
import java.io.File;
import java.io.FileNotFoundException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class for importing Pronto CCF files of the first generation.
 */
public class ccf_import {

    private CCF ccf;
    private Document doc;
    private Element root;
    private String encoding = "UTF-8";

    private boolean scrutinize(CCFChild children[], Element the_panel) {
        boolean has_codes = false;
        for (int i = 0; i < children.length; i++) {
            CCFChild child = children[i];
            CCFButton button = child.getButton();
            if (button != null) {
                boolean has_content = false;
                Element the_code = doc.createElement("command");
                String n = button.getName();
                if (n == null)
                    n = "";
                the_code.setAttribute("cmdref", n);
                the_code.setAttribute("name", n);
                CCFAction action[] = button.getActions();
                if (action != null) {
                    int no_ccf = 0;
                    for (int j = 0; j < action.length; j++) {
                        if (action[j].getActionType() == CCFAction.ACT_IRCODE) {
                            has_content = true;
                            ActionIRCode code = (ActionIRCode) action[j];
                            CCFIRCode ir = code.getIRCode();
                            Element ccf_el = doc.createElement("ccf");
                            ccf_el.appendChild(doc.createTextNode(ir.getCode()));
                            the_code.appendChild(ccf_el);
                            no_ccf++;
                        }
                    }
                    if (no_ccf > 1)
                        System.err.println("Warning: " + no_ccf + " > 1 codes found in button " + n + ". XML file will not be valid.");
                }
                if (has_content) {
                    the_panel.appendChild(the_code);
                    has_codes = true;
                }
            } else {
                CCFFrame frame = child.getFrame();
                boolean has_sub_codes = scrutinize(frame.getChildren(), the_panel);
                has_codes = has_codes || has_sub_codes;
            }
        }
        return has_codes;
    }

    public Document get_doc() {
        return doc;
    }

    ccf_import(String filename, ProntoModel prontomodel) {
        ccf = new CCF(prontomodel);
        try {
            ccf.load(filename);
        } catch (java.io.IOException e) {
            System.err.println("Cannot open ccf");
        }
        doc = harcutils.newDocument();
        Element remotes = doc.createElement("devices");
        doc.appendChild(remotes);
        root = doc.getDocumentElement();

        for (CCFDevice dev = ccf.getFirstDevice(); dev != null; dev = dev.getNextDevice()) {
            Element the_remote = doc.createElement("device");
            the_remote.setAttribute("id", dev.getName());
            the_remote.setAttribute("name", dev.getName());
            the_remote.setAttribute("model", "unknown");
            for (CCFPanel panel = dev.getFirstPanel(); panel != null; panel = panel.getNextPanel()) {
                Element the_panel = doc.createElement("commandset");
                the_panel.setAttribute("name", panel.getName());
                boolean has_content = scrutinize(panel.getChildren(), the_panel);
                if (has_content)
                    the_remote.appendChild(the_panel);
            }
            root.appendChild(the_remote);
        }
    }

    public void dump(String filename) throws FileNotFoundException {
        harcutils.printDOM(filename, doc, harcprops.get_instance().get_dtddir() + File.separator + "devices.dtd");
    }

    public static void main(String args[]) {
        ccf_import ccfdmp = new ccf_import(args[0], ProntoModel.getModel(ProntoModel.RU890));
        try {
            harcutils.printDOM(/*harcprops.get_instance().get_export_dir() + File.separator +*/ "ccf_import.xml",
                    ccfdmp.get_doc(), harcprops.get_instance().get_dtddir() + File.separator + "devices.dtd");
        } catch (FileNotFoundException e) {
        }
    }
}
