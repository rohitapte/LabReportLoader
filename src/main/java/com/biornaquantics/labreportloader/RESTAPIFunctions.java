/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONObject;

/**
 *
 * @author tihor
 */
public final class RESTAPIFunctions {
    public static final JSONObject http_post(String sUrl,Map<String,String> header,Map<String,Object> parameters) throws UnsupportedEncodingException,IOException,RESTAPIException{
        JSONObject jsonObject=new JSONObject(parameters);
        JSONObject returnObject=new JSONObject();
        String jsonFormattedMap=jsonObject.toString();
        //System.out.println(jsonFormattedMap);
        CloseableHttpClient client=HttpClients.createDefault();
        HttpPost httpPost=new HttpPost(sUrl);
        StringEntity entity = new StringEntity(jsonFormattedMap);
        httpPost.setEntity(entity);
        for(String key:header.keySet())
            httpPost.setHeader(key,header.get(key).toString());
        CloseableHttpResponse response = client.execute(httpPost);
        try{
            int statusCode=response.getStatusLine().getStatusCode();
            if(statusCode>=200 && statusCode<300){
                returnObject=parseResponseAsJSON(response);
            }else if(statusCode==401){
                throw new RESTAPISessionExpiredException(statusCode+": "+response.getStatusLine().getReasonPhrase());
            }
            else{
                throw new RESTAPIException(statusCode+": "+response.getStatusLine().getReasonPhrase());
            }
        }catch(IOException e){
            throw(e);
        }
        return returnObject;
    }
    public static final JSONObject http_get(String sUrl,Map<String,String> header) throws UnsupportedEncodingException,IOException,RESTAPIException{
        JSONObject returnObject=new JSONObject();
        CloseableHttpClient client=HttpClients.createDefault();
        HttpGet httpGet=new HttpGet(sUrl);
        for(String key:header.keySet())
            httpGet.setHeader(key,header.get(key).toString());
        CloseableHttpResponse response = client.execute(httpGet);
        try{
            int statusCode=response.getStatusLine().getStatusCode();
            if(statusCode>=200 && statusCode<300){
                returnObject=parseResponseAsJSON(response);
            }else if(statusCode==401){
                throw new RESTAPISessionExpiredException(statusCode+": "+response.getStatusLine().getReasonPhrase());
            }else{
                throw new RESTAPIException(statusCode+": "+response.getStatusLine().getReasonPhrase());
            }
        }catch(IOException e){
            throw(e);
        }
        return returnObject;
    }
    public static final JSONObject parseResponseAsJSON(CloseableHttpResponse response) throws IOException{
        String responseString=parseResponseAsString(response);
        JSONObject returnObject=new JSONObject(responseString);
        return returnObject;
    }
    public static final String parseResponseAsString(CloseableHttpResponse response) throws IOException{
        BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line+"\n");
            }
        return(result.toString());
    }
}
