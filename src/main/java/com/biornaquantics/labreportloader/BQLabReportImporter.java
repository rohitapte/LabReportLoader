/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

/**
 *
 * @author tihor
 */
public class BQLabReportImporter extends javax.swing.JFrame {
    int currentPage=0;
    PDDocument document=null;
    PDFRenderer renderer=null;
    private float RENDER_DPI=200;
    String sCMEPLocation="",sIgG4Location="",sIgG4ToPDFLocation="",sIgG4PDFToInternal="",sCMEPToPDFLocation="",sCMEPPDFToInternal="";

    /**
     * Creates new form BQLabReportImporter
     */
    public BQLabReportImporter() {
        initComponents();
        Properties prop=new Properties();
        InputStream input = null;
        
        try{
            input=new FileInputStream("config.properties");
            prop.load(input);
            sCMEPLocation=prop.getProperty("CMEPLocation");
            sCMEPToPDFLocation=prop.getProperty("CMEP_PDF_location");
            sCMEPPDFToInternal=prop.getProperty("CMEP_pdf_to_internal");
            sIgG4Location=prop.getProperty("IgG4Location");
            sIgG4ToPDFLocation=prop.getProperty("IgG4_PDF_location");
            sIgG4PDFToInternal=prop.getProperty("IgG4_pdf_to_internal");
        }catch(IOException e){
            e.printStackTrace();
        }
        /*sCMEPLocation="D:\\BiornaQuantics\\Complete Metabolic Energy Profile";
        sCMEPToPDFLocation="D:\\BiornaQuantics\\pdf_mapping_CMEP.json";
        sCMEPPDFToInternal="D:\\BiornaQuantics\\lab_to_internal_mapping_CMEP.json";
        sIgG4Location="D:\\BiornaQuantics\\Food Sensitivities IgG4";
        sIgG4ToPDFLocation="D:\\BiornaQuantics\\pdf_mapping_IgG4.json";
        sIgG4PDFToInternal="D:\\BiornaQuantics\\lab_to_internal_mapping_IgG4.json";*/
        
        
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
        jLabelCMEPDirectory = new javax.swing.JLabel();
        jTextFieldCMEPDirectory = new javax.swing.JTextField();
        jLabelIgG4Directory = new javax.swing.JLabel();
        jTextFieldIgG4Directory = new javax.swing.JTextField();
        jLabelCMEPPDFMapping = new javax.swing.JLabel();
        jTextFieldCMEPPDFMapping = new javax.swing.JTextField();
        jLabelCMEPLabToInternalMarker = new javax.swing.JLabel();
        jTextFieldCMEPLabToInternalMarker = new javax.swing.JTextField();
        jLabelIgG4PDFMapping = new javax.swing.JLabel();
        jTextFieldIgG4PDFMapping = new javax.swing.JTextField();
        jLabelIgG4LabToInternalMarker = new javax.swing.JLabel();
        jTextFieldIgG4LabToInternalMarker = new javax.swing.JTextField();
        jPanelEmpty = new javax.swing.JPanel();
        jPanelButtons = new javax.swing.JPanel();
        jButtonSave = new javax.swing.JButton();
        jButtonClose = new javax.swing.JButton();
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
        jButtonCMEP = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        jLabelStatus = new javax.swing.JLabel();
        jMenuBarMainMenu = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuEdit = new javax.swing.JMenu();
        jMenuItemSettings = new javax.swing.JMenuItem();
        jMenuLabReports = new javax.swing.JMenu();
        jMenuItemCMEP = new javax.swing.JMenuItem();
        jMenuItemIgG4 = new javax.swing.JMenuItem();

        jDialogSettings.getContentPane().setLayout(new java.awt.GridLayout(7, 2));

        jLabelCMEPDirectory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelCMEPDirectory.setText("CMEP Directory:");
        jDialogSettings.getContentPane().add(jLabelCMEPDirectory);
        jDialogSettings.getContentPane().add(jTextFieldCMEPDirectory);

        jLabelIgG4Directory.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelIgG4Directory.setText("IgG4 Directory:");
        jDialogSettings.getContentPane().add(jLabelIgG4Directory);
        jDialogSettings.getContentPane().add(jTextFieldIgG4Directory);

        jLabelCMEPPDFMapping.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelCMEPPDFMapping.setText("CMEP PDF Mapping:");
        jDialogSettings.getContentPane().add(jLabelCMEPPDFMapping);
        jDialogSettings.getContentPane().add(jTextFieldCMEPPDFMapping);

        jLabelCMEPLabToInternalMarker.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelCMEPLabToInternalMarker.setText("CMEP Lab to Internal Marker:");
        jDialogSettings.getContentPane().add(jLabelCMEPLabToInternalMarker);
        jDialogSettings.getContentPane().add(jTextFieldCMEPLabToInternalMarker);

        jLabelIgG4PDFMapping.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelIgG4PDFMapping.setText("IgG4 PDF Mapping:");
        jDialogSettings.getContentPane().add(jLabelIgG4PDFMapping);
        jDialogSettings.getContentPane().add(jTextFieldIgG4PDFMapping);

        jLabelIgG4LabToInternalMarker.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabelIgG4LabToInternalMarker.setText("IgG4 Lab to Internal Marker:");
        jDialogSettings.getContentPane().add(jLabelIgG4LabToInternalMarker);
        jDialogSettings.getContentPane().add(jTextFieldIgG4LabToInternalMarker);
        jDialogSettings.getContentPane().add(jPanelEmpty);

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

        jTablePDF.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Lab Marker", "Value", "Internal Marker"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Double.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTablePDF.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_LAST_COLUMN);
        jTablePDF.getColumnModel().getColumn(0).setPreferredWidth(100);
        jTablePDF.getColumnModel().getColumn(1).setPreferredWidth(15);
        jScrollPanePDF.setViewportView(jTablePDF);

        jPanelData.add(jScrollPanePDF, java.awt.BorderLayout.CENTER);

        jPanelMain.add(jPanelData);

        getContentPane().add(jPanelMain, java.awt.BorderLayout.CENTER);

        jToolBarLoaderButtons.setRollover(true);

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

        jButton1.setText("IgG4");
        jButton1.setFocusable(false);
        jButton1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jButton1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jToolBarLoaderButtons.add(jButton1);

        getContentPane().add(jToolBarLoaderButtons, java.awt.BorderLayout.PAGE_START);

        jLabelStatus.setText("Status:");
        getContentPane().add(jLabelStatus, java.awt.BorderLayout.PAGE_END);

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

        jMenuBarMainMenu.add(jMenuEdit);

        jMenuLabReports.setText("Lab Reports");

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

        jMenuBarMainMenu.add(jMenuLabReports);

        setJMenuBar(jMenuBarMainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCMEPActionPerformed
        // TODO add your handling code here:
        loadCMEPReport();
    }//GEN-LAST:event_jButtonCMEPActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        loadIgG4Report();
    }//GEN-LAST:event_jButton1ActionPerformed

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
        //renderPage();
    }//GEN-LAST:event_formComponentResized

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemCMEPActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCMEPActionPerformed
        // TODO add your handling code here:
        loadCMEPReport();
    }//GEN-LAST:event_jMenuItemCMEPActionPerformed

    private void jMenuItemIgG4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemIgG4ActionPerformed
        // TODO add your handling code here:
        loadIgG4Report();
    }//GEN-LAST:event_jMenuItemIgG4ActionPerformed

    private void jButtonToCSVActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonToCSVActionPerformed
        // TODO add your handling code here:
        JFileChooser fileChooser=new JFileChooser();
        int returnVal = fileChooser.showSaveDialog(new JFrame());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                jLabelStatus.setText("Saving file "+file.toString());
                PrintWriter os = new PrintWriter(file);
                os.println("Name\t"+jTextFieldName.getText());
                os.println("DateCollected\t"+jTextFieldDateCollected.getText());
                
                for (int i = 0; i < jTablePDF.getRowCount(); i++) {
                    for (int j = 0; j < jTablePDF.getColumnCount(); j++) {
                        os.print(jTablePDF.getValueAt(i, j).toString() + "\t");
                    }
                    os.println("");
                }
                os.close();
                jLabelStatus.setText("Saving file "+file.toString()+"...Done");

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_jButtonToCSVActionPerformed

    private void jMenuItemSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSettingsActionPerformed
        // TODO add your handling code here:
        jTextFieldCMEPDirectory.setText(sCMEPLocation);
        jTextFieldIgG4Directory.setText(sIgG4Location);
        jTextFieldCMEPPDFMapping.setText(sCMEPToPDFLocation);
        jTextFieldCMEPLabToInternalMarker.setText(sCMEPPDFToInternal);
        jTextFieldIgG4PDFMapping.setText(sIgG4ToPDFLocation);
        jTextFieldIgG4LabToInternalMarker.setText(sIgG4PDFToInternal);
        jDialogSettings.pack();
        jDialogSettings.setVisible(true);
    }//GEN-LAST:event_jMenuItemSettingsActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        // TODO add your handling code here:
        sCMEPLocation=jTextFieldCMEPDirectory.getText();
        sIgG4Location=jTextFieldIgG4Directory.getText();
        sCMEPToPDFLocation=jTextFieldCMEPPDFMapping.getText();
        sCMEPPDFToInternal=jTextFieldCMEPLabToInternalMarker.getText();
        sIgG4ToPDFLocation=jTextFieldIgG4PDFMapping.getText();
        sIgG4PDFToInternal=jTextFieldIgG4LabToInternalMarker.getText();
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        Properties prop=new Properties();
        OutputStream output=null;
        try{
            output = new FileOutputStream("config.properties");

            // set the properties value
            prop.setProperty("CMEPLocation", sCMEPLocation);
            prop.setProperty("IgG4Location", sIgG4Location);
            prop.setProperty("IgG4_PDF_location", sCMEPToPDFLocation);
            prop.setProperty("IgG4_pdf_to_internal", sCMEPPDFToInternal);
            prop.setProperty("CMEP_PDF_location", sIgG4ToPDFLocation);
            prop.setProperty("CMEP_pdf_to_internal", sIgG4PDFToInternal);

            // save properties to project root folder
            prop.store(output, null);

        }catch(IOException e){
            e.printStackTrace();
        }
    }//GEN-LAST:event_formWindowClosing

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        // TODO add your handling code here:
        jDialogSettings.setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

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

    private void loadCMEPReport(){
        try{
            FileFilter pdfFilter = new FileNameExtensionFilter("PDF file", "pdf", "pdf");
            JFileChooser pdfFileChooser=new JFileChooser();
            pdfFileChooser.setCurrentDirectory(new File(sCMEPLocation));
            pdfFileChooser.setFileFilter(pdfFilter);
            int result = pdfFileChooser.showOpenDialog(new JFrame());
            if (result == JFileChooser.APPROVE_OPTION){
                List<JSONObject> pdf_location_mappings = BQJSONParser.parseJSONFile(sCMEPToPDFLocation);
                Map<String,String> lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sCMEPPDFToInternal);
                File selectedFile = pdfFileChooser.getSelectedFile();
                sCMEPLocation=selectedFile.getParent();
                String sFileWithPath=selectedFile.getAbsolutePath();
                jLabelStatus.setText("Status: Parsing "+sFileWithPath);
                Map<String,String> pdfExtract= PDFExtractor.ExtractCMEPPDFData(sFileWithPath,pdf_location_mappings);
                displayPdf(pdfExtract,lab_to_internal_mappings);
                currentPage=1;
                document=PDDocument.load(selectedFile);
                renderer = new PDFRenderer(document);
                renderPage();
            }
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(ParseException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void loadIgG4Report(){
        try{
            FileFilter pdfFilter = new FileNameExtensionFilter("PDF file", "pdf", "pdf");
            JFileChooser pdfFileChooser=new JFileChooser();
            pdfFileChooser.setCurrentDirectory(new File(sIgG4Location));
            pdfFileChooser.setFileFilter(pdfFilter);
            int result = pdfFileChooser.showOpenDialog(new JFrame());
            if (result == JFileChooser.APPROVE_OPTION){
                List<JSONObject> pdf_location_mappings = BQJSONParser.parseJSONFile(sIgG4ToPDFLocation);
                Map<String,String> lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON(sIgG4PDFToInternal);
                File selectedFile = pdfFileChooser.getSelectedFile();
                String sFileWithPath=selectedFile.getAbsolutePath();
                sIgG4Location=selectedFile.getParent();
                jLabelStatus.setText("Status: Parsing "+sFileWithPath);
                Map<String,String> pdfExtract= PDFExtractor.ExtractIgG4PDFData(sFileWithPath,pdf_location_mappings);
                displayPdf(pdfExtract,lab_to_internal_mappings);
                currentPage=1;
                document=PDDocument.load(selectedFile);
                renderer = new PDFRenderer(document);
                renderPage();
            }
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }catch(ParseException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
            JOptionPane.showMessageDialog(new JFrame(), message, "Dialog",JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void renderPage(){
        if(currentPage<=0)
            return;
        try{
            BufferedImage image=renderer.renderImageWithDPI(currentPage-1,RENDER_DPI);
            image=resizeImage(image,jLabelPDF.getHeight(),jLabelPDF.getWidth());
            ImageIcon iconLogo = new ImageIcon(image);
            jLabelPDF.setIcon(iconLogo);
            //jLabelPDF=new javax.swing.JLabel(iconLogo);
            jLabelRecordStatus.setText("Page "+currentPage+" of "+document.getNumberOfPages());
        }catch(IOException e){
            String message="Error occured while parsing pdf file.\n"+e.toString();
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
                row[1]=Double.parseDouble(pdfExtract.get(key).replace(" ",""));
                if(lab_to_internal_mappings.containsKey(row[0]))
                    row[2]=lab_to_internal_mappings.get(row[0]).toString();
                else
                    row[2]="";
                tableModel.insertRow(i++, row);
            }
        }
        jTextFieldName.setText(pdfExtract.get("Name_ReportDetails").trim());
        jTextFieldDateCollected.setText(pdfExtract.get("DateOfCollection_ReportDetails"));
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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButtonCMEP;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonFirst;
    private javax.swing.JButton jButtonLast;
    private javax.swing.JButton jButtonNext;
    private javax.swing.JButton jButtonPrevious;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JButton jButtonToCSV;
    private javax.swing.JButton jButtonUpload;
    private javax.swing.JDialog jDialogSettings;
    private javax.swing.JLabel jLabelCMEPDirectory;
    private javax.swing.JLabel jLabelCMEPLabToInternalMarker;
    private javax.swing.JLabel jLabelCMEPPDFMapping;
    private javax.swing.JLabel jLabelDateCollected;
    private javax.swing.JLabel jLabelIgG4Directory;
    private javax.swing.JLabel jLabelIgG4LabToInternalMarker;
    private javax.swing.JLabel jLabelIgG4PDFMapping;
    private javax.swing.JLabel jLabelName;
    private javax.swing.JLabel jLabelPDF;
    private javax.swing.JLabel jLabelRecordStatus;
    private javax.swing.JLabel jLabelStatus;
    private javax.swing.JMenuBar jMenuBarMainMenu;
    private javax.swing.JMenu jMenuEdit;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItemCMEP;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemIgG4;
    private javax.swing.JMenuItem jMenuItemSettings;
    private javax.swing.JMenu jMenuLabReports;
    private javax.swing.JPanel jPanelButtons;
    private javax.swing.JPanel jPanelData;
    private javax.swing.JPanel jPanelEmpty;
    private javax.swing.JPanel jPanelMain;
    private javax.swing.JPanel jPanelPDF;
    private javax.swing.JPanel jPanelUserDetails;
    private javax.swing.JScrollPane jScrollPanePDF;
    private javax.swing.JTable jTablePDF;
    private javax.swing.JTextField jTextFieldCMEPDirectory;
    private javax.swing.JTextField jTextFieldCMEPLabToInternalMarker;
    private javax.swing.JTextField jTextFieldCMEPPDFMapping;
    private javax.swing.JTextField jTextFieldDateCollected;
    private javax.swing.JTextField jTextFieldIgG4Directory;
    private javax.swing.JTextField jTextFieldIgG4LabToInternalMarker;
    private javax.swing.JTextField jTextFieldIgG4PDFMapping;
    private javax.swing.JTextField jTextFieldName;
    private javax.swing.JToolBar jToolBarLoaderButtons;
    private javax.swing.JToolBar jToolBarRecordParser;
    // End of variables declaration//GEN-END:variables
}
