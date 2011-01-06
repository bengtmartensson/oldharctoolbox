/*
Copyright (C) 2009, 2010 Bengt Martensson.

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

import com.neuron.app.tonto.ProntoModel;
import java.awt.Dimension;
import javax.swing.*;
import java.io.*;
import java.net.*;
import org.xml.sax.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;
import java.util.Hashtable;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * This class implements a GUI for most functionallity in Harc.
 */
// TODO: Implement limited functionallity without home/macro file.

public class gui_main extends javax.swing.JFrame {

    private home hm = null;
    private jython_engine engine = null;
    private String last_rmdu_export = null;
    private int debug = 0;
    private boolean verbose = false;
    private DefaultComboBoxModel macros_dcbm;
    private DefaultComboBoxModel toplevel_macrofolders_dcbm;
    private DefaultComboBoxModel secondlevel_macrofolders_dcbm;
    private DefaultComboBoxModel devices_dcbm;
    private DefaultComboBoxModel commands_dcbm;
    private DefaultComboBoxModel devicegroups_dcbm;
    private DefaultComboBoxModel selecting_devices_dcbm;
    private DefaultComboBoxModel src_devices_dcbm;
    private DefaultComboBoxModel zones_dcbm;
    private DefaultComboBoxModel deviceclasses_dcbm;
    private DefaultComboBoxModel device_commands_dcbm;
    private DefaultComboBoxModel connection_types_dcbm;
    private DefaultComboBoxModel device_remotes_dcbm;
    private DefaultComboBoxModel gc_modules_dcbm;
    private DefaultComboBoxModel rdf_dcbm;
    private String[] prontomodelnames;
    private String[] button_remotenames;
    private resultformatter formatter = new resultformatter();
    private resultformatter cmd_formatter = new resultformatter(harcprops.get_instance().get_commandformat());
    private static final String dummy_no_selection = "--------";

    private macro_thread the_macro_thread = null;
    private command_thread the_command_thread = null;
    private globalcache_thread the_globalcache_device_thread = null;
    private globalcache_thread the_globalcache_protocol_thread = null;
    private irtrans_thread the_irtrans_thread = null;
    
    private globalcache gc = null;
    private irtrans irt = null;

    private final static int default_rmdu_export_remoteindex = 1; //FIXME

    private Hashtable<String, String> filechooserdirs = new Hashtable<String, String>();

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

        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void to_clipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }
    }

    /** Creates new form gui_main */
    public gui_main(String homefilename/*, String macrofilename, boolean verbose, int debug, String browser*/) {
        try {
            hm = new home(homefilename);
        } catch (IOException e) {
            System.err.println("Cannot open home file");
            System.exit(harcutils.exit_config_read_error);
        } catch (SAXParseException e) {
            System.err.println("home file parse error" + e.getMessage());
            System.exit(harcutils.exit_xml_error);
        } catch (SAXException e) {
            System.err.println("home file parse error" + e.getMessage());
            System.exit(harcutils.exit_xml_error);
        }

        engine = new jython_engine(hm, false);
        String[] macs = engine.get_argumentless_macros();
        if (macs != null) {
            macros_dcbm = new DefaultComboBoxModel(macs);
            //String[] toplevel_macrofolders = engine.get_folders();
            //toplevel_macrofolders_dcbm = new DefaultComboBoxModel(toplevel_macrofolders);
            //secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(
            //        (toplevel_macrofolders != null && toplevel_macrofolders.length > 0)
            //        ? engine.get_folders(toplevel_macrofolders[0], 1)
            //        : (new String[]{dummy_no_selection}));
        } else {
            engine = null;
            macros_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});
        }
        // FIXME
        toplevel_macrofolders_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});
        secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});

        devices_dcbm = new DefaultComboBoxModel(hm.get_devices());
        devicegroups_dcbm = new DefaultComboBoxModel(hm.get_devicegroups());
        commands_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});
        selecting_devices_dcbm = new DefaultComboBoxModel(hm.get_selecting_devices());
        src_devices_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});
        zones_dcbm = new DefaultComboBoxModel(new String[]{"--"});
        deviceclasses_dcbm = new DefaultComboBoxModel(harcutils.sort_unique(device.get_devices()));
        device_commands_dcbm = new DefaultComboBoxModel(new String[]{"--"});
        connection_types_dcbm = new DefaultComboBoxModel(new String[]{"--"});
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

        button_remotenames = button_remote.get_button_remotes();
        
        if (button_remotenames == null)
            button_remotenames = new String[]{""};
        java.util.Arrays.sort(button_remotenames);

        initComponents();

        System.setErr(console_PrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    harcprops.get_instance().save();
                    socket_storage.dispose_sockets(true);
                } catch (Exception e) {
                    System.out.println("Problems saving properties; " + e.getMessage());
                }
                System.out.println("*************** This is GUI shutdown **********");
            }
        });

        //update_macro_menu();
        update_device_menu();
        //update_command_menu();
        update_src_device_menu();
        update_zone_menu();
        update_device_commands_menu();
        update_protocol_parameters();
        update_device_remotes_menu();
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
        browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), commandtype_t.www, command_t.browse));

        gc = new globalcache("globalcache", globalcache.gc_model.gc_unknown, verbose);
        irt = new irtrans("irtrans", verbose);

        homeconf_TextField.setText(homefilename);
        //macro_TextField.setText(macrofilename);
        aliases_TextField.setText(harcprops.get_instance().get_aliasfilename());
        browser_TextField.setText(harcprops.get_instance().get_browser());
        exportdir_TextField.setText(harcprops.get_instance().get_exportdir());
        remotemaster_home_TextField.setText(harcprops.get_instance().get_remotemaster_home());
        rmdu_button_rules_TextField.setText(harcprops.get_instance().get_rmdu_button_rules());
        update_from_frequency();

        //System.setOut(console_PrintStream);
        
    }

    public gui_main() {
        this(harcprops.get_instance().get_homefilename());
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

        jScrollPane1 = new javax.swing.JScrollPane();
        console_TextArea = new javax.swing.JTextArea();
        output_hw_TabbedPane = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        device_ComboBox = new javax.swing.JComboBox();
        macroComboBox = new javax.swing.JComboBox();
        macroButton = new javax.swing.JButton();
        toplevel_macrofolders_ComboBox = new javax.swing.JComboBox();
        secondlevel_macrofolders_ComboBox = new javax.swing.JComboBox();
        devicegroup_ComboBox = new javax.swing.JComboBox();
        command_ComboBox = new javax.swing.JComboBox();
        selecting_device_ComboBox = new javax.swing.JComboBox();
        src_device_ComboBox = new javax.swing.JComboBox();
        commandButton = new javax.swing.JButton();
        command_argument_TextField = new javax.swing.JTextField();
        zones_ComboBox = new javax.swing.JComboBox();
        select_Button = new javax.swing.JButton();
        audio_video_ComboBox = new javax.swing.JComboBox();
        connection_type_ComboBox = new javax.swing.JComboBox();
        stop_macro_Button = new javax.swing.JButton();
        stop_command_Button = new javax.swing.JButton();
        deviceclassesPanel = new javax.swing.JPanel();
        deviceclass_ComboBox = new javax.swing.JComboBox();
        device_command_ComboBox = new javax.swing.JComboBox();
        deviceclass_send_Button = new javax.swing.JButton();
        no_sends_ComboBox = new javax.swing.JComboBox();
        output_deviceComboBox = new javax.swing.JComboBox();
        deviceclass_stop_Button = new javax.swing.JButton();
        device_remote_ComboBox = new javax.swing.JComboBox();
        protocolsPanel = new javax.swing.JPanel();
        protocol_ComboBox = new javax.swing.JComboBox();
        deviceno_TextField = new javax.swing.JTextField();
        subdevice_TextField = new javax.swing.JTextField();
        commandno_TextField = new javax.swing.JTextField();
        toggle_ComboBox = new javax.swing.JComboBox();
        protocol_generate_Button = new javax.swing.JButton();
        protocol_send_Button = new javax.swing.JButton();
        protocol_cooked_TextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        protocol_raw_TextArea = new javax.swing.JTextArea();
        protocol_decode_Button = new javax.swing.JButton();
        protocol_stop_Button = new javax.swing.JButton();
        no_sends_protocol_ComboBox = new javax.swing.JComboBox();
        protocol_outputhw_ComboBox = new javax.swing.JComboBox();
        protocol_clear_Button = new javax.swing.JButton();
        icf_import_Button = new javax.swing.JButton();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        globalcache_Panel = new javax.swing.JPanel();
        gc_address_TextField = new javax.swing.JTextField();
        gc_module_ComboBox = new javax.swing.JComboBox();
        gc_connector_ComboBox = new javax.swing.JComboBox();
        gc_browse_Button = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        irtrans_Panel = new javax.swing.JPanel();
        irtrans_address_TextField = new javax.swing.JTextField();
        irtrans_led_ComboBox = new javax.swing.JComboBox();
        irtrans_browse_Button = new javax.swing.JButton();
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
        hexcalcPanel = new javax.swing.JPanel();
        decimal_TextField = new javax.swing.JTextField();
        hex_TextField = new javax.swing.JTextField();
        complement_decimal_TextField = new javax.swing.JTextField();
        complement_hex_TextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        reverse_hex_TextField = new javax.swing.JTextField();
        reverse_decimal_TextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        no_periods_TextField = new javax.swing.JTextField();
        frequency_TextField = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        time_TextField = new javax.swing.JTextField();
        jLabel24 = new javax.swing.JLabel();
        prontocode_TextField = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        period_selection_enable_CheckBox = new javax.swing.JCheckBox();
        jSeparator6 = new javax.swing.JSeparator();
        optsTabbedPane = new javax.swing.JTabbedPane();
        general_Panel = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        homeconf_TextField = new javax.swing.JTextField();
        home_select_Button = new javax.swing.JButton();
        home_browse_Button = new javax.swing.JButton();
        home_load_Button = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        macro_TextField = new javax.swing.JTextField();
        browser_TextField = new javax.swing.JTextField();
        aliases_TextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        macro_select_Button = new javax.swing.JButton();
        aliases_select_Button = new javax.swing.JButton();
        browser_select_Button = new javax.swing.JButton();
        macro_browse_Button = new javax.swing.JButton();
        alias_browse_Button = new javax.swing.JButton();
        macro_load_Button = new javax.swing.JButton();
        exportopts_TabbedPane = new javax.swing.JTabbedPane();
        general_export_opts_Panel = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        exportdir_TextField = new javax.swing.JTextField();
        exportdir_browse_Button = new javax.swing.JButton();
        ccf_export_opts_Panel = new javax.swing.JPanel();
        ccf_export_prontomodel_ComboBox = new javax.swing.JComboBox();
        ccf_export_raw_CheckBox = new javax.swing.JCheckBox();
        ccf_export_screenwidth_TextField = new javax.swing.JTextField();
        ccf_export_screenheight_TextField = new javax.swing.JTextField();
        ccf_export_buttonwidth_TextField = new javax.swing.JTextField();
        ccf_export_buttonheight_TextField = new javax.swing.JTextField();
        ccf_export_export_Button = new javax.swing.JButton();
        rmdu_export_opts_Panel = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        rmdu_button_rules_TextField = new javax.swing.JTextField();
        jLabel20 = new javax.swing.JLabel();
        keymap_rules_browse_Button = new javax.swing.JButton();
        rdf_ComboBox = new javax.swing.JComboBox();
        remotemaster_home_TextField = new javax.swing.JTextField();
        remotemaster_home_browse_Button = new javax.swing.JButton();
        jLabel21 = new javax.swing.JLabel();
        debug_Panel = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        debug_TextField = new javax.swing.JTextField();
        verbose_CheckBox = new javax.swing.JCheckBox();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        export_device_MenuItem = new javax.swing.JMenuItem();
        export_all_MenuItem = new javax.swing.JMenuItem();
        lirc_export_device_MenuItem = new javax.swing.JMenuItem();
        lirc_export_all_MenuItem = new javax.swing.JMenuItem();
        lirc_export_server_MenuItem = new javax.swing.JMenuItem();
        rem_export_device_MenuItem = new javax.swing.JMenuItem();
        rem_export_all_MenuItem = new javax.swing.JMenuItem();
        ccf_export_MenuItem = new javax.swing.JMenuItem();
        rmdu_export_MenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        import_ccf_MenuItem = new javax.swing.JMenuItem();
        import_rmdu_MenuItem = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        copy_console_to_clipboard_MenuItem = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        enable_devicegroups_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        enable_macro_folders_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        immediate_execution_macros_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        immediate_execution_commands_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sort_macros_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sort_devices_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        sort_commands_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        verbose_CheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        jMenu2 = new javax.swing.JMenu();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        browse_device_MenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        launch_remotemaster_MenuItem = new javax.swing.JMenuItem();
        launch_rmir_MenuItem = new javax.swing.JMenuItem();
        launch_tonto_MenuItem = new javax.swing.JMenuItem();
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

        console_TextArea.setColumns(20);
        console_TextArea.setEditable(false);
        console_TextArea.setLineWrap(true);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is a popup free zone!");
        console_TextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(console_TextArea);

        jLabel1.setText("Device");

        jLabel2.setText("Macro");

        jLabel3.setText("Select");

        device_ComboBox.setMaximumRowCount(20);
        device_ComboBox.setModel(devices_dcbm);
        device_ComboBox.setToolTipText("Deviceinstance");
        device_ComboBox.setMaximumSize(new java.awt.Dimension(125, 25));
        device_ComboBox.setMinimumSize(new java.awt.Dimension(125, 25));
        device_ComboBox.setPreferredSize(new java.awt.Dimension(125, 25));
        device_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                device_ComboBoxActionPerformed(evt);
            }
        });

        macroComboBox.setMaximumRowCount(20);
        macroComboBox.setModel(macros_dcbm);
        macroComboBox.setMaximumSize(new java.awt.Dimension(170, 25));
        macroComboBox.setMinimumSize(new java.awt.Dimension(170, 25));
        macroComboBox.setPreferredSize(new java.awt.Dimension(150, 25));
        macroComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macroComboBoxActionPerformed(evt);
            }
        });

        macroButton.setMnemonic('G');
        macroButton.setText("Go!");
        macroButton.setToolTipText("Execute macro");
        macroButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macroButtonActionPerformed(evt);
            }
        });

        toplevel_macrofolders_ComboBox.setModel(toplevel_macrofolders_dcbm);
        toplevel_macrofolders_ComboBox.setToolTipText("Top level folder");
        toplevel_macrofolders_ComboBox.setEnabled(false);
        toplevel_macrofolders_ComboBox.setMaximumSize(new java.awt.Dimension(120, 25));
        toplevel_macrofolders_ComboBox.setMinimumSize(new java.awt.Dimension(120, 25));
        toplevel_macrofolders_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toplevel_macrofolders_ComboBoxActionPerformed(evt);
            }
        });

        secondlevel_macrofolders_ComboBox.setMaximumRowCount(20);
        secondlevel_macrofolders_ComboBox.setModel(secondlevel_macrofolders_dcbm);
        secondlevel_macrofolders_ComboBox.setToolTipText("Second level folder");
        secondlevel_macrofolders_ComboBox.setEnabled(false);
        secondlevel_macrofolders_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secondlevel_macrofolders_ComboBoxActionPerformed(evt);
            }
        });

        devicegroup_ComboBox.setMaximumRowCount(20);
        devicegroup_ComboBox.setModel(devicegroups_dcbm);
        devicegroup_ComboBox.setToolTipText("Devicegroup");
        devicegroup_ComboBox.setEnabled(false);
        devicegroup_ComboBox.setMaximumSize(new java.awt.Dimension(120, 25));
        devicegroup_ComboBox.setMinimumSize(new java.awt.Dimension(120, 25));
        devicegroup_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        devicegroup_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                devicegroup_ComboBoxActionPerformed(evt);
            }
        });

        command_ComboBox.setMaximumRowCount(25);
        command_ComboBox.setModel(commands_dcbm);
        command_ComboBox.setToolTipText("Command");
        command_ComboBox.setMaximumSize(new java.awt.Dimension(150, 25));
        command_ComboBox.setMinimumSize(new java.awt.Dimension(150, 25));
        command_ComboBox.setPreferredSize(new java.awt.Dimension(150, 25));
        command_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                command_ComboBoxActionPerformed(evt);
            }
        });

        selecting_device_ComboBox.setMaximumRowCount(20);
        selecting_device_ComboBox.setModel(selecting_devices_dcbm);
        selecting_device_ComboBox.setToolTipText("Device to select input for");
        selecting_device_ComboBox.setMaximumSize(new java.awt.Dimension(120, 25));
        selecting_device_ComboBox.setMinimumSize(new java.awt.Dimension(120, 25));
        selecting_device_ComboBox.setPreferredSize(new java.awt.Dimension(120, 25));
        selecting_device_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selecting_device_ComboBoxActionPerformed(evt);
            }
        });

        src_device_ComboBox.setMaximumRowCount(20);
        src_device_ComboBox.setModel(src_devices_dcbm);
        src_device_ComboBox.setToolTipText("Device as input");
        src_device_ComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        src_device_ComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        src_device_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        src_device_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                src_device_ComboBoxActionPerformed(evt);
            }
        });

        commandButton.setText("Go!");
        commandButton.setToolTipText("Execute command");
        commandButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandButtonActionPerformed(evt);
            }
        });

        command_argument_TextField.setToolTipText("argument for command");
        command_argument_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        command_argument_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        command_argument_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        zones_ComboBox.setModel(zones_dcbm);
        zones_ComboBox.setToolTipText("zone");
        zones_ComboBox.setMaximumSize(new java.awt.Dimension(50, 25));
        zones_ComboBox.setMinimumSize(new java.awt.Dimension(50, 25));
        zones_ComboBox.setPreferredSize(new java.awt.Dimension(50, 25));
        zones_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zones_ComboBoxActionPerformed(evt);
            }
        });

        select_Button.setText("Go!");
        select_Button.setToolTipText("Execute selection command");
        select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                select_ButtonActionPerformed(evt);
            }
        });

        audio_video_ComboBox.setModel(new DefaultComboBoxModel(mediatype.values()));
        audio_video_ComboBox.setToolTipText("video and/or audio");
        audio_video_ComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        audio_video_ComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        audio_video_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        audio_video_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                audio_video_ComboBoxActionPerformed(evt);
            }
        });

        connection_type_ComboBox.setModel(connection_types_dcbm);
        connection_type_ComboBox.setToolTipText("Connection Type");
        connection_type_ComboBox.setMaximumSize(new java.awt.Dimension(60, 25));
        connection_type_ComboBox.setMinimumSize(new java.awt.Dimension(60, 25));
        connection_type_ComboBox.setPreferredSize(new java.awt.Dimension(60, 25));

        stop_macro_Button.setText("Stop");
        stop_macro_Button.setToolTipText("Stop executing macro");
        stop_macro_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_macro_ButtonActionPerformed(evt);
            }
        });

        stop_command_Button.setText("Stop");
        stop_command_Button.setToolTipText("Stop executing command");
        stop_command_Button.setEnabled(false);
        stop_command_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stop_command_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1))
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(toplevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(selecting_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(devicegroup_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(zones_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(audio_video_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(src_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(connection_type_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(command_argument_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stop_command_Button))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(secondlevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(macroComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(stop_macro_Button, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(select_Button)
                    .addComponent(commandButton)
                    .addComponent(macroButton))
                .addGap(73, 73, 73))
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(devicegroup_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(command_argument_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stop_command_Button)
                    .addComponent(commandButton))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(selecting_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(zones_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(audio_video_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(src_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(connection_type_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(select_Button))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(toplevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondlevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(macroComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stop_macro_Button)
                    .addComponent(macroButton))
                .addContainerGap(52, Short.MAX_VALUE))
        );

        output_hw_TabbedPane.addTab("Home", mainPanel);

        deviceclass_ComboBox.setMaximumRowCount(20);
        deviceclass_ComboBox.setModel(deviceclasses_dcbm);
        deviceclass_ComboBox.setToolTipText("Deviceclass");
        deviceclass_ComboBox.setMaximumSize(new java.awt.Dimension(150, 25));
        deviceclass_ComboBox.setMinimumSize(new java.awt.Dimension(150, 25));
        deviceclass_ComboBox.setPreferredSize(new java.awt.Dimension(150, 25));
        deviceclass_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceclass_ComboBoxActionPerformed(evt);
            }
        });

        device_command_ComboBox.setMaximumRowCount(20);
        device_command_ComboBox.setModel(device_commands_dcbm);
        device_command_ComboBox.setToolTipText("command");
        device_command_ComboBox.setMaximumSize(new java.awt.Dimension(150, 25));
        device_command_ComboBox.setMinimumSize(new java.awt.Dimension(150, 25));
        device_command_ComboBox.setPreferredSize(new java.awt.Dimension(150, 25));

        deviceclass_send_Button.setText("Send");
        deviceclass_send_Button.setToolTipText("Send command using selected hardware");
        deviceclass_send_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceclass_send_ButtonActionPerformed(evt);
            }
        });

        no_sends_ComboBox.setMaximumRowCount(20);
        no_sends_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" }));
        no_sends_ComboBox.setToolTipText("The number of times the signal should be sent.");
        no_sends_ComboBox.setMaximumSize(new java.awt.Dimension(60, 27));
        no_sends_ComboBox.setMinimumSize(new java.awt.Dimension(40, 27));
        no_sends_ComboBox.setPreferredSize(new java.awt.Dimension(60, 27));

        output_deviceComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (preprog_ascii)", "IRTrans (web_api)", "IRTrans (udp)" }));
        output_deviceComboBox.setToolTipText("Device to use for IR output");

        deviceclass_stop_Button.setText("Stop");
        deviceclass_stop_Button.setToolTipText("Stop ongoing IR transmission");
        deviceclass_stop_Button.setEnabled(false);
        deviceclass_stop_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceclass_stop_ButtonActionPerformed(evt);
            }
        });

        device_remote_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "*" }));
        device_remote_ComboBox.setToolTipText("Selected remote for the selected device.");
        device_remote_ComboBox.setMaximumSize(new java.awt.Dimension(170, 32767));
        device_remote_ComboBox.setMinimumSize(new java.awt.Dimension(170, 27));
        device_remote_ComboBox.setPreferredSize(new java.awt.Dimension(170, 27));
        device_remote_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                device_remote_ComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout deviceclassesPanelLayout = new javax.swing.GroupLayout(deviceclassesPanel);
        deviceclassesPanel.setLayout(deviceclassesPanelLayout);
        deviceclassesPanelLayout.setHorizontalGroup(
            deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addComponent(deviceclass_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(device_remote_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addComponent(output_deviceComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(no_sends_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addComponent(deviceclass_stop_Button)
                        .addGap(18, 18, 18)
                        .addComponent(deviceclass_send_Button))
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(device_command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(85, 85, 85))
        );
        deviceclassesPanelLayout.setVerticalGroup(
            deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, deviceclassesPanelLayout.createSequentialGroup()
                .addContainerGap(35, Short.MAX_VALUE)
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deviceclass_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(device_remote_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(device_command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(output_deviceComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(no_sends_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deviceclass_stop_Button)
                    .addComponent(deviceclass_send_Button))
                .addGap(81, 81, 81))
        );

        output_hw_TabbedPane.addTab("Device classes", deviceclassesPanel);

        protocol_ComboBox.setMaximumRowCount(20);
        protocol_ComboBox.setModel(new DefaultComboBoxModel(harcutils.sort_unique(protocol.get_protocols())));
        protocol_ComboBox.setToolTipText("Protocol name");
        protocol_ComboBox.setMaximumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setMinimumSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.setPreferredSize(new java.awt.Dimension(100, 25));
        protocol_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_ComboBoxActionPerformed(evt);
            }
        });

        deviceno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        deviceno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceno_TextFieldActionPerformed(evt);
            }
        });
        deviceno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                deviceno_TextFieldFocusLost(evt);
            }
        });

        subdevice_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        subdevice_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        commandno_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        commandno_TextField.setPreferredSize(new java.awt.Dimension(35, 27));
        commandno_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                commandno_TextFieldActionPerformed(evt);
            }
        });
        commandno_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                commandno_TextFieldFocusLost(evt);
            }
        });

        toggle_ComboBox.setModel(new DefaultComboBoxModel(toggletype.values()));
        toggle_ComboBox.setToolTipText("Toggles to generate");
        toggle_ComboBox.setMaximumSize(new java.awt.Dimension(50, 32767));

        protocol_generate_Button.setText("Encode");
        protocol_generate_Button.setToolTipText("Fill Code from upper row protocol description");
        protocol_generate_Button.setEnabled(false);
        protocol_generate_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_generate_ButtonActionPerformed(evt);
            }
        });

        protocol_send_Button.setText("Send");
        protocol_send_Button.setToolTipText("Send code in Code window to GlobalCache selected in Deviceclasses tab.");
        protocol_send_Button.setEnabled(false);
        protocol_send_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_send_ButtonActionPerformed(evt);
            }
        });

        protocol_cooked_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_cooked_TextFieldActionPerformed(evt);
            }
        });
        protocol_cooked_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                protocol_cooked_TextFieldFocusLost(evt);
            }
        });

        jLabel4.setText("Cooked");

        jLabel5.setText("Raw Code");

        protocol_raw_TextArea.setColumns(20);
        protocol_raw_TextArea.setLineWrap(true);
        protocol_raw_TextArea.setRows(5);
        protocol_raw_TextArea.setToolTipText("Raw code, edit if desired");
        protocol_raw_TextArea.setWrapStyleWord(true);
        protocol_raw_TextArea.setMinimumSize(new java.awt.Dimension(240, 17));
        protocol_raw_TextArea.setPreferredSize(new java.awt.Dimension(240, 85));
        protocol_raw_TextArea.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                protocol_raw_TextAreaFocusLost(evt);
            }
        });
        jScrollPane3.setViewportView(protocol_raw_TextArea);

        protocol_decode_Button.setText("Decode");
        protocol_decode_Button.setToolTipText("Send content of Code window(s) to DecodeIR");
        protocol_decode_Button.setEnabled(false);
        protocol_decode_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_decode_ButtonActionPerformed(evt);
            }
        });

        protocol_stop_Button.setText("Stop");
        protocol_stop_Button.setToolTipText("Stop ongoing IR transmission");
        protocol_stop_Button.setEnabled(false);
        protocol_stop_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_stop_ButtonActionPerformed(evt);
            }
        });

        no_sends_protocol_ComboBox.setMaximumRowCount(20);
        no_sends_protocol_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "20", "50", "100" }));
        no_sends_protocol_ComboBox.setToolTipText("Number of times to send IR signal");

        protocol_outputhw_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "GlobalCache", "IRTrans (udp)" }));
        protocol_outputhw_ComboBox.setToolTipText("Device used for when sending");

        protocol_clear_Button.setText("Clear");
        protocol_clear_Button.setToolTipText("Press to clear cooked and raw code text areas");
        protocol_clear_Button.setEnabled(false);
        protocol_clear_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocol_clear_ButtonActionPerformed(evt);
            }
        });

        icf_import_Button.setText("ict...");
        icf_import_Button.setToolTipText("Import file from IR WIdget/IRScope");
        icf_import_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                icf_import_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout protocolsPanelLayout = new javax.swing.GroupLayout(protocolsPanel);
        protocolsPanel.setLayout(protocolsPanelLayout);
        protocolsPanelLayout.setHorizontalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5)
                            .addComponent(icf_import_Button))
                        .addGap(31, 31, 31)
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)
                            .addComponent(protocol_cooked_TextField, javax.swing.GroupLayout.DEFAULT_SIZE, 485, Short.MAX_VALUE)))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(protocol_generate_Button)
                    .addComponent(protocol_decode_Button)
                    .addComponent(protocol_send_Button)
                    .addComponent(protocol_stop_Button)
                    .addComponent(protocol_clear_Button))
                .addContainerGap())
        );
        protocolsPanelLayout.setVerticalGroup(
            protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(protocolsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(protocol_generate_Button)
                    .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(no_sends_protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_outputhw_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(protocol_cooked_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(protocolsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(protocolsPanelLayout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addGap(18, 18, 18)
                                .addComponent(icf_import_Button))))
                    .addGroup(protocolsPanelLayout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(protocol_decode_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_send_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_stop_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(protocol_clear_Button)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        output_hw_TabbedPane.addTab("IR Protocols", protocolsPanel);

        gc_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        gc_address_TextField.setText("192.168.1.70");
        gc_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        gc_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        gc_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_address_TextFieldActionPerformed(evt);
            }
        });

        gc_module_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "2" }));
        gc_module_ComboBox.setToolTipText("GlobalCache IR Module to use");
        gc_module_ComboBox.setMaximumSize(new java.awt.Dimension(40, 27));
        gc_module_ComboBox.setMinimumSize(new java.awt.Dimension(40, 27));
        gc_module_ComboBox.setPreferredSize(new java.awt.Dimension(40, 27));

        gc_connector_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "1", "2", "3" }));
        gc_connector_ComboBox.setToolTipText("GlobalCache IR Connector to use");
        gc_connector_ComboBox.setMaximumSize(new java.awt.Dimension(32767, 27));

        gc_browse_Button.setText("Browse");
        gc_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_browse_ButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Stop IR");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_stop_ir_ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalcache_PanelLayout = new javax.swing.GroupLayout(globalcache_Panel);
        globalcache_Panel.setLayout(globalcache_PanelLayout);
        globalcache_PanelLayout.setHorizontalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gc_browse_Button)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButton1)
                .addContainerGap(274, Short.MAX_VALUE))
        );
        globalcache_PanelLayout.setVerticalGroup(
            globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalcache_PanelLayout.createSequentialGroup()
                .addGap(47, 47, 47)
                .addGroup(globalcache_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(gc_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_module_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_connector_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gc_browse_Button)
                    .addComponent(jButton1))
                .addContainerGap(73, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("GlobalCache", globalcache_Panel);

        irtrans_address_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        irtrans_address_TextField.setText("192.168.1.71");
        irtrans_address_TextField.setToolTipText("IP-Address of GlobalCache to use");
        irtrans_address_TextField.setMinimumSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.setPreferredSize(new java.awt.Dimension(120, 27));
        irtrans_address_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_address_TextFieldActionPerformed(evt);
            }
        });

        irtrans_led_ComboBox.setMaximumRowCount(12);
        irtrans_led_ComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "intern", "extern", "both", "0", "1", "2", "3", "4", "5", "6", "7", "8" }));

        irtrans_browse_Button.setText("Browse");
        irtrans_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                irtrans_browse_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout irtrans_PanelLayout = new javax.swing.GroupLayout(irtrans_Panel);
        irtrans_Panel.setLayout(irtrans_PanelLayout);
        irtrans_PanelLayout.setHorizontalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34)
                .addComponent(irtrans_browse_Button)
                .addContainerGap(337, Short.MAX_VALUE))
        );
        irtrans_PanelLayout.setVerticalGroup(
            irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(irtrans_PanelLayout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(irtrans_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(irtrans_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_led_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(irtrans_browse_Button))
                .addContainerGap(71, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("IRTrans", irtrans_Panel);

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
                        .addContainerGap(173, Short.MAX_VALUE))
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
                        .addContainerGap(209, Short.MAX_VALUE))))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(t10_get_timers_Button)
                    .addComponent(t10_get_status_Button)
                    .addComponent(jLabel13)
                    .addComponent(t10_address_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(t10_browse_Button))
                .addContainerGap())
        );

        outputHWTabbedPane.addTab("EZControl", ezcontrolPanel);

        output_hw_TabbedPane.addTab("Output HW", outputHWTabbedPane);

        decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        decimal_TextField.setText("0");
        decimal_TextField.setToolTipText("Enter decimal number here, then press return.");
        decimal_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        decimal_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decimal_TextFieldActionPerformed(evt);
            }
        });
        decimal_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                decimal_TextFieldFocusLost(evt);
            }
        });

        hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        hex_TextField.setText("0");
        hex_TextField.setToolTipText("Enter hexadecimal number here, then press return.");
        hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        hex_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hex_TextFieldActionPerformed(evt);
            }
        });
        hex_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                hex_TextFieldFocusLost(evt);
            }
        });

        complement_decimal_TextField.setEditable(false);
        complement_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_decimal_TextField.setText("255");
        complement_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        complement_hex_TextField.setEditable(false);
        complement_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        complement_hex_TextField.setText("FF");
        complement_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        complement_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        jLabel6.setText("Decimal");

        jLabel7.setText("Hex");

        jLabel8.setText("Complement");

        jLabel9.setText("Complement");

        jLabel14.setText("Reverse");

        reverse_hex_TextField.setEditable(false);
        reverse_hex_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_hex_TextField.setText("FF");
        reverse_hex_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        reverse_hex_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        reverse_decimal_TextField.setEditable(false);
        reverse_decimal_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        reverse_decimal_TextField.setText("255");
        reverse_decimal_TextField.setPreferredSize(new java.awt.Dimension(100, 27));

        jLabel15.setText("Reverse");

        no_periods_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        no_periods_TextField.setText("1");
        no_periods_TextField.setToolTipText("Number of periods");
        no_periods_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        no_periods_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                no_periods_TextFieldActionPerformed(evt);
            }
        });
        no_periods_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                no_periods_TextFieldFocusLost(evt);
            }
        });

        frequency_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        frequency_TextField.setText("40000");
        frequency_TextField.setToolTipText("Enter modulation frequency here, then press return.");
        frequency_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        frequency_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        frequency_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                frequency_TextFieldActionPerformed(evt);
            }
        });
        frequency_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                frequency_TextFieldFocusLost(evt);
            }
        });

        jLabel22.setText("Frequency (Hz)");

        jLabel23.setText("# Periods");

        time_TextField.setEditable(false);
        time_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        time_TextField.setText("0");
        time_TextField.setToolTipText("Time interval = # periods/frequency");
        time_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        time_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        time_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                time_TextFieldActionPerformed(evt);
            }
        });
        time_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                time_TextFieldFocusLost(evt);
            }
        });

        jLabel24.setText("Time (us)");

        prontocode_TextField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        prontocode_TextField.setText("0");
        prontocode_TextField.setToolTipText("Enter Pronto frequency code here, then press return.");
        prontocode_TextField.setMinimumSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.setPreferredSize(new java.awt.Dimension(100, 27));
        prontocode_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                prontocode_TextFieldActionPerformed(evt);
            }
        });
        prontocode_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                prontocode_TextFieldFocusLost(evt);
            }
        });

        jLabel25.setText("Prontocode");

        period_selection_enable_CheckBox.setSelected(true);
        period_selection_enable_CheckBox.setText("Enable");
        period_selection_enable_CheckBox.setToolTipText("Select periods or time interval");
        period_selection_enable_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                period_selection_enable_CheckBoxActionPerformed(evt);
            }
        });

        jSeparator6.setOrientation(javax.swing.SwingConstants.VERTICAL);

        javax.swing.GroupLayout hexcalcPanelLayout = new javax.swing.GroupLayout(hexcalcPanel);
        hexcalcPanel.setLayout(hexcalcPanelLayout);
        hexcalcPanelLayout.setHorizontalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel8)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel15)
                            .addComponent(jLabel9)
                            .addComponent(jLabel7)
                            .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel22))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel25)
                            .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(period_selection_enable_CheckBox))
                    .addComponent(jLabel23)
                    .addComponent(jLabel24)
                    .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(189, Short.MAX_VALUE))
        );
        hexcalcPanelLayout.setVerticalGroup(
            hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, hexcalcPanelLayout.createSequentialGroup()
                .addGap(3, 3, 3)
                .addComponent(jSeparator6, javax.swing.GroupLayout.DEFAULT_SIZE, 227, Short.MAX_VALUE))
            .addGroup(hexcalcPanelLayout.createSequentialGroup()
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(jLabel22)
                    .addComponent(jLabel25))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(frequency_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(prontocode_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel8)
                        .addComponent(jLabel9))
                    .addComponent(jLabel23))
                .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(no_periods_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(period_selection_enable_CheckBox))
                        .addGap(5, 5, 5)
                        .addComponent(jLabel24)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(time_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(75, 75, 75))
                    .addGroup(hexcalcPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(complement_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(complement_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(hexcalcPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(reverse_decimal_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(reverse_hex_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addContainerGap())))
        );

        output_hw_TabbedPane.addTab("IRcalc", hexcalcPanel);

        jLabel16.setText("Home");

        homeconf_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        homeconf_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeconf_TextFieldActionPerformed(evt);
            }
        });
        homeconf_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                homeconf_TextFieldFocusLost(evt);
            }
        });

        home_select_Button.setText("...");
        home_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                home_select_ButtonActionPerformed(evt);
            }
        });

        home_browse_Button.setText("Browse");
        home_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                home_browse_ButtonActionPerformed(evt);
            }
        });

        home_load_Button.setText("Load");
        home_load_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                home_load_ButtonActionPerformed(evt);
            }
        });

        jLabel17.setText("Macro");
        jLabel17.setEnabled(false);

        macro_TextField.setEnabled(false);
        macro_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        macro_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        macro_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        macro_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macro_TextFieldActionPerformed(evt);
            }
        });
        macro_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                macro_TextFieldFocusLost(evt);
            }
        });

        browser_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        browser_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        browser_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        browser_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browser_TextFieldActionPerformed(evt);
            }
        });
        browser_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                browser_TextFieldFocusLost(evt);
            }
        });

        aliases_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        aliases_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        aliases_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        aliases_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aliases_TextFieldActionPerformed(evt);
            }
        });
        aliases_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                aliases_TextFieldFocusLost(evt);
            }
        });

        jLabel10.setText("Aliases");

        jLabel18.setText("Browser");

        macro_select_Button.setText("...");
        macro_select_Button.setEnabled(false);
        macro_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macro_select_ButtonActionPerformed(evt);
            }
        });

        aliases_select_Button.setText("...");
        aliases_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aliases_select_ButtonActionPerformed(evt);
            }
        });

        browser_select_Button.setText("...");
        browser_select_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browser_select_ButtonActionPerformed(evt);
            }
        });

        macro_browse_Button.setText("Browse");
        macro_browse_Button.setEnabled(false);
        macro_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macro_browse_ButtonActionPerformed(evt);
            }
        });

        alias_browse_Button.setText("Browse");
        alias_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                alias_browse_ButtonActionPerformed(evt);
            }
        });

        macro_load_Button.setText("Load");
        macro_load_Button.setEnabled(false);
        macro_load_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macro_load_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout general_PanelLayout = new javax.swing.GroupLayout(general_Panel);
        general_Panel.setLayout(general_PanelLayout);
        general_PanelLayout.setHorizontalGroup(
            general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel16)
                        .addGap(15, 15, 15))
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel17)
                            .addComponent(jLabel18))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addComponent(homeconf_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_select_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_browse_Button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(home_load_Button))
                    .addGroup(general_PanelLayout.createSequentialGroup()
                        .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(aliases_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(macro_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(browser_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(general_PanelLayout.createSequentialGroup()
                                .addComponent(macro_select_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(macro_browse_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(macro_load_Button))
                            .addGroup(general_PanelLayout.createSequentialGroup()
                                .addComponent(aliases_select_Button)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(alias_browse_Button))
                            .addComponent(browser_select_Button))))
                .addContainerGap(133, Short.MAX_VALUE))
        );
        general_PanelLayout.setVerticalGroup(
            general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(homeconf_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(home_select_Button)
                    .addComponent(home_browse_Button)
                    .addComponent(home_load_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(macro_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel17)
                    .addComponent(macro_select_Button)
                    .addComponent(macro_browse_Button)
                    .addComponent(macro_load_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(aliases_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(aliases_select_Button)
                    .addComponent(alias_browse_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(general_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(browser_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel18)
                    .addComponent(browser_select_Button))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        optsTabbedPane.addTab("General", general_Panel);

        jLabel19.setText("Exportdir");

        exportdir_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.setPreferredSize(new java.awt.Dimension(300, 27));
        exportdir_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_TextFieldActionPerformed(evt);
            }
        });
        exportdir_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                exportdir_TextFieldFocusLost(evt);
            }
        });

        exportdir_browse_Button.setText("...");
        exportdir_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportdir_browse_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout general_export_opts_PanelLayout = new javax.swing.GroupLayout(general_export_opts_Panel);
        general_export_opts_Panel.setLayout(general_export_opts_PanelLayout);
        general_export_opts_PanelLayout.setHorizontalGroup(
            general_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_export_opts_PanelLayout.createSequentialGroup()
                .addComponent(jLabel19)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(exportdir_browse_Button)
                .addContainerGap(233, Short.MAX_VALUE))
        );
        general_export_opts_PanelLayout.setVerticalGroup(
            general_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(general_export_opts_PanelLayout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addGroup(general_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel19)
                    .addComponent(exportdir_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportdir_browse_Button))
                .addContainerGap(54, Short.MAX_VALUE))
        );

        exportopts_TabbedPane.addTab("General", general_export_opts_Panel);

        ccf_export_prontomodel_ComboBox.setMaximumRowCount(14);
        ccf_export_prontomodel_ComboBox.setModel(new DefaultComboBoxModel(prontomodelnames));
        ccf_export_prontomodel_ComboBox.setToolTipText("Pronto Model");
        ccf_export_prontomodel_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccf_export_prontomodel_ComboBoxActionPerformed(evt);
            }
        });

        ccf_export_raw_CheckBox.setSelected(true);
        ccf_export_raw_CheckBox.setText("Raw Codes");
        ccf_export_raw_CheckBox.setToolTipText("Prohibit cooked codes in CCF export");

        ccf_export_screenwidth_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_screenwidth_TextField.setText("240");
        ccf_export_screenwidth_TextField.setToolTipText("Screen width (pixels)");
        ccf_export_screenwidth_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenwidth_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenwidth_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        ccf_export_screenheight_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_screenheight_TextField.setText("220");
        ccf_export_screenheight_TextField.setToolTipText("Screen height (pixels)");
        ccf_export_screenheight_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenheight_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        ccf_export_screenheight_TextField.setPreferredSize(new java.awt.Dimension(50, 27));

        ccf_export_buttonwidth_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_buttonwidth_TextField.setText("60");
        ccf_export_buttonwidth_TextField.setToolTipText("Button width (pixels)");
        ccf_export_buttonwidth_TextField.setMaximumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonwidth_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonwidth_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        ccf_export_buttonheight_TextField.setHorizontalAlignment(javax.swing.JTextField.TRAILING);
        ccf_export_buttonheight_TextField.setText("30");
        ccf_export_buttonheight_TextField.setToolTipText("Button height (pixels)");
        ccf_export_buttonheight_TextField.setMaximumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonheight_TextField.setMinimumSize(new java.awt.Dimension(35, 27));
        ccf_export_buttonheight_TextField.setPreferredSize(new java.awt.Dimension(35, 27));

        ccf_export_export_Button.setText("Export");
        ccf_export_export_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccf_export_export_ButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout ccf_export_opts_PanelLayout = new javax.swing.GroupLayout(ccf_export_opts_Panel);
        ccf_export_opts_Panel.setLayout(ccf_export_opts_PanelLayout);
        ccf_export_opts_PanelLayout.setHorizontalGroup(
            ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccf_export_opts_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(ccf_export_prontomodel_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_screenwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_screenheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(ccf_export_buttonwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ccf_export_buttonheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ccf_export_raw_CheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 212, Short.MAX_VALUE)
                .addComponent(ccf_export_export_Button)
                .addContainerGap())
        );
        ccf_export_opts_PanelLayout.setVerticalGroup(
            ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ccf_export_opts_PanelLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(ccf_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ccf_export_prontomodel_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_screenwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_screenheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_buttonwidth_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_buttonheight_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ccf_export_raw_CheckBox)
                    .addComponent(ccf_export_export_Button))
                .addContainerGap(60, Short.MAX_VALUE))
        );

        exportopts_TabbedPane.addTab("CCF", ccf_export_opts_Panel);

        jLabel12.setText("Remote");

        rmdu_button_rules_TextField.setToolTipText("Path to file with rules mapping commands to buttons.");
        rmdu_button_rules_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        rmdu_button_rules_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        rmdu_button_rules_TextField.setPreferredSize(new java.awt.Dimension(300, 27));

        jLabel20.setText("Keymap Rules");

        keymap_rules_browse_Button.setText("...");
        keymap_rules_browse_Button.setToolTipText("Select rules file");
        keymap_rules_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keymap_rules_browse_ButtonActionPerformed(evt);
            }
        });

        rdf_ComboBox.setModel(new DefaultComboBoxModel(button_remotenames));
        rdf_ComboBox.setSelectedIndex(default_rmdu_export_remoteindex < button_remotenames.length ? default_rmdu_export_remoteindex : 0);
        rdf_ComboBox.setToolTipText("Rdf file for the desired JP1 remote");
        rdf_ComboBox.setMaximumSize(new java.awt.Dimension(300, 32767));
        rdf_ComboBox.setMinimumSize(new java.awt.Dimension(300, 27));
        rdf_ComboBox.setPreferredSize(new java.awt.Dimension(300, 27));

        remotemaster_home_TextField.setToolTipText("Path to RemoteMaster's directory");
        remotemaster_home_TextField.setMaximumSize(new java.awt.Dimension(300, 27));
        remotemaster_home_TextField.setMinimumSize(new java.awt.Dimension(300, 27));
        remotemaster_home_TextField.setPreferredSize(new java.awt.Dimension(300, 27));

        remotemaster_home_browse_Button.setText("...");
        remotemaster_home_browse_Button.setToolTipText("Select RemoteMaster directory");
        remotemaster_home_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remotemaster_home_browse_ButtonActionPerformed(evt);
            }
        });

        jLabel21.setText("RM Home");

        javax.swing.GroupLayout rmdu_export_opts_PanelLayout = new javax.swing.GroupLayout(rmdu_export_opts_Panel);
        rmdu_export_opts_Panel.setLayout(rmdu_export_opts_PanelLayout);
        rmdu_export_opts_PanelLayout.setHorizontalGroup(
            rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rmdu_export_opts_PanelLayout.createSequentialGroup()
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel20)
                    .addComponent(jLabel12)
                    .addComponent(jLabel21))
                .addGap(24, 24, 24)
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(rmdu_button_rules_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(remotemaster_home_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(rdf_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(keymap_rules_browse_Button)
                    .addComponent(remotemaster_home_browse_Button))
                .addContainerGap(194, Short.MAX_VALUE))
        );
        rmdu_export_opts_PanelLayout.setVerticalGroup(
            rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(rmdu_export_opts_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rdf_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel12))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rmdu_button_rules_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel20)
                    .addComponent(keymap_rules_browse_Button))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(rmdu_export_opts_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(remotemaster_home_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(remotemaster_home_browse_Button))
                    .addComponent(jLabel21))
                .addContainerGap())
        );

        exportopts_TabbedPane.addTab("RMDU", rmdu_export_opts_Panel);

        optsTabbedPane.addTab("Exportopts", exportopts_TabbedPane);

        jLabel11.setText("Debugcode");

        debug_TextField.setText("0");
        debug_TextField.setMaximumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setMinimumSize(new java.awt.Dimension(50, 27));
        debug_TextField.setPreferredSize(new java.awt.Dimension(50, 27));
        debug_TextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                debug_TextFieldActionPerformed(evt);
            }
        });
        debug_TextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                debug_TextFieldFocusLost(evt);
            }
        });

        verbose_CheckBox.setText("Verbose");
        verbose_CheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                verbose_CheckBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout debug_PanelLayout = new javax.swing.GroupLayout(debug_Panel);
        debug_Panel.setLayout(debug_PanelLayout);
        debug_PanelLayout.setHorizontalGroup(
            debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debug_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(debug_PanelLayout.createSequentialGroup()
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(verbose_CheckBox))
                .addContainerGap(502, Short.MAX_VALUE))
        );
        debug_PanelLayout.setVerticalGroup(
            debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(debug_PanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(debug_PanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(debug_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(verbose_CheckBox)
                .addContainerGap(74, Short.MAX_VALUE))
        );

        optsTabbedPane.addTab("Debug", debug_Panel);

        output_hw_TabbedPane.addTab("Options", optsTabbedPane);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save properties");
        saveMenuItem.setToolTipText("Save properites");
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setMnemonic('A');
        saveAsMenuItem.setText("Save properties as ...");
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(jSeparator4);

        consoletext_save_MenuItem.setMnemonic('c');
        consoletext_save_MenuItem.setText("Save console text as...");
        consoletext_save_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                consoletext_save_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(consoletext_save_MenuItem);
        fileMenu.add(jSeparator1);

        export_device_MenuItem.setText("Export XML (deviceclass)");
        export_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                export_device_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(export_device_MenuItem);

        export_all_MenuItem.setText("Export XML (all)");
        export_all_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                export_all_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(export_all_MenuItem);

        lirc_export_device_MenuItem.setText("Export LIRC (deviceclass)");
        lirc_export_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lirc_export_device_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(lirc_export_device_MenuItem);

        lirc_export_all_MenuItem.setText("Export LIRC (all)");
        lirc_export_all_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lirc_export_all_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(lirc_export_all_MenuItem);

        lirc_export_server_MenuItem.setText("Export LIRC Server config.");
        lirc_export_server_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                lirc_export_server_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(lirc_export_server_MenuItem);

        rem_export_device_MenuItem.setText("Export IRTrans (deviceclass)");
        rem_export_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rem_export_device_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(rem_export_device_MenuItem);

        rem_export_all_MenuItem.setText("Export IRTrans (all)");
        rem_export_all_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rem_export_all_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(rem_export_all_MenuItem);

        ccf_export_MenuItem.setText("Export CCF (deviceclass)");
        ccf_export_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ccf_export_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(ccf_export_MenuItem);

        rmdu_export_MenuItem.setText("Export RMDU (deviceclass)");
        rmdu_export_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rmdu_export_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(rmdu_export_MenuItem);
        fileMenu.add(jSeparator2);

        import_ccf_MenuItem.setText("Import CCF...");
        import_ccf_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_ccf_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(import_ccf_MenuItem);

        import_rmdu_MenuItem.setText("Import RMDU...");
        import_rmdu_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                import_rmdu_MenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(import_rmdu_MenuItem);
        fileMenu.add(jSeparator5);

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

        enable_devicegroups_CheckBoxMenuItem.setText("Enable Device Groups");
        enable_devicegroups_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_devicegroups_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(enable_devicegroups_CheckBoxMenuItem);

        enable_macro_folders_CheckBoxMenuItem.setMnemonic('C');
        enable_macro_folders_CheckBoxMenuItem.setText("Enable Macro Folders");
        enable_macro_folders_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enable_macro_folders_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(enable_macro_folders_CheckBoxMenuItem);

        immediate_execution_macros_CheckBoxMenuItem.setMnemonic('m');
        immediate_execution_macros_CheckBoxMenuItem.setText("Immediate execution of macros");
        jMenu1.add(immediate_execution_macros_CheckBoxMenuItem);

        immediate_execution_commands_CheckBoxMenuItem.setMnemonic('c');
        immediate_execution_commands_CheckBoxMenuItem.setText("Immediate execution of (argumentless) commands");
        jMenu1.add(immediate_execution_commands_CheckBoxMenuItem);

        sort_macros_CheckBoxMenuItem.setMnemonic('S');
        sort_macros_CheckBoxMenuItem.setText("Sort Macros");
        sort_macros_CheckBoxMenuItem.setToolTipText("If selected, entries in the macro menu will be alphabetically sorted.");
        sort_macros_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sort_macros_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(sort_macros_CheckBoxMenuItem);

        sort_devices_CheckBoxMenuItem.setText("Sort Devices");
        sort_devices_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sort_devices_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(sort_devices_CheckBoxMenuItem);

        sort_commands_CheckBoxMenuItem.setSelected(true);
        sort_commands_CheckBoxMenuItem.setText("Sort Commands");
        sort_commands_CheckBoxMenuItem.setEnabled(false);
        sort_commands_CheckBoxMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sort_commands_CheckBoxMenuItemActionPerformed(evt);
            }
        });
        jMenu1.add(sort_commands_CheckBoxMenuItem);

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

        jMenu2.setMnemonic('M');
        jMenu2.setText("Misc.");

        clear_console_MenuItem.setText("Clear console");
        clear_console_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_console_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(clear_console_MenuItem);

        browse_device_MenuItem.setText("Browse selected device");
        browse_device_MenuItem.setToolTipText("Point the browser to this device");
        browse_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browse_device_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(browse_device_MenuItem);
        jMenu2.add(jSeparator3);

        launch_remotemaster_MenuItem.setText("Launch RemoteMaster");
        launch_remotemaster_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launch_remotemaster_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(launch_remotemaster_MenuItem);

        launch_rmir_MenuItem.setText("Launch RMIR");
        launch_rmir_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launch_rmir_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(launch_rmir_MenuItem);

        launch_tonto_MenuItem.setText("Launch Tonto");
        launch_tonto_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                launch_tonto_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(launch_tonto_MenuItem);

        menuBar.add(jMenu2);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        contentMenuItem.setMnemonic('C');
        contentMenuItem.setText("Content...");
        contentMenuItem.setToolTipText("Extensive documentation");
        contentMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contentMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(contentMenuItem);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About...");
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
            .addComponent(output_hw_TabbedPane, 0, 0, Short.MAX_VALUE)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 657, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(output_hw_TabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 217, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class macro_thread extends Thread {
        private String macroname;
        public macro_thread(String name) {
            super("macro_thread");
            macroname = name;
        }

        public String get_name() {
            return macroname;
        }

        @Override
        public void run() {
            String result = null;
            //String cmd = (String) macros_dcbm.getSelectedItem();
            System.err.println(cmd_formatter.format("harcmacros." + macroname + "()"));
            //try {
                result = engine.eval("harcmacros." + macroname + "()");
            //} catch (non_existing_command_exception e) {
                // This should not happen
            //    System.err.println("*** Non existing macro " + e.getMessage());
            //} catch (InterruptedException e) {
            //    System.err.println("*** Interrupted ***" + e.getMessage());
            //}
            if (result == null)
                System.err.println("** Failed **");
            else if (!result.equals(""))
                System.err.println(formatter.format(result));

            macroButton.setEnabled(engine != null);
            stop_macro_Button.setEnabled(false);
            the_macro_thread = null;
        }
    }

    private class command_thread extends Thread {
        private String device;
        private command_t cmd;
        private String[] args;

        public command_thread(String device, command_t cmd, String[] args) {
            super("command_thread");
            this.device = device;
            this.cmd = cmd;
            this.args = args;
        }

        @Override
        public void run() {
            String result = null;
            //String cmd = (String) macros_dcbm.getSelectedItem();
            //System.err.println(cmd_formatter.format(macroname));
            try {
                result = hm.do_command(device, cmd, args, commandtype_t.any, 1, toggletype.no_toggle, false);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted ***" + e.getMessage());
            }
            if (result == null)
                System.err.println("** Failed **");
            else if (!result.equals(""))
                System.err.println(formatter.format(result));

            commandButton.setEnabled(true);
            stop_command_Button.setEnabled(false);
            the_command_thread = null;
        }
    }

    private class globalcache_thread extends Thread {
        private ir_code code;
        private int module;
        private int connector;
        private int count;
        private JButton start_button;
        private JButton stop_button;

        public globalcache_thread(ir_code code, int module, int connector, int count,
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

    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        try {
            harcprops.get_instance().save();
            System.err.println("Property file written");
        } catch (Exception e) {
            warning("Problems saving properties: " + e.getMessage());
            return;
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            //JFrame mainFrame = gui_main.getApplication().getMainFrame();
            aboutBox = new about_popup(this/*mainFrame*/, false);
            aboutBox.setLocationRelativeTo(/*mainFrame*/this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contentMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contentMenuItemActionPerformed
        browse(harcprops.get_instance().get_helpfilename());
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = select_file("Select properties save", "xml", "XML Files", true, null).getAbsolutePath();
            harcprops.get_instance().save(props);
        } catch (IOException e) {
            System.err.println(e);
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void macroComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macroComboBoxActionPerformed
        //macroComboBox.setToolTipText(engine.describe_macro((String) macros_dcbm.getSelectedItem()));
        if (immediate_execution_macros_CheckBoxMenuItem.isSelected())
            macroButtonActionPerformed(null);
}//GEN-LAST:event_macroComboBoxActionPerformed

    private void macroButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macroButtonActionPerformed
        if (the_macro_thread != null)
            System.err.println("Internal error: the_macro_thread != null");
        
        the_macro_thread = new macro_thread((String) macros_dcbm.getSelectedItem());
        macroButton.setEnabled(false);
        stop_macro_Button.setEnabled(true);
        the_macro_thread.start();
    }//GEN-LAST:event_macroButtonActionPerformed

    private void update_zone_menu() {
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String[] zones = hm.get_zones(device);
        zones_dcbm = new DefaultComboBoxModel((zones != null && zones.length > 0) ? zones : new String[] {"  "});
        zones_ComboBox.setModel(zones_dcbm);
        zones_ComboBox.setEnabled(zones != null && zones.length > 0);
        update_src_device_menu();
    }

    private void update_src_device_menu() {
        String zone = (String) zones_dcbm.getSelectedItem();
        if (zone.equals("  "))
            zone = null;
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String[] src_devices = hm.get_sources(device, zone);
        src_devices_dcbm = new DefaultComboBoxModel(src_devices != null ? src_devices : (new String[]{"--"}));
        src_device_ComboBox.setModel(src_devices_dcbm);
        update_connection_types_menu();
    }

    private void update_devicegroup_menu() {
        update_device_menu();
    }

    private void update_command_menu() {
        String[] commands = null;

        commands = hm.get_commands((String) devices_dcbm.getSelectedItem(), commandtype_t.any);
        if (commands == null)
            commands = new String[]{ dummy_no_selection };
        if (sort_commands_CheckBoxMenuItem.isSelected())
            java.util.Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
        commands_dcbm = new DefaultComboBoxModel(commands);
        command_ComboBox.setModel(commands_dcbm);
        String device = (String) devices_dcbm.getSelectedItem();
        String cmdname = (String) commands_dcbm.getSelectedItem();
        // Not working, why??
        //commandButton.setEnabled(hm.get_arguments(device, command_t.parse(cmdname), commandtype_t.any)  < 2);
        command_argument_TextField.setEnabled(hm.get_arguments(device, command_t.parse(cmdname), commandtype_t.any) == 1);
    }

    private void update_device_menu() {
        //warning("Update_devices menu");
        String[] devices = null;

        devices = enable_devicegroups_CheckBoxMenuItem.isSelected()
                ? hm.get_devices((String) devicegroups_dcbm.getSelectedItem())
                : hm.get_devices();

        if (sort_devices_CheckBoxMenuItem.isSelected())
            java.util.Arrays.sort(devices, String.CASE_INSENSITIVE_ORDER);

        devices_dcbm = new DefaultComboBoxModel(devices);
        device_ComboBox.setModel(devices_dcbm);
        //device_ComboBox.setToolTipText(...);
        update_command_menu();
    }

    /*private void update_macro_menu() {
        if (engine != null) {
            String[] macros = null;
            if (enable_macro_folders_CheckBoxMenuItem.isSelected())
                macros = engine.get_macros(
                        (String) ((secondlevel_macrofolders_dcbm.getSize() > 1) ? secondlevel_macrofolders_dcbm.getSelectedItem()
                        : toplevel_macrofolders_dcbm.getSelectedItem()));
            else
                macros = engine.get_macros(false);

            if (macros == null || macros.length == 0) {
                macros = new String[]{dummy_no_selection};
                macroComboBox.setEnabled(false);
                macroComboBox.setToolTipText(null);
            } else {
                if (sort_macros_CheckBoxMenuItem.isSelected())
                    java.util.Arrays.sort(macros, String.CASE_INSENSITIVE_ORDER);

                macroComboBox.setEnabled(true);
                macroComboBox.setToolTipText(engine.describe_macro((String) macros_dcbm.getSelectedItem()));
            }
            macros_dcbm = new DefaultComboBoxModel(macros);
            macroComboBox.setModel(macros_dcbm);

            toplevel_macrofolders_ComboBox.setEnabled(enable_macro_folders_CheckBoxMenuItem.isSelected());
            secondlevel_macrofolders_ComboBox.setEnabled(enable_macro_folders_CheckBoxMenuItem.isSelected());
        } else {
            macroButton.setEnabled(false);
            macroComboBox.setEnabled(false);
            macroComboBox.setToolTipText("Macro file not found or erroneous.");
            toplevel_macrofolders_ComboBox.setEnabled(false);
            secondlevel_macrofolders_ComboBox.setEnabled(false);
        }
    }
*/
    private void sort_macros_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sort_macros_CheckBoxMenuItemActionPerformed
        //update_macro_menu();
    }//GEN-LAST:event_sort_macros_CheckBoxMenuItemActionPerformed

    private void update_verbosity() {
        userprefs.get_instance().set_verbose(verbose);
        gc.set_verbosity(verbose);
        irt.set_verbosity(verbose);
        verbose_CheckBoxMenuItem.setSelected(verbose);
        verbose_CheckBox.setSelected(verbose);
    }

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        verbose = verbose_CheckBoxMenuItem.isSelected();
        update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed

    private void toplevel_macrofolders_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toplevel_macrofolders_ComboBoxActionPerformed
 /*       toplevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String) toplevel_macrofolders_dcbm.getSelectedItem()));
        String[] secondlevel_macrofolders = engine.get_folders((String) toplevel_macrofolders_ComboBox.getSelectedItem(), 1);
        if (secondlevel_macrofolders != null && secondlevel_macrofolders.length > 0) {
            secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(secondlevel_macrofolders);
            secondlevel_macrofolders_ComboBox.setModel(secondlevel_macrofolders_dcbm);
            secondlevel_macrofolders_ComboBox.setEnabled(true);
            secondlevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String) secondlevel_macrofolders_dcbm.getSelectedItem()));
        } else {
            secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(new String[]{dummy_no_selection});
            secondlevel_macrofolders_ComboBox.setModel(secondlevel_macrofolders_dcbm);
            secondlevel_macrofolders_ComboBox.setEnabled(false);
            secondlevel_macrofolders_ComboBox.setToolTipText(null);
        }
        update_macro_menu();
  */
}//GEN-LAST:event_toplevel_macrofolders_ComboBoxActionPerformed

    private void secondlevel_macrofolders_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondlevel_macrofolders_ComboBoxActionPerformed
        //secondlevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String) secondlevel_macrofolders_dcbm.getSelectedItem()));
        //update_macro_menu();
}//GEN-LAST:event_secondlevel_macrofolders_ComboBoxActionPerformed

    private void enable_macro_folders_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_macro_folders_CheckBoxMenuItemActionPerformed
        //update_macro_menu();
}//GEN-LAST:event_enable_macro_folders_CheckBoxMenuItemActionPerformed

    private void enable_devicegroups_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_devicegroups_CheckBoxMenuItemActionPerformed
        update_device_menu();
        devicegroup_ComboBox.setEnabled(enable_devicegroups_CheckBoxMenuItem.isSelected());
}//GEN-LAST:event_enable_devicegroups_CheckBoxMenuItemActionPerformed

    private void device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_device_ComboBoxActionPerformed
        update_command_menu();
        browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), commandtype_t.www, command_t.browse));
    }//GEN-LAST:event_device_ComboBoxActionPerformed

    private void sort_devices_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sort_devices_CheckBoxMenuItemActionPerformed
        update_device_menu();
    }//GEN-LAST:event_sort_devices_CheckBoxMenuItemActionPerformed

    private void devicegroup_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_devicegroup_ComboBoxActionPerformed
        update_devicegroup_menu();
    }//GEN-LAST:event_devicegroup_ComboBoxActionPerformed

    private void command_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_command_ComboBoxActionPerformed
        String device = (String) devices_dcbm.getSelectedItem();
        String cmdname = (String) commands_dcbm.getSelectedItem();
        command_argument_TextField.setText(null);
        command_argument_TextField.setEnabled(hm.get_arguments(device, command_t.parse(cmdname), commandtype_t.any) == 1);
    }//GEN-LAST:event_command_ComboBoxActionPerformed

    private void update_device_remotes_menu() {
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        try {
            device dvc = new device(dev);
            String[] remotes = dvc.get_remotenames();
            //command_t[] commands = dvc.get_commands(commandtype_t.ir);
            java.util.Arrays.sort(remotes);
            device_remotes_dcbm = new DefaultComboBoxModel(remotes);
            device_remote_ComboBox.setModel(device_remotes_dcbm);
            update_device_commands_menu();
        } catch (IOException e) {
            System.err.println(e.getMessage());
       } catch (SAXParseException e) {
            System.err.println(e.getMessage());
       } catch (SAXException e) {
            System.err.println(e.getMessage());
       }
    }

    private void update_device_commands_menu() {
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        String remote = device_remotes_dcbm != null ? (String) device_remotes_dcbm.getSelectedItem() :
            null;
        try {
            device dvc = new device(dev);
            command_t[] commands = dvc.get_commands(commandtype_t.ir, remote);
            if (commands == null)
                return;
            java.util.Arrays.sort(commands);
            device_commands_dcbm = new DefaultComboBoxModel(commands);
            device_command_ComboBox.setModel(device_commands_dcbm);
        } catch (IOException e) {
            System.err.println(e.getMessage());
       } catch (SAXParseException e) {
            System.err.println(e.getMessage());
       } catch (SAXException e) {
            System.err.println(e.getMessage());
       }
    }

    private void update_connection_types_menu() {
        connectiontype[] con_types = hm.get_connection_types((String) selecting_devices_dcbm.getSelectedItem(),
                (String) src_devices_dcbm.getSelectedItem());
        connection_types_dcbm =new DefaultComboBoxModel((con_types != null && con_types.length > 0)
                ? con_types : new String[]{ "    "});
        connection_type_ComboBox.setModel(connection_types_dcbm);
        connection_type_ComboBox.setEnabled(con_types != null && con_types.length > 1);
    }

    private void selecting_device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selecting_device_ComboBoxActionPerformed
        update_zone_menu();
        update_connection_types_menu();
        audio_video_ComboBox.setEnabled(hm.has_av_only((String) selecting_devices_dcbm.getSelectedItem()));
}//GEN-LAST:event_selecting_device_ComboBoxActionPerformed

    private void src_device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_src_device_ComboBoxActionPerformed
        update_connection_types_menu();
    }//GEN-LAST:event_src_device_ComboBoxActionPerformed

    private void commandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandButtonActionPerformed
        String result = null;
        String cmd_name = (String) commands_dcbm.getSelectedItem();
        command_t cmd = command_t.parse(cmd_name);
        String device = (String) devices_dcbm.getSelectedItem();
        System.err.println(cmd_formatter.format(device + " " + cmd_name));
        int no_required_args = hm.get_arguments(device, cmd, commandtype_t.any);
        //warning("# args: " + no_required_args);
        String arg0 = command_argument_TextField.getText().trim();
        String[] args = arg0.equals("") ? new String[0] : new String[]{arg0};
        if (args.length < no_required_args) {
            warning("To few arguments to command. Not executed.");
            return;
        } else if (args.length > no_required_args) {
            // Should not happen.
            warning("Excess arguments ignored");
        }

        if (false) {
            try {
                result = hm.do_command(device, cmd, args, commandtype_t.any, 1, toggletype.no_toggle, false);
            } catch (InterruptedException e) {
                System.err.println("Interrupted");
            }
            if (result == null)
                System.err.println("**Failed**");
            else if (!result.equals(""))
                System.err.println(formatter.format(result));
        } else {
            the_command_thread = new command_thread(device, cmd, args);
            commandButton.setEnabled(false);
            stop_command_Button.setEnabled(true);
            the_command_thread.start();
        }
        
    }//GEN-LAST:event_commandButtonActionPerformed

    private void sort_commands_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sort_commands_CheckBoxMenuItemActionPerformed
        update_command_menu();
    }//GEN-LAST:event_sort_commands_CheckBoxMenuItemActionPerformed

    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        (new copy_clipboard_text()).to_clipboard(console_TextArea.getText());
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private void select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_select_ButtonActionPerformed
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String src_device = (String) src_devices_dcbm.getSelectedItem();
        String zone = ((String) zones_dcbm.getSelectedItem()).trim();
        if (zone.startsWith("-") || zone.isEmpty())
            zone = null;
        mediatype mt = (mediatype) audio_video_ComboBox.getSelectedItem();
        System.err.println(cmd_formatter.format("--select " + device + " " + src_device
                + (zone != null ? (" (zone = " + zone + ")") : "")
                + (audio_video_ComboBox.isEnabled() ? (" (" + mt + ")") : "")
                + (connection_type_ComboBox.isEnabled() ? (" (" + (connectiontype) connection_types_dcbm.getSelectedItem() +")") : "")));
        boolean success = false;
        try {
            success = hm.select(device, src_device, commandtype_t.any, zone, mt,
                    connection_types_dcbm.getSize() > 1 ? (connectiontype) connection_types_dcbm.getSelectedItem() : connectiontype.any);
        } catch (InterruptedException e) {
            System.err.println("Interrupted");
        }
        if (!success)
            System.err.println("**Failed**");
    }//GEN-LAST:event_select_ButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        System.out.println("asfkad");//do_exit();
    }//GEN-LAST:event_formWindowClosed

    private void deviceclass_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_ComboBoxActionPerformed
        update_device_remotes_menu();//commands_menu();
    }//GEN-LAST:event_deviceclass_ComboBoxActionPerformed

    private void browse_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browse_device_MenuItemActionPerformed
        try {
            hm.do_command((String) devices_dcbm.getSelectedItem(), command_t.browse, null, commandtype_t.www, 1, toggletype.no_toggle, false);
        } catch (InterruptedException e) {
        }
    }//GEN-LAST:event_browse_device_MenuItemActionPerformed

    private ir_code extract_code() throws NumberFormatException {
        String protocol_name = (String) protocol_ComboBox.getModel().getSelectedItem();
        short devno = ir_code.parse_shortnumber(deviceno_TextField.getText());
        short sub_devno = -1;
        if (protocol.has_subdevice(protocol_name) && !(protocol.subdevice_optional(protocol_name) && subdevice_TextField.getText().trim().equals("")))
            sub_devno = ir_code.parse_shortnumber(subdevice_TextField.getText());
        short cmd_no = ir_code.parse_shortnumber(commandno_TextField.getText());
        toggletype toggle = (toggletype) toggle_ComboBox.getModel().getSelectedItem();
        //System.err.println(protocol_name + devno + " " + sub_devno + " " + cmd_no + toggle);
        return protocol.encode(protocol_name, devno, sub_devno, cmd_no, toggle, false);
    }

    private void protocol_generate_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_generate_ButtonActionPerformed
        try {
            ir_code code = extract_code();
            protocol_raw_TextArea.setText(code.raw_ccf_string());
            protocol_cooked_TextField.setText(code.cooked_ccf_string());
            protocol_decode_Button.setEnabled(true);
            protocol_clear_Button.setEnabled(true);
            protocol_send_Button.setEnabled(true);
        } catch (NumberFormatException e) {
            System.err.println("Parse error " + e.getMessage());
        }
    }//GEN-LAST:event_protocol_generate_ButtonActionPerformed

    private void protocol_decode_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_decode_ButtonActionPerformed
        String code = protocol_cooked_TextField.getText().trim();
        if (code == null || code.equals(""))
            code = protocol_raw_TextArea.getText().trim();

        try {
            com.hifiremote.decodeir.DecodeIR.DecodedSignal[] result = com.hifiremote.decodeir.DecodeIR.decode(code);
            if (result == null || result.length == 0) {
                System.err.println("DecodeIR failed.");
                return;
            }
            for (int i = 0; i < result.length; i++) {
                System.err.println(result[i]);
            }
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Error: DecodeIR not found.");
        } catch (NumberFormatException e) {
            System.err.println("Parse error in string; " + e.getMessage());
        }
}//GEN-LAST:event_protocol_decode_ButtonActionPerformed
    private void update_protocol_parameters() {
        deviceno_TextField.setText(null);
        commandno_TextField.setText(null);
        subdevice_TextField.setText(null);
        toggle_ComboBox.setSelectedItem(toggletype.no_toggle);
        subdevice_TextField.setEnabled(protocol.has_subdevice((String)protocol_ComboBox.getModel().getSelectedItem()));
        toggle_ComboBox.setEnabled(protocol.has_toggle((String)protocol_ComboBox.getModel().getSelectedItem()));
    }

    private void protocol_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_ComboBoxActionPerformed
        update_protocol_parameters();
    }//GEN-LAST:event_protocol_ComboBoxActionPerformed

    private void protocol_send_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_send_ButtonActionPerformed
        int count = Integer.parseInt((String)no_sends_protocol_ComboBox.getModel().getSelectedItem());
        boolean use_globalcache = protocol_outputhw_ComboBox.getSelectedIndex()==0;
        try {
            String ccf = protocol_raw_TextArea.getText();
            if (ccf == null || ccf.trim().equals("")) {
                // Take code from the upper row, ignoring text areas
                ir_code code = extract_code();
                if (use_globalcache) {
                    //gc.send_ir(code, get_gc_module(), get_gc_connector(), count);
                    if (the_globalcache_protocol_thread != null)
                        System.err.println("Warning: the_globalcache_protocol_thread != null");

                    the_globalcache_protocol_thread = new globalcache_thread(code,
                            get_gc_module(), get_gc_connector(), count, protocol_send_Button, protocol_stop_Button);
                    the_globalcache_protocol_thread.start();
                } else
                    irt.send_ir(code, get_irtrans_led(), count);
            } else {
                // Take code from the text area
                if (use_globalcache)
                    gc.send_ir(ccf, get_gc_module(), get_gc_connector(), count);
                else
                    irt.send_ir(ccf, get_irtrans_led(), count);
            }
        } catch (NumberFormatException e) {
            System.err.println("Parse error " + e.getMessage());
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());// FIXME
        }
    }//GEN-LAST:event_protocol_send_ButtonActionPerformed

    private void deviceclass_send_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_send_ButtonActionPerformed
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        command c = null;
        command_t cmd = command_t.invalid;
        String remote = (String) device_remotes_dcbm.getSelectedItem();
        int no_sends = Integer.parseInt((String)no_sends_ComboBox.getModel().getSelectedItem());
        //boolean verbose = verbose_CheckBoxMenuItem.getState();

        try {
            device dvc = new device(dev);
            cmd = (command_t) device_commands_dcbm.getSelectedItem();
            c = dvc.get_command(cmd, commandtype_t.ir, remote);
            //remote = dvc.
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } catch (SAXParseException e) {
            System.err.println(e.getMessage());
        } catch (SAXException e) {
            System.err.println(e.getMessage());
       }

        try {
            if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("GlobalCache")) {
                //gc.send_ir(c.get_ir_code(toggletype.do_toggle, verbose), get_gc_module(), get_gc_connector(), no_sends);
                if (the_globalcache_device_thread != null && the_globalcache_device_thread.isAlive())
                    System.err.println("Internal error: the_globalcache_device thread active!!?");

                the_globalcache_device_thread = new globalcache_thread(c.get_ir_code(toggletype.do_toggle, verbose), get_gc_module(), get_gc_connector(), no_sends, deviceclass_send_Button, deviceclass_stop_Button);
                the_globalcache_device_thread.start();
            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (preprog_ascii)")) {
                //irt.send_flashed_command(remote, cmd, this.get_irtrans_led(), no_sends);
                if (the_irtrans_thread != null && the_irtrans_thread.isAlive())
                    System.err.println("Internal error: the_irtrans_thread active??!");

                the_irtrans_thread = new irtrans_thread(remote, cmd.toString(), this.get_irtrans_led(), no_sends, deviceclass_send_Button, deviceclass_stop_Button);
                the_irtrans_thread.start();
            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (web_api)")) {
                if (no_sends > 1)
                    System.err.println("Warning: Sending only one time");
                String url = irtrans.make_url(irtrans_address_TextField.getText(),
                        remote, cmd, get_irtrans_led());
                if (verbose)
                    System.err.println("Getting URL " + url);
                // TODO: Right now, I dont care to implement status checking...
                (new URL(url)).getContent();
            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (udp)")) {
                irt.send_ir(c.get_ir_code(toggletype.do_toggle, verbose), get_irtrans_led(), no_sends);
            } else {
                System.err.println("Internal error: cannot find output device: " + (String) output_deviceComboBox.getModel().getSelectedItem());
            }
        } catch (UnknownHostException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        //} catch (InterruptedException e) {
        //    System.err.println(e.getMessage());
        }
    }//GEN-LAST:event_deviceclass_send_ButtonActionPerformed

    private void zones_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zones_ComboBoxActionPerformed
        update_src_device_menu();
    }//GEN-LAST:event_zones_ComboBoxActionPerformed

    private void audio_video_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_audio_video_ComboBoxActionPerformed
    
    }//GEN-LAST:event_audio_video_ComboBoxActionPerformed

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

    private void export_all_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_all_MenuItemActionPerformed
        device.export_all_devices(harcprops.get_instance().get_exportdir());
}//GEN-LAST:event_export_all_MenuItemActionPerformed

    private void lirc_export_all_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lirc_export_all_MenuItemActionPerformed
        lirc_export.export_all();
}//GEN-LAST:event_lirc_export_all_MenuItemActionPerformed

    private void rem_export_all_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rem_export_all_MenuItemActionPerformed
        rem_export.export_all();
}//GEN-LAST:event_rem_export_all_MenuItemActionPerformed

    private void ccf_export_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ccf_export_MenuItemActionPerformed
        ccf_export();
    }//GEN-LAST:event_ccf_export_MenuItemActionPerformed

    private void ccf_export() {
        String devname = (String) deviceclasses_dcbm.getSelectedItem();
        com.neuron.app.tonto.ProntoModel prontomodel = com.neuron.app.tonto.ProntoModel.getModelByName((String)ccf_export_prontomodel_ComboBox.getModel().getSelectedItem());
        int buttonwidth = Integer.parseInt(ccf_export_buttonwidth_TextField.getText());
        int buttonheight = Integer.parseInt(ccf_export_buttonheight_TextField.getText());
        int screenwidth = Integer.parseInt(ccf_export_screenwidth_TextField.getText());
        int screenheight = Integer.parseInt(ccf_export_screenheight_TextField.getText());
        String filename = harcprops.get_instance().get_exportdir() + File.separator + devname + ".ccf";

        ccf_export.ccf_exporter(new String[]{devname}, prontomodel,
                ccf_export_raw_CheckBox.isEnabled(),
                buttonwidth, buttonheight, screenwidth, screenheight, filename);
        System.err.println("Exported " + devname + " to " + filename + " for " + prontomodel.toString());
    }

    private void update_hexcalc(int in) {
        int comp = in > 255 ? 65535 : 255;
        int rev = in > 255 ? ((Integer.reverse(in) >> 16) & 65535) : ((Integer.reverse(in) >> 24) & 255);

        complement_decimal_TextField.setText(Integer.toString(comp - in));
        complement_hex_TextField.setText(Integer.toHexString(comp - in));
        reverse_decimal_TextField.setText(Integer.toString(rev));
        reverse_hex_TextField.setText(Integer.toHexString(rev));
    }

    private void hexcalc_silly_number(NumberFormatException e) {
        System.err.println("Parse error " + e.getMessage());
        complement_decimal_TextField.setText("****");
        complement_hex_TextField.setText("****");
        reverse_decimal_TextField.setText("****");
        reverse_hex_TextField.setText("****");
    }

    private void decimal_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decimal_TextFieldActionPerformed
        try {
            int in = Integer.parseInt(decimal_TextField.getText());
            hex_TextField.setText(Integer.toHexString(in));
            update_hexcalc(in);
        } catch (NumberFormatException e) {
            hex_TextField.setText("*");
            hexcalc_silly_number(e);
        }
    }//GEN-LAST:event_decimal_TextFieldActionPerformed

    private void hex_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hex_TextFieldActionPerformed
        try {
            int in = Integer.parseInt(hex_TextField.getText(), 16);
            decimal_TextField.setText(Integer.toString(in));
            update_hexcalc(in);
        } catch (NumberFormatException e) {
            decimal_TextField.setText("*");
            hexcalc_silly_number(e);
        }
    }//GEN-LAST:event_hex_TextFieldActionPerformed

    private void import_ccf_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_ccf_MenuItemActionPerformed
        try {
            String filename = select_file("Select file for ccf import", "ccf", "Pronto CCF files", false, null).getAbsolutePath();
            ccf_import ccfi = new ccf_import(filename, ProntoModel.getModelByName((String) ccf_export_prontomodel_ComboBox.getModel().getSelectedItem()));
            String outfilename = harcprops.get_instance().get_exportdir() + File.separatorChar + "ccf_import.xml";
            ccfi.dump(outfilename);
            System.err.println("CCF Import successfully written to " + outfilename);
        } catch (NullPointerException e) {
        } catch (FileNotFoundException ex) {
            System.err.println(ex);
        }
        
    }//GEN-LAST:event_import_ccf_MenuItemActionPerformed

    private void stop_macro_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_macro_ButtonActionPerformed
        the_macro_thread.interrupt();
        macroButton.setEnabled(true);
        stop_macro_Button.setEnabled(false);       
        System.err.println("************ Execution of macro `"
                + (the_macro_thread != null ? the_macro_thread.get_name() : "") + "' interrupted *************");
        the_macro_thread = null;
    }//GEN-LAST:event_stop_macro_ButtonActionPerformed

    private void export_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_device_MenuItemActionPerformed
        device.export_device(harcprops.get_instance().get_exportdir(), (String) deviceclasses_dcbm.getSelectedItem());
    }//GEN-LAST:event_export_device_MenuItemActionPerformed

    private void lirc_export_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lirc_export_device_MenuItemActionPerformed
        try {
            lirc_export.export(harcprops.get_instance().get_exportdir(), (String) deviceclasses_dcbm.getSelectedItem());
        } catch (SAXParseException ex) {
            System.err.println(ex);
        } catch (SAXException ex) {
            System.err.println(ex);
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }//GEN-LAST:event_lirc_export_device_MenuItemActionPerformed

    private void rem_export_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rem_export_device_MenuItemActionPerformed
        rem_export.export(harcprops.get_instance().get_exportdir(), (String) deviceclasses_dcbm.getSelectedItem());
    }//GEN-LAST:event_rem_export_device_MenuItemActionPerformed

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

    private void gc_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_address_TextFieldActionPerformed
        gc = new globalcache(gc_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
        try {
            gc_module_ComboBox.setEnabled(false);
            gc_connector_ComboBox.setEnabled(false);
            String devs = gc.getdevices();
            String[] dvs = devs.split("\n");
            String[] s = new String[dvs.length];
            for (int i = 0; i < s.length; i++)
                s[i] = dvs[i].endsWith("IR") ? dvs[i].substring(7,8) : null;

            String[] modules = harcutils.nonnulls(s);
            gc_modules_dcbm = new DefaultComboBoxModel(modules != null ? modules : new String[]{"-"});
            gc_module_ComboBox.setModel(gc_modules_dcbm);
            gc_module_ComboBox.setEnabled(modules != null);
            gc_connector_ComboBox.setEnabled(modules != null);
        } catch (UnknownHostException e) {
            gc = null;
            System.err.println(e.getMessage());
        } catch (IOException e) {
            gc = null;
            System.err.println(e.getMessage());
        } catch (InterruptedException e) {
            gc = null;
            System.err.println(e.getMessage());
        }
        deviceclass_send_Button.setEnabled(gc != null);
        protocol_send_Button.setEnabled(gc != null);
}//GEN-LAST:event_gc_address_TextFieldActionPerformed

    private void irtrans_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_address_TextFieldActionPerformed
        irt = new irtrans(irtrans_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
}//GEN-LAST:event_irtrans_address_TextFieldActionPerformed

    private void device_remote_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_device_remote_ComboBoxActionPerformed
        update_device_commands_menu();
    }//GEN-LAST:event_device_remote_ComboBoxActionPerformed

    private void commandno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldActionPerformed

    private void deviceno_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceno_TextFieldActionPerformed
        possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldActionPerformed

    private void commandno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_commandno_TextFieldFocusLost
        possibly_enable_encode_send();
    }//GEN-LAST:event_commandno_TextFieldFocusLost

    private void deviceno_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_deviceno_TextFieldFocusLost
        possibly_enable_encode_send();
    }//GEN-LAST:event_deviceno_TextFieldFocusLost

    private void protocol_clear_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_clear_ButtonActionPerformed
        protocol_raw_TextArea.setText(null);
        protocol_cooked_TextField.setText(null);
        protocol_clear_Button.setEnabled(false);
        protocol_decode_Button.setEnabled(false);
        protocol_send_Button.setEnabled(false);
    }//GEN-LAST:event_protocol_clear_ButtonActionPerformed

    private void protocol_cooked_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_cooked_TextFieldActionPerformed
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_cooked_TextFieldActionPerformed

    private void protocol_cooked_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_protocol_cooked_TextFieldFocusLost
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_cooked_TextFieldFocusLost

    private void protocol_raw_TextAreaFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_protocol_raw_TextAreaFocusLost
        possibly_enable_decode_button();
    }//GEN-LAST:event_protocol_raw_TextAreaFocusLost

    private void hex_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_hex_TextFieldFocusLost
        hex_TextFieldActionPerformed(null);
    }//GEN-LAST:event_hex_TextFieldFocusLost

    private void decimal_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_decimal_TextFieldFocusLost
        decimal_TextFieldActionPerformed(null);
    }//GEN-LAST:event_decimal_TextFieldFocusLost

    private void t10_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_browse_ButtonActionPerformed
        browse(t10_address_TextField.getText());
    }//GEN-LAST:event_t10_browse_ButtonActionPerformed

    private void irtrans_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_irtrans_browse_ButtonActionPerformed
        browse(irtrans_address_TextField.getText());
    }//GEN-LAST:event_irtrans_browse_ButtonActionPerformed

    private void gc_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_browse_ButtonActionPerformed
        browse(gc_address_TextField.getText());
    }//GEN-LAST:event_gc_browse_ButtonActionPerformed

    private void deviceclass_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_stop_ButtonActionPerformed
        try {
            if (the_globalcache_device_thread != null)
                the_globalcache_device_thread.interrupt();
            else if (the_irtrans_thread != null) {
                //System.err.println("$$$$$$$$$$$$$$$$$$$$interruptt");
                the_irtrans_thread.interrupt();
            }

            if (this.output_deviceComboBox.getSelectedIndex() == 0)
                gc.stop_ir(this.get_gc_module(), this.get_gc_connector());
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }//GEN-LAST:event_deviceclass_stop_ButtonActionPerformed

    private void protocol_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocol_stop_ButtonActionPerformed
        try {
            if (the_globalcache_protocol_thread != null)
                this.the_globalcache_protocol_thread.interrupt();
            gc.stop_ir(this.get_gc_module(), this.get_gc_connector());
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } catch (InterruptedException e) {
            System.err.println(e);
        }
    }//GEN-LAST:event_protocol_stop_ButtonActionPerformed

    private void stop_command_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_command_ButtonActionPerformed
        the_command_thread.interrupt();
    }//GEN-LAST:event_stop_command_ButtonActionPerformed

    private void lirc_export_server_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lirc_export_server_MenuItemActionPerformed
        hm.lirc_conf_export();
    }//GEN-LAST:event_lirc_export_server_MenuItemActionPerformed

    private void ccf_export_prontomodel_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ccf_export_prontomodel_ComboBoxActionPerformed
        com.neuron.app.tonto.ProntoModel prontomodel = com.neuron.app.tonto.ProntoModel.getModelByName((String)ccf_export_prontomodel_ComboBox.getModel().getSelectedItem());
        Dimension size = prontomodel.getScreenSize();
        this.ccf_export_screenwidth_TextField.setText(Integer.toString(size.width));
        this.ccf_export_screenheight_TextField.setText(Integer.toString(size.height));
        //System.err.println(prontomodel + " " + size.height + " "+size.width);
    }//GEN-LAST:event_ccf_export_prontomodel_ComboBoxActionPerformed

    private void ccf_export_export_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ccf_export_export_ButtonActionPerformed
        ccf_export();
    }//GEN-LAST:event_ccf_export_export_ButtonActionPerformed

    private void home_select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_home_select_ButtonActionPerformed
        String filename = select_file("Select home file", "xml", "XML Files", false,
                (new File(harcprops.get_instance().get_homefilename())).getAbsoluteFile().getParent()).getAbsolutePath();
        homeconf_TextField.setText(filename);
        harcprops.get_instance().set_homefilename(filename);
}//GEN-LAST:event_home_select_ButtonActionPerformed

    private void home_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_home_browse_ButtonActionPerformed
        browse(homeconf_TextField.getText());
    }//GEN-LAST:event_home_browse_ButtonActionPerformed

    private void debug_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_debug_TextFieldActionPerformed
        debug = Integer.parseInt(debug_TextField.getText());
        userprefs.get_instance().set_debug(debug);
        //hm.set_debug(debug);
        //engine.set_debug(debug);
    }//GEN-LAST:event_debug_TextFieldActionPerformed

    private void home_load_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_home_load_ButtonActionPerformed
        try {
            //hm.load(homeconf_TextField.getText());
            hm = new home(homeconf_TextField.getText());
            // TODO: update GUI state
            System.err.println("Warning: This operation should update the GUI state; this is not yet implemented");
        } catch (IOException ex) {
            System.err.println(ex);
        } catch (SAXParseException ex) {
            System.err.println(ex);
        } catch (SAXException ex) {
            System.err.println(ex);
        }
}//GEN-LAST:event_home_load_ButtonActionPerformed

    private void macro_select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macro_select_ButtonActionPerformed
        /*String filename = select_file("Select macro file", "xml", "XML Files", false,
                (new File(harcprops.get_instance().get_macrofilename())).getAbsoluteFile().getParent()).getAbsolutePath();
        if (filename != null) {
            macro_TextField.setText(filename);
            harcprops.get_instance().set_macrofilename(filename);
        }*/
}//GEN-LAST:event_macro_select_ButtonActionPerformed

    private void aliases_select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aliases_select_ButtonActionPerformed
        String filename = select_file("Select alias file", "xml", "XML Files", false,
                (new File(harcprops.get_instance().get_aliasfilename())).getAbsoluteFile().getParent()).getAbsolutePath();
        if (filename != null) {
            aliases_TextField.setText(filename);
            harcprops.get_instance().set_aliasfilename(filename);
        }
}//GEN-LAST:event_aliases_select_ButtonActionPerformed

    private void browser_select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browser_select_ButtonActionPerformed
        String filename = select_file("Select browser program", "exe", "exe-files", false, null).getAbsolutePath();
        if (filename != null) {
            this.browser_TextField.setText(filename);
            harcprops.get_instance().set_browser(filename);
        }
}//GEN-LAST:event_browser_select_ButtonActionPerformed

    private void macro_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macro_browse_ButtonActionPerformed
        browse(macro_TextField.getText());
}//GEN-LAST:event_macro_browse_ButtonActionPerformed

    private void alias_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_alias_browse_ButtonActionPerformed
        browse(aliases_TextField.getText());
}//GEN-LAST:event_alias_browse_ButtonActionPerformed

    private void macro_load_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macro_load_ButtonActionPerformed
 /*       try {
            engine.load(this.macro_TextField.getText());
            // TODO:...
            System.err.println("Warning: this does not updaste the state of the GUI (not yet implemented).");
        } catch (SAXParseException ex) {
            System.err.println(ex);
        } catch (SAXException ex) {
            System.err.println(ex);
        } catch (IOException e) {
            System.err.println(e);
        }
        // TODO: ...
  * */
}//GEN-LAST:event_macro_load_ButtonActionPerformed

    private void homeconf_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeconf_TextFieldActionPerformed
        harcprops.get_instance().set_homefilename(homeconf_TextField.getText());
    }//GEN-LAST:event_homeconf_TextFieldActionPerformed

    private void browser_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browser_TextFieldActionPerformed
        harcprops.get_instance().set_browser(browser_TextField.getText());
    }//GEN-LAST:event_browser_TextFieldActionPerformed

    private void browser_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_browser_TextFieldFocusLost
        harcprops.get_instance().set_browser(browser_TextField.getText());
    }//GEN-LAST:event_browser_TextFieldFocusLost

    private void homeconf_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_homeconf_TextFieldFocusLost
        harcprops.get_instance().set_homefilename(homeconf_TextField.getText());
    }//GEN-LAST:event_homeconf_TextFieldFocusLost

    private void macro_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macro_TextFieldActionPerformed
        //harcprops.get_instance().set_macrofilename(macro_TextField.getText());
    }//GEN-LAST:event_macro_TextFieldActionPerformed

    private void macro_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_macro_TextFieldFocusLost
        //harcprops.get_instance().set_macrofilename(macro_TextField.getText());
    }//GEN-LAST:event_macro_TextFieldFocusLost

    private void aliases_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aliases_TextFieldActionPerformed
        harcprops.get_instance().set_aliasfilename(this.aliases_TextField.getText());
    }//GEN-LAST:event_aliases_TextFieldActionPerformed

    private void aliases_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_aliases_TextFieldFocusLost
        harcprops.get_instance().set_aliasfilename(this.aliases_TextField.getText());
    }//GEN-LAST:event_aliases_TextFieldFocusLost

    private void debug_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_debug_TextFieldFocusLost
        debug_TextFieldActionPerformed(null);
    }//GEN-LAST:event_debug_TextFieldFocusLost

    private void verbose_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxActionPerformed
        verbose = this.verbose_CheckBox.isSelected();
        update_verbosity();
    }//GEN-LAST:event_verbose_CheckBoxActionPerformed

    private void exportdir_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_TextFieldActionPerformed
        harcprops.get_instance().set_exportdir(exportdir_TextField.getText());
    }//GEN-LAST:event_exportdir_TextFieldActionPerformed

    private void exportdir_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_exportdir_TextFieldFocusLost
        harcprops.get_instance().set_exportdir(exportdir_TextField.getText());
    }//GEN-LAST:event_exportdir_TextFieldFocusLost

    private void exportdir_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportdir_browse_ButtonActionPerformed
        try {
            String dir = select_file("Select export directory", null, null, false, ((new File(harcprops.get_instance().get_exportdir())).getAbsoluteFile().getParent())).getAbsolutePath();
            harcprops.get_instance().set_exportdir(dir);
            exportdir_TextField.setText(dir);
        } catch (NullPointerException e) {
        }
}//GEN-LAST:event_exportdir_browse_ButtonActionPerformed

    private void import_rmdu_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_import_rmdu_MenuItemActionPerformed
        try {
            String filename = select_file("Select file for rmdu import", "rmdu", "RemoteMaster device update files", false, null).getAbsolutePath();
            String outfilename = harcprops.get_instance().get_exportdir() + File.separatorChar + "rmdu_import.xml";
            boolean result = rmdu_import.convert(filename, outfilename);
            System.err.println(result ? ("RMDU Import successfully written to " + outfilename) : "RMDU import failed.");
        } catch (NullPointerException e) {
        }
    }//GEN-LAST:event_import_rmdu_MenuItemActionPerformed

    private void launch_tonto_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launch_tonto_MenuItemActionPerformed
        try {
            com.neuron.app.tonto.Tonto.main(new String[]{});
        } catch (Exception ex) {
            System.err.println(ex);
        }
}//GEN-LAST:event_launch_tonto_MenuItemActionPerformed

    private void launch_remotemaster_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launch_remotemaster_MenuItemActionPerformed
        remotemaster.launch(false, null, last_rmdu_export);
}//GEN-LAST:event_launch_remotemaster_MenuItemActionPerformed

    private void launch_rmir_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_launch_rmir_MenuItemActionPerformed
        remotemaster.launch(true);
    }//GEN-LAST:event_launch_rmir_MenuItemActionPerformed

    private void rmdu_export_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rmdu_export_MenuItemActionPerformed
        try {
            br_export re = new br_export((String) rdf_ComboBox.getModel().getSelectedItem(),
                    (String) deviceclass_ComboBox.getModel().getSelectedItem(),
                    (String) rmdu_button_rules_TextField.getText(),
                    (String) device_remote_ComboBox.getModel().getSelectedItem());
            String filename = harcprops.get_instance().get_exportdir() + File.separator
                    + ((String) rdf_ComboBox.getModel().getSelectedItem()).replaceAll(" .*$", "").toLowerCase()
                    + "_" + deviceclass_ComboBox.getModel().getSelectedItem()
                    + (device_remote_ComboBox.getModel().getSize() > 1 ? ("_" + (String) device_remote_ComboBox.getModel().getSelectedItem()) : "")
                    + "." + br_export.extension;
            re.generate_xml(filename);
            System.err.println("Exported to " + filename);
            last_rmdu_export = filename;
        } catch (IOException ex) {
            System.err.println(ex);
        } catch (SAXParseException ex) {
            System.err.println(ex);
        } catch (SAXException ex) {
            System.err.println(ex);
        }
    }//GEN-LAST:event_rmdu_export_MenuItemActionPerformed

    private void keymap_rules_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keymap_rules_browse_ButtonActionPerformed
        try {
            String filename = select_file("Select rules file", "xml", "XML files", false, (new File(harcprops.get_instance().get_rmdu_button_rules())).getAbsoluteFile().getParent()).getAbsolutePath();
            rmdu_button_rules_TextField.setText(filename);
            harcprops.get_instance().set_rmdu_button_rules(filename);
        } catch (NullPointerException e) {
        }
}//GEN-LAST:event_keymap_rules_browse_ButtonActionPerformed

    private void remotemaster_home_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remotemaster_home_browse_ButtonActionPerformed
        try {
            String dir = this.select_file("Select RemoteMaster directory", null, null, false, ((new File(harcprops.get_instance().get_remotemaster_home())).getAbsoluteFile().getParent())).getAbsolutePath();
            harcprops.get_instance().set_remotemaster_home(dir);
            this.remotemaster_home_TextField.setText(dir);
        } catch (NullPointerException e) {
        }
}//GEN-LAST:event_remotemaster_home_browse_ButtonActionPerformed

    private void update_from_frequency() {
        int freq = Integer.parseInt(frequency_TextField.getText());
        prontocode_TextField.setText(ir_code.ccf_integer(ir_code.get_frequency_code(freq)));
        update_from_frequency(freq);
    }
    
    private void update_from_frequencycode() {
        int freq = ir_code.get_frequency(Integer.parseInt(prontocode_TextField.getText(),16));
        frequency_TextField.setText(Integer.toString(freq));
        update_from_frequency(freq);
    }
        
    private void update_from_frequency(int freq) {
        if (period_selection_enable_CheckBox.isSelected()) {
            double no_periods = Double.parseDouble(no_periods_TextField.getText());
            time_TextField.setText(Integer.toString((int)(1000000.0*no_periods/freq)));
        } else {
            int time = Integer.parseInt(time_TextField.getText());
            no_periods_TextField.setText(String.format("%.1f", (time*freq)/1000000.0));
        }
    }
    
    private void no_periods_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_no_periods_TextFieldActionPerformed
        double no_periods = Double.parseDouble(no_periods_TextField.getText());
        int freq = Integer.parseInt(frequency_TextField.getText());
        time_TextField.setText(Integer.toString((int) (1000000.0 * no_periods / freq)));
}//GEN-LAST:event_no_periods_TextFieldActionPerformed

    private void no_periods_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_no_periods_TextFieldFocusLost
        no_periods_TextFieldActionPerformed(null);
}//GEN-LAST:event_no_periods_TextFieldFocusLost

    private void frequency_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_frequency_TextFieldActionPerformed
        update_from_frequency();
}//GEN-LAST:event_frequency_TextFieldActionPerformed

    private void frequency_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_frequency_TextFieldFocusLost
        update_from_frequency();
}//GEN-LAST:event_frequency_TextFieldFocusLost

    private void time_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_time_TextFieldActionPerformed
        int time = Integer.parseInt(time_TextField.getText());
        int freq = Integer.parseInt(frequency_TextField.getText());
        no_periods_TextField.setText(String.format("%.1f", time * freq / 1000000.0));
}//GEN-LAST:event_time_TextFieldActionPerformed

    private void time_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_time_TextFieldFocusLost
        time_TextFieldActionPerformed(null);
}//GEN-LAST:event_time_TextFieldFocusLost

    private void prontocode_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_prontocode_TextFieldActionPerformed
        update_from_frequencycode();
}//GEN-LAST:event_prontocode_TextFieldActionPerformed

    private void prontocode_TextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_prontocode_TextFieldFocusLost
        update_from_frequencycode();
}//GEN-LAST:event_prontocode_TextFieldFocusLost

    private void period_selection_enable_CheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_period_selection_enable_CheckBoxActionPerformed
        boolean mystate = period_selection_enable_CheckBox.isSelected();
        no_periods_TextField.setEditable(mystate);
        time_TextField.setEditable(!mystate);
    }//GEN-LAST:event_period_selection_enable_CheckBoxActionPerformed

    private void icf_import_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_icf_import_ButtonActionPerformed
        File file = this.select_file("Select ict file", "ict", "ict Files", false, null);
        if (file != null) {
            try {
                if (verbose)
                    System.err.println("Imported " + file.getName());
                ict_parse ip = new ict_parse(file);
                protocol_raw_TextArea.setText(ip.ccf_string());
                this.protocol_send_Button.setEnabled(true);
                this.protocol_decode_Button.setEnabled(true);
                this.protocol_clear_Button.setEnabled(true);
            } catch (FileNotFoundException ex) {
                System.err.println(ex);
            } catch (IOException ex) {
                System.err.println(ex);
            }
        }
    }//GEN-LAST:event_icf_import_ButtonActionPerformed

    private void gc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_stop_ir_ActionPerformed
        try {
            gc.stop_ir(get_gc_module(), get_gc_connector());
        } catch (UnknownHostException ex) {
            System.err.println(ex.getMessage());
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        } catch (InterruptedException ex) {
            System.err.println(ex.getMessage());
        }
    }//GEN-LAST:event_gc_stop_ir_ActionPerformed

    private void browse(String address) {
        String[] cmd = new String[2];
        cmd[0] = harcprops.get_instance().get_browser();
        if (cmd[0] == null || cmd[0].isEmpty()) {
            System.err.println("No browser.");
            return;
        }
        cmd[1] = /*"http://" +*/ address;
        try {
            Process proc = Runtime.getRuntime().exec(cmd);
        } catch (IOException ex) {
            System.err.println("Could not start browser command `" + cmd[0] + " " + cmd[1]);
        }
    }

    private void possibly_enable_decode_button() {
        boolean looks_ok = !protocol_cooked_TextField.getText().isEmpty()
                || !protocol_raw_TextArea.getText().isEmpty();
        protocol_decode_Button.setEnabled(looks_ok);
        protocol_clear_Button.setEnabled(looks_ok);
        protocol_send_Button.setEnabled(looks_ok);
    }

    private void possibly_enable_encode_send() {
        boolean looks_ok = !commandno_TextField.getText().isEmpty() && !deviceno_TextField.getText().isEmpty();
        protocol_send_Button.setEnabled(looks_ok || !protocol_cooked_TextField.getText().isEmpty()
                || !protocol_raw_TextArea.getText().isEmpty());
        protocol_generate_Button.setEnabled(looks_ok);
        //wav_export_Button.setEnabled(looks_ok);
    }

    private int get_gc_module() {
        return Integer.parseInt((String) gc_modules_dcbm.getSelectedItem());
    }

    private int get_gc_connector() {
        return Integer.parseInt((String) gc_connector_ComboBox.getModel().getSelectedItem());
    }

    private irtrans.led_t get_irtrans_led() {
        return irtrans.led_t.parse((String)irtrans_led_ComboBox.getSelectedItem());
    }

    //public static gui_main getApplication() {
    //  return Application.getInstance(gui_main.class);
    //}
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new gui_main().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton alias_browse_Button;
    private javax.swing.JTextField aliases_TextField;
    private javax.swing.JButton aliases_select_Button;
    private javax.swing.JComboBox audio_video_ComboBox;
    private javax.swing.JMenuItem browse_device_MenuItem;
    private javax.swing.JTextField browser_TextField;
    private javax.swing.JButton browser_select_Button;
    private javax.swing.JMenuItem ccf_export_MenuItem;
    private javax.swing.JTextField ccf_export_buttonheight_TextField;
    private javax.swing.JTextField ccf_export_buttonwidth_TextField;
    private javax.swing.JButton ccf_export_export_Button;
    private javax.swing.JPanel ccf_export_opts_Panel;
    private javax.swing.JComboBox ccf_export_prontomodel_ComboBox;
    private javax.swing.JCheckBox ccf_export_raw_CheckBox;
    private javax.swing.JTextField ccf_export_screenheight_TextField;
    private javax.swing.JTextField ccf_export_screenwidth_TextField;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JButton commandButton;
    private javax.swing.JComboBox command_ComboBox;
    private javax.swing.JTextField command_argument_TextField;
    private javax.swing.JTextField commandno_TextField;
    private javax.swing.JTextField complement_decimal_TextField;
    private javax.swing.JTextField complement_hex_TextField;
    private javax.swing.JComboBox connection_type_ComboBox;
    private javax.swing.JTextArea console_TextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JPanel debug_Panel;
    private javax.swing.JTextField debug_TextField;
    private javax.swing.JTextField decimal_TextField;
    private javax.swing.JComboBox device_ComboBox;
    private javax.swing.JComboBox device_command_ComboBox;
    private javax.swing.JComboBox device_remote_ComboBox;
    private javax.swing.JComboBox deviceclass_ComboBox;
    private javax.swing.JButton deviceclass_send_Button;
    private javax.swing.JButton deviceclass_stop_Button;
    private javax.swing.JPanel deviceclassesPanel;
    private javax.swing.JComboBox devicegroup_ComboBox;
    private javax.swing.JTextField deviceno_TextField;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem enable_devicegroups_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem enable_macro_folders_CheckBoxMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem export_all_MenuItem;
    private javax.swing.JMenuItem export_device_MenuItem;
    private javax.swing.JTextField exportdir_TextField;
    private javax.swing.JButton exportdir_browse_Button;
    private javax.swing.JTabbedPane exportopts_TabbedPane;
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
    private javax.swing.JTextField frequency_TextField;
    private javax.swing.JTextField gc_address_TextField;
    private javax.swing.JButton gc_browse_Button;
    private javax.swing.JComboBox gc_connector_ComboBox;
    private javax.swing.JComboBox gc_module_ComboBox;
    private javax.swing.JPanel general_Panel;
    private javax.swing.JPanel general_export_opts_Panel;
    private javax.swing.JPanel globalcache_Panel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JTextField hex_TextField;
    private javax.swing.JPanel hexcalcPanel;
    private javax.swing.JButton home_browse_Button;
    private javax.swing.JButton home_load_Button;
    private javax.swing.JButton home_select_Button;
    private javax.swing.JTextField homeconf_TextField;
    private javax.swing.JButton icf_import_Button;
    private javax.swing.JCheckBoxMenuItem immediate_execution_commands_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem immediate_execution_macros_CheckBoxMenuItem;
    private javax.swing.JMenuItem import_ccf_MenuItem;
    private javax.swing.JMenuItem import_rmdu_MenuItem;
    private javax.swing.JPanel irtrans_Panel;
    private javax.swing.JTextField irtrans_address_TextField;
    private javax.swing.JButton irtrans_browse_Button;
    private javax.swing.JComboBox irtrans_led_ComboBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JButton keymap_rules_browse_Button;
    private javax.swing.JMenuItem launch_remotemaster_MenuItem;
    private javax.swing.JMenuItem launch_rmir_MenuItem;
    private javax.swing.JMenuItem launch_tonto_MenuItem;
    private javax.swing.JMenuItem lirc_export_all_MenuItem;
    private javax.swing.JMenuItem lirc_export_device_MenuItem;
    private javax.swing.JMenuItem lirc_export_server_MenuItem;
    private javax.swing.JButton macroButton;
    private javax.swing.JComboBox macroComboBox;
    private javax.swing.JTextField macro_TextField;
    private javax.swing.JButton macro_browse_Button;
    private javax.swing.JButton macro_load_Button;
    private javax.swing.JButton macro_select_Button;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JComboBox n_ezcontrol_ComboBox;
    private javax.swing.JTextField no_periods_TextField;
    private javax.swing.JComboBox no_sends_ComboBox;
    private javax.swing.JComboBox no_sends_protocol_ComboBox;
    private javax.swing.JTabbedPane optsTabbedPane;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JComboBox output_deviceComboBox;
    private javax.swing.JTabbedPane output_hw_TabbedPane;
    private javax.swing.JCheckBox period_selection_enable_CheckBox;
    private javax.swing.JTextField prontocode_TextField;
    private javax.swing.JComboBox protocol_ComboBox;
    private javax.swing.JButton protocol_clear_Button;
    private javax.swing.JTextField protocol_cooked_TextField;
    private javax.swing.JButton protocol_decode_Button;
    private javax.swing.JButton protocol_generate_Button;
    private javax.swing.JComboBox protocol_outputhw_ComboBox;
    private javax.swing.JTextArea protocol_raw_TextArea;
    private javax.swing.JButton protocol_send_Button;
    private javax.swing.JButton protocol_stop_Button;
    private javax.swing.JPanel protocolsPanel;
    private javax.swing.JComboBox rdf_ComboBox;
    private javax.swing.JMenuItem rem_export_all_MenuItem;
    private javax.swing.JMenuItem rem_export_device_MenuItem;
    private javax.swing.JTextField remotemaster_home_TextField;
    private javax.swing.JButton remotemaster_home_browse_Button;
    private javax.swing.JTextField reverse_decimal_TextField;
    private javax.swing.JTextField reverse_hex_TextField;
    private javax.swing.JTextField rmdu_button_rules_TextField;
    private javax.swing.JMenuItem rmdu_export_MenuItem;
    private javax.swing.JPanel rmdu_export_opts_Panel;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JComboBox secondlevel_macrofolders_ComboBox;
    private javax.swing.JButton select_Button;
    private javax.swing.JComboBox selecting_device_ComboBox;
    private javax.swing.JCheckBoxMenuItem sort_commands_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sort_devices_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sort_macros_CheckBoxMenuItem;
    private javax.swing.JComboBox src_device_ComboBox;
    private javax.swing.JButton stop_command_Button;
    private javax.swing.JButton stop_macro_Button;
    private javax.swing.JTextField subdevice_TextField;
    private javax.swing.JTextField t10_address_TextField;
    private javax.swing.JButton t10_browse_Button;
    private javax.swing.JButton t10_get_status_Button;
    private javax.swing.JButton t10_get_timers_Button;
    private javax.swing.JButton t10_update_Button;
    private javax.swing.JTextField time_TextField;
    private javax.swing.JComboBox toggle_ComboBox;
    private javax.swing.JComboBox toplevel_macrofolders_ComboBox;
    private javax.swing.JCheckBox verbose_CheckBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JComboBox zones_ComboBox;
    // End of variables declaration//GEN-END:variables
    private about_popup aboutBox;
}
