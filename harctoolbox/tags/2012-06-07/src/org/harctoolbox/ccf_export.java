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

import org.harctoolbox.IrpMaster.IncompatibleArgumentException;
import org.harctoolbox.IrpMaster.IrSignal;
import com.neuron.app.tonto.ActionIRCode;
import com.neuron.app.tonto.CCF;
import com.neuron.app.tonto.CCFButton;
import com.neuron.app.tonto.CCFColor;
import com.neuron.app.tonto.CCFDevice;
import com.neuron.app.tonto.CCFFont;
import com.neuron.app.tonto.CCFIRCode;
import com.neuron.app.tonto.CCFIconSet;
import com.neuron.app.tonto.CCFNode;
import com.neuron.app.tonto.CCFPanel;
import com.neuron.app.tonto.ProntoModel;
import java.awt.Point;
import java.awt.Dimension;
import java.io.IOException;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Class for exporting one or more devices into a Proto CCF file of the first generation.
 * This uses the Tonto library by Stewart Allen, see http://www.giantlaser.com.
 */

public class ccf_export {
    private static final int default_button_width = 60;
    private static final int default_button_height = 20;
    private static final int button_label_length = 100;
    private static final int default_screenwidth = 240;
    private static final int default_screenheight = 220;
    
    CCF ccf = null;
    ProntoModel pronto_model;

    public ccf_export(String[] devices, boolean generate_both_toggles, toggletype toggle, ProntoModel pronto_model,
            boolean raw, int button_width, int button_height, int screenwidth, int screenheight,
            int used_screenwidth, int used_screenheight) {
        this.pronto_model = pronto_model;
        if (pronto_model.getModel() != ProntoModel.CUSTOM) {
            Dimension screensize = pronto_model.getScreenSize();
            screenwidth = pronto_model.getScreenSize().width;
            screenheight = pronto_model.getScreenSize().width;
        }
        int rows = used_screenheight / button_height;
        int columns = used_screenwidth / button_width;
        int v_rest = used_screenheight % button_height;
        int h_rest = used_screenwidth % button_width;
        ccf = new CCF(pronto_model);
        if (pronto_model.getModel() == ProntoModel.CUSTOM)
            ccf.setScreenSize(screenwidth, screenheight);
        ccf.setVersionString("testccf");
        //ccf.conformTo(ProntoModel.getModel(pronto_model));

        for (int d = 0; d < devices.length; d++) {
            device dvc = null;
            try {
                dvc = new device(devices[d]);
            } catch (IOException e) {
                System.err.println(e.getMessage());
                continue;
            } catch (SAXParseException e) {
                System.err.println(e.getMessage());
                continue;
            } catch (SAXException e) {
                System.err.println(e.getMessage());
                continue;
            }

            String[] remotes = dvc.get_remotenames();
            for (int r = 0; r < remotes.length; r++) {
                String remote_name = remotes[r];
                command_t[] cmds = dvc.get_commands(commandtype_t.ir, remote_name);

                CCFDevice dev = ccf.createDevice(remote_name);
                ccf.appendDevice(dev);

                int no_buttons = (generate_both_toggles ? 2 : 1) * cmds.length;
                for (int panel_no = 0; panel_no < (int) (((double) no_buttons) / (rows * columns) + 0.999); panel_no++) {
                    CCFPanel panel = dev.createPanel(remote_name + "_" + "codes_" + (panel_no + 1));
                    dev.addPanel(panel);

                    for (int x = 0; x < columns; x++) {
                        for (int y = 0; y < rows; y++) {
                            int index;
                            toggletype act_toggle = toggle;
                            if (generate_both_toggles) {
                                index = (panel_no * rows * columns + x + columns * y) / 2;
                                act_toggle = (panel_no * rows * columns + x + columns * y) % 2 == 1 ? toggletype.toggle_1 : toggletype.toggle_0;
                            } else
                                index = panel_no * rows * columns + x + columns * y;

                            if (index < cmds.length) {
                                String buttonname = cmds[index] +
                                        (generate_both_toggles ? ("_" + act_toggle) : "");
                                if (buttonname.length() > button_label_length)
                                    buttonname = buttonname.substring(0, button_label_length);
                                CCFButton b1 = panel.createButton(buttonname);
                                b1.setFont(CCFFont.SIZE_8);
                                b1.setTextAlignment(CCFNode.TEXT_LEFT);
                                b1.setLocation(new Point(x * button_width + (x * h_rest) / (columns - 1), y * button_height + (y * v_rest) / (rows - 1)));
                                b1.setSize(new Dimension(button_width, button_height));
                                panel.addButton(b1);
                                String ccfstring = null;
                                IrSignal irc = dvc.get_ir_code(cmds[index], act_toggle, false);
                                try {
                                    //ccfstring = raw ? irc.raw_ccf_string() : irc.ccf_string();
                                    ccfstring = irc.ccfString();
                                } catch (IncompatibleArgumentException ex) {
                                    System.err.println(ex.getMessage());
                                }
                                if ((pronto_model.getModel() == ProntoModel.CUSTOM) || (pronto_model.getCapability() & (1 << 18)) != 0)
                                    ccfstring = "0000 0000 0000 " + ccfstring;
                                //System.err.println(cmd_names[index] + ":" + ccfstring);
                                CCFIRCode code = new CCFIRCode(dev.getHeader(),
                                        //ir_code.package_name,
                                        ccfstring);
                                b1.appendAction(new ActionIRCode(code));

                                CCFIconSet icon_set = b1.getIconSet();
                                icon_set.setForeground(CCFIconSet.ACTIVE_UNSELECTED, CCFColor.getColor(CCFColor.BLACK));
                                icon_set.setBackground(CCFIconSet.ACTIVE_UNSELECTED, CCFColor.getColor(245/*CCFColor.LIGHT_GRAY*/));
                                icon_set.setForeground(CCFIconSet.ACTIVE_SELECTED, CCFColor.getColor(245/*CCFColor.LIGHT_GRAY*/));
                                icon_set.setBackground(CCFIconSet.ACTIVE_SELECTED, CCFColor.getColor(CCFColor.BLACK));
                                b1.setIconSet(icon_set);
                            }
                        }
                    }
                }
            }
        }
    }

    public ccf_export(String[] devices, com.neuron.app.tonto.ProntoModel pronto_model, boolean raw) {
        this(devices, false, toggletype.dont_care, pronto_model, raw, default_button_width,
                default_button_height, default_screenwidth, default_screenheight,
                default_screenwidth, default_screenheight);
    }

    public boolean export(String filename) {
        try {
            ccf.save(filename);
            //System.err.println("Wrote " + filename + " for " + pronto_model.getName());
            return false;
        } catch (IOException e) {
            System.err.println("Error when saving " + filename);
        }
        return true;
    }

    public static boolean ccf_exporter(String[] devices, ProntoModel pronto_model, boolean raw, String filename) {
        return (new ccf_export(devices, pronto_model, raw)).export(filename);
    }
    
    public static boolean ccf_exporter(String[] devices, ProntoModel pronto_model, boolean raw,
            int button_width, int button_height, int screenwidth, int screenheight, String filename) {
        return (new ccf_export(devices, false, toggletype.dont_care, pronto_model, raw,
                button_width, button_height, screenwidth, screenheight,
                screenwidth, screenheight)).export(filename);
    }

    private static void usage() {
        System.err.println("Usage: ccf_export [-t[0|1]] [-r] [-w <button_width>] [-h <button_height>] [-p <prontomodel>] [-o <outputfile>] <device(s)>");
        System.exit(harcutils.exit_usage_error);
    }
    
    /**
     * Usage: ccf_export [-t[0|1]] [-r] [-w <button_width>] [-h <button_height>] [-p <prontomodel>] [-o <outputfile>] <device(s)>
     */
    public static void main(String args[]) {
        boolean raw = false;
        boolean generate_both_toggles = false;
        toggletype toggle = toggletype.dont_care;
        ProntoModel pronto_model = ProntoModel.getModel(ProntoModel.TSU6000);
        int screenwidth = 240;
        int screenheight = 220;
        int used_screenwidth = 240;
        int used_screenheight = 220;
        int button_width = default_button_width;
        int button_height = default_button_height;
        int optind = 0;
        String filename = "harcexport.ccf";

        if (args.length > optind && args[optind].equals("-t")) {
            generate_both_toggles = true;
            //toggle = toggletype.do_toggle;
            optind++;
        }
        if (args.length > optind && args[optind].equals("-t0")) {
            toggle = toggletype.toggle_0;
            optind++;
        }
        if (args.length > optind && args[optind].equals("-t1")) {
            toggle = toggletype.toggle_1;
            optind++;
        }
        if (args.length > optind && args[optind].equals("-r")) {
            raw = true;
            optind++;
        }
        if (args.length > optind + 1 && args[optind].equals("-w")) {
            button_width = Integer.parseInt(args[++optind]);
            optind++;
        }
        if (args.length > optind + 1 && args[optind].equals("-h")) {
            button_height = Integer.parseInt(args[++optind]);
            optind++;
        }
        if (args.length > optind + 1 && args[optind].equals("-p")) {
            pronto_model = ProntoModel.getModelByName(args[++optind]);
            optind++;
            if (pronto_model.getModel() == ProntoModel.CUSTOM) {
                screenwidth = Integer.parseInt(args[optind++]);
                screenheight = Integer.parseInt(args[optind++]);
                used_screenwidth = Integer.parseInt(args[optind++]);
                used_screenheight = Integer.parseInt(args[optind++]);
            }
        }
        if (args.length > optind + 1 && args[optind].equals("-o")) {
            filename = args[++optind];
            optind++;
        }
        if (args.length <= optind)
            usage();

        String[] remotes = new String[args.length - optind];
        for (int i = 0; i < args.length - optind; i++) {
            remotes[i] = args[i + optind];
        }

        (new ccf_export(remotes, generate_both_toggles, toggle, pronto_model, raw,
                button_width, button_height, screenwidth, screenheight,
                used_screenwidth, used_screenheight)).export(filename);
    }
}
