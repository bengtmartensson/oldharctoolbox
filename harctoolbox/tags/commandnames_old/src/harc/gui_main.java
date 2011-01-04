package harc;

import javax.swing.*;
//import java.lang.*;
import java.io.*;

/**
 *
 */
public class gui_main extends javax.swing.JFrame {

    private home hm = null;
    private macro_engine engine = null;
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
    private resultformatter formatter = new resultformatter();
    private resultformatter cmd_formatter = new resultformatter(harcprops.get_instance().get_commandformat());
    private static final String dummy_no_selection = "--------";

    // TODO: Implement limited functionallity without macro file.

    // TODO: run shutdown when closing from WM.

    /** Creates new form gui_main */
    public gui_main(String homefilename, String macrofilename, int verbose, int debug, String browser) {
        System.setOut(console_PrintStream);
        System.setErr(console_PrintStream);
        try {
            hm = new home(homefilename, verbose, debug, browser);
        } catch (IOException e) {
            System.err.println("Cannot open home file");
            System.exit(harcutils.exit_config_read_error);
        }
        try {
            engine = new macro_engine(macrofilename, hm, debug);
        } catch (IOException e) {
            System.err.println("Cannot open macro file");
            System.exit(harcutils.exit_config_read_error);
        }
        //macros = engine.listmacros(false);
        macros_dcbm = new DefaultComboBoxModel(engine.get_macros(false));
        String[] toplevel_macrofolders = engine.get_folders();
        toplevel_macrofolders_dcbm = new DefaultComboBoxModel(toplevel_macrofolders);
        secondlevel_macrofolders_dcbm =  new DefaultComboBoxModel(
                (toplevel_macrofolders != null && toplevel_macrofolders.length > 0)
                ? engine.get_folders(toplevel_macrofolders[0], 1)
                : (new String[]{ dummy_no_selection }));

        devices_dcbm = new DefaultComboBoxModel(hm.get_devices());
        devicegroups_dcbm = new DefaultComboBoxModel(hm.get_devicegroups());
        commands_dcbm = new DefaultComboBoxModel(new String[] { dummy_no_selection });
        selecting_devices_dcbm = new DefaultComboBoxModel(hm.get_selecting_devices());
        src_devices_dcbm = new DefaultComboBoxModel(new String[] { dummy_no_selection });
        zones_dcbm = new DefaultComboBoxModel(new String[] { "--" });
        deviceclasses_dcbm = new DefaultComboBoxModel(harcutils.sort_unique(device.get_devices()));
        device_commands_dcbm = new DefaultComboBoxModel(new String[] { "--" });

        initComponents();

        update_macro_menu();
        update_device_menu();
        update_command_menu();
        update_src_device_menu();
        update_zone_menu();
        update_device_commands_menu();
        verbose_CheckBoxMenuItem.setSelected(verbose > 0);

    }

    public gui_main() {
        this(harcprops.get_instance().get_home_file(),
                harcprops.get_instance().get_macro_file(), 1, 0,
                harcprops.get_instance().get_browser());
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

    //boolean logFile;

    private void warning(String message) { //FIXME
        System.err.println("Warning: " + message);
        //console_TextArea.append("Warning: " + message + "\n");
    }

    /*
     private void acknowledge(String message) { //FIXME
        //System.err.println("Ack: " + message);
        console_TextArea.append(message + "\n");
    }*/

    private void not_yet_implemented(String functionname) {
        console_TextArea.append(functionname + " is not yet implemented, sorry.\n");
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        parentTabbedPane = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        console_TextArea = new javax.swing.JTextArea();
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
        jPanel2 = new javax.swing.JPanel();
        deviceclass_ComboBox = new javax.swing.JComboBox();
        device_command_ComboBox = new javax.swing.JComboBox();
        jPanel3 = new javax.swing.JPanel();
        protocol_ComboBox = new javax.swing.JComboBox();
        deviceno_TextField = new javax.swing.JTextField();
        subdevice_TextField = new javax.swing.JTextField();
        commandno_TextField = new javax.swing.JTextField();
        toggle_ComboBox = new javax.swing.JComboBox();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        homeMenuItem = new javax.swing.JMenuItem();
        macroMenuItem = new javax.swing.JMenuItem();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        consoletext_save_MenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
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
        debug_MenuItem = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        clear_console_MenuItem = new javax.swing.JMenuItem();
        browse_device_MenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        contentMenuItem = new javax.swing.JMenuItem();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("HARC: Home Automation and Remote Control"); // NOI18N
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jLabel1.setText("Device");

        jLabel2.setText("Macro");

        console_TextArea.setColumns(20);
        console_TextArea.setEditable(false);
        console_TextArea.setRows(5);
        console_TextArea.setToolTipText("This is a popup free zone!");
        console_TextArea.setWrapStyleWord(true);
        jScrollPane1.setViewportView(console_TextArea);

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
        macroComboBox.setToolTipText(engine.describe_macro((String)macros_dcbm.getSelectedItem()));
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

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(devicegroup_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(selecting_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(toplevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(6, 6, 6)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(secondlevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(macroComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 162, Short.MAX_VALUE)
                        .addComponent(macroButton))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(command_argument_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(commandButton))
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addComponent(zones_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(audio_video_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(src_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 81, Short.MAX_VALUE)
                        .addComponent(select_Button))))
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 560, Short.MAX_VALUE)
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(commandButton)
                    .addComponent(jLabel1)
                    .addComponent(devicegroup_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(command_argument_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(selecting_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(zones_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(audio_video_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(src_device_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(select_Button))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(toplevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(secondlevel_macrofolders_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(macroComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(macroButton))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 269, Short.MAX_VALUE))
        );

        parentTabbedPane.addTab("Main", mainPanel);

        deviceclass_ComboBox.setMaximumRowCount(20);
        deviceclass_ComboBox.setModel(deviceclasses_dcbm);
        deviceclass_ComboBox.setToolTipText("Deviceclass");
        deviceclass_ComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deviceclass_ComboBoxActionPerformed(evt);
            }
        });

        device_command_ComboBox.setModel(device_commands_dcbm);
        device_command_ComboBox.setToolTipText("command");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(57, 57, 57)
                .addComponent(deviceclass_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(149, 149, 149)
                .addComponent(device_command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(304, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deviceclass_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(device_command_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(371, Short.MAX_VALUE))
        );

        parentTabbedPane.addTab("Devices", jPanel2);

        protocol_ComboBox.setModel(new DefaultComboBoxModel(protocol.get_protocols()));
        protocol_ComboBox.setToolTipText("Protocol name");

        deviceno_TextField.setText("device #");

        subdevice_TextField.setText("subdevice #");

        commandno_TextField.setText("command #");

        toggle_ComboBox.setModel(new DefaultComboBoxModel(toggletype.values()));
        toggle_ComboBox.setToolTipText("Toggles to generate");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(162, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deviceno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(subdevice_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(commandno_TextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(toggle_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(protocol_ComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(342, Short.MAX_VALUE))
        );

        parentTabbedPane.addTab("Protocols", jPanel3);

        jTextArea1.setColumns(20);
        jTextArea1.setEditable(false);
        jTextArea1.setRows(5);
        jTextArea1.setText("Misc. tools?");
        jScrollPane2.setViewportView(jTextArea1);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 560, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(0, 168, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 168, Short.MAX_VALUE)))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 423, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(0, 167, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 167, Short.MAX_VALUE)))
        );

        parentTabbedPane.addTab("Misc", jPanel4);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        homeMenuItem.setMnemonic('H');
        homeMenuItem.setText("Open Homefile");
        homeMenuItem.setToolTipText("Open a home file");
        homeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                homeMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(homeMenuItem);

        macroMenuItem.setMnemonic('M');
        macroMenuItem.setText("Open Macrofile...");
        macroMenuItem.setToolTipText("Open a macro file");
        macroMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                macroMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(macroMenuItem);

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

        consoletext_save_MenuItem.setMnemonic('c');
        consoletext_save_MenuItem.setText("Save console text as...");
        fileMenu.add(consoletext_save_MenuItem);
        fileMenu.add(jSeparator4);

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

        jMenuItem1.setMnemonic('C');
        jMenuItem1.setText("Copy");
        editMenu.add(jMenuItem1);

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

        debug_MenuItem.setMnemonic('D');
        debug_MenuItem.setText("Debug...");
        debug_MenuItem.setToolTipText("Set debug options");
        jMenu1.add(debug_MenuItem);

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
        browse_device_MenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browse_device_MenuItemActionPerformed(evt);
            }
        });
        jMenu2.add(browse_device_MenuItem);

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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(parentTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 572, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(parentTabbedPane, javax.swing.GroupLayout.PREFERRED_SIZE, 466, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void do_exit() {
        try {
            harcprops.get_instance().save();
        } catch (Exception e) {
            warning("Problems saving properties; " + e.getMessage());
        }
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
        String[] cmd_array = new String[]{harcprops.get_instance().get_browser(),
            harcprops.get_instance().get_helpfile()};
        if (cmd_array[0] == null || cmd_array[0].equals("") || cmd_array[1] == null || cmd_array[1].equals("")) {
            warning("Error: Browser or helpfile not set.");
            return;
        }

        try {
            Runtime.getRuntime().exec(cmd_array);
        } catch (IOException e) {
            warning("Could not start browser: " + e.getMessage());
        }
}//GEN-LAST:event_contentMenuItemActionPerformed

    private void homeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_homeMenuItemActionPerformed
        not_yet_implemented("Open Home file");
        warning("For now use command line arguments or edit the properties file.");
        warning("Present value: " + harcprops.get_instance().get_home_file());
    }//GEN-LAST:event_homeMenuItemActionPerformed

    private void macroMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macroMenuItemActionPerformed
        not_yet_implemented("Open Macro file");
        warning("For now use command line arguments or edit the properties file.");
        warning("Present value: " + harcprops.get_instance().get_macro_file());
    }//GEN-LAST:event_macroMenuItemActionPerformed

    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
         not_yet_implemented("Save Props as");
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void macroComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macroComboBoxActionPerformed
        macroComboBox.setToolTipText(engine.describe_macro((String)macros_dcbm.getSelectedItem()));
        if (immediate_execution_macros_CheckBoxMenuItem.isSelected())
            macroButtonActionPerformed(null);
}//GEN-LAST:event_macroComboBoxActionPerformed

    private void macroButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_macroButtonActionPerformed
        String result = null;
        String cmd = (String) macros_dcbm.getSelectedItem();
        console_TextArea.append(cmd_formatter.format(cmd) + "\n");
        try {
            result = engine.eval_macro(cmd, null, 0, false);
        } catch (non_existing_command_exception e) {
        } catch (InterruptedException e) {
            warning("Interrupted");
        }
        if (!result.equals(""))
            console_TextArea.append(formatter.format(result) + "\n");
    }//GEN-LAST:event_macroButtonActionPerformed

    private void update_zone_menu() {
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String [] zones = hm.get_zones(device);
        if (zones.length == 0)
            zones = new String[] { "--" };
        zones_dcbm = new DefaultComboBoxModel(zones);
        zones_ComboBox.setModel(zones_dcbm);
    }

    private void update_src_device_menu() {
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String[] src_devices = hm.get_sources(device, null/*zone*/);
        src_devices_dcbm = new DefaultComboBoxModel(src_devices);
        src_device_ComboBox.setModel(src_devices_dcbm);
    }

    private void update_devicegroup_menu() {
        update_device_menu();
    }
    
    private void update_command_menu() {
        String[] commands = null;

        commands = hm.get_commands((String) devices_dcbm.getSelectedItem(), commandset.any);
        if (sort_commands_CheckBoxMenuItem.isSelected())
            java.util.Arrays.sort(commands, String.CASE_INSENSITIVE_ORDER);
        commands_dcbm = new DefaultComboBoxModel(commands);
        command_ComboBox.setModel(commands_dcbm);
        String device = (String) devices_dcbm.getSelectedItem();
        String cmdname = (String) commands_dcbm.getSelectedItem();
        command_argument_TextField.setEnabled(hm.get_arguments(device, ir_code.decode_command(cmdname), commandset.any) == 1);
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

    private void update_macro_menu() {
        String[] macros = null;
        if (enable_macro_folders_CheckBoxMenuItem.isSelected())
            macros = engine.get_macros(
                    (String) ((secondlevel_macrofolders_dcbm.getSize() > 1) ? secondlevel_macrofolders_dcbm.getSelectedItem()
                    : toplevel_macrofolders_dcbm.getSelectedItem()));
        else
            macros = engine.get_macros(false);

        if (macros == null || macros.length == 0) {
            macros = new String[] { dummy_no_selection };
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
    }

    private void sort_macros_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sort_macros_CheckBoxMenuItemActionPerformed
        update_macro_menu();
    }//GEN-LAST:event_sort_macros_CheckBoxMenuItemActionPerformed

    private void verbose_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_verbose_CheckBoxMenuItemActionPerformed
        hm.set_verbosity(verbose_CheckBoxMenuItem.isSelected() ? 1 : 0);
    }//GEN-LAST:event_verbose_CheckBoxMenuItemActionPerformed

    private void toplevel_macrofolders_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toplevel_macrofolders_ComboBoxActionPerformed
        toplevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String) toplevel_macrofolders_dcbm.getSelectedItem()));
        String[] secondlevel_macrofolders = engine.get_folders((String) toplevel_macrofolders_ComboBox.getSelectedItem(), 1);
        if (secondlevel_macrofolders != null && secondlevel_macrofolders.length > 0) {
            secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(secondlevel_macrofolders);
            secondlevel_macrofolders_ComboBox.setModel(secondlevel_macrofolders_dcbm);
            secondlevel_macrofolders_ComboBox.setEnabled(true);
            secondlevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String) secondlevel_macrofolders_dcbm.getSelectedItem()));
        } else {
            secondlevel_macrofolders_dcbm = new DefaultComboBoxModel(new String[]{ dummy_no_selection });
            secondlevel_macrofolders_ComboBox.setModel(secondlevel_macrofolders_dcbm);
            secondlevel_macrofolders_ComboBox.setEnabled(false);
            secondlevel_macrofolders_ComboBox.setToolTipText(null);
        }
        update_macro_menu();
}//GEN-LAST:event_toplevel_macrofolders_ComboBoxActionPerformed

    private void secondlevel_macrofolders_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_secondlevel_macrofolders_ComboBoxActionPerformed
        secondlevel_macrofolders_ComboBox.setToolTipText(engine.describe_folder((String)secondlevel_macrofolders_dcbm.getSelectedItem()));
        update_macro_menu();
}//GEN-LAST:event_secondlevel_macrofolders_ComboBoxActionPerformed

    private void enable_macro_folders_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_macro_folders_CheckBoxMenuItemActionPerformed
        update_macro_menu();
}//GEN-LAST:event_enable_macro_folders_CheckBoxMenuItemActionPerformed

    private void enable_devicegroups_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enable_devicegroups_CheckBoxMenuItemActionPerformed
        update_device_menu();
        devicegroup_ComboBox.setEnabled(enable_devicegroups_CheckBoxMenuItem.isSelected());
}//GEN-LAST:event_enable_devicegroups_CheckBoxMenuItemActionPerformed

    private void device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_device_ComboBoxActionPerformed
        update_command_menu();
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
        command_argument_TextField.setEnabled(hm.get_arguments(device, ir_code.decode_command(cmdname), commandset.any) == 1);
    }//GEN-LAST:event_command_ComboBoxActionPerformed

    private void update_device_commands_menu() {
        // Do not imlement this until Enum commandtype.
        /*
        String dev = (String) deviceclasses_dcbm.getSelectedItem();
        try {
            device dvc = new device(dev);
            int[] commands = dvc.get_commands(commandset.any);
            String[] commandnames = new String[commands.length];
        } catch (IOException e) {

        }
        */
    }

    private void selecting_device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selecting_device_ComboBoxActionPerformed
        update_src_device_menu();
        update_zone_menu();
}//GEN-LAST:event_selecting_device_ComboBoxActionPerformed

    private void src_device_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_src_device_ComboBoxActionPerformed
 
    }//GEN-LAST:event_src_device_ComboBoxActionPerformed

    private void commandButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_commandButtonActionPerformed
        String result = null;
        String cmd_name = (String) commands_dcbm.getSelectedItem();
        int cmd = ir_code.decode_command(cmd_name);
        String device = (String) devices_dcbm.getSelectedItem();
        console_TextArea.append(cmd_formatter.format(device + " " + cmd_name) + "\n");
        int no_required_args = hm.get_arguments(device, cmd, commandset.any);
        //warning("# args: " + no_required_args);
        String arg0 = command_argument_TextField.getText().trim();
        String[] args = arg0.equals("") ? new String[0] : new String[] { arg0 };
        if (args.length < no_required_args) {
            warning("To few arguments to command. Not executed.");
            return;
        } else if (args.length > no_required_args) {
            // Should not happen.
            warning("Excess arguments ignored");
        }
        
        try {
            result = hm.do_command(device, cmd, args, commandset.any, 1, toggletype.no_toggle, false);
        //} catch (non_existing_command_exception e) {
        } catch (InterruptedException e) {
            warning("Interrupted");
        }
        if (!result.equals(""))
            console_TextArea.append(formatter.format(result) + "\n");
    }//GEN-LAST:event_commandButtonActionPerformed

    private void sort_commands_CheckBoxMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sort_commands_CheckBoxMenuItemActionPerformed
        update_command_menu();
    }//GEN-LAST:event_sort_commands_CheckBoxMenuItemActionPerformed

    private void copy_console_to_clipboard_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copy_console_to_clipboard_MenuItemActionPerformed
        not_yet_implemented("Copy Console to clipboard");
    }//GEN-LAST:event_copy_console_to_clipboard_MenuItemActionPerformed

    private void clear_console_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clear_console_MenuItemActionPerformed
        console_TextArea.setText(null);
    }//GEN-LAST:event_clear_console_MenuItemActionPerformed

    private void select_ButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_select_ButtonActionPerformed
        String device = (String) selecting_devices_dcbm.getSelectedItem();
        String src_device = (String) src_devices_dcbm.getSelectedItem();
        String zone = (String) zones_dcbm.getSelectedItem();
        if (zone.startsWith("-"))
            zone = null;
        mediatype mt = (mediatype) audio_video_ComboBox.getSelectedItem();
        console_TextArea.append(cmd_formatter.format("--select " + device + " " + src_device
                + (zone != null ? (" (zone = " + zone + ")") : "") + " (" + mt + ")") + "\n");
        boolean success = false;
        try {
            success = hm.select(device, src_device, commandset.any, zone, mt, null /*connectiontype*/);
        } catch (InterruptedException e) {
            warning("Interrupted");
        }
        if (!success)
            console_TextArea.append("Failed\n");
    }//GEN-LAST:event_select_ButtonActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        do_exit();
    }//GEN-LAST:event_formWindowClosed

    private void deviceclass_ComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deviceclass_ComboBoxActionPerformed
        update_device_commands_menu();
    }//GEN-LAST:event_deviceclass_ComboBoxActionPerformed

    private void browse_device_MenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browse_device_MenuItemActionPerformed
        try {
            hm.do_command((String)devices_dcbm.getSelectedItem(), commandnames.cmd_invalid, null, commandset.www, 1, toggletype.no_toggle, false);
        } catch (InterruptedException e) {
        }
    }//GEN-LAST:event_browse_device_MenuItemActionPerformed

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
    private javax.swing.JComboBox audio_video_ComboBox;
    private javax.swing.JMenuItem browse_device_MenuItem;
    private javax.swing.JMenuItem clear_console_MenuItem;
    private javax.swing.JButton commandButton;
    private javax.swing.JComboBox command_ComboBox;
    private javax.swing.JTextField command_argument_TextField;
    private javax.swing.JTextField commandno_TextField;
    private javax.swing.JTextArea console_TextArea;
    private javax.swing.JMenuItem consoletext_save_MenuItem;
    private javax.swing.JMenuItem contentMenuItem;
    private javax.swing.JMenuItem copy_console_to_clipboard_MenuItem;
    private javax.swing.JMenuItem debug_MenuItem;
    private javax.swing.JComboBox device_ComboBox;
    private javax.swing.JComboBox device_command_ComboBox;
    private javax.swing.JComboBox deviceclass_ComboBox;
    private javax.swing.JComboBox devicegroup_ComboBox;
    private javax.swing.JTextField deviceno_TextField;
    private javax.swing.JMenu editMenu;
    private javax.swing.JCheckBoxMenuItem enable_devicegroups_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem enable_macro_folders_CheckBoxMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem homeMenuItem;
    private javax.swing.JCheckBoxMenuItem immediate_execution_commands_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem immediate_execution_macros_CheckBoxMenuItem;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JButton macroButton;
    private javax.swing.JComboBox macroComboBox;
    private javax.swing.JMenuItem macroMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JTabbedPane parentTabbedPane;
    private javax.swing.JComboBox protocol_ComboBox;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JComboBox secondlevel_macrofolders_ComboBox;
    private javax.swing.JButton select_Button;
    private javax.swing.JComboBox selecting_device_ComboBox;
    private javax.swing.JCheckBoxMenuItem sort_commands_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sort_devices_CheckBoxMenuItem;
    private javax.swing.JCheckBoxMenuItem sort_macros_CheckBoxMenuItem;
    private javax.swing.JComboBox src_device_ComboBox;
    private javax.swing.JTextField subdevice_TextField;
    private javax.swing.JComboBox toggle_ComboBox;
    private javax.swing.JComboBox toplevel_macrofolders_ComboBox;
    private javax.swing.JCheckBoxMenuItem verbose_CheckBoxMenuItem;
    private javax.swing.JComboBox zones_ComboBox;
    // End of variables declaration//GEN-END:variables

    private about_popup aboutBox;
}
