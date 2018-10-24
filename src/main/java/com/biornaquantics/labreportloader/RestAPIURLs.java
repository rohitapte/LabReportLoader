/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 *
 * @author tihor
 */
public final class RestAPIURLs {
    public static String new_panel_creation,add_measurements_to_panel,login_token,refresh_token,internal_marker_keys,user_details;
    public RestAPIURLs() throws IOException{
        Properties prop=new Properties();
        InputStream input = null;        
        input=new FileInputStream(System.getProperty("user.dir")+"/httpURLs.properties");
        prop.load(input);
        new_panel_creation=prop.getProperty("new_panel_creation");
        add_measurements_to_panel=prop.getProperty("add_measurements_to_panel");
        login_token=prop.getProperty("login_token");
        refresh_token=prop.getProperty("refresh_token");
        internal_marker_keys=prop.getProperty("internal_marker_keys");
        user_details=prop.getProperty("user_details");
    }
}
