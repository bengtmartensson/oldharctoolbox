/*
Copyright (C) 2011 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope thlat it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox;

import IrpMaster.DecodeIR;
import IrpMaster.ICT;
import IrpMaster.IncompatibleArgumentException;
import IrpMaster.IrSignal;
import IrpMaster.IrpMaster;
import IrpMaster.IrpMasterException;
import IrpMaster.IrpUtils;
import IrpMaster.Pronto;
import IrpMaster.Protocol;
import IrpMaster.UnassignedException;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.UnknownHostException;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.antlr.runtime.RecognitionException;
// do not import org.harctoolbox.protocol;

/**
 * This class implements a GUI for most functionality in Harc.
 */
// TODO: Implement limited functionallity without home/macro file.

public class ezcontrolGUI extends javax.swing.JFrame {
    private static IrpMaster irpMaster = null;
    private static HashMap<String, Protocol> protocols = null;
    //private String last_rmdu_export = null;
    private final static short invalid_parameter = -1;
    private int debug = 0;
    private boolean verbose = false;
    private DefaultComboBoxModel gc_modules_dcbm;
    private DefaultComboBoxModel rdf_dcbm;
    private String[] prontomodelnames;
    private String[] button_remotenames;
    //private resultformatter formatter = new resultformatter();
    //private resultformatter cmd_formatter = new resultformatter(Props.get_instance().get_commandformat());
    private static final String dummy_no_selection = "--------";

    private globalcache_thread the_globalcache_device_thread = null;
    private globalcache_thread the_globalcache_protocol_thread = null;
    private irtrans_thread the_irtrans_thread = null;
    
    private globalcache gc = null;
    private irtrans irt = null;

    private final static int default_rmdu_export_remoteindex = 1; //FIXME

    private HashMap<String, String> filechooserdirs = new HashMap<String, String>();

    private File select_file(String title, String extension, String file_type_desc, boolean save, String defaultdir) {
        String startdir = this.filechooserdirs.containsKey(title) ? this.filechooserdirs.get(title) : defaultdir;
        JFileChooser chooser = new JFileChooser(startdir);
        chooser.setDialogTitle(title);
        if (extension == null) {
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        } else
            chooser.setFileFilter(new FileNameExtensionFilter(file_type_desc, extension));

        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            filechooserdirs.put(title, chooser.getSelectedFile().getAbsoluteFile().getParent());
            return chooser.getSelectedFile();
        } else
            return null;
    }

    private class copy_clipboard_text implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void to_clipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }
    }
    
    private static Protocol get_protocol(String name) throws UnassignedException, RecognitionException {
        if (!protocols.containsKey(name)) {
            Protocol protocol = irpMaster.newProtocol(name);
            protocols.put(name, protocol);
        }
        return protocols.get(name);            
    }

    /** Creates new form gui_main */
    public ezcontrolGUI() {
        gc_modules_dcbm = new DefaultComboBoxModel(new String[]{"2"}); // ?

        // TODO: check behavior in abscense of tonto
        com.neuron.app.tonto.ProntoModel[] prontomodels = com.neuron.app.tonto.ProntoModel.getModels();
        prontomodelnames = new String[prontomodels.length];
        for (int i = 0; i < prontomodels.length; i++)
            prontomodelnames[i] = prontomodels[i].toString();
        
        // Since remotemaster generates an enormous amount of noise on stderr,
        // we redirect stderr temporarilly.
        //try {
        //    System.setErr(new PrintStream(new FileOutputStream(".harc_rmaster.err")));
        //} catch (FileNotFoundException ex) {
        //    ex.printStackTrace();
        //}

        // FIXME button_remotenames = button_remote.get_button_remotes();
        
        //if (button_remotenames == null || button_remotenames.length == 0)
        //    button_remotenames = new String[]{"*** Error ***"}; // FIXME
        //java.util.Arrays.sort(button_remotenames);

      
        
        initComponents();
    

        System.setErr(console_PrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("*************** This is GUI shutdown **********");
            }
        });

   
        //browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), commandtype_t.www, command_t.browse));

        gc = new globalcache("globalcache", globalcache.gc_model.gc_unknown, verbose);
        irt = new irtrans("irtrans", verbose);

       
        //remotemaster_home_TextField.setText(Props.get_instance().get_remotemaster_home());
        //rmdu_button_rules_TextField.setText(Props.get_instance().get_rmdu_button_rules());

        //System.setOut(console_PrintStream);
        
    }

    // From Real Gagnon        
    class FilteredStream extends FilterOutputStream {

        public FilteredStream(OutputStream aStream) {
            super(aStream);
        }

        @Override
        public void write(byte b[]) throws IOException {
            String aString = new String(b);
            console_TextArea.append(aString);
        }

        @Override
        public void write(byte b[], int off, int len) throws IOException {
            String aString = new String(b, off, len);
            console_TextArea.append(aString);
            console_TextArea.setCaretPosition(console_TextArea.getDocument().getLength());
        /*
        if (logFile) {
        FileWriter aWriter = new FileWriter("error.log", true);
        aWriter.write(aString);
        aWriter.close();
        }*/
        }
    }
    
    PrintStream console_PrintStream = new PrintStream(
            new FilteredStream(
            new ByteArrayOutputStream()));

    //TODO: boolean logFile;
    private void warning(String message) {
        System.err.println("Warning: " + message);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        mainTabbedPane = new javax.swing.JTabbedPane();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        ezcontrolPanel = new javax.swing.JPanel();
        t10_address_TextField = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        ezcontrol_preset_no_ComboBox = new javax.swing.JComboBox();
        ezcontrol_preset_name_TextField = new javax.swing.JTextField();
        ezcontrol_preset_state_TextField = new javax.swing.JTextField();
        ezcontrol_preset_on_Button = new javax.swing.JButton();
        ezcontrol_preset_off_Button = new javax.swing.JButton();
        t10_update_Button = new javax.swing.JButton();
        t10_get_timers_Button = new javax.swing.JButton();
        t10_get_status_Button = new javax.swing.JButton();
        ezcontrol_system_ComboBox = new javax.swing.JComboBox();
        ezcontrol_house_ComboBox = new javax.swing.JComboBox();
        ezcontrol_deviceno_ComboBox = new javax.swing.JComboBox();
        ezcontrol_onButton = new javax.swing.JButton();
        ezcontrol_off_Button = new javax.swing.JButton();
        n_ezcontrol_ComboBox = new javax.swing.JComboBox();
        t10_browse_Button = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        console_TextArea = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        copy_console_to_clipboard_MenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        verbose_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        miscMenu = new javax.swing.JMenu();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HARCToolbox: Home Automation and Remote Control Toolbox"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        t10_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        t10_address_TextField.setText("192.168.1.42");
        t10_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        t10_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        t10_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        t10_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                t10_address_TextFieldActionPerformed(evt);
            }
        });

        jLabel13.setText("IP-Address");

        ezcontrol_preset_no_ComboBox.setMaximumRowCount(16);
        ezcontrol_preset_no_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32" }));
        ezcontrol_preset_no_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ezcontrol_preset_no_ComboBoxActionPerformed(evt);
            }
        });

        ezcontrol_preset_name_TextField.setEditable(false);
        ezcontrol_preset_name_TextField.setText("?????????");
        ezcontrol_preset_name_TextField.setToolTipText("Name of selected preset");
        ezcontrol_preset_name_TextField.setMaximumSize(new java.awt.Dimension(150, 27));
        ezcontrol_preset_name_TextField.setMinimumSize(new java.awt.Dimension(150, 27));
        ezcontrol_preset_name_TextField.setPreferredSize(new java.awt.Dimension(150, 27));

        ezcontrol_preset_state_TextField.setEditable(false);
        ezcontrol_preset_state_TextField.setText("??");
        ezcontrol_preset_state_TextField.setMaximumSize(new java.awt.Dimension(50, 2147483647));
        ezcontrol_preset_state_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        ezcontrol_preset_state_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        ezcontrol_preset_on_Button.setText("On");
        ezcontrol_preset_on_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ezcontrol_preset_on_ButtonActionPerformed(evt);
            }
        });

        ezcontrol_preset_off_Button.setText("Off");
        ezcontrol_preset_off_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ezcontrol_preset_off_ButtonActionPerformed(evt);
            }
        });

        t10_update_Button.setText("Update");
        t10_update_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                t10_update_ButtonActionPerformed(evt);
            }
        });

        t10_get_timers_Button.setText("Get Timers");
        t10_get_timers_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                t10_get_timers_ButtonActionPerformed(evt);
            }
        });

        t10_get_status_Button.setText("Get Status");
        t10_get_status_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                t10_get_status_ButtonActionPerformed(evt);
            }
        });

        ezcontrol_system_ComboBox.setMaximumRowCount(16);
        ezcontrol_system_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "FS10", "FS20", "RS200", "AB400", "AB601", "Intertechno", "REV", "BS-QU", "X10", "OA-FM", "Kopp First Control (1st gen)", "RS862" }));
        ezcontrol_system_ComboBox.setSelectedIndex(5);

        ezcontrol_house_ComboBox.setMaximumRowCount(16);
        ezcontrol_house_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P" }));
        ezcontrol_house_ComboBox.setToolTipText("House");

        ezcontrol_deviceno_ComboBox.setMaximumRowCount(16);
        ezcontrol_deviceno_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" }));
        ezcontrol_deviceno_ComboBox.setToolTipText("device address");

        ezcontrol_onButton.setText("On");
        ezcontrol_onButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ezcontrol_onButtonActionPerformed(evt);
            }
        });

        ezcontrol_off_Button.setText("Off");
        ezcontrol_off_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ezcontrol_off_ButtonActionPerformed(evt);
            }
        });

        n_ezcontrol_ComboBox.setMaximumRowCount(10);
        n_ezcontrol_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10" }));
        n_ezcontrol_ComboBox.setToolTipText("Number of times to send the command.");

        t10_browse_Button.setText("Browse");
        t10_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                t10_browse_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ezcontrolPanelLayout = new javax.swing.GroupLayout(ezcontrolPanel);
        ezcontrolPanel.setLayout(ezcontrolPanelLayout);
        ezcontrolPanelLayout.setHorizontalGroup(
            ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ezcontrolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ezcontrolPanelLayout.createSequentialGroup()
                        .addComponent(jLabel13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(t10_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(t10_browse_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(t10_get_status_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(t10_get_timers_Button)
                        .addGap(697, 697, 697))
                    .addGroup(ezcontrolPanelLayout.createSequentialGroup()
                        .addComponent(ezcontrol_system_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_house_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_deviceno_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(n_ezcontrol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_onButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_off_Button)
                        .addContainerGap(73, Short.MAX_VALUE))
                    .addGroup(ezcontrolPanelLayout.createSequentialGroup()
                        .addComponent(ezcontrol_preset_no_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_preset_name_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_preset_state_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_preset_on_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ezcontrol_preset_off_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(t10_update_Button)
                        .addContainerGap(159, Short.MAX_VALUE))))
        );
        ezcontrolPanelLayout.setVerticalGroup(
            ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, ezcontrolPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ezcontrol_system_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_house_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_deviceno_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(n_ezcontrol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_onButton)
                    .addComponent(ezcontrol_off_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ezcontrol_preset_no_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_preset_name_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_preset_state_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ezcontrol_preset_on_Button)
                    .addComponent(ezcontrol_preset_off_Button)
                    .addComponent(t10_update_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(t10_get_timers_Button)
                    .addComponent(t10_get_status_Button)
                    .addComponent(jLabel13)
                    .addComponent(t10_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t10_browse_Button))
                .addContainerGap())
        );

        outputHWTabbedPane.addTab("EZControl", ezcontrolPanel);

        mainTabbedPane.addTab("Output HW", outputHWTabbedPane);

        console_TextArea.setColumns(20);
        console_TextArea.setEditable(false);
        console_TextArea.setLineWrap(true);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is the console, where errors and messages go, instead of annoying you with popups.");
        console_TextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(console_TextArea);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        consoletext_save_MenuItem.setMnemonic('c');
        consoletext_save_MenuItem.setText("Save console text as...");
        consoletext_save_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletext_save_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(consoletext_save_MenuItem);
        fileMenu.add(jSeparator1);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setMnemonic('E');
        editMenu.setText("Edit");

        copy_console_to_clipboard_MenuItem.setText("Copy Console to clipboard");
        copy_console_to_clipboard_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copy_console_to_clipboard_MenuItemActionPerformed(evt);
            }
        });
        editMenu.add(copy_console_to_clipboard_MenuItem);

        menuBar.add(editMenu);

        jMenu1.setMnemonic('O');
        jMenu1.setText("Options");

        verbose_CheckBoxMenuItem.setMnemonic('v');
        verbose_CheckBoxMenuItem.setText("Verbose");
        verbose_CheckBoxMenuItem.setToolTipText("Report actual command sent to devices");
        verbose_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(verbose_CheckBoxMenuItem);

        menuBar.add(jMenu1);

        miscMenu.setMnemonic('M');
        miscMenu.setText("Misc.");

        clear_console_MenuItem.setText("Clear console");
        clear_console_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_console_MenuItemActionPerformed(evt);
            }
        });
        miscMenu.add(clear_console_MenuItem);

        menuBar.add(miscMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        contentMenuItem.setMnemonic('C');
        contentMenuItem.setText("Content...");
        contentMenuItem.setToolTipText("Brings up documentation.");
        contentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentMenuItem);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
        aboutMenuItem.setToolTipText("The mandatory About popup");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(mainTabbedPane, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(mainTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 219, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class globalcache_thread extends Thread {
        private IrSignal code;
        private int module;
        private int connector;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public globalcache_thread(IrSignal code, int module, int connector, int count,
                JButton start_button, JButton stop_button) {
            super("globalcache_thread");
            this.code = code;
            this.module = module;
            this.connector = connector;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
            boolean success = false;
            try {
                success = gc.send_ir(code, module, connector, count);
            } catch (UnknownHostException ex) {
                System.err.println("Globalcache hostname is not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted *** ");
                success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            //the_globalcache_thread = null;
            start_button.setEnabled(true);
            stop_button.setEnabled(false);
        }
    }

    private class irtrans_thread extends Thread {
        private String remote;
        private String commandname;
        private irtrans.led_t led;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public irtrans_thread(String remote, String commandname, irtrans.led_t led, int count,
                JButton start_button, JButton stop_button) {
            super("irtrans_thread");
            this.remote = remote;
            this.commandname = commandname;
            this.led = led;
            this.count = count;
            this.start_button = start_button;
            this.stop_button = stop_button;
        }

        @Override
        public void run() {
            start_button.setEnabled(false);
            stop_button.setEnabled(true);
            boolean success = false;
            try {
                success = irt.send_flashed_command(remote, commandname, led, count);
            } catch (UnknownHostException ex) {
                System.err.println("IRTrans hostname not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted *** ");
                success = true;
            }

            if (!success)
                System.err.println("** Failed **");

            the_irtrans_thread = null;
            start_button.setEnabled(true);
            stop_button.setEnabled(false);
        }
    }

    private void do_exit() {
        System.out.println("Exiting...");
        System.exit(0);
    }

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        do_exit();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            //JFrame mainFrame = gui_main.getApplication().getMainFrame();
            aboutBox = new ez_about_popup(this/*mainFrame*/, false);
            aboutBox.setLocationRelativeTo(/*mainFrame*/this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentMenuItemActionPerformed
        //harcutils.browse(Props.get_instance().get_helpfilename());
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        verbose = verbose_CheckBoxMenuItem.isSelected();
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed


    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getText());
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.out.println("asfkad");//do_exit();
    }//GEN-LAST:event_formWindowClosed

   
  


    private void consoletext_save_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_consoletext_save_MenuItemActionPerformed
        try {
            String filename = select_file("Save console text as...", "txt", "Text file", true, null).getAbsolutePath();
            PrintStream ps = new PrintStream(new FileOutputStream(filename));
            ps.println(console_TextArea.getText());
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_consoletext_save_MenuItemActionPerformed


  

    private void t10_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_address_TextFieldActionPerformed

}//GEN-LAST:event_t10_address_TextFieldActionPerformed

    private void t10_get_timers_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_get_timers_ButtonActionPerformed
        System.err.println(ezcontrol_t10.get_timers(t10_address_TextField.getText()));
}//GEN-LAST:event_t10_get_timers_ButtonActionPerformed

    private void t10_get_status_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_get_status_ButtonActionPerformed
        System.err.println(ezcontrol_t10.get_status(t10_address_TextField.getText()));
}//GEN-LAST:event_t10_get_status_ButtonActionPerformed

    private void t10_send_manual_command(command_t cmd) {
    try {
            (new ezcontrol_t10(t10_address_TextField.getText())).send_manual(
                    (String) ezcontrol_system_ComboBox.getModel().getSelectedItem(),
                    (String) ezcontrol_house_ComboBox.getModel().getSelectedItem(),
                    Integer.parseInt((String) ezcontrol_deviceno_ComboBox.getModel().getSelectedItem()),
                    cmd, -1,
                    Integer.parseInt((String) this.n_ezcontrol_ComboBox.getModel().getSelectedItem()));
        } catch (non_existing_command_exception ex) {
            System.err.println("This cannot happen.");
        }
    }

    private void t10_send_preset_command(command_t cmd) {
        try {
            (new ezcontrol_t10(t10_address_TextField.getText())).send_preset(Integer.parseInt((String) ezcontrol_preset_no_ComboBox.getModel().getSelectedItem()), cmd);
        } catch (non_existing_command_exception ex) {
        }
    }

    private void ezcontrol_onButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ezcontrol_onButtonActionPerformed
        t10_send_manual_command(command_t.power_on);
    }//GEN-LAST:event_ezcontrol_onButtonActionPerformed

    private void ezcontrol_off_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ezcontrol_off_ButtonActionPerformed
        t10_send_manual_command(command_t.power_off);
    }//GEN-LAST:event_ezcontrol_off_ButtonActionPerformed

    private void ezcontrol_preset_on_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ezcontrol_preset_on_ButtonActionPerformed
        t10_send_preset_command(command_t.power_on);
        this.ezcontrol_preset_state_TextField.setText("on");
    }//GEN-LAST:event_ezcontrol_preset_on_ButtonActionPerformed

    private void ezcontrol_preset_off_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ezcontrol_preset_off_ButtonActionPerformed
        t10_send_preset_command(command_t.power_off);
        this.ezcontrol_preset_state_TextField.setText("off");
}//GEN-LAST:event_ezcontrol_preset_off_ButtonActionPerformed

    private void ezcontrol_preset_no_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ezcontrol_preset_no_ComboBoxActionPerformed
        ezcontrol_t10 ez = new ezcontrol_t10(this.t10_address_TextField.getText());
        int preset_number = Integer.parseInt((String)ezcontrol_preset_no_ComboBox.getModel().getSelectedItem());
        ezcontrol_preset_name_TextField.setText(ez.get_preset_name(preset_number));
        this.ezcontrol_preset_state_TextField.setText(ez.get_preset_status(preset_number));
    }//GEN-LAST:event_ezcontrol_preset_no_ComboBoxActionPerformed

    private void t10_update_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_update_ButtonActionPerformed
        ezcontrol_preset_no_ComboBoxActionPerformed(evt);
}//GEN-LAST:event_t10_update_ButtonActionPerformed

    private void t10_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_browse_ButtonActionPerformed
        harcutils.browse(t10_address_TextField.getText());
    }//GEN-LAST:event_t10_browse_ButtonActionPerformed

  
    

    //public static gui_main getApplication() {
    //  return Application.getInstance(gui_main.class);
    //}
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                new ezcontrolGUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JTextArea console_TextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JPanel ezcontrolPanel;
    private javax.swing.JComboBox ezcontrol_deviceno_ComboBox;
    private javax.swing.JComboBox ezcontrol_house_ComboBox;
    private javax.swing.JButton ezcontrol_off_Button;
    private javax.swing.JButton ezcontrol_onButton;
    private javax.swing.JTextField ezcontrol_preset_name_TextField;
    private javax.swing.JComboBox ezcontrol_preset_no_ComboBox;
    private javax.swing.JButton ezcontrol_preset_off_Button;
    private javax.swing.JButton ezcontrol_preset_on_Button;
    private javax.swing.JTextField ezcontrol_preset_state_TextField;
    private javax.swing.JComboBox ezcontrol_system_ComboBox;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JTabbedPane mainTabbedPane;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu miscMenu;
    private javax.swing.JComboBox n_ezcontrol_ComboBox;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JTextField t10_address_TextField;
    private javax.swing.JButton t10_browse_Button;
    private javax.swing.JButton t10_get_status_Button;
    private javax.swing.JButton t10_get_timers_Button;
    private javax.swing.JButton t10_update_Button;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    // End of variables declaration//GEN-END:variables
    private ez_about_popup aboutBox;
}
