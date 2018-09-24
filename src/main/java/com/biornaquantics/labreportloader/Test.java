
/* 
 * To change this license header, choose License Headers in Project Properties. 
 * To change this template file, choose Tools | Templates 
 * and open the template in the editor. 
 */ 
package com.biornaquantics.labreportloader; 
 
import com.itextpdf.kernel.geom.Rectangle; 
import com.itextpdf.kernel.pdf.PdfDocument; 
import com.itextpdf.kernel.pdf.PdfReader; 
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor; 
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter; 
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredTextEventListener; 
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextExtractionStrategy; 
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy; 
import java.awt.image.BufferedImage; 
import java.io.File; 
import java.io.IOException; 
import java.util.ArrayList; 
import java.util.LinkedHashMap; 
import java.util.List; 
import java.util.Map; 
import javax.imageio.ImageIO; 
 
/** 
 * 
 * @author tihor 
 */ 
 
import org.apache.pdfbox.pdmodel.PDDocument; 
import org.apache.pdfbox.pdmodel.PDPage; 
import org.apache.pdfbox.rendering.PDFRenderer; 
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject; 

public class Test { 
    public static void main(String[] args){ 
        try{ 
            //List<JSONObject> pdf_location_mappings = BQJSONParser.parseJSONFile("D:\\BiornaQuantics\\pdf_mapping_IgG4.json");
            //Map<String,String> lab_to_internal_mappings=BQJSONParser.parseLabToInternalMappingJSON("D:\\BiornaQuantics\\lab_to_internal_mapping_IgG4.json");
            //String sFileWithPath="D:\\BiornaQuantics\\Food Sensitivities IgG4\\FoodS_Abi Tyrrell_2016.06.02.pdf";
            //String sFileWithPath="D:\\BiornaQuantics\\Food Sensitivities IgG4\\FoodS_Abi Tyrrell_2016.06.02.pdf";
            Map<String,String> sTempMap;
            //File folder = new File("D:\\BiornaQuantics\\GI-MAP\\");
            File folder = new File("d:\\BiornaQuantics\\GI-MAP\\");
            File[] listOfFiles = folder.listFiles();
            for(int i=0;i<listOfFiles.length;i++){ 
                if(i<4) 
                    continue;
                //String sZZZ=listOfFiles[i].getAbsolutePath();
                
                //if(sZZZ.equals("D:\\BiornaQuantics\\GI-MAP\\GIMAPZ_Michelle Saddington_2018.07.30.pdf")){
                //    System.out.println(i+":"+listOfFiles[i].getAbsolutePath());
                //    sZZZ=PDFExtractor.ExtractPageText(sZZZ,2);
                //    System.out.println(sZZZ);
                //}
                //sTempMap=ExtractGIMAPData(listOfFiles[i].getAbsolutePath()); 
                System.out.println(PDFExtractor.ExtractPageText(listOfFiles[i].getAbsolutePath(),1));
                //for(String key:sTempMap.keySet()) 
                //    System.out.println(key+":"+sTempMap.get(key)); 
                //if(i>5) 
                    //break; 
            } 
        }catch(IOException e){ 
            e.printStackTrace(); 
        } 
    } 
    public static Map<String,String> ExtractGIMAPData(String pdfFile) throws IOException{
        System.out.println(pdfFile); 
        Map<String,String> returnValues=new LinkedHashMap<>(); 
        try { 
            PdfDocument pdfDoc=new PdfDocument(new PdfReader(pdfFile)); 
            Rectangle rect = new Rectangle(300,680,80,10); 
            TextRegionEventFilter regionFilter = new TextRegionEventFilter(rect); 
            ITextExtractionStrategy strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
            String value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(2), strategy).trim();
            if(value.length()>0)
                value=value.replace("Patient: ","");
            //System.out.println(value);
            returnValues.put("Name_ReportDetails",value); 
            rect = new Rectangle(300,660,80,10); 
            regionFilter = new TextRegionEventFilter(rect); 
            strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
            value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(2), strategy).trim();
            value=value.split("\n")[0]; 
            value=value.replace(" ","").replace("Collected:","").replace("Received:",""); 
            returnValues.put("DateOfCollection_ReportDetails",value);
            //System.out.println(value);
            //value=value.split("\n")[0]; 
            //value=value.replace(" ","").replace("Collected:","").replace("Received:",""); 
            //returnValues.put("DateOfCollection_ReportDetails",value);  
            for(int page=2;page<=5;page++){ 
                for(float y=700;y>=40;y-=10){ 
                    rect = new Rectangle(35,y,100,10); 
                    regionFilter = new TextRegionEventFilter(rect); 
                    strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
                    String field = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim(); 
                    rect = new Rectangle(250,y,60,10); 
                    regionFilter = new TextRegionEventFilter(rect); 
                    strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
                    value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim();
                    rect = new Rectangle(464,y,60,10); 
                    regionFilter = new TextRegionEventFilter(rect); 
                    strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
                    String range = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(page), strategy).trim();
                    if(field.length()>0 && value.length()>0 ){
                        if(!value.contains("Result")){
                            //System.out.println(field+"\t"+value+"\t"+range);
                            value=value.replace("<dl","0");
                            value=value.replace("N/A","Negative");
                            try{
                                double dTemp=Double.parseDouble(value);
                                value=""+dTemp;
                            }catch(NumberFormatException e){
                                //do nothing
                                //System.out.println(field+"\t"+value+"\t"+range);
                            }
                            returnValues.put(field+"_Measurement",value);
                        }
                    }
                }
            }
        } catch (IOException e) { 
            System.out.println("Could not find file "+pdfFile); 
            throw(e); 
        } 
        return returnValues;
    }
    public static Map<String,String> ExtractIgG4PDFData(String pdfFile, List<JSONObject> jsonData) throws IOException{ 
        System.out.println(pdfFile); 
        Map<String,String> returnValues=new LinkedHashMap<>(); 
        try { 
            PdfDocument pdfDoc=new PdfDocument(new PdfReader(pdfFile)); 
            Rectangle rect = new Rectangle(100,(float)725.72,100,10); 
            TextRegionEventFilter regionFilter = new TextRegionEventFilter(rect); 
            ITextExtractionStrategy strategy = new FilteredTextEventListener(new LocationTextExtractionStrategy(), regionFilter); 
            String value = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(1), strategy).trim(); 
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
                        //System.out.println("Field:"+field+" Value:"+str+" Field len:"+field.length()+" Value len:"+str.length()); 
                            returnValues.put(field+"_Measurement",value); 
                    } 
                } 
            } 
        } catch (IOException e) { 
            System.out.println("Could not find file "+pdfFile); 
            throw(e); 
        } 
        return returnValues; 
    } 
} 