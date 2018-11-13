/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.util.Scanner;

/**
 *
 * @author tihor
 */
public class TestCipher {
    public static void main(String[] args)
    {
        String str = "jani.siivola@biorna-quantics.com";
        int encryptKey = 22;

        encryptKey = encryptKey % 26;
        int decryptKey=26-encryptKey;
        
        //String sTemp=encrypt(str, encryptKey);
        String sTemp="Nepvu Hwqnawja";
        System.out.println(sTemp);
        System.out.println(encrypt(sTemp, decryptKey));
        
    }

    private static String encrypt(String str, int shift)
    {
        StringBuilder strBuilder = new StringBuilder();
        char c;
        for (int i = 0; i < str.length(); i++)
        {
            c = str.charAt(i);
            int iTemp=(int)c;
            // if c is letter ONLY then shift them, else directly add it
            if (Character.isLetter(c))
            {
                c = (char) (str.charAt(i) + shift);
                if ((Character.isLowerCase(str.charAt(i)) && c > 'z') || (Character.isUpperCase(str.charAt(i)) && c > 'Z')){
                    c = (char) (str.charAt(i) - (26 - shift));
                    iTemp=(int)c;
                }
                
            }
            strBuilder.append(c);
        }
    return strBuilder.toString();
    }
    
}
