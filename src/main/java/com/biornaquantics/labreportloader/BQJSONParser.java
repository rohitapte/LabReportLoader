/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Rohit Apte
 */
public class BQJSONParser {
    public static List<JSONObject> parseJSONFile(String filename) throws IOException,ParseException{
        List<String> list=new ArrayList<>();
        List<JSONObject> returnArray=new ArrayList<>();
        Stream<String> stream= Files.lines(Paths.get(filename));
        list=stream.collect(Collectors.toList());
        for (String item : list) {
            JSONObject jo=(JSONObject) new JSONParser().parse(item);
            returnArray.add(jo);
        }
        return returnArray;
    }
    public static Map<String,String> parseLabToInternalMappingJSON(String filename) throws IOException,ParseException{
        List<String> list=new ArrayList<>();
        Map<String,String> returnMap=new HashMap<>();
        Stream<String> stream= Files.lines(Paths.get(filename));
        list=stream.collect(Collectors.toList());
        for (String item : list) {
            JSONObject jo=(JSONObject) new JSONParser().parse(item);
            returnMap.put(jo.get("LabName").toString(), jo.get("InternalName").toString());
        }
        return returnMap;
    }
    public static List<String> parseKeys(String filename) throws IOException{
        List<String> returnValues=new ArrayList<>();
        Stream<String> stream= Files.lines(Paths.get(filename));
        returnValues=stream.collect(Collectors.toList());
        return returnValues;
    }
}
