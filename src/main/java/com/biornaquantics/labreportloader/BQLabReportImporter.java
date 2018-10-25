/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author tihor
 */
public class BQLabReportImporter extends javax.swing.JFrame {
    long executeEvery=60*5*1000L;
    Timer timer;
    String sToken="",sRefreshToken="";
    Map<String,String> username_to_id_map=new HashMap<>();
    List<String> userNames=new ArrayList<>();
    List<String> internalMarkers=new ArrayList<>();
    List<String> labReportNames=Arrays.asList(LabReportNames.reportNames);
    int currentPage=0;
    PDDocument document=null;
    PDFRenderer renderer=null;
    private float RENDER_DPI=200;
    String sAutoDetectLocation="",sCMEPLocation="",sIgG4Location="",sGIMAPLocation="",sCMEPToPDFLocation="",sBQEmail="",sBQPassword="",sPDFToInternal="",sTokenPrefix="";
    Java2sAutoComboBox editInternalSlugBox,editLabReportBox;
    String sCurrentReport="";
    List<JSONObject> pdf_location_mappings;
    Map<String,String> lab_to_internal_mappings;
    BufferedImage originalImage=null;
    TableRowSorter<TableModel> sorter=null;
    RestAPIURLs restAPIURLs;

    /**
     * Creates new form BQLabReportImporter
     */
    public BQLabReportImporter() {
        Properties prop=new Properties();
        InputStream input = null;
        
        try{
            input=new FileInputStream(System.getProperty("user.dir")+"/config.properties");
            prop.load(input);
            sAutoDetectLocation=prop.getProperty("AutoDetectLocation");
            sCMEPLocation=prop.getProperty("CMEPLocation");
            sCMEPToPDFLocation=prop.getProperty("CMEP_PDF_location");
            sIgG4Location=prop.getProperty("IgG4Location");
            sGIMAPLocation=prop.getProperty("GIMAPLocation");
            sPDFToInternal=prop.getProperty("pdf_to_internal");
            sBQEmail=prop.getProperty("BQEmail");
            sBQPassword=prop.getProperty("BQPassword");
            sTokenPrefix=prop.getProperty("TokenPrefix");
            
            RestAPIURLs restAPIURLs=new RestAPIURLs();
        }catch(IOException e){
            String message="Error loading properties file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        try{
            get_login_token();
        }catch(IOException e){
            String message="IOException occured while loading BQ.com token.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(RESTAPIException e){
            String message="RESTAPI Exception occured while loading BQ.com Token.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        populate_markerKeys();
        internalMarkers.add(0,"");
        editInternalSlugBox=new Java2sAutoComboBox(internalMarkers);
        //labReportNames.add(0,"");
        editLabReportBox=new Java2sAutoComboBox(labReportNames);
        
        initComponents();
        TableColumnModel colModel=jTablePDF.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(10);  
        colModel.getColumn(1).setPreferredWidth(5);
        TableColumn col=colModel.getColumn(2);    
        col.setCellEditor(new DefaultCellEditor(editInternalSlugBox));
        col=jTableMappingPDFToInternal.getColumnModel().getColumn(2);
        col.setCellEditor(new DefaultCellEditor(editInternalSlugBox));
        col=jTableMappingPDFToInternal.getColumnModel().getColumn(0);
        col.setCellEditor(new DefaultCellEditor(editLabReportBox));
        
        sorter=new TableRowSorter<TableModel>(jTableMappingPDFToInternal.getModel());
        jTableMappingPDFToInternal.setRowSorter(sorter);
        
        jTablePDF.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent tme) {
                int numRows=jTablePDF.getRowCount();
                int numUpdates=0;
                for(int i=0;i<numRows;i++){
                    String sLabMarker=jTablePDF.getValueAt(i,0).toString().trim();
                    String sValue=jTablePDF.getValueAt(i,1).toString().trim();
                    String sInternalMarker=jTablePDF.getValueAt(i,2).toString().trim();
                    if(sLabMarker.length()>0 && sValue.length()>0 && sInternalMarker.length()>0)
                        numUpdates++;
                }
                jLabelTableStatus.setText("Table: "+numRows+" Rows, "+numUpdates+" to upload");
            }
        });
        this.setTitle("Biorna Quantics Lab Report Importer");
        TimerTask repeatedTask=new TimerTask() {
            @Override
            public void run() {
                try{
                    get_login_token();
                }catch(IOException e){
                    String message="IOException occured while loading BQ.com token.\n"+e.toString();
                    System.out.println(message);
                    JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
                    cancel();
                }catch(RESTAPIException e){
                    String message="RESTAPI Exception occured while loading BQ.com Token.\n"+e.toString();
                    System.out.println(message);
                    JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
                    cancel();
                }
                String thisMoment = DateTimeFormatter.ofPattern("HH:mm:ss")
                                  .withZone(ZoneOffset.systemDefault())
                                  .format(Instant.now());
                jLabelRESTAPIStatus.setText("Token Refresh: "+thisMoment);
                if(sToken.length()>0)
                    jLabelRESTAPIStatus.setBackground(new java.awt.Color(0,142, 0));
                else
                    jLabelRESTAPIStatus.setBackground(new java.awt.Color(255,0, 0));
            }
        };
        timer=new Timer("tokenRefresh");
        timer.scheduleAtFixedRate(repeatedTask,1000L,executeEvery);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialogSettings = new javax.swing.JDialog();
        jLabelBQEmail = new javax.swing.JLabel();
        jTextFieldBQEmail = new javax.swing.JTextField();
        jLabelBQPassword = new javax.swing.JLabel();
        jPasswordFieldBQPassword = new javax.swing.JPasswordField();
        jLabelAutoDetectDirectory = new javax.swing.JLabel();
        jPanelAutoDetectDirectory = new javax.swing.JPanel();
        jTextFieldAutoDetectDirectory = new javax.swing.JTextField();
        jButtonAutoDetectDirectory = new javax.swing.JButton();
        jLabelCMEPDirectory = new javax.swing.JLabel();
        jPanelCMEPDirectory = new javax.swing.JPanel();
        jTextFieldCMEPDirectory = new javax.swing.JTextField();
        jButtonCMEPDirectory = new javax.swing.JButton();
        jLabelIgG4Directory = new javax.swing.JLabel();
        jPanelIgG4Directory = new javax.swing.JPanel();
        jTextFieldIgG4Directory = new javax.swing.JTextField();
        jButtonIgG4Directory = new javax.swing.JButton();
        jLabelGIMAPDirectory = new javax.swing.JLabel();
        jPanelGIMAPDirectory = new javax.swing.JPanel();
        jTextFieldGIMAPDirectory = new javax.swing.JTextField();
        jButtonGIMAPDirectory = new javax.swing.JButton();
        jLabelCMEPPDFMapping = new javax.swing.JLabel();
        jPanelCMEPPDFMapping = new javax.swing.JPanel();
        jTextFieldCMEPPDFMapping = new javax.swing.JTextField();
        jButtonCMEPPDFMapping = new javax.swing.JButton();
        jLabelLabToInternalMarker = new javax.swing.JLabel();
        jPanelCMEPLabToInternalMapping = new javax.swing.JPanel();
        jTextFieldLabToInternalMarker = new javax.swing.JTextField();
        jButtonLabToInternalMapping = new javax.swing.JButton();
        jPanelEmpty = new javax.swing.JPanel();
        jPanelButtons = new javax.swing.JPanel();
        jButtonTestDBConnection = new javax.swing.JButton();
        jButtonSave = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();
        jDialogMapping = new javax.swing.JDialog();
        jPanelMappingMain = new javax.swing.JPanel();
        jPanelMappingPDFSelector = new javax.swing.JPanel();
        jLabelMappingPDFSelector = new javax.swing.JLabel();
        jComboBoxPDFMappingSelector = new javax.swing.JComboBox<>();
        jScrollPaneMappingPDFToInternal = new javax.swing.JScrollPane();
        jTableMappingPDFToInternal = new javax.swing.JTable();
        jPanelMappingButtons = new javax.swing.JPanel();
        jButtonMappingSave = new javax.swing.JButton();
        jButtonMappingClose = new javax.swing.JButton();
        jPopupMenuPDF = new javax.swing.JPopupMenu();
        jMenuItemInsertPDF = new javax.swing.JMenuItem();
        jMenuItemDeletePDF = new javax.swing.JMenuItem();
        jPopupMenuPDFMapping = new javax.swing.JPopupMenu();
        jMenuItemInsertCMEP = new javax.swing.JMenuItem();
        jMenuItemDeleteCMEP = new javax.swing.JMenuItem();
        jDialogUpload = new javax.swing.JDialog();
        jPanelUploadPane = new javax.swing.JPanel();
        jPanelUploadInputs = new javax.swing.JPanel();
        jLabelUploadUser = new javax.swing.JLabel();
        jComboBoxUploadUser = new Java2sAutoComboBox(userNames);
        jLabelPanel = new javax.swing.JLabel();
        jTextFieldUploadPanel = new javax.swing.JTextField();
        jLabelDNAReportTemplace = new javax.swing.JLabel();
        jTextFieldUploadReportTemplate = new javax.swing.JTextField();
        jPanelUploadButtons = new javax.swing.JPanel();
        jButtonUploadSave = new javax.swing.JButton();
        jButtonUploadClose = new javax.swing.JButton();
        jPanelUploadStatus = new javax.swing.JPanel();
        jScrollPaneUploadStatus = new javax.swing.JScrollPane();
        jTextAreaUploadStatus = new javax.swing.JTextArea();
        jPanelMain = new javax.swing.JPanel();
        jPanelPDF = new javax.swing.JPanel();
        jToolBarRecordParser = new javax.swing.JToolBar();
        jButtonFirst = new javax.swing.JButton();
        jButtonPrevious = new javax.swing.JButton();
        jLabelRecordStatus = new javax.swing.JLabel();
        jButtonNext = new javax.swing.JButton();
        jButtonLast = new javax.swing.JButton();
        jLabelPDF = new javax.swing.JLabel();
        jPanelData = new javax.swing.JPanel();
        jPanelUserDetails = new javax.swing.JPanel();
        jLabelName = new javax.swing.JLabel();
        jTextFieldName = new javax.swing.JTextField();
        jButtonUpload = new javax.swing.JButton();
        jLabelDateCollected = new javax.swing.JLabel();
        jTextFieldDateCollected = new javax.swing.JTextField();
        jButtonToCSV = new javax.swing.JButton();
        jScrollPanePDF = new javax.swing.JScrollPane();
        jTablePDF = new javax.swing.JTable();
        jToolBarLoaderButtons = new javax.swing.JToolBar();
        jButtonAutoDetect = new javax.swing.JButton();
        jButtonCMEP = new javax.swing.JButton();
        jButtonIgG4 = new javax.swing.JButton();
        jButtonGIMAP = new javax.swing.JButton();
        jPanelStatusbar = new javax.swing.JPanel();
        jLabelStatus = new javax.swing.JLabel();
        jPanelStatusBarSubBar = new javax.swing.JPanel();
        jLabelTableStatus = new javax.swing.JLabel();
        jPanelStatusBarSubSubBar = new javax.swing.JPanel();
        jLabelRESTAPIStatus = new javax.swing.JLabel();
        jMenuBarMainMenu = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuItemSettings = new javax.swing.JMenuItem();
        jMenuItemMappings = new javax.swing.JMenuItem();
        jMenuLabReports = new javax.swing.JMenu();
        jMenuItemAutoDetect = new javax.swing.JMenuItem();
        jMenuItemCMEP = new javax.swing.JMenuItem();
        jMenuItemIgG4 = new javax.swing.JMenuItem();
        jMenuItemGIMAP = new javax.swing.JMenuItem();

        jDialogSettings.setTitle("Settings");
        jDialogSettings.setModal(true);
        jDialogSettings.getContentPane().setLayout(new java.awt.GridLayout(9, 2));

        jLabelBQEmail.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBQEmail.setText("BQ Email:");
        jDialogSettings.getContentPane().add(jLabelBQEmail);
        jDialogSettings.getContentPane().add(jTextFieldBQEmail);

        jLabelBQPassword.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelBQPassword.setText("BQ Password:");
        jDialogSettings.getContentPane().add(jLabelBQPassword);
        jDialogSettings.getContentPane().add(jPasswordFieldBQPassword);

        jLabelAutoDetectDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelAutoDetectDirectory.setText("Auto Detect:");
        jDialogSettings.getContentPane().add(jLabelAutoDetectDirectory);

        jPanelAutoDetectDirectory.setLayout(new java.awt.BorderLayout());
        jPanelAutoDetectDirectory.add(jTextFieldAutoDetectDirectory, java.awt.BorderLayout.CENTER);

        jButtonAutoDetectDirectory.setText("...");
        jButtonAutoDetectDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAutoDetectDirectoryActionPerformed(evt);
            }
        });
        jPanelAutoDetectDirectory.add(jButtonAutoDetectDirectory, java.awt.BorderLayout.EAST);

        jDialogSettings.getContentPane().add(jPanelAutoDetectDirectory);

        jLabelCMEPDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelCMEPDirectory.setText("CMEP Directory:");
        jDialogSettings.getContentPane().add(jLabelCMEPDirectory);

        jPanelCMEPDirectory.setLayout(new java.awt.BorderLayout());
        jPanelCMEPDirectory.add(jTextFieldCMEPDirectory, java.awt.BorderLayout.CENTER);

        jButtonCMEPDirectory.setText("...");
        jButtonCMEPDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCMEPDirectoryActionPerformed(evt);
            }
        });
        jPanelCMEPDirectory.add(jButtonCMEPDirectory, java.awt.BorderLayout.EAST);

        jDialogSettings.getContentPane().add(jPanelCMEPDirectory);

        jLabelIgG4Directory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelIgG4Directory.setText("IgG4 Directory:");
        jDialogSettings.getContentPane().add(jLabelIgG4Directory);

        jPanelIgG4Directory.setLayout(new java.awt.BorderLayout());
        jPanelIgG4Directory.add(jTextFieldIgG4Directory, java.awt.BorderLayout.CENTER);

        jButtonIgG4Directory.setText("...");
        jButtonIgG4Directory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonIgG4DirectoryActionPerformed(evt);
            }
        });
        jPanelIgG4Directory.add(jButtonIgG4Directory, java.awt.BorderLayout.EAST);

        jDialogSettings.getContentPane().add(jPanelIgG4Directory);

        jLabelGIMAPDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelGIMAPDirectory.setText("GI-MAP Directory:");
        jDialogSettings.getContentPane().add(jLabelGIMAPDirectory);

        jPanelGIMAPDirectory.setLayout(new java.awt.BorderLayout());
        jPanelGIMAPDirectory.add(jTextFieldGIMAPDirectory, java.awt.BorderLayout.CENTER);

        jButtonGIMAPDirectory.setText("...");
        jButtonGIMAPDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGIMAPDirectoryActionPerformed(evt);
            }
        });
        jPanelGIMAPDirectory.add(jButtonGIMAPDirectory, java.awt.BorderLayout.LINE_END);

        jDialogSettings.getContentPane().add(jPanelGIMAPDirectory);

        jLabelCMEPPDFMapping.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelCMEPPDFMapping.setText("CMEP PDF Mapping:");
        jDialogSettings.getContentPane().add(jLabelCMEPPDFMapping);

        jPanelCMEPPDFMapping.setLayout(new java.awt.BorderLayout());
        jPanelCMEPPDFMapping.add(jTextFieldCMEPPDFMapping, java.awt.BorderLayout.CENTER);

        jButtonCMEPPDFMapping.setText("...");
        jButtonCMEPPDFMapping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCMEPPDFMappingActionPerformed(evt);
            }
        });
        jPanelCMEPPDFMapping.add(jButtonCMEPPDFMapping, java.awt.BorderLayout.EAST);

        jDialogSettings.getContentPane().add(jPanelCMEPPDFMapping);

        jLabelLabToInternalMarker.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelLabToInternalMarker.setText("Lab to Internal Marker:");
        jDialogSettings.getContentPane().add(jLabelLabToInternalMarker);

        jPanelCMEPLabToInternalMapping.setLayout(new java.awt.BorderLayout());
        jPanelCMEPLabToInternalMapping.add(jTextFieldLabToInternalMarker, java.awt.BorderLayout.CENTER);

        jButtonLabToInternalMapping.setText("...");
        jButtonLabToInternalMapping.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLabToInternalMappingActionPerformed(evt);
            }
        });
        jPanelCMEPLabToInternalMapping.add(jButtonLabToInternalMapping, java.awt.BorderLayout.LINE_END);

        jDialogSettings.getContentPane().add(jPanelCMEPLabToInternalMapping);
        jDialogSettings.getContentPane().add(jPanelEmpty);

        jPanelButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButtonTestDBConnection.setText("Test");
        jButtonTestDBConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTestDBConnectionActionPerformed(evt);
            }
        });
        jPanelButtons.add(jButtonTestDBConnection);

        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });
        jPanelButtons.add(jButtonSave);

        jButtonClose.setText("Close");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });
        jPanelButtons.add(jButtonClose);

        jDialogSettings.getContentPane().add(jPanelButtons);

        jDialogMapping.setTitle("Mappings");
        jDialogMapping.setModal(true);

        jPanelMappingMain.setLayout(new java.awt.BorderLayout());

        jPanelMappingPDFSelector.setLayout(new java.awt.GridLayout(1, 2));

        jLabelMappingPDFSelector.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabelMappingPDFSelector.setText("Filter");
        jPanelMappingPDFSelector.add(jLabelMappingPDFSelector);

        jComboBoxPDFMappingSelector.setModel(new DefaultComboBoxModel(LabReportNames.reportNames));
        jComboBoxPDFMappingSelector.setSelectedIndex(0);
        jComboBoxPDFMappingSelector.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxPDFMappingSelectorItemStateChanged(evt);
            }
        });
        jPanelMappingPDFSelector.add(jComboBoxPDFMappingSelector);

        jPanelMappingMain.add(jPanelMappingPDFSelector, java.awt.BorderLayout.NORTH);

        jTableMappingPDFToInternal.setAutoCreateRowSorter(true);
        jTableMappingPDFToInternal.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Lab Report", "Lab Marker", "Internal Marker"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTableMappingPDFToInternal.setComponentPopupMenu(jPopupMenuPDFMapping);
        jTableMappingPDFToInternal.setRowHeight(22);
        jTableMappingPDFToInternal.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTableMappingPDFToInternalMouseClicked(evt);
            }
        });
        jScrollPaneMappingPDFToInternal.setViewportView(jTableMappingPDFToInternal);

        jPanelMappingMain.add(jScrollPaneMappingPDFToInternal, java.awt.BorderLayout.CENTER);

        jDialogMapping.getContentPane().add(jPanelMappingMain, java.awt.BorderLayout.CENTER);

        jPanelMappingButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButtonMappingSave.setText("Save");
        jButtonMappingSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonMappingSaveActionPerformed(evt);
            }
        });
        jPanelMappingButtons.add(jButtonMappingSave);

        jButtonMappingClose.setText("Close");
        jButtonMappingClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonMappingCloseActionPerformed(evt);
            }
        });
        jPanelMappingButtons.add(jButtonMappingClose);

        jDialogMapping.getContentPane().add(jPanelMappingButtons, java.awt.BorderLayout.SOUTH);

        jMenuItemInsertPDF.setText("Insert Row");
        jMenuItemInsertPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemInsertPDFActionPerformed(evt);
            }
        });
        jPopupMenuPDF.add(jMenuItemInsertPDF);

        jMenuItemDeletePDF.setText("Delete Row");
        jMenuItemDeletePDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeletePDFActionPerformed(evt);
            }
        });
        jPopupMenuPDF.add(jMenuItemDeletePDF);

        jMenuItemInsertCMEP.setText("Insert Row");
        jMenuItemInsertCMEP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemInsertCMEPActionPerformed(evt);
            }
        });
        jPopupMenuPDFMapping.add(jMenuItemInsertCMEP);

        jMenuItemDeleteCMEP.setText("Delete Row");
        jMenuItemDeleteCMEP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemDeleteCMEPActionPerformed(evt);
            }
        });
        jPopupMenuPDFMapping.add(jMenuItemDeleteCMEP);

        jDialogUpload.setTitle("Upload to BQ.com");
        jDialogUpload.setModal(true);

        jPanelUploadPane.setLayout(new java.awt.BorderLayout());

        jPanelUploadInputs.setLayout(new java.awt.GridLayout(3, 2));

        jLabelUploadUser.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelUploadUser.setText("Panel User");
        jPanelUploadInputs.add(jLabelUploadUser);

        jComboBoxUploadUser.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jComboBoxUploadUserItemStateChanged(evt);
            }
        });
        jPanelUploadInputs.add(jComboBoxUploadUser);

        jLabelPanel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelPanel.setText("Panel");
        jPanelUploadInputs.add(jLabelPanel);

        jTextFieldUploadPanel.setEnabled(false);
        jPanelUploadInputs.add(jTextFieldUploadPanel);

        jLabelDNAReportTemplace.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelDNAReportTemplace.setText("DNA Report Template");
        jPanelUploadInputs.add(jLabelDNAReportTemplace);
        jPanelUploadInputs.add(jTextFieldUploadReportTemplate);

        jPanelUploadPane.add(jPanelUploadInputs, java.awt.BorderLayout.CENTER);

        jPanelUploadButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jButtonUploadSave.setText("Save");
        jButtonUploadSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadSaveActionPerformed(evt);
            }
        });
        jPanelUploadButtons.add(jButtonUploadSave);

        jButtonUploadClose.setText("Close");
        jButtonUploadClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadCloseActionPerformed(evt);
            }
        });
        jPanelUploadButtons.add(jButtonUploadClose);

        jPanelUploadPane.add(jPanelUploadButtons, java.awt.BorderLayout.SOUTH);

        jDialogUpload.getContentPane().add(jPanelUploadPane, java.awt.BorderLayout.CENTER);

        jPanelUploadStatus.setLayout(new java.awt.BorderLayout());

        jTextAreaUploadStatus.setColumns(20);
        jTextAreaUploadStatus.setRows(5);
        jScrollPaneUploadStatus.setViewportView(jTextAreaUploadStatus);

        jPanelUploadStatus.add(jScrollPaneUploadStatus, java.awt.BorderLayout.CENTER);

        jDialogUpload.getContentPane().add(jPanelUploadStatus, java.awt.BorderLayout.SOUTH);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanelMain.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanelMain.setLayout(new java.awt.GridLayout(1, 2));

        jPanelPDF.setBorder(javax.swing.BorderFactory.createTitledBorder("PDF"));
        jPanelPDF.setLayout(new java.awt.BorderLayout());

        jToolBarRecordParser.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBarRecordParser.setRollover(true);

        jButtonFirst.setText("<<");
        jButtonFirst.setFocusable(false);
        jButtonFirst.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonFirst.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonFirst.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonFirstActionPerformed(evt);
            }
        });
        jToolBarRecordParser.add(jButtonFirst);

        jButtonPrevious.setText("<");
        jButtonPrevious.setFocusable(false);
        jButtonPrevious.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonPrevious.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonPrevious.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPreviousActionPerformed(evt);
            }
        });
        jToolBarRecordParser.add(jButtonPrevious);

        jLabelRecordStatus.setText("Page 0 of 0  ");
        jToolBarRecordParser.add(jLabelRecordStatus);

        jButtonNext.setText(">");
        jButtonNext.setFocusable(false);
        jButtonNext.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonNext.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonNextActionPerformed(evt);
            }
        });
        jToolBarRecordParser.add(jButtonNext);

        jButtonLast.setText(">>");
        jButtonLast.setFocusable(false);
        jButtonLast.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonLast.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonLast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLastActionPerformed(evt);
            }
        });
        jToolBarRecordParser.add(jButtonLast);

        jPanelPDF.add(jToolBarRecordParser, java.awt.BorderLayout.PAGE_START);
        jPanelPDF.add(jLabelPDF, java.awt.BorderLayout.CENTER);

        jPanelMain.add(jPanelPDF);

        jPanelData.setBorder(javax.swing.BorderFactory.createTitledBorder("Data"));
        jPanelData.setLayout(new java.awt.BorderLayout());

        jPanelUserDetails.setLayout(new java.awt.GridLayout(2, 3));

        jLabelName.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelName.setText("Name:");
        jPanelUserDetails.add(jLabelName);
        jPanelUserDetails.add(jTextFieldName);

        jButtonUpload.setText("Upload");
        jButtonUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUploadActionPerformed(evt);
            }
        });
        jPanelUserDetails.add(jButtonUpload);

        jLabelDateCollected.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelDateCollected.setText("Date Collected:");
        jPanelUserDetails.add(jLabelDateCollected);
        jPanelUserDetails.add(jTextFieldDateCollected);

        jButtonToCSV.setText("To CSV");
        jButtonToCSV.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonToCSVActionPerformed(evt);
            }
        });
        jPanelUserDetails.add(jButtonToCSV);

        jPanelData.add(jPanelUserDetails, java.awt.BorderLayout.PAGE_START);

        jScrollPanePDF.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        jTablePDF.setAutoCreateRowSorter(true);
        jTablePDF.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Lab Marker", "Value", "Internal Marker"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTablePDF.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_NEXT_COLUMN);
        jTablePDF.setComponentPopupMenu(jPopupMenuPDF);
        jTablePDF.setRowHeight(22);
        jTablePDF.getColumnModel().getColumn(0).setPreferredWidth(100);
        jTablePDF.getColumnModel().getColumn(1).setPreferredWidth(15);
        jTablePDF.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTablePDFMousePressed(evt);
            }
        });
        jScrollPanePDF.setViewportView(jTablePDF);

        jPanelData.add(jScrollPanePDF, java.awt.BorderLayout.CENTER);

        jPanelMain.add(jPanelData);

        getContentPane().add(jPanelMain, java.awt.BorderLayout.CENTER);

        jToolBarLoaderButtons.setRollover(true);

        jButtonAutoDetect.setText("Auto Detect");
        jButtonAutoDetect.setFocusable(false);
        jButtonAutoDetect.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonAutoDetect.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonAutoDetect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAutoDetectActionPerformed(evt);
            }
        });
        jToolBarLoaderButtons.add(jButtonAutoDetect);

        jButtonCMEP.setText("CMEP");
        jButtonCMEP.setFocusable(false);
        jButtonCMEP.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonCMEP.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonCMEP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCMEPActionPerformed(evt);
            }
        });
        jToolBarLoaderButtons.add(jButtonCMEP);

        jButtonIgG4.setText("IgG4");
        jButtonIgG4.setFocusable(false);
        jButtonIgG4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonIgG4.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonIgG4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonIgG4ActionPerformed(evt);
            }
        });
        jToolBarLoaderButtons.add(jButtonIgG4);

        jButtonGIMAP.setText("GI-MAP");
        jButtonGIMAP.setFocusable(false);
        jButtonGIMAP.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButtonGIMAP.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButtonGIMAP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGIMAPActionPerformed(evt);
            }
        });
        jToolBarLoaderButtons.add(jButtonGIMAP);

        getContentPane().add(jToolBarLoaderButtons, java.awt.BorderLayout.PAGE_START);

        jPanelStatusbar.setLayout(new java.awt.GridLayout(1, 2));

        jLabelStatus.setText("Status:");
        jPanelStatusbar.add(jLabelStatus);

        jPanelStatusBarSubBar.setLayout(new java.awt.GridLayout(1, 2));

        jLabelTableStatus.setText("Table: 0 Rows, 0 to upload");
        jPanelStatusBarSubBar.add(jLabelTableStatus);

        jPanelStatusBarSubSubBar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jLabelRESTAPIStatus.setBackground(new java.awt.Color(255, 0, 0));
        jLabelRESTAPIStatus.setForeground(new java.awt.Color(255, 255, 255));
        jLabelRESTAPIStatus.setText("Token Refresh: 00:00:00");
        jLabelRESTAPIStatus.setOpaque(true);
        jPanelStatusBarSubSubBar.add(jLabelRESTAPIStatus);

        jPanelStatusBarSubBar.add(jPanelStatusBarSubSubBar);

        jPanelStatusbar.add(jPanelStatusBarSubBar);

        getContentPane().add(jPanelStatusbar, java.awt.BorderLayout.SOUTH);

        jMenuFile.setText("File");

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBarMainMenu.add(jMenuFile);

        jMenuEdit.setText("Edit");

        jMenuItemSettings.setText("Settings");
        jMenuItemSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSettingsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemSettings);

        jMenuItemMappings.setText("Mappings");
        jMenuItemMappings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemMappingsActionPerformed(evt);
            }
        });
        jMenuEdit.add(jMenuItemMappings);

        jMenuBarMainMenu.add(jMenuEdit);

        jMenuLabReports.setText("Lab Reports");

        jMenuItemAutoDetect.setText("Auto Detect");
        jMenuItemAutoDetect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemAutoDetectActionPerformed(evt);
            }
        });
        jMenuLabReports.add(jMenuItemAutoDetect);

        jMenuItemCMEP.setText("CMEP");
        jMenuItemCMEP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCMEPActionPerformed(evt);
            }
        });
        jMenuLabReports.add(jMenuItemCMEP);

        jMenuItemIgG4.setText("IgG4");
        jMenuItemIgG4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemIgG4ActionPerformed(evt);
            }
        });
        jMenuLabReports.add(jMenuItemIgG4);

        jMenuItemGIMAP.setText("GI-MAP");
        jMenuItemGIMAP.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemGIMAPActionPerformed(evt);
            }
        });
        jMenuLabReports.add(jMenuItemGIMAP);

        jMenuBarMainMenu.add(jMenuLabReports);

        setJMenuBar(jMenuBarMainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCMEPActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sCMEPLocation);
        if(sFileWithPath.length()>0)
            loadCMEPReport(sFileWithPath);
    }//GEN-LAST:event_jButtonCMEPActionPerformed

    private void jButtonIgG4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonIgG4ActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sIgG4Location);
        if(sFileWithPath.length()>0)
            loadIgG4Report(sFileWithPath);
    }//GEN-LAST:event_jButtonIgG4ActionPerformed

    private void jButtonFirstActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonFirstActionPerformed
        // TODO add your handling code here:
        if(document!=null){
            currentPage=1;
            renderPage();
        }
    }//GEN-LAST:event_jButtonFirstActionPerformed

    private void jButtonPreviousActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPreviousActionPerformed
        // TODO add your handling code here:
        if(document!=null){
            if(currentPage>1){
                currentPage--;
                renderPage();
            }
        }
    }//GEN-LAST:event_jButtonPreviousActionPerformed

    private void jButtonNextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonNextActionPerformed
        // TODO add your handling code here:
        if(document!=null){
            if(currentPage<document.getNumberOfPages()){
                currentPage++;
                renderPage();
            }
        }
    }//GEN-LAST:event_jButtonNextActionPerformed

    private void jButtonLastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLastActionPerformed
        // TODO add your handling code here:
        if(document!=null){
            currentPage=document.getNumberOfPages();
            renderPage();
        }
    }//GEN-LAST:event_jButtonLastActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        // TODO add your handling code here:
        if(originalImage!=null){
            BufferedImage image=resizeImage(originalImage,jLabelPDF.getHeight(),jLabelPDF.getWidth());
            ImageIcon iconLogo = new ImageIcon(image);
            jLabelPDF.setIcon(iconLogo);
        }
    }//GEN-LAST:event_formComponentResized

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        // TODO add your handling code here:
        timer.cancel();
        System.exit(0);
        
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCMEPActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sCMEPLocation);
        if(sFileWithPath.length()>0)
            loadCMEPReport(sFileWithPath);
    }//GEN-LAST:event_jMenuItemCMEPActionPerformed

    private void jMenuItemIgG4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemIgG4ActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sIgG4Location);
        if(sFileWithPath.length()>0)
            loadIgG4Report(sFileWithPath);
    }//GEN-LAST:event_jMenuItemIgG4ActionPerformed

    private void jButtonToCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToCSVActionPerformed
        // TODO add your handling code here:
        updateInternalMappings();
        if(sCurrentReport.length()>0 && jTablePDF.getRowCount()>0){
            JFileChooser fileChooser=new JFileChooser();
            int returnVal = fileChooser.showSaveDialog(new JFrame());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                try {
                    File file = fileChooser.getSelectedFile();
                    jLabelStatus.setText("Saving file "+file.toString());
                    PrintWriter os = new PrintWriter(file);
                    //os.println("Name\t"+jTextFieldName.getText());
                    //os.println("DateCollected\t"+jTextFieldDateCollected.getText());
                    os.println("pdf\tkey\tvalue\tmeasuredAt");
                    String thisMoment = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                  .withZone(ZoneOffset.UTC)
                                  .format(Instant.now());
                    String[] dateSplit=jTextFieldDateCollected.getText().split("/");
                    if(dateSplit.length==3){
                        if(dateSplit[0].length()==1)
                            dateSplit[0]="0"+dateSplit[0];
                        if(dateSplit[1].length()==1)
                            dateSplit[1]="0"+dateSplit[1];
                        thisMoment=dateSplit[2]+"-"+dateSplit[0]+"-"+dateSplit[1];
                    }
                    thisMoment+="T00:00:00.000Z";
                    for (int i = 0; i < jTablePDF.getRowCount(); i++) {
                        String sTemp=jTablePDF.getValueAt(i, 2).toString().trim();
                        if(sTemp.length()>0){
                            sTemp=jTablePDF.getValueAt(i, 0).toString().trim()+"\t"+sTemp;
                            sTemp+="\t"+jTablePDF.getValueAt(i, 1)+"\t"+thisMoment;
                            //for (int j = 0; j < jTablePDF.getColumnCount(); j++) {
                                //os.print(jTablePDF.getValueAt(i, j).toString() + "\t");
                            //}
                            os.println(sTemp);
                        }
                    }
                    os.close();
                    jLabelStatus.setText("Saving file "+file.toString()+"...Done");

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    String message="Error loading PDF.\n"+e.toString();
                    System.out.println(message);
                    JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_jButtonToCSVActionPerformed

    private void jMenuItemSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSettingsActionPerformed
        // TODO add your handling code here:
        jTextFieldAutoDetectDirectory.setText(sAutoDetectLocation);
        jTextFieldCMEPDirectory.setText(sCMEPLocation);
        jTextFieldIgG4Directory.setText(sIgG4Location);
        jTextFieldGIMAPDirectory.setText(sGIMAPLocation);
        jTextFieldCMEPPDFMapping.setText(sCMEPToPDFLocation);
        jTextFieldLabToInternalMarker.setText(sPDFToInternal);
        jTextFieldBQEmail.setText(sBQEmail);
        jPasswordFieldBQPassword.setText(sBQPassword);
        jDialogSettings.pack();
        jDialogSettings.setVisible(true);
    }//GEN-LAST:event_jMenuItemSettingsActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        // TODO add your handling code here:
        sAutoDetectLocation=jTextFieldAutoDetectDirectory.getText();
        sCMEPLocation=jTextFieldCMEPDirectory.getText();
        sIgG4Location=jTextFieldIgG4Directory.getText();
        sGIMAPLocation=jTextFieldGIMAPDirectory.getText();
        sCMEPToPDFLocation=jTextFieldCMEPPDFMapping.getText();
        sPDFToInternal=jTextFieldLabToInternalMarker.getText();
        sBQEmail=jTextFieldBQEmail.getText();
        sBQPassword=String.valueOf(jPasswordFieldBQPassword.getPassword());
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        Properties prop=new Properties();
        OutputStream output=null;
        try{
            output = new FileOutputStream(System.getProperty("user.dir")+"/config.properties");
            
            // set the properties value
            prop.setProperty("AutoDetectLocation",sAutoDetectLocation);
            prop.setProperty("CMEPLocation", sCMEPLocation);
            prop.setProperty("IgG4Location", sIgG4Location);
            prop.setProperty("GIMAPLocation",sGIMAPLocation);
            prop.setProperty("CMEP_PDF_location", sCMEPToPDFLocation);
            prop.setProperty("pdf_to_internal",sPDFToInternal);
            prop.setProperty("BQEmail", sBQEmail);
            prop.setProperty("BQPassword", sBQPassword);

            // save properties to project root folder
            prop.store(output, null);

        }catch(IOException e){
            String message="IOException occured while loading properties file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(NullPointerException e){
            String message="NullPointerException occured while loading properties file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        timer.cancel();
    }//GEN-LAST:event_formWindowClosing

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        // TODO add your handling code here:
        jDialogSettings.setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jMenuItemMappingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemMappingsActionPerformed
        // TODO add your handling code here:
        try{
            List<JSONObject> lab_to_internal_mappings=BQJSONParser.parseJSONFile(sPDFToInternal);
            DefaultTableModel tableModel = (DefaultTableModel) jTableMappingPDFToInternal.getModel();
            tableModel.setRowCount(0);
            int columns = tableModel.getColumnCount();
            for(int i=0;i<lab_to_internal_mappings.size();i++){
                JSONObject obj=lab_to_internal_mappings.get(i);
                String[] row=new String[columns];
                row[0]=obj.getString("LabReport");
                row[1]=obj.getString("LabName");
                row[2]=obj.getString("InternalName");
                tableModel.insertRow(i, row);
            }
            /*Map<String,String> lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sCMEPPDFToInternal);
            DefaultTableModel tableModel = (DefaultTableModel) jTableMappingCMEP.getModel();
            tableModel.setRowCount(0);
            int i=0;
            int columns = tableModel.getColumnCount();
            for(String key:lab_to_internal_mappings.keySet()){
                String[] row=new String[columns];
                row[0]=key;
                row[1]=lab_to_internal_mappings.get(key).toString();
                tableModel.insertRow(i++, row);
            }
            lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sIgG4PDFToInternal);
            tableModel = (DefaultTableModel) jTableMappingIgG4.getModel();
            tableModel.setRowCount(0);
            i=0;
            columns = tableModel.getColumnCount();
            for(String key:lab_to_internal_mappings.keySet()){
                String[] row=new String[columns];
                row[0]=key;
                row[1]=lab_to_internal_mappings.get(key).toString();
                tableModel.insertRow(i++, row);
            }
            lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sGIMAPPDFToInternal);
            tableModel = (DefaultTableModel) jTableMappingGIMAP.getModel();
            tableModel.setRowCount(0);
            i=0;
            columns = tableModel.getColumnCount();
            for(String key:lab_to_internal_mappings.keySet()){
                String[] row=new String[columns];
                row[0]=key;
                row[1]=lab_to_internal_mappings.get(key).toString();
                tableModel.insertRow(i++, row);
            }*/
        }catch(IOException e){
            String message="IOException occured while parsing internal mapping file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        jDialogMapping.pack();
        jDialogMapping.setVisible(true);
    }//GEN-LAST:event_jMenuItemMappingsActionPerformed

    private void jButtonMappingCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonMappingCloseActionPerformed
        // TODO add your handling code here:
        jDialogMapping.setVisible(false);
    }//GEN-LAST:event_jButtonMappingCloseActionPerformed

    private void jButtonMappingSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonMappingSaveActionPerformed
        // TODO add your handling code here:
        sorter.setRowFilter(null);
        try{
            PrintWriter os = new PrintWriter(sPDFToInternal);
            for (int i = 0; i < jTableMappingPDFToInternal.getRowCount(); i++) {
                String sLabReport=jTableMappingPDFToInternal.getValueAt(i,0).toString().trim();
                String sLabMarker=jTableMappingPDFToInternal.getValueAt(i,1).toString().trim();
                String sInternalMarker=jTableMappingPDFToInternal.getValueAt(i,2).toString().trim();
                if(sLabReport.length()>0 && sLabMarker.length()>0 && sInternalMarker.length()>0){
                    String sTemp="{\"LabReport\":\""+sLabReport+"\",\"LabName\":\""+sLabMarker+"\",\"InternalName\":\""+sInternalMarker+"\"}";
                    os.println(sTemp);
                }
            }
            os.close();
        }catch(FileNotFoundException e){
            String message="FileNotFoundException occured while saving mapping file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        String selectedValue=jComboBoxPDFMappingSelector.getItemAt(jComboBoxPDFMappingSelector.getSelectedIndex()).trim();
        if(selectedValue.length()>0)
            sorter.setRowFilter(RowFilter.regexFilter(selectedValue));
        else
            sorter.setRowFilter(null);
    }//GEN-LAST:event_jButtonMappingSaveActionPerformed

    private void jTablePDFMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTablePDFMousePressed
        // TODO add your handling code here:
        Point point = evt.getPoint();
        int currentRow = jTablePDF.rowAtPoint(point);
        jTablePDF.setRowSelectionInterval(currentRow, currentRow);
    }//GEN-LAST:event_jTablePDFMousePressed

    private void jMenuItemInsertPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemInsertPDFActionPerformed
        // TODO add your handling code here:
        DefaultTableModel tableModel = (DefaultTableModel)jTablePDF.getModel();
        int columns = tableModel.getColumnCount();
        
        Object[] row=new Object[columns];
        row[0]="";
        row[1]=0.0;
        row[2]="";
        int currentRow=jTablePDF.getSelectedRow();
        if(currentRow==-1)
            currentRow=0;
        tableModel.insertRow(currentRow,row);
        jTablePDF.setRowSelectionInterval(currentRow, currentRow);
    }//GEN-LAST:event_jMenuItemInsertPDFActionPerformed

    private void jMenuItemDeletePDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDeletePDFActionPerformed
        // TODO add your handling code here:
        DefaultTableModel tableModel = (DefaultTableModel)jTablePDF.getModel();
        tableModel.removeRow(jTablePDF.getSelectedRow());
    }//GEN-LAST:event_jMenuItemDeletePDFActionPerformed

    private void jMenuItemInsertCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemInsertCMEPActionPerformed
        // TODO add your handling code here:
        DefaultTableModel tableModel = (DefaultTableModel)jTableMappingPDFToInternal.getModel();
        int columns = tableModel.getColumnCount();
        
        String[] row=new String[columns];
        row[0]="";
        row[1]="";
        int currentRow=jTableMappingPDFToInternal.getSelectedRow();
        if(currentRow==-1)
            currentRow=0;
        tableModel.insertRow(currentRow,row);
        jTableMappingPDFToInternal.setRowSelectionInterval(currentRow, currentRow);
    }//GEN-LAST:event_jMenuItemInsertCMEPActionPerformed

    private void jMenuItemDeleteCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemDeleteCMEPActionPerformed
        // TODO add your handling code here:
        DefaultTableModel tableModel = (DefaultTableModel)jTableMappingPDFToInternal.getModel();
        tableModel.removeRow(jTableMappingPDFToInternal.getSelectedRow());
    }//GEN-LAST:event_jMenuItemDeleteCMEPActionPerformed

    private void jButtonUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUploadActionPerformed
        // TODO add your handling code here:
        updateInternalMappings();
        if(sCurrentReport.length()>0 && jTablePDF.getRowCount()>0){
            populate_usernames();
            ((Java2sAutoComboBox)jComboBoxUploadUser).setDataList(userNames);
            jTextAreaUploadStatus.setText("");
            jDialogUpload.pack();
            jDialogUpload.setVisible(true);
        }else{
            JOptionPane.showMessageDialog(new JFrame(), "Need to import a PDF to upload.", "Dialog",JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_jButtonUploadActionPerformed

    private void jButtonUploadCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUploadCloseActionPerformed
        // TODO add your handling code here:
        jDialogUpload.setVisible(false);
    }//GEN-LAST:event_jButtonUploadCloseActionPerformed

    private void jButtonUploadSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUploadSaveActionPerformed
        // TODO add your handling code here:
        String thisMoment = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                  .withZone(ZoneOffset.UTC)
                                  .format(Instant.now());
        String[] dateSplit=jTextFieldDateCollected.getText().split("/");
        if(dateSplit.length==3){
            if(dateSplit[0].length()==1)
                dateSplit[0]="0"+dateSplit[0];
            if(dateSplit[1].length()==1)
                dateSplit[1]="0"+dateSplit[1];
            thisMoment=dateSplit[2]+"-"+dateSplit[0]+"-"+dateSplit[1];
        }
        jTextAreaUploadStatus.append("Using date collected: "+thisMoment+"\n");
        String sPanel=jTextFieldUploadPanel.getText();
        if(sPanel.length()>0){
            String sUser=jComboBoxUploadUser.getSelectedItem().toString();
            String sUserID=username_to_id_map.get(sUser);
            try{
                Map<String,Object> payload=new HashMap<>();
                payload.put("title",sPanel);
                payload.put("user",sUserID);
                Map<String,String> headers=new HashMap<>();
                headers.put("Accept", "application/json");
                headers.put("Content-type", "application/json");
                headers.put("origin","https://lab.biorna-quantics.com");
                headers.put("authorization","Bearer "+sToken);
                jTextAreaUploadStatus.append("Creating panel using "+payload.toString()+"\n");
                //JSONObject returnObject=RESTAPIFunctions.http_post("https://staging-api.biorna-quantics.com/api/v1/panels", headers, payload);
                JSONObject returnObject=RESTAPIFunctions.http_post(restAPIURLs.new_panel_creation, headers, payload);
                String panel_id="";
                if(returnObject.length()>0){
                        panel_id=returnObject.get("id").toString();
                        jTextAreaUploadStatus.append("Panel created. Panel ID:"+panel_id+"\n");
                        Map<String,Object> parameters=new HashMap<>();
                        parameters.put("model","measurements");
                        Map<String,String> whereMap=new HashMap<>();
                        List<Object> measurementsList=new ArrayList<>();
                        for (int i = 0; i < jTablePDF.getRowCount(); i++) {
                            String sTemp=jTablePDF.getValueAt(i, 2).toString().trim();
                            if(sTemp.length()>0){
                                Map<String,String> measurement=new HashMap<>();
                                measurement.put("key",sTemp);
                                measurement.put("value" ,jTablePDF.getValueAt(i, 1).toString());
                                measurement.put("measuredAt",thisMoment);
                                measurementsList.add(measurement);
                            }
                        }
                        parameters.put("importRows",measurementsList);
                        whereMap.put("author",sUserID);
                        whereMap.put("panelId",panel_id);
                        whereMap.put("status","pending");
                        parameters.put("where",whereMap);
                        //returnObject=RESTAPIFunctions.http_post("https://staging-api.biorna-quantics.com/api/v1/import", headers, parameters);
                        returnObject=RESTAPIFunctions.http_post(restAPIURLs.add_measurements_to_panel, headers, parameters);
                        if(returnObject.length()>0){
                            jTextAreaUploadStatus.append("Uploaded " + measurementsList.size() + " measurements to panel page.\n");
                        }else{
                            jTextAreaUploadStatus.append("Failed to upload measurements to panel page.\n");
                        }
                }else{
                    jTextAreaUploadStatus.append("Could not get panel ID. Cannot upload data.\n");
                }
            }catch(IOException e){
                jTextAreaUploadStatus.append(e.getMessage()+"\n");
                String message="IOException occured while uploading data to BQ.com.\n"+e.toString();
                System.out.println(message);
                JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
            }catch(RESTAPIException e){
                jTextAreaUploadStatus.append(e.getMessage()+"\n");
                String message="RESTAPIException occured while uploading data to BQ.com.\n"+e.toString();
                System.out.println(message);
                JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_jButtonUploadSaveActionPerformed

    private void jButtonCMEPDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCMEPDirectoryActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldCMEPDirectory.getText()));
        chooser.setDialogTitle("Select Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldCMEPDirectory.setText(chooser.getSelectedFile().toString());

    }//GEN-LAST:event_jButtonCMEPDirectoryActionPerformed

    private void jButtonIgG4DirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonIgG4DirectoryActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldIgG4Directory.getText()));
        chooser.setDialogTitle("Select Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldIgG4Directory.setText(chooser.getSelectedFile().toString());
    }//GEN-LAST:event_jButtonIgG4DirectoryActionPerformed

    private void jButtonCMEPPDFMappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCMEPPDFMappingActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldCMEPPDFMapping.getText()));
        chooser.setDialogTitle("Select CMEP PDF Map file");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldCMEPPDFMapping.setText(chooser.getSelectedFile().toString());
    }//GEN-LAST:event_jButtonCMEPPDFMappingActionPerformed

    private void jButtonLabToInternalMappingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLabToInternalMappingActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldLabToInternalMarker.getText()));
        chooser.setDialogTitle("Select CMEP Lab to internal mapping file");
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldLabToInternalMarker.setText(chooser.getSelectedFile().toString());
    }//GEN-LAST:event_jButtonLabToInternalMappingActionPerformed

    private void jButtonTestDBConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTestDBConnectionActionPerformed
        // TODO add your handling code here:
        sBQEmail=jTextFieldBQEmail.getText();
        sBQPassword=String.valueOf(jPasswordFieldBQPassword.getPassword());
        sToken="";
        sRefreshToken="";
        try{
            get_login_token();
        }catch(IOException e){
            String message="IOException occured while loading BQ.com token.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(RESTAPIException e){
            String message="RESTAPI Exception occured while loading BQ.com Token.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
        if(sToken.length()>0){
            JOptionPane.showMessageDialog(new JFrame(), "Successfully logged into BQ.com.", "Dialog",JOptionPane.INFORMATION_MESSAGE);
            jLabelRESTAPIStatus.setBackground(new java.awt.Color(0,142, 0));
        }
        else{
            JOptionPane.showMessageDialog(new JFrame(), "Failed to log into BQ.com", "Dialog",JOptionPane.WARNING_MESSAGE);
            jLabelRESTAPIStatus.setBackground(new java.awt.Color(255, 0, 0));
        }
    }//GEN-LAST:event_jButtonTestDBConnectionActionPerformed

    private void jButtonGIMAPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGIMAPActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sGIMAPLocation);
        if(sFileWithPath.length()>0)
            loadGIMAP(sFileWithPath);
    }//GEN-LAST:event_jButtonGIMAPActionPerformed

    private void jButtonGIMAPDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGIMAPDirectoryActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldGIMAPDirectory.getText()));
        chooser.setDialogTitle("Select Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldGIMAPDirectory.setText(chooser.getSelectedFile().toString());
    }//GEN-LAST:event_jButtonGIMAPDirectoryActionPerformed

    private void jMenuItemGIMAPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemGIMAPActionPerformed
        // TODO add your handling code here:
        String sFileWithPath=displayFileDialog(sGIMAPLocation);
        if(sFileWithPath.length()>0)
            loadGIMAP(sFileWithPath);
    }//GEN-LAST:event_jMenuItemGIMAPActionPerformed

    private void jTableMappingPDFToInternalMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTableMappingPDFToInternalMouseClicked
        // TODO add your handling code here:
        Point point = evt.getPoint();
        int currentRow = jTableMappingPDFToInternal.rowAtPoint(point);
        jTableMappingPDFToInternal.setRowSelectionInterval(currentRow, currentRow);
    }//GEN-LAST:event_jTableMappingPDFToInternalMouseClicked

    private void jComboBoxPDFMappingSelectorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxPDFMappingSelectorItemStateChanged
        // TODO add your handling code here:
        String selectedValue=jComboBoxPDFMappingSelector.getItemAt(jComboBoxPDFMappingSelector.getSelectedIndex()).trim();
        if(selectedValue.length()>0)
            sorter.setRowFilter(RowFilter.regexFilter(selectedValue));
        else
            sorter.setRowFilter(null);
    }//GEN-LAST:event_jComboBoxPDFMappingSelectorItemStateChanged

    private void jButtonAutoDetectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAutoDetectActionPerformed
        // TODO add your handling code here:
        loadAutoDetectFiles();
    }//GEN-LAST:event_jButtonAutoDetectActionPerformed

    private void jButtonAutoDetectDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAutoDetectDirectoryActionPerformed
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser(); 
        chooser.setCurrentDirectory(new java.io.File(jTextFieldAutoDetectDirectory.getText()));
        chooser.setDialogTitle("Select Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //
        // disable the "All files" option.
        //
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            jTextFieldAutoDetectDirectory.setText(chooser.getSelectedFile().toString());
    }//GEN-LAST:event_jButtonAutoDetectDirectoryActionPerformed

    private void jMenuItemAutoDetectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemAutoDetectActionPerformed
        // TODO add your handling code here:
        loadAutoDetectFiles();
    }//GEN-LAST:event_jMenuItemAutoDetectActionPerformed

    private void jComboBoxUploadUserItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jComboBoxUploadUserItemStateChanged
        // TODO add your handling code here:
        String[] sUserFields=jComboBoxUploadUser.getSelectedItem().toString().trim().split(" ");
        String sText=sCurrentReport+"_";
        if(sUserFields.length>=2){
            sText+=sUserFields[0]+"_"+sUserFields[1].trim();
        }else{
            sText+=sUserFields[0];
        }
        String thisMoment = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                  .withZone(ZoneOffset.UTC)
                                  .format(Instant.now());
        String[] dateSplit=jTextFieldDateCollected.getText().split("/");
        if(dateSplit.length==3){
            if(dateSplit[0].length()==1)
                dateSplit[0]="0"+dateSplit[0];
            if(dateSplit[1].length()==1)
                dateSplit[1]="0"+dateSplit[1];
            thisMoment=dateSplit[2]+"_"+dateSplit[0]+"_"+dateSplit[1];
        }
        sText+="_"+thisMoment;        
        jTextFieldUploadPanel.setText(sText);
    }//GEN-LAST:event_jComboBoxUploadUserItemStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(BQLabReportImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(BQLabReportImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(BQLabReportImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(BQLabReportImporter.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new BQLabReportImporter().setVisible(true);
            }
        });
    }
    private void get_login_token() throws IOException,RESTAPIException{
        sToken="";
        sRefreshToken="";
        Map<String,Object> payload=new HashMap<>();
        payload.put("email",sBQEmail);
        payload.put("password", sBQPassword);
        Map<String,String> headers=new HashMap<>();
        headers.put("Accept", "application/json");
        headers.put("Content-type", "application/json");
        headers.put("origin","https://lab.biorna-quantics.com");
        //JSONObject returnObject=RESTAPIFunctions.http_post("https://staging-api.biorna-quantics.com/api/v1/auth", headers, payload);
        JSONObject returnObject=RESTAPIFunctions.http_post(restAPIURLs.login_token, headers, payload);
        if(returnObject.length()>0){
            sToken=returnObject.get("token").toString();
            sRefreshToken=returnObject.get("refreshToken").toString();
        }
    }
    private void populate_markerKeys(){
        try{
            Map<String,String> headers=new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-type", "application/json");
            headers.put("origin","https://lab.biorna-quantics.com");
            headers.put("authorization","Bearer "+sToken);
            //JSONObject returnValue=RESTAPIFunctions.http_get("https://staging-api.biorna-quantics.com/api/v1/list/keys?sort=title%20asc&skip=0&populate=subgroups,criticalities&select=slug",headers);
            JSONObject returnValue=RESTAPIFunctions.http_get(restAPIURLs.internal_marker_keys,headers);
            if(returnValue.length()>0){
                internalMarkers=new ArrayList<>();
                JSONArray jsonArray=returnValue.getJSONArray("items");
                if(jsonArray!=null){
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject obj=jsonArray.getJSONObject(i);
                        String sName=obj.getString("slug");
                        internalMarkers.add(sName);
                    }
                }
                java.util.Collections.sort(internalMarkers);
            }
        }catch(IOException e){
            String message="IOException occured while loading lab markers."+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(RESTAPIException e){
            String message="RESTAPIException occured while loading lab markers.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    private void populate_usernames(){
        try{
            Map<String,String> headers=new HashMap<>();
            headers.put("Accept", "application/json");
            headers.put("Content-type", "application/json");
            
            headers.put("authorization","Bearer "+sToken);
            //JSONObject returnValue=RESTAPIFunctions.http_get("https://staging-api.biorna-quantics.com/api/v1/list/users?sort=firstName%20ASC&skip=NaN&select=firstName,lastName,email",headers);
            JSONObject returnValue=RESTAPIFunctions.http_get(restAPIURLs.user_details,headers);
            if(returnValue.length()>0){
                userNames=new ArrayList<>();
                username_to_id_map=new HashMap<String,String>();
                JSONArray jsonArray=returnValue.getJSONArray("items");
                if(jsonArray!=null){
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject obj=jsonArray.getJSONObject(i);
                        String sName=obj.getString("firstName")+" "+obj.getString("lastName")+" ("+obj.get("email")+")";
                         username_to_id_map.put(sName,obj.getString("id"));
                        userNames.add(sName);
                    }
                }
                java.util.Collections.sort(userNames);
            }
        }catch(IOException e){
            String message="IOException occured while loading list of users.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(RESTAPIException e){
            String message="RESTAPIException occured while loading list of users.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    private void loadCMEPReport(String sFileWithPath){
        sCurrentReport="CMEP";
        try{
            pdf_location_mappings = BQJSONParser.parseJSONFile(sCMEPToPDFLocation);
            lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sPDFToInternal,"CMEP");
            File selectedFile = new File(sFileWithPath);
            sCMEPLocation=selectedFile.getParent();
            jLabelStatus.setText("Status: Parsing "+sFileWithPath);
            String sText=PDFExtractor.ExtractPageText(sFileWithPath,1);
            if(!(sText.contains("Metabolic") && sText.contains("Markers"))){
                JOptionPane.showMessageDialog(new JFrame(),"This doesnt appear to be a CMEP report. Please check the file.","Incorrect PDF format",JOptionPane.WARNING_MESSAGE);
            }else{
                Map<String,String> pdfExtract= PDFExtractor.ExtractCMEPPDFData(sFileWithPath,pdf_location_mappings);
                displayPdf(pdfExtract,lab_to_internal_mappings);
                currentPage=1;
                document=PDDocument.load(selectedFile);
                renderer = new PDFRenderer(document);
                renderPage();
        }
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadIgG4Report(String sFileWithPath){
        sCurrentReport="IgG4";
        try{            
            pdf_location_mappings = BQJSONParser.parseJSONFile("");
            lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sPDFToInternal,"IgG4");
            File selectedFile = new File(sFileWithPath);
            sIgG4Location=selectedFile.getParent();
            jLabelStatus.setText("Status: Parsing "+sFileWithPath);
            String sText=PDFExtractor.ExtractPageText(sFileWithPath,1);
            if(!(sText.contains("ALLERGEN") && sText.contains("RESULT"))){
                JOptionPane.showMessageDialog(new JFrame(),"This doesnt appear to be an IgG4 food sensitivity report. Please check the file.","Incorrect PDF format",JOptionPane.WARNING_MESSAGE);
            }else{
                Map<String,String> pdfExtract= PDFExtractor.ExtractIgG4PDFData(sFileWithPath,pdf_location_mappings);
                displayPdf(pdfExtract,lab_to_internal_mappings);
                currentPage=1;
                document=PDDocument.load(selectedFile);
                renderer = new PDFRenderer(document);
                renderPage();
            }
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    private void loadGIMAP(String sFileWithPath){
        sCurrentReport="GIMAP";
        try{
            //pdf_location_mappings = BQJSONParser.parseJSONFile(sIgG4ToPDFLocation);   dont need this
            lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sPDFToInternal,"");
            File selectedFile = new File(sFileWithPath);
            sGIMAPLocation=selectedFile.getParent();
            jLabelStatus.setText("Status: Parsing "+sFileWithPath);
            String sText=PDFExtractor.ExtractPageText(sFileWithPath,1);
            if(!sText.contains("GI-MAP")){
                JOptionPane.showMessageDialog(new JFrame(),"This doesnt appear to be an IgG4 food sensitivity report. Please check the file.","Incorrect PDF format",JOptionPane.WARNING_MESSAGE);
            }else{
                Map<String,String> pdfExtract= PDFExtractor.ExtractGIMAPData(sFileWithPath);
                displayPdf(pdfExtract,lab_to_internal_mappings);
                currentPage=1;
                document=PDDocument.load(selectedFile);
                renderer = new PDFRenderer(document);
                renderPage();
            }
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateInternalMappings(){
        Map<String,String> unfound_items=new HashMap<>();
        String sUnfound="";
        for(int i=0;i<jTablePDF.getRowCount();i++){
            String sLabMarker=jTablePDF.getValueAt(i,0).toString().trim();
            String sInternalMarker=jTablePDF.getValueAt(i,2).toString().trim();
            if(sLabMarker.length()>0 && sInternalMarker.length()>0){
                if(!lab_to_internal_mappings.containsKey(sLabMarker)){
                    unfound_items.put(sLabMarker,sInternalMarker);
                    sUnfound+=sLabMarker+":"+sInternalMarker+"\n";
                }
            }            
        }
        if(unfound_items.size()>0){
            String sMessage="The following items are not in the internal mapping table.\n\n"+sUnfound+"\nDo you want to add them?";
            int dialogResult=JOptionPane.showConfirmDialog(new JFrame(), sMessage, "Dialog",JOptionPane.OK_CANCEL_OPTION);
            if(dialogResult==JOptionPane.OK_OPTION){
                String sOutput="";
                for(String key:unfound_items.keySet())
                    sOutput+="{\"LabReport\":\""+sCurrentReport+"\",\"LabName\":\""+key+"\",\"InternalName\":\""+unfound_items.get(key).toString()+"\"}"+System.lineSeparator();
                try{
                    Files.write(
                        Paths.get(sPDFToInternal), 
                        sOutput.getBytes(), 
                        StandardOpenOption.APPEND);
                }catch(IOException e){
                    String message="IOException occured while saving mapping file.\n"+e.toString();
                    System.out.println(message);
                    JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }
    private String displayFileDialog(String sFileLocation){
        FileFilter pdfFilter = new FileNameExtensionFilter("PDF file", "pdf", "pdf");
        JFileChooser pdfFileChooser=new JFileChooser();
        pdfFileChooser.setCurrentDirectory(new File(sFileLocation));
        pdfFileChooser.setFileFilter(pdfFilter);
        int result = pdfFileChooser.showOpenDialog(new JFrame());
        if (result == JFileChooser.APPROVE_OPTION){
            File selectedFile = pdfFileChooser.getSelectedFile();
            return selectedFile.getAbsolutePath();
        }else
            return "";
    }
    private void loadAutoDetectFiles()
    {
        String sFileWithPath=displayFileDialog(sAutoDetectLocation);
        if(sFileWithPath.length()>0){
            try{
                File fTemp=new File(sFileWithPath);
                sAutoDetectLocation=fTemp.getParent();
                String sText=PDFExtractor.ExtractPageText(sFileWithPath,1);
                if((sText.contains("Metabolic") && sText.contains("Markers"))){
                    loadCMEPReport(sFileWithPath);
                }else if((sText.contains("ALLERGEN") && sText.contains("RESULT"))){
                    loadIgG4Report(sFileWithPath);
                }else if(sText.contains("GI-MAP")){
                    loadGIMAP(sFileWithPath);
                }
            }catch(IOException e){
                String message="Error auto detecting file.\n"+e.toString();
                System.out.println(message);
                JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void renderPage(){
        if(currentPage<=0)
            return;
        try{
            originalImage=renderer.renderImageWithDPI(currentPage-1,RENDER_DPI);
            BufferedImage image=resizeImage(originalImage,jLabelPDF.getHeight(),jLabelPDF.getWidth());
            ImageIcon iconLogo = new ImageIcon(image);
            jLabelPDF.setIcon(iconLogo);
            //jLabelPDF=new javax.swing.JLabel(iconLogo);
            jLabelRecordStatus.setText("Page "+currentPage+" of "+document.getNumberOfPages());
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            System.out.println(message);
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    private void displayPdf(Map<String,String> pdfExtract,Map<String,String> lab_to_internal_mappings){
        DefaultTableModel tableModel = (DefaultTableModel)jTablePDF.getModel();
        tableModel.setRowCount(0);
        int i=0;
        int columns = tableModel.getColumnCount();
        for(String key:pdfExtract.keySet()){
            //System.out.println(key+" "+pdfExtract.get(key));
            String[] splitKeys=key.split("_");
            if(splitKeys[1].equals("Measurement")){
                Object[] row=new Object[columns];
                row[0]=splitKeys[0];
                try{
                    row[1]=Double.parseDouble(pdfExtract.get(key).replace(" ",""));
                }catch(NumberFormatException e){
                    row[1]=pdfExtract.get(key).replace(" ","");
                }
                if(lab_to_internal_mappings.containsKey(row[0]))
                    row[2]=lab_to_internal_mappings.get(row[0]).toString();
                else
                    row[2]="";
                tableModel.insertRow(i++, row);
            }
        }
        //if(pdfExtract.containsKey("Name_ReportDetails"))
            jTextFieldName.setText(pdfExtract.get("Name_ReportDetails").trim());
        //else
        //    jTextFieldName.setText("");
        //if(pdfExtract.containsKey("DateOfCollection_ReportDetails"))
            jTextFieldDateCollected.setText(pdfExtract.get("DateOfCollection_ReportDetails"));
        //else
        //    jTextFieldDateCollected.setText("");
    }
    private static BufferedImage resizeImage(BufferedImage img, int height, int width) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return resized;
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAutoDetect;
    private javax.swing.JButton jButtonAutoDetectDirectory;
    private javax.swing.JButton jButtonCMEP;
    private javax.swing.JButton jButtonCMEPDirectory;
    private javax.swing.JButton jButtonCMEPPDFMapping;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonFirst;
    private javax.swing.JButton jButtonGIMAP;
    private javax.swing.JButton jButtonGIMAPDirectory;
    private javax.swing.JButton jButtonIgG4;
    private javax.swing.JButton jButtonIgG4Directory;
    private javax.swing.JButton jButtonLabToInternalMapping;
    private javax.swing.JButton jButtonLast;
    private javax.swing.JButton jButtonMappingClose;
    private javax.swing.JButton jButtonMappingSave;
    private javax.swing.JButton jButtonNext;
    private javax.swing.JButton jButtonPrevious;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JButton jButtonTestDBConnection;
    private javax.swing.JButton jButtonToCSV;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JButton jButtonUploadClose;
    private javax.swing.JButton jButtonUploadSave;
    private javax.swing.JComboBox<String> jComboBoxPDFMappingSelector;
    private javax.swing.JComboBox<String> jComboBoxUploadUser;
    private javax.swing.JDialog jDialogMapping;
    private javax.swing.JDialog jDialogSettings;
    private javax.swing.JDialog jDialogUpload;
    private javax.swing.JLabel jLabelAutoDetectDirectory;
    private javax.swing.JLabel jLabelBQEmail;
    private javax.swing.JLabel jLabelBQPassword;
    private javax.swing.JLabel jLabelCMEPDirectory;
    private javax.swing.JLabel jLabelCMEPPDFMapping;
    private javax.swing.JLabel jLabelDNAReportTemplace;
    private javax.swing.JLabel jLabelDateCollected;
    private javax.swing.JLabel jLabelGIMAPDirectory;
    private javax.swing.JLabel jLabelIgG4Directory;
    private javax.swing.JLabel jLabelLabToInternalMarker;
    private javax.swing.JLabel jLabelMappingPDFSelector;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelPDF;
    private javax.swing.JLabel jLabelPanel;
    private javax.swing.JLabel jLabelRESTAPIStatus;
    private javax.swing.JLabel jLabelRecordStatus;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JLabel jLabelTableStatus;
    private javax.swing.JLabel jLabelUploadUser;
    private javax.swing.JMenuBar jMenuBarMainMenu;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemAutoDetect;
    private javax.swing.JMenuItem jMenuItemCMEP;
    private javax.swing.JMenuItem jMenuItemDeleteCMEP;
    private javax.swing.JMenuItem jMenuItemDeletePDF;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemGIMAP;
    private javax.swing.JMenuItem jMenuItemIgG4;
    private javax.swing.JMenuItem jMenuItemInsertCMEP;
    private javax.swing.JMenuItem jMenuItemInsertPDF;
    private javax.swing.JMenuItem jMenuItemMappings;
    private javax.swing.JMenuItem jMenuItemSettings;
    private javax.swing.JMenu jMenuLabReports;
    private javax.swing.JPanel jPanelAutoDetectDirectory;
    private javax.swing.JPanel jPanelButtons;
    private javax.swing.JPanel jPanelCMEPDirectory;
    private javax.swing.JPanel jPanelCMEPLabToInternalMapping;
    private javax.swing.JPanel jPanelCMEPPDFMapping;
    private javax.swing.JPanel jPanelData;
    private javax.swing.JPanel jPanelEmpty;
    private javax.swing.JPanel jPanelGIMAPDirectory;
    private javax.swing.JPanel jPanelIgG4Directory;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelMappingButtons;
    private javax.swing.JPanel jPanelMappingMain;
    private javax.swing.JPanel jPanelMappingPDFSelector;
    private javax.swing.JPanel jPanelPDF;
    private javax.swing.JPanel jPanelStatusBarSubBar;
    private javax.swing.JPanel jPanelStatusBarSubSubBar;
    private javax.swing.JPanel jPanelStatusbar;
    private javax.swing.JPanel jPanelUploadButtons;
    private javax.swing.JPanel jPanelUploadInputs;
    private javax.swing.JPanel jPanelUploadPane;
    private javax.swing.JPanel jPanelUploadStatus;
    private javax.swing.JPanel jPanelUserDetails;
    private javax.swing.JPasswordField jPasswordFieldBQPassword;
    private javax.swing.JPopupMenu jPopupMenuPDF;
    private javax.swing.JPopupMenu jPopupMenuPDFMapping;
    private javax.swing.JScrollPane jScrollPaneMappingPDFToInternal;
    private javax.swing.JScrollPane jScrollPanePDF;
    private javax.swing.JScrollPane jScrollPaneUploadStatus;
    private javax.swing.JTable jTableMappingPDFToInternal;
    private javax.swing.JTable jTablePDF;
    private javax.swing.JTextArea jTextAreaUploadStatus;
    private javax.swing.JTextField jTextFieldAutoDetectDirectory;
    private javax.swing.JTextField jTextFieldBQEmail;
    private javax.swing.JTextField jTextFieldCMEPDirectory;
    private javax.swing.JTextField jTextFieldCMEPPDFMapping;
    private javax.swing.JTextField jTextFieldDateCollected;
    private javax.swing.JTextField jTextFieldGIMAPDirectory;
    private javax.swing.JTextField jTextFieldIgG4Directory;
    private javax.swing.JTextField jTextFieldLabToInternalMarker;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JTextField jTextFieldUploadPanel;
    private javax.swing.JTextField jTextFieldUploadReportTemplate;
    private javax.swing.JToolBar jToolBarLoaderButtons;
    private javax.swing.JToolBar jToolBarRecordParser;
    // End of variables declaration//GEN-END:variables
}
