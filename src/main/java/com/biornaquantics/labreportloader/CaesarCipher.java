/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

/**
 *
 * @author tihor
 */
public class CaesarCipher {
    public static int encryptionKey=22%26;
    public static int decryptionKey=26-encryptionKey;
    public static String encrypt(String str){
        return codify(str,encryptionKey);
    } 
    public static String decrypt(String str){
        return codify(str,decryptionKey);
    }
    private static String codify(String str, int shift)
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
