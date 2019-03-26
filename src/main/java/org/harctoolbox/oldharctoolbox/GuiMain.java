/*
Copyright (C) 2009-2011, 2019 Bengt Martensson.

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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.harctoolbox.harchardware.HarcHardwareException;
import org.harctoolbox.harchardware.ir.GlobalCache;
import org.harctoolbox.harchardware.ir.NoSuchTransmitterException;
import org.harctoolbox.harchardware.misc.EzControlT10;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.irp.IrpUtils;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * This class implements a GUI for most functionality in Harc.
 */
// TODO: Implement limited functionallity without home/macro file.

public final class GuiMain extends javax.swing.JFrame {

    private Home hm = null;
    private JythonEngine engine = null;
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
    private ResultFormatter formatter = new ResultFormatter();
    private ResultFormatter cmd_formatter = new ResultFormatter(HarcProps.get_instance().get_commandformat());
    private static final String dummy_no_selection = "--------";

    private macro_thread the_macro_thread = null;
    private command_thread the_command_thread = null;
    private globalcache_thread the_globalcache_device_thread = null;

    private GlobalCache gc = null;

    private HashMap<String, String> filechooserdirs = new HashMap<>(8);

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

    private static class copy_clipboard_text implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard c, Transferable t) {
        }

        public void to_clipboard(String str) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(str), this);
        }

        public String from_clipboard() {
            try {
                return (String) Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this).getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException ex) {
                System.err.println(ex.getMessage());
            }
            return null;
        }
    }

    /**
     * Creates new form gui_main
     * @param homefilename
     */
    public GuiMain(String homefilename/*, String macrofilename, boolean verbose, int debug, String browser*/) {
        try {
            hm = new Home(homefilename);
        } catch (IOException e) {
            System.err.println("Cannot open home file");
            System.exit(IrpUtils.EXIT_CONFIG_READ_ERROR);
        } catch (SAXParseException e) {
            System.err.println("home file parse error" + e.getMessage());
            System.exit(IrpUtils.EXIT_XML_ERROR);
        } catch (SAXException e) {
            System.err.println("home file parse error" + e.getMessage());
            System.exit(IrpUtils.EXIT_XML_ERROR);
        }

        engine = new JythonEngine(hm, false);
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
        deviceclasses_dcbm = new DefaultComboBoxModel(HarcUtils.sort_unique(Device.get_devices()));
        device_commands_dcbm = new DefaultComboBoxModel(new String[]{"--"});
        connection_types_dcbm = new DefaultComboBoxModel(new String[]{"--"});
        gc_modules_dcbm = new DefaultComboBoxModel(new String[]{"2"}); // ?

        initComponents();

        System.setErr(console_PrintStream);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    HarcProps.get_instance().save();
                    SocketStorage.dispose_sockets(true);
                } catch (IOException e) {
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
        //update_protocol_parameters();
        update_device_remotes_menu();
        verbose_CheckBoxMenuItem.setSelected(verbose);
        //verbose_CheckBox.setSelected(verbose);
        browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), CommandType_t.www, command_t.browse));

        try {
            gc = new GlobalCache("globalcache", verbose);
        } catch (IOException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        //homeconf_TextField.setText(homefilename);
        //macro_TextField.setText(macrofilename);
        //aliases_TextField.setText(HarcProps.get_instance().get_aliasfilename());
        //exportdir_TextField.setText(HarcProps.get_instance().get_exportdir());
        //System.setOut(console_PrintStream);
    }

    public GuiMain() {
        this(HarcProps.get_instance().get_homefilename());
    }

    // From Real Gagnon
    private class FilteredStream extends FilterOutputStream {

        FilteredStream(OutputStream aStream) {
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
        }
    }

    private PrintStream console_PrintStream = new PrintStream(new FilteredStream(new ByteArrayOutputStream()));

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
        xmlDeviceExportButton = new javax.swing.JButton();
        jLabel27 = new javax.swing.JLabel();
        jLabel28 = new javax.swing.JLabel();
        xmlAllDevicesExportButton = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JSeparator();
        outputHWTabbedPane = new javax.swing.JTabbedPane();
        globalcache_Panel = new javax.swing.JPanel();
        gc_address_TextField = new javax.swing.JTextField();
        gc_module_ComboBox = new javax.swing.JComboBox();
        gc_connector_ComboBox = new javax.swing.JComboBox();
        gc_browse_Button = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
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
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        export_device_MenuItem = new javax.swing.JMenuItem();
        export_all_MenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
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
        miscMenu = new javax.swing.JMenu();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        browse_device_MenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HARCToolbox: Home Automation and Remote Control Toolbox"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

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

        audio_video_ComboBox.setModel(new DefaultComboBoxModel(org.harctoolbox.oldharctoolbox.MediaType.values()));
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
                .addContainerGap(32, Short.MAX_VALUE))
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

        xmlDeviceExportButton.setText("XML");
        xmlDeviceExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xmlDeviceExportButtonActionPerformed(evt);
            }
        });

        jLabel27.setText("Export current device class");

        jLabel28.setText("Export all known device classes");

        xmlAllDevicesExportButton.setText("XML");
        xmlAllDevicesExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                xmlAllDevicesExportButtonActionPerformed(evt);
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
                        .addComponent(no_sends_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(xmlDeviceExportButton)
                    .addComponent(jLabel27))
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(device_command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                        .addGap(11, 11, 11)
                        .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                                .addComponent(deviceclass_stop_Button)
                                .addGap(18, 18, 18)
                                .addComponent(deviceclass_send_Button))
                            .addComponent(xmlAllDevicesExportButton)
                            .addComponent(jLabel28))))
                .addContainerGap(62, Short.MAX_VALUE))
            .addComponent(jSeparator3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 633, Short.MAX_VALUE)
        );
        deviceclassesPanelLayout.setVerticalGroup(
            deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(deviceclassesPanelLayout.createSequentialGroup()
                .addContainerGap(19, Short.MAX_VALUE)
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
                .addGap(9, 9, 9)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel28)
                    .addComponent(jLabel27))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(deviceclassesPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(xmlAllDevicesExportButton)
                    .addComponent(xmlDeviceExportButton))
                .addGap(30, 30, 30))
        );

        output_hw_TabbedPane.addTab("Device classes", deviceclassesPanel);

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
        gc_browse_Button.setToolTipText("Open selected GlobalCache in the browser.");
        gc_browse_Button.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gc_browse_ButtonActionPerformed(evt);
            }
        });

        jButton1.setText("Stop IR");
        jButton1.setToolTipText("Send the selected GlobalCache the stopir command.");
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
                .addContainerGap(214, Short.MAX_VALUE))
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
                .addContainerGap(91, Short.MAX_VALUE))
        );

        outputHWTabbedPane.addTab("GlobalCache", globalcache_Panel);

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
                        .addGroup(ezcontrolPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
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
                                .addComponent(ezcontrol_off_Button))
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
                                .addComponent(t10_update_Button)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        console_TextArea.setEditable(false);
        console_TextArea.setColumns(20);
        console_TextArea.setLineWrap(true);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is the console, where errors and messages go, instead of annoying you with popups.");
        console_TextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(console_TextArea);

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
        fileMenu.add(jSeparator2);

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
        enable_macro_folders_CheckBoxMenuItem.setToolTipText("Presently disabled.");
        enable_macro_folders_CheckBoxMenuItem.setEnabled(false);
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

        miscMenu.setMnemonic('M');
        miscMenu.setText("Misc.");

        clear_console_MenuItem.setText("Clear console");
        clear_console_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clear_console_MenuItemActionPerformed(evt);
            }
        });
        miscMenu.add(clear_console_MenuItem);

        browse_device_MenuItem.setText("Browse selected device");
        browse_device_MenuItem.setToolTipText("Point the browser to this device");
        browse_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browse_device_MenuItemActionPerformed(evt);
            }
        });
        miscMenu.add(browse_device_MenuItem);

        menuBar.add(miscMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

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
                    .addComponent(output_hw_TabbedPane, 0, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 645, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(output_hw_TabbedPane)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private class macro_thread extends Thread {
        private final String macroname;
        macro_thread(String name) {
            super("macro_thread");
            macroname = name;
        }

        public String get_name() {
            return macroname;
        }

        @Override
        public void run() {
            //String cmd = (String) macros_dcbm.getSelectedItem();
            System.err.println(cmd_formatter.format("harcmacros." + macroname + "()"));
            //try {
            String result = engine.eval("harcmacros." + macroname + "()");
            //} catch (non_existing_command_exception e) {
                // This should not happen
            //    System.err.println("*** Non existing macro " + e.getMessage());
            //} catch (InterruptedException e) {
            //    System.err.println("*** Interrupted ***" + e.getMessage());
            //}
            if (result == null)
                System.err.println("** Failed **");
            else if (!result.isEmpty())
                System.err.println(formatter.format(result));

            macroButton.setEnabled(engine != null);
            stop_macro_Button.setEnabled(false);
            the_macro_thread = null;
        }
    }

    private class command_thread extends Thread {
        private final String device;
        private final command_t cmd;
        private final String[] args;

        command_thread(String device, command_t cmd, String[] args) {
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
                result = hm.do_command(device, cmd, args, CommandType_t.any, 1, ToggleType.dont_care, false);
            } catch (InterruptedException e) {
                System.err.println("*** Interrupted ***" + e.getMessage());
            }
            if (result == null)
                System.err.println("** Failed **");
            else if (!result.isEmpty())
                System.err.println(formatter.format(result));

            commandButton.setEnabled(true);
            stop_command_Button.setEnabled(false);
            the_command_thread = null;
        }
    }

    private class globalcache_thread extends Thread {
        private final IrSignal code;
        private final int module;
        private final int connector;
        private final int count;
        private final JButton start_button;
        private final JButton stop_button;

        globalcache_thread(IrSignal code, int module, int connector, int count,
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
                success = gc.sendIr(code, module, connector, count);
            } catch (UnknownHostException ex) {
                System.err.println("Globalcache hostname is not found.");
            } catch (IOException e) {
                System.err.println(e);
            } catch (NoSuchTransmitterException ex) {
                Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, ex.getMessage());
            }

            if (!success)
                System.err.println("** Failed **");

            //the_globalcache_thread = null;
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
            String result = HarcProps.get_instance().save();
            System.err.println(result == null ? "No need to save properties." : ("Property file written to " + result + "."));
        } catch (IOException e) {
            warning("Problems saving properties: " + e.getMessage());
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        if (aboutBox == null) {
            //JFrame mainFrame = gui_main.getApplication().getMainFrame();
            aboutBox = new AboutPopup(this/*mainFrame*/, false);
            aboutBox.setLocationRelativeTo(/*mainFrame*/this);
        }
        aboutBox.setVisible(true);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        try {
            String props = select_file("Select properties save", "xml", "XML Files", true, null).getAbsolutePath();
            HarcProps.get_instance().save(props);
            System.err.println("Property file written to " + props + ".");
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
        String[] commands = hm.get_commands((String) devices_dcbm.getSelectedItem(), CommandType_t.any);
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
        command_argument_TextField.setEnabled(hm.get_arguments(device, command_t.parse(cmdname), CommandType_t.any) == 1);
    }

    private void update_device_menu() {
        String[] devices = enable_devicegroups_CheckBoxMenuItem.isSelected()
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
        UserPrefs.get_instance().set_verbose(verbose);
        gc.setVerbose(verbose);
        verbose_CheckBoxMenuItem.setSelected(verbose);
        //verbose_CheckBox.setSelected(verbose);
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
        browse_device_MenuItem.setEnabled(hm.has_command((String)devices_dcbm.getSelectedItem(), CommandType_t.www, command_t.browse));
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
        command_argument_TextField.setEnabled(hm.get_arguments(device, command_t.parse(cmdname), CommandType_t.any) == 1);
    }//GEN-LAST:event_command_ComboBoxActionPerformed

    private void update_device_remotes_menu() {
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        try {
            Device dvc = new Device(dev);
            String[] remotes = dvc.get_remotenames();
            //command_t[] commands = dvc.get_commands(commandtype_t.ir);
            java.util.Arrays.sort(remotes);
            device_remotes_dcbm = new DefaultComboBoxModel(remotes);
            device_remote_ComboBox.setModel(device_remotes_dcbm);
            update_device_commands_menu();
        } catch (IOException | SAXException e) {
            System.err.println(e.getMessage());
        }
    }

    private void update_device_commands_menu() {
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        String remote = device_remotes_dcbm != null ? (String) device_remotes_dcbm.getSelectedItem()
                : null;
        try {
            Device dvc = new Device(dev);
            command_t[] commands = dvc.get_commands(CommandType_t.ir, remote);
            if (commands == null)
                return;
            java.util.Arrays.sort(commands);
            device_commands_dcbm = new DefaultComboBoxModel(commands);
            device_command_ComboBox.setModel(device_commands_dcbm);
        } catch (IOException | SAXException e) {
            System.err.println(e.getMessage());
        }
    }

    private void update_connection_types_menu() {
        ConnectionType[] con_types = hm.get_connection_types((String) selecting_devices_dcbm.getSelectedItem(),
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
        //String result = null;
        String cmd_name = (String) commands_dcbm.getSelectedItem();
        command_t cmd = command_t.parse(cmd_name);
        String device = (String) devices_dcbm.getSelectedItem();
        System.err.println(cmd_formatter.format(device + " " + cmd_name));
        int no_required_args = hm.get_arguments(device, cmd, CommandType_t.any);
        //warning("# args: " + no_required_args);
        String arg0 = command_argument_TextField.getText().trim();
        String[] args = arg0.isEmpty() ? new String[0] : new String[]{arg0};
        if (args.length < no_required_args) {
            warning("To few arguments to command. Not executed.");
            return;
        } else if (args.length > no_required_args) {
            // Should not happen.
            warning("Excess arguments ignored");
        }

//        if (false) {
//            try {
//                result = hm.do_command(device, cmd, args, commandtype_t.any, 1, toggletype.dont_care, false);
//            } catch (InterruptedException e) {
//                System.err.println("Interrupted");
//            }
//            if (result == null)
//                System.err.println("**Failed**");
//            else if (!result.isEmpty())
//                System.err.println(formatter.format(result));
//        } else {
        the_command_thread = new command_thread(device, cmd, args);
        commandButton.setEnabled(false);
        stop_command_Button.setEnabled(true);
        the_command_thread.start();
//        }

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
        MediaType mt = (MediaType) audio_video_ComboBox.getSelectedItem();
        System.err.println(cmd_formatter.format("--select " + device + " " + src_device
                + (zone != null ? (" (zone = " + zone + ")") : "")
                + (audio_video_ComboBox.isEnabled() ? (" (" + mt + ")") : "")
                + (connection_type_ComboBox.isEnabled() ? (" (" + connection_types_dcbm.getSelectedItem() +")") : "")));
        boolean success = false;
        try {
            success = hm.select(device, src_device, CommandType_t.any, zone, mt,
                    connection_types_dcbm.getSize() > 1 ? (ConnectionType) connection_types_dcbm.getSelectedItem() : ConnectionType.any);
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
            hm.do_command((String) devices_dcbm.getSelectedItem(), command_t.browse, null, CommandType_t.www, 1, ToggleType.dont_care, false);
        } catch (InterruptedException e) {
        }
    }//GEN-LAST:event_browse_device_MenuItemActionPerformed

    private void deviceclass_send_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_send_ButtonActionPerformed
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        Command c = null;
        command_t cmd = command_t.invalid;
        String remote = (String) device_remotes_dcbm.getSelectedItem();
        int no_sends = Integer.parseInt((String)no_sends_ComboBox.getModel().getSelectedItem());
        //boolean verbose = verbose_CheckBoxMenuItem.getState();

        try {
            Device dvc = new Device(dev);
            cmd = (command_t) device_commands_dcbm.getSelectedItem();
            c = dvc.get_command(cmd, CommandType_t.ir, remote);
            //remote = dvc.
        } catch (IOException | SAXException e) {
            System.err.println(e.getMessage());
        }

        if (c == null) {
            System.err.println("No IR command for " + cmd + " found.");
            return;
        }

//        try {
            if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("GlobalCache")) {
                //gc.send_ir(c.get_ir_code(toggletype.do_toggle, verbose), get_gc_module(), get_gc_connector(), no_sends);
                if (the_globalcache_device_thread != null && the_globalcache_device_thread.isAlive())
                    System.err.println("Internal error: the_globalcache_device thread active!!?");

                the_globalcache_device_thread = new globalcache_thread(c.get_ir_code(ToggleType.dont_care, verbose), get_gc_module(), get_gc_connector(), no_sends, deviceclass_send_Button, deviceclass_stop_Button);
                the_globalcache_device_thread.start();
//            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (preprog_ascii)")) {
//                //irt.send_flashed_command(remote, cmd, this.get_irtrans_led(), no_sends);
//                if (the_irtrans_thread != null && the_irtrans_thread.isAlive())
//                    System.err.println("Internal error: the_irtrans_thread active??!");
//
//                the_irtrans_thread = new irtrans_thread(remote, cmd.toString(), this.get_irtrans_led(), no_sends, deviceclass_send_Button, deviceclass_stop_Button);
//                the_irtrans_thread.start();
//            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (web_api)")) {
//                if (no_sends > 1)
//                    System.err.println("Warning: Sending only one time");
//                String url = irtrans.make_url(irtrans_address_TextField.getText(),
//                        remote, cmd, get_irtrans_led());
//                if (verbose)
//                    System.err.println("Getting URL " + url);
//                // TODO: Right now, I dont care to implement status checking...
//                (new URL(url)).getContent();
//            } else if (((String) output_deviceComboBox.getModel().getSelectedItem()).equalsIgnoreCase("IRTrans (udp)")) {
//                irt.send_ir(c.get_ir_code(toggletype.dont_care, verbose), get_irtrans_led(), no_sends);
            } else {
                System.err.println("Internal error: cannot find output device: " + output_deviceComboBox.getModel().getSelectedItem());
//            }
//        } catch (UnknownHostException e) {
//            System.err.println(e.getMessage());
//        } catch (IOException e) {
//            System.err.println(e.getMessage());
//        } catch (IrpMasterException e) {
//            System.err.println(e.getMessage());
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
        String filename = select_file("Save console text as...", "txt", "Text file", true, null).getAbsolutePath();
        try (PrintStream ps = new PrintStream(new FileOutputStream(filename), false, IrCoreUtils.DEFAULT_CHARSET_NAME)) {
            ps.println(console_TextArea.getText());
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            System.err.println(ex);
        }
    }//GEN-LAST:event_consoletext_save_MenuItemActionPerformed

    private void export_all_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_all_MenuItemActionPerformed
        Device.export_all_devices(HarcProps.get_instance().get_exportdir());
}//GEN-LAST:event_export_all_MenuItemActionPerformed

    private void stop_macro_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_macro_ButtonActionPerformed
        the_macro_thread.interrupt();
        macroButton.setEnabled(true);
        stop_macro_Button.setEnabled(false);
        System.err.println("************ Execution of macro `"
                + (the_macro_thread.get_name()) + "' interrupted *************");
        the_macro_thread = null;
    }//GEN-LAST:event_stop_macro_ButtonActionPerformed

    private void export_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_export_device_MenuItemActionPerformed
        Device.export_device(HarcProps.get_instance().get_exportdir(), (String) deviceclasses_dcbm.getSelectedItem());
    }//GEN-LAST:event_export_device_MenuItemActionPerformed

    private void t10_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_address_TextFieldActionPerformed

}//GEN-LAST:event_t10_address_TextFieldActionPerformed

    private void t10_get_timers_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_get_timers_ButtonActionPerformed
        System.err.println(EzControlT10.getTimers(t10_address_TextField.getText()));
}//GEN-LAST:event_t10_get_timers_ButtonActionPerformed

    private void t10_get_status_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_get_status_ButtonActionPerformed
        System.err.println(EzControlT10.getStatus(t10_address_TextField.getText()));
}//GEN-LAST:event_t10_get_status_ButtonActionPerformed

    private void t10_send_manual_command(command_t cmd) {
        try {
            EzControlT10.Command t10cmd = EzControlT10.Command.valueOf(cmd.toString());
            EzControlT10.EZSystem system = EzControlT10.EZSystem.valueOf((String) ezcontrol_system_ComboBox.getModel().getSelectedItem());
            (new EzControlT10(t10_address_TextField.getText())).sendManual(
                    system,
                    (String) ezcontrol_house_ComboBox.getModel().getSelectedItem(),
                    Integer.parseInt((String) ezcontrol_deviceno_ComboBox.getModel().getSelectedItem()),
                    t10cmd, -1,
                    Integer.parseInt((String) this.n_ezcontrol_ComboBox.getModel().getSelectedItem()));
        } catch (IllegalArgumentException ex) {
            System.err.println("This cannot happen.");
        } catch (HarcHardwareException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }

    private void t10_send_preset_command(command_t cmd) {
        try {
            EzControlT10.Command t10cmd = EzControlT10.Command.valueOf(cmd.toString());
            (new EzControlT10(t10_address_TextField.getText())).sendPreset(Integer.parseInt((String) ezcontrol_preset_no_ComboBox.getModel().getSelectedItem()),
                    t10cmd);
        } catch (IllegalArgumentException | HarcHardwareException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, ex.getMessage());
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
        EzControlT10 ez = new EzControlT10(this.t10_address_TextField.getText());
        int preset_number = Integer.parseInt((String)ezcontrol_preset_no_ComboBox.getModel().getSelectedItem());
        ezcontrol_preset_name_TextField.setText(ez.getPresetName(preset_number));
        this.ezcontrol_preset_state_TextField.setText(ez.getPresetStatus(preset_number));
    }//GEN-LAST:event_ezcontrol_preset_no_ComboBoxActionPerformed

    private void t10_update_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_update_ButtonActionPerformed
        ezcontrol_preset_no_ComboBoxActionPerformed(evt);
}//GEN-LAST:event_t10_update_ButtonActionPerformed

    private void gc_address_TextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_address_TextFieldActionPerformed
        try {
            gc = new GlobalCache(gc_address_TextField.getText(), verbose_CheckBoxMenuItem.getState());
            gc_module_ComboBox.setEnabled(false);
            gc_connector_ComboBox.setEnabled(false);
            String[] dvs = gc.getDevices();
            //String[] dvs = devs.split("\n");
            String[] s = new String[dvs.length];
            for (int i = 0; i < s.length; i++)
                s[i] = dvs[i].endsWith("IR") ? dvs[i].substring(7, 8) : null;

            String[] modules = HarcUtils.nonnulls(s);
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
        }
        deviceclass_send_Button.setEnabled(gc != null);
        //protocol_send_Button.setEnabled(gc != null);
}//GEN-LAST:event_gc_address_TextFieldActionPerformed

    private void device_remote_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_device_remote_ComboBoxActionPerformed
        update_device_commands_menu();
    }//GEN-LAST:event_device_remote_ComboBoxActionPerformed

    private void t10_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_t10_browse_ButtonActionPerformed
        HarcUtils.browse("http://" + t10_address_TextField.getText());
    }//GEN-LAST:event_t10_browse_ButtonActionPerformed

    private void gc_browse_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_browse_ButtonActionPerformed
        HarcUtils.browse("http://" + gc_address_TextField.getText());
    }//GEN-LAST:event_gc_browse_ButtonActionPerformed

    private void deviceclass_stop_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_stop_ButtonActionPerformed
        try {
            if (the_globalcache_device_thread != null)
                the_globalcache_device_thread.interrupt();

            if (this.output_deviceComboBox.getSelectedIndex() == 0)
                gc.stopIr(this.get_gc_module(), this.get_gc_connector());
        } catch (IOException | NoSuchTransmitterException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_deviceclass_stop_ButtonActionPerformed

    private void stop_command_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stop_command_ButtonActionPerformed
        the_command_thread.interrupt();
    }//GEN-LAST:event_stop_command_ButtonActionPerformed

    private void gc_stop_ir_ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gc_stop_ir_ActionPerformed
        try {
            gc.stopIr(get_gc_module(), get_gc_connector());
        } catch (IOException | NoSuchTransmitterException ex) {
            Logger.getLogger(GuiMain.class.getName()).log(Level.SEVERE, ex.getMessage());
        }
    }//GEN-LAST:event_gc_stop_ir_ActionPerformed

private void xmlDeviceExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xmlDeviceExportButtonActionPerformed
    Device.export_device(HarcProps.get_instance().get_exportdir(), (String) deviceclasses_dcbm.getSelectedItem());
}//GEN-LAST:event_xmlDeviceExportButtonActionPerformed

private void xmlAllDevicesExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_xmlAllDevicesExportButtonActionPerformed
    Device.export_all_devices(HarcProps.get_instance().get_exportdir());
}//GEN-LAST:event_xmlAllDevicesExportButtonActionPerformed

    private int get_gc_module() {
        return Integer.parseInt((String) gc_modules_dcbm.getSelectedItem());
    }

    private int get_gc_connector() {
        return Integer.parseInt((String) gc_connector_ComboBox.getModel().getSelectedItem());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> {
            new GuiMain().setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JComboBox audio_video_ComboBox;
    private javax.swing.JMenuItem browse_device_MenuItem;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JButton commandButton;
    private javax.swing.JComboBox command_ComboBox;
    private javax.swing.JTextField command_argument_TextField;
    private javax.swing.JComboBox connection_type_ComboBox;
    private javax.swing.JTextArea console_TextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JComboBox device_ComboBox;
    private javax.swing.JComboBox device_command_ComboBox;
    private javax.swing.JComboBox device_remote_ComboBox;
    private javax.swing.JComboBox deviceclass_ComboBox;
    private javax.swing.JButton deviceclass_send_Button;
    private javax.swing.JButton deviceclass_stop_Button;
    private javax.swing.JPanel deviceclassesPanel;
    private javax.swing.JComboBox devicegroup_ComboBox;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem enable_devicegroups_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem enable_macro_folders_CheckBoxMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem export_all_MenuItem;
    private javax.swing.JMenuItem export_device_MenuItem;
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
    private javax.swing.JTextField gc_address_TextField;
    private javax.swing.JButton gc_browse_Button;
    private javax.swing.JComboBox gc_connector_ComboBox;
    private javax.swing.JComboBox gc_module_ComboBox;
    private javax.swing.JPanel globalcache_Panel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JCheckBoxMenuItem immediate_execution_commands_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem immediate_execution_macros_CheckBoxMenuItem;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel27;
    private javax.swing.JLabel jLabel28;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JButton macroButton;
    private javax.swing.JComboBox macroComboBox;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenu miscMenu;
    private javax.swing.JComboBox n_ezcontrol_ComboBox;
    private javax.swing.JComboBox no_sends_ComboBox;
    private javax.swing.JTabbedPane outputHWTabbedPane;
    private javax.swing.JComboBox output_deviceComboBox;
    private javax.swing.JTabbedPane output_hw_TabbedPane;
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
    private javax.swing.JTextField t10_address_TextField;
    private javax.swing.JButton t10_browse_Button;
    private javax.swing.JButton t10_get_status_Button;
    private javax.swing.JButton t10_get_timers_Button;
    private javax.swing.JButton t10_update_Button;
    private javax.swing.JComboBox toplevel_macrofolders_ComboBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JButton xmlAllDevicesExportButton;
    private javax.swing.JButton xmlDeviceExportButton;
    private javax.swing.JComboBox zones_ComboBox;
    // End of variables declaration//GEN-END:variables
    private AboutPopup aboutBox;
}
