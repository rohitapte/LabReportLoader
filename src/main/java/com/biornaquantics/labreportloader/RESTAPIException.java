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
public class RESTAPIException  extends Exception{
    public RESTAPIException(){}
    public RESTAPIException(String message){
        super(message);
    }
    public RESTAPIException(Throwable cause){
        super(cause);
    }
    public RESTAPIException(String message,Throwable cause){
        super(message,cause);
    }
}
