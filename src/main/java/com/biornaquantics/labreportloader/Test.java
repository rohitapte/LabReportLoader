/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;


public class Test {
    public static void main(String[] args){
        Map<String,Object> parameters=new HashMap<>();
        parameters.put("model","measurements");
        Map<String,String> whereMap=new HashMap<>();
        List<Object> measurementsList=new ArrayList<>();
        Map<String,String> measurement=new HashMap<>();
        measurement.put("key","4-hydroxyphenyllactic-acid-urine");
        measurement.put("value" ,"0.35");
        measurement.put("measuredAt","9/4/18");
        measurementsList.add(measurement);
        measurement=new HashMap<>();
        measurement.put("key","4-hydroxybutyric-acid-urine");
        measurement.put("value" ,"0.32");
        measurement.put("measuredAt","9/4/18");
        measurementsList.add(measurement);
        measurement=new HashMap<>();
        measurement.put("key","4-hydroxybutyric-acid-urine");
        measurement.put("value" ,"1.7");
        measurement.put("measuredAt","9/4/18");
        measurementsList.add(measurement);
        parameters.put("importRows",measurementsList);
        whereMap.put("author","5add2f328ed48dc879934eba");
        whereMap.put("panelId","5b9e5bb12d1eb83c28c18669");
        whereMap.put("status","pending");
        parameters.put("where",whereMap);
        JSONObject jsonObject=new JSONObject(parameters);
        JSONObject returnObject=new JSONObject();
        String jsonFormattedMap=jsonObject.toString();
        System.out.println(jsonFormattedMap);
    }
}
