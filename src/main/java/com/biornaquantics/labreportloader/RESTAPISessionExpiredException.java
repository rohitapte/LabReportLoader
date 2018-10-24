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
public class RESTAPISessionExpiredException extends RESTAPIException{
    public RESTAPISessionExpiredException(){}
    public RESTAPISessionExpiredException(String message){
        super(message);
    }
    public RESTAPISessionExpiredException(Throwable cause){
        super(cause);
    }
    public RESTAPISessionExpiredException(String message,Throwable cause){
        super(message,cause);
    }
}
