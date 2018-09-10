/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.biornaquantics.labreportloader;

/**
 *
 * @author Rohit Apte
 */
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter;
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredTextEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextExtractionStrategy;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PDFExtractor {
    public static Map<String,String> ExtractCMEPPDFData(String pdfFile, List<JSONObject> jsonData) throws IOException{
        Map<String,String> returnValues=new LinkedHashMap<>();
        returnValues.put("Name_ReportDetails","");
        returnValues.put("DateOfCollection_ReportDetails","");
        try {
            PdfDocument pdfDoc=new PdfDocument(new PdfReader(pdfFile));
            for(JSONObject item:jsonData){
                float x=Float.parseFloat(item.get("X").toString());
                float y=Float.parseFloat(item.get("Y").toString());
                int page=Integer.parseInt(item.get("Page").toString());
                String field=item.get("Field").toString()+"_"+item.get("Type").toString();
                //System.out.println(item.toString());
                //System.out.println(x+" "+y+" "+page+" "+field+" "+type);
                Rectangle rect = new Rectangle(x,y, 10, 10);
                TextRegionEventFilter regionFilter = new TextRegionEventFilter(rect);
                ITextExtractionStrategy strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter);
                String str = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim();
                returnValues.put(field,str);
            }
            pdfDoc.close();
        } catch (IOException e) {
            System.out.println("Could not find file "+pdfFile);
            throw(e);
        }
        return returnValues;
    }
    public static Map<String,String> ExtractIgG4PDFData(String pdfFile, List<JSONObject> jsonData) throws IOException{
        Map<String,String> returnValues=new LinkedHashMap<>();
        returnValues.put("Name_ReportDetails","");
        returnValues.put("DateOfCollection_ReportDetails","");
        try {
            PdfDocument pdfDoc=new PdfDocument(new PdfReader(pdfFile));
            Rectangle rect = new Rectangle(100,(float)725.72,100,10);
            TextRegionEventFilter regionFilter = new TextRegionEventFilter(rect);
            ITextExtractionStrategy strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter);
            String value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1), strategy).trim();
            value=value.replaceAll("[^a-zA-Z, ]","");
            returnValues.put("Name_ReportDetails",value);
            rect = new Rectangle(530,700,100,20);
            regionFilter = new TextRegionEventFilter(rect);
            strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter);
            value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1), strategy).trim();
            value=value.split("\n")[0];
            value=value.replace(" ","").replace("Collected:","").replace("Received:","");
            returnValues.put("DateOfCollection_ReportDetails",value);            
            for(int page=1;page<=2;page++){
                for(float y=610;y>=40;y--){
                    rect = new Rectangle(35,y,30,10);
                    regionFilter = new TextRegionEventFilter(rect);
                    strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter);
                    value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim();
                    rect = new Rectangle(250,y,60,10);
                    regionFilter = new TextRegionEventFilter(rect);
                    strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter);
                    String field = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim();
                    value=value.replaceAll("[^0-9.]", "");
                    if(field.length()>0 && value.length()>0){
                        if(value.contains("\n")){
                            String[] temp=value.split("\n");
                            if(temp.length>1)
                                value=temp[1].trim();
                        }
                        if(field.contains("\n")){
                            String[] temp=field.split("\n");
                            field=temp[0].trim();
                        }
                        if(!field.toUpperCase().equals(field))
                            returnValues.put(field+"_Measurement",value);
                    }
                }
            }
            pdfDoc.close();
        } catch (IOException e) {
            System.out.println("Could not find file "+pdfFile);
            throw(e);
        }
        return returnValues;
    }
}