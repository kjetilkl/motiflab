

package motiflab.engine.data.analysis;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.task.OperationTask;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleCollection;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.NumericVariable;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.dataformat.DataFormat;
import org.apache.commons.math3.distribution.HypergeometricDistribution;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFTextbox;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author kjetikl
 */
public class CompareCollectionsAnalysis extends Analysis {
    private final static String typedescription="Analysis: compare collections";
    private final static String analysisName="compare collections";
    private final static String description="Estimates p-value for overlap between two collections according to Fisher's exact test";
    private String firstCollectionName=null;
    private String secondCollectionName=null;
    private String backgroundCollectionName=null;
    private String collectionType=null;
    private int[] contingencyTable=null;  
    private double pvalueAtLeastObservedOverlap=0;
    private double pvalueAtMostObservedOverlap=0;

    private static final int FIRST_SIZE=0;
    private static final int SECOND_SIZE=1;
    private static final int BACKGROUND_SIZE=2;
    private static final int FIRST_INTERSECTION_SECOND=3;
    private static final int FIRST_UNION_SECOND=4;
    private static final int NEITHER_FIRST_NOR_SECOND=5;

    public CompareCollectionsAnalysis() {
        this.name="CompareCollectionsAnalysis_temp";
        addParameter("First",DataCollection.class, null,new Class[]{DataCollection.class},null,true,false);
        addParameter("Second",DataCollection.class, null,new Class[]{DataCollection.class},null,true,false);
        addParameter("Total",DataCollection.class, null,new Class[]{DataCollection.class},"The total collection is used to find the number of entries that are not present in either of the two collections",false,false);
    }

    @Override
    public String[] getSourceProxyParameters() {return new String[]{"First","Second"};}   
    
    @Override
    public boolean canUseAsSourceProxy(Data data) {
        return (data instanceof DataCollection);
    }  
    
    
    /** Returns a list of output parameters that can be set when an Analysis is output */
    @Override
    public Parameter[] getOutputParameters() {
         Parameter imageformat=new Parameter("Image format",String.class, "png",new String[]{"png","svg","pdf","eps"},"The image format to use for the graph",false,false);                       
         Parameter scalepar=new Parameter("Graph scale",Integer.class,100,new Integer[]{10,2000},"Scale of graphics plot (in percent)",false,false);
         return new Parameter[]{imageformat,scalepar};
    }
    
    @Override
    public String[] getOutputParameterFilter(String parameter) {
        if (parameter.equals("Graph scale") || parameter.equals("Image format")) return new String[]{HTML};       
        return null;
    }       

    @Override
    public String[] getResultVariables() {
        return new String[]{"p-value at least observed overlap","p-value at most observed overlap"};
    }

    @Override
    public Data getResult(String variablename, MotifLabEngine engine) throws ExecutionError {
        if (variablename==null || variablename.isEmpty()) throw new ExecutionError("'"+getName()+"' does not have a result for ''");
        else if (variablename.equals("p-value at least observed overlap")) return new NumericVariable("temp",pvalueAtLeastObservedOverlap);
        else if (variablename.equals("p-value at most observed overlap")) return new NumericVariable("temp",pvalueAtMostObservedOverlap);
        else throw new ExecutionError("'"+getName()+"' does not have a result for '"+variablename+"'");
    }

    @Override
    public Class getResultType(String variablename) {
       if (!hasResult(variablename)) return null;
       else return NumericVariable.class;
    }

    @Override
    public String getAnalysisName() {
        return analysisName;
    }

    @Override
    public String getDescription() {return description;}



    @Override
    @SuppressWarnings("unchecked")
    public CompareCollectionsAnalysis clone() {
        CompareCollectionsAnalysis newanalysis=new CompareCollectionsAnalysis();
        super.cloneCommonSettings(newanalysis);
        newanalysis.firstCollectionName=this.firstCollectionName;
        newanalysis.secondCollectionName=this.secondCollectionName;
        newanalysis.backgroundCollectionName=this.backgroundCollectionName;
        newanalysis.contingencyTable=this.contingencyTable.clone();
        newanalysis.pvalueAtLeastObservedOverlap=this.pvalueAtLeastObservedOverlap;
        newanalysis.pvalueAtMostObservedOverlap=this.pvalueAtMostObservedOverlap;
        newanalysis.collectionType=this.collectionType;
        return newanalysis;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void importData(Data source) throws ClassCastException {
        super.importData(source);
        CompareCollectionsAnalysis other=((CompareCollectionsAnalysis)source);
        this.firstCollectionName=other.firstCollectionName;
        this.secondCollectionName=other.secondCollectionName;
        this.backgroundCollectionName=other.backgroundCollectionName;
        this.contingencyTable=other.contingencyTable;
        this.pvalueAtLeastObservedOverlap=other.pvalueAtLeastObservedOverlap;
        this.pvalueAtMostObservedOverlap=other.pvalueAtMostObservedOverlap;
        this.collectionType=other.collectionType;       
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription;}


    @Override
    public OutputData formatHTML(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (format!=null) format.setProgress(5);
        int totalA=contingencyTable[FIRST_SIZE];
        int totalB=contingencyTable[SECOND_SIZE];
        int total=contingencyTable[BACKGROUND_SIZE];
        int A_B=contingencyTable[FIRST_INTERSECTION_SECOND];
        int A_notB=totalA-A_B;
        int notA_B=totalB-A_B;
        int notA_notB=total-contingencyTable[FIRST_UNION_SECOND];

        int scalepercent=100;
        String imageFormat="png";
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             imageFormat=(String)settings.getResolvedParameter("Image format",defaults,engine);
             scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);             
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);
        File imagefile=outputobject.createDependentFile(engine,imageFormat); 
        int width=300, height=200;
        try {
            saveVennDiagramAsImage(imagefile, scale, width, height);
        } catch (IOException e) {
            engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
        }
        if (format!=null) format.setProgress(50);
        DecimalFormat decimalformatter=new DecimalFormat("0.0");
        engine.createHTMLheader("Compare Collections Analysis", null, null, false, true, true, outputobject);
        outputobject.append("<div align=\"center\">\n",HTML);
        outputobject.append("<h2 class=\"headline\">Compare Collections Analysis</h2>\n",HTML);
        //outputobject.append("<br />",HTML);
        outputobject.append("<div class=\"summary\">\n",HTML);
        outputobject.append("Comparison between "+collectionType.toLowerCase()+" collections <span class=\"dataitem\">"+firstCollectionName+"</span> and <span class=\"dataitem\">"+secondCollectionName+"</span><br>\n",HTML);
        outputobject.append("with respect to a total of "+total+" entries",HTML);
        if (backgroundCollectionName!=null) {
            outputobject.append(" from collection <span class=\"dataitem\">",HTML);
            outputobject.append(backgroundCollectionName,HTML);
            outputobject.append("</span>",HTML);
        }
         outputobject.append("\n</div>\n",HTML);
        outputobject.append("<br>\n",HTML);
        if (imageFormat.equals("pdf")) outputobject.append("<object type=\"application/pdf\" data=\"file:///"+imagefile.getAbsolutePath()+"\"></object>",HTML);
        else {
            outputobject.append("<img src=\"file:///"+imagefile.getAbsolutePath()+"\"",HTML);
            outputobject.append(" width="+(int)Math.ceil(width*scale)+" height="+(int)Math.ceil(height*scale),HTML);                    
            outputobject.append(" />\n",HTML);  
        }
        
        outputobject.append("<br>\n<br>\n<table width=300>\n",HTML);
        outputobject.append("<tr><td style=\"background-color:#CCCCCC\"> </td><td style=\"background-color:#CCCCCC;text-align:center\"> <b>"+secondCollectionName+"</b> </td><td style=\"background-color:#CCCCCC;text-align:center\"> <b>&not;&nbsp;"+secondCollectionName+"</b> </td><td style=\"background-color:#CCCCCC;text-align:center\"> Total </td></tr>\n",HTML);
        outputobject.append("<tr><td style=\"background-color:#CCCCCC;text-align:right\"> <b>&nbsp;&nbsp;"+firstCollectionName+"</b> </td><td class=\"num\" style=\"background-color:#F0A0FF\"> "+A_B+" </td><td class=\"num\" style=\"background-color:#FFA0A0\"> "+A_notB+" </td><td class=\"num\" style=\"background-color:#EEEECC\"> "+totalA+" </td></tr>\n",HTML);
        outputobject.append("<tr><td style=\"background-color:#CCCCCC;text-align:right\"> <b>&not;&nbsp;"+firstCollectionName+"</b> </td><td class=\"num\" style=\"background-color:#A0A0FF\"> "+notA_B+" </td><td class=\"num\" style=\"background-color:#FFFFA0\"> "+notA_notB+" </td><td class=\"num\" style=\"background-color:#EEEECC\"> "+(total-totalA)+" </td></tr>\n",HTML);
        outputobject.append("<tr><td style=\"background-color:#CCCCCC;text-align:right\"> Total </td><td class=\"num\" style=\"background-color:#EEEECC\"> "+totalB+" </td><td class=\"num\" style=\"background-color:#EEEECC\"> "+(total-totalB)+" </td><td class=\"num\" style=\"background-color:#EEEECC\"> "+total+" </td></tr>\n",HTML);
        outputobject.append("</table>\n<br>\n",HTML);
        outputobject.append(decimalformatter.format(((double)A_B/(double)totalA)*100.0)+"% of <span class=\"dataitem\">"+firstCollectionName+"</span> overlaps with <span class=\"dataitem\">"+secondCollectionName+"</span><br>\n",HTML);
        outputobject.append(decimalformatter.format(((double)A_B/(double)totalB)*100.0)+"% of <span class=\"dataitem\">"+secondCollectionName+"</span> overlaps with <span class=\"dataitem\">"+firstCollectionName+"</span><br>\n",HTML);
        outputobject.append("<br>p-value (overlap&gt;=observed) = "+pvalueAtLeastObservedOverlap,HTML);
        outputobject.append("<br>p-value (overlap&lt;=observed) = "+pvalueAtMostObservedOverlap+"<br>\n",HTML);
        outputobject.append("</div>\n",HTML);
        outputobject.append("</body>\n</html>\n",HTML);
        if (format!=null) format.setProgress(100);
        return outputobject;
    }
    
    private byte[] saveVennDiagramAsImage(File file, double scale, int width, int height) throws IOException {
        // write the image to file
        if (file!=null) {
            if (file.getName().endsWith(".png")) { // bitmap PNG format   
                BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
                Graphics2D g=image.createGraphics();   
                paintVennDiagram(scale, width, height, g);            
                OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
                ImageIO.write(image, "png", output);
                output.close(); 
                g.dispose();
                return null;                                
            } else { // vector format      
                VectorGraphics2D g=null;
                String filename=file.getName();
                     if (filename.endsWith(".svg")) g = new SVGGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                else if (filename.endsWith(".pdf")) g = new PDFGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                else if (filename.endsWith(".eps")) g = new EPSGraphics2D(0, 0, Math.ceil(width*scale), Math.ceil(height*scale));
                g.setClip(0, 0, (int)Math.ceil(width*scale),(int)Math.ceil(height*scale));
                paintVennDiagram(scale, width, height, g);                           
                FileOutputStream fileStream = new FileOutputStream(file);
                try {
                    fileStream.write(g.getBytes());
                } finally {
                    fileStream.close();
                } 
                return null;
            }
        } else { // No output file. Create the image as a byte[] array for inclusion in Excel
            BufferedImage image=new BufferedImage((int)Math.ceil(width*scale),(int)Math.ceil(height*scale), BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();   
            paintVennDiagram(scale, width, height, g);
            org.apache.commons.io.output.ByteArrayOutputStream outputStream=new org.apache.commons.io.output.ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            g.dispose();     
            byte[] array=outputStream.toByteArray();
            outputStream.close();
            return array;            
        }        
    }    
    

    private void paintVennDiagram(double scale, int width, int height, Graphics2D g) throws IOException {
        g.scale(scale, scale);
        g.setColor(java.awt.Color.WHITE);
        g.fillRect(0, 0, width+10, height+10);
        g.setColor(new Color(255,255,240));
        g.fillRect(0, 0, width-1, height-1);
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width-1, height-1);
        int sidemargin=15;
        int topmargin=15;
        int circlediameter=height-(topmargin*2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // left circle
        g.setColor(new Color(255,0,0,80));
        g.fillOval(sidemargin, topmargin, circlediameter, circlediameter);
        g.setColor(Color.RED);
        g.drawOval(sidemargin, topmargin, circlediameter, circlediameter);
        int leftCenter=sidemargin+(int)(circlediameter/2.0);
        String label=firstCollectionName+" ("+contingencyTable[FIRST_SIZE]+")";
        int stringwidth=g.getFontMetrics().stringWidth(label);
        int xpos=leftCenter-20-(int)(stringwidth/2.0);
        if (xpos<5) xpos=5;
        g.drawString(label,xpos,topmargin+30);
        int endlabel=xpos+stringwidth;
        // right circle
        int rightCenter=width-(sidemargin+(int)(circlediameter/2.0)+1);
        g.setColor(new Color(0,0,255,80));
        g.fillOval(width-(circlediameter+sidemargin), topmargin, circlediameter, circlediameter);
        g.setColor(Color.BLUE);
        g.drawOval(width-(circlediameter+sidemargin), topmargin, circlediameter, circlediameter);
        label=secondCollectionName+" ("+contingencyTable[SECOND_SIZE]+")";
        stringwidth=g.getFontMetrics().stringWidth(label);
        xpos=rightCenter+20-(int)(stringwidth/2.0);
        if (xpos+stringwidth>width-5) xpos=(width-5)-stringwidth;
        int yoffset=topmargin+30;
        if (xpos<endlabel+10) yoffset+=15; // two avoid label crash
        g.drawString(label,xpos,yoffset);
        // draw counts
        Font oldfont=g.getFont();
        g.setFont(oldfont.deriveFont(Font.BOLD,18));
        // count just A
        g.setColor(Color.RED);
        String countString=""+(contingencyTable[FIRST_SIZE]-contingencyTable[FIRST_INTERSECTION_SECOND]); // A but not B
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, leftCenter-(int)(circlediameter*0.2+stringwidth/2.0), (int)(height/2.0)+5);
        // count intersection
        g.setColor(new Color(180,0,180));
        countString=""+contingencyTable[FIRST_INTERSECTION_SECOND]; // A intersects B
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, (int)((width-stringwidth)/2.0), (int)(height/2.0)+5);
        // count just B
        g.setColor(Color.BLUE);
        countString=""+(contingencyTable[SECOND_SIZE]-contingencyTable[FIRST_INTERSECTION_SECOND]); // B but not A
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, rightCenter+(int)(circlediameter*0.2-stringwidth/2.0), (int)(height/2.0)+5);
        // neither A nor B
        g.setFont(oldfont);
        g.setColor(new Color(180,180,0));
        countString=""+(contingencyTable[BACKGROUND_SIZE]-contingencyTable[FIRST_UNION_SECOND]); //
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, width-(stringwidth+10), topmargin);
        // total
        g.setFont(oldfont);
        g.setColor(Color.BLACK);
        countString="Total="+contingencyTable[BACKGROUND_SIZE]; //
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, 8, topmargin);        
        // union
        g.setColor(new Color(140,0,160));
        countString="Union="+(contingencyTable[FIRST_UNION_SECOND]); //
        stringwidth=g.getFontMetrics().stringWidth(countString);
        g.drawString(countString, (int)((width-stringwidth)/2.0), height-4);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }
    
    // @Override
    public OutputData formatExcelOld(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        if (format!=null) format.setProgress(5);
        int totalA=contingencyTable[FIRST_SIZE];
        int totalB=contingencyTable[SECOND_SIZE];
        int total=contingencyTable[BACKGROUND_SIZE];
        int A_B=contingencyTable[FIRST_INTERSECTION_SECOND];
        int A_notB=totalA-A_B;
        int notA_B=totalB-A_B;
        int notA_notB=total-contingencyTable[FIRST_UNION_SECOND];

        int scalepercent=100;
        if (settings!=null) {
          try {
             Parameter[] defaults=getOutputParameters();
             scalepercent=(Integer)settings.getResolvedParameter("Graph scale",defaults,engine);
          } 
          catch (ExecutionError e) {throw e;} 
          catch (Exception ex) {throw new ExecutionError("An error occurred during output formatting", ex);}
        } 
        double scale=(scalepercent==100)?1.0:(((double)scalepercent)/100.0);

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName());
        CreationHelper helper = workbook.getCreationHelper();
        Drawing drawing = sheet.createDrawingPatriarch();       
        
        CellStyle title=createExcelStyle(workbook, HSSFCellStyle.BORDER_NONE, (short)0, HSSFCellStyle.ALIGN_LEFT, false);      
        addFontToExcelCellStyle(workbook, title, null, (short)(workbook.getFontAt((short)0).getFontHeightInPoints()*2.5), true, false);
                
        CellStyle table_GRAY=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.GREY_25_PERCENT.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle table_RED=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.CORAL.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle table_YELLOW=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LIGHT_YELLOW.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle table_BLUE=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.PALE_BLUE.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle table_VIOLET=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.LAVENDER.index, HSSFCellStyle.ALIGN_RIGHT, false);      
        CellStyle table_SUMMARY=createExcelStyle(workbook, HSSFCellStyle.BORDER_THIN, HSSFColor.GOLD.index, HSSFCellStyle.ALIGN_RIGHT, false);      
            
        int tablestartrow=6;
        int tablestartcol=1;
        try {
            Row row=sheet.createRow(4);
            row.setHeight((short)3200);
            byte[] image=saveVennDiagramAsImage(null, scale, 300, 200);
            int imageIndex=workbook.addPicture(image, HSSFWorkbook.PICTURE_TYPE_PNG);
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(1);
            anchor.setRow1(4);
            anchor.setAnchorType(ClientAnchor.MOVE_DONT_RESIZE);
            Picture pict=drawing.createPicture(anchor, imageIndex);	
            pict.resize(); 
        } catch (Exception e) {
            outputStringValueInCell(sheet.createRow(4), 1, "Error when creating image!", null); 
            e.printStackTrace(System.err);
        }        
        if (format!=null) format.setProgress(50);   
        Row row=sheet.createRow(tablestartrow); 
        outputStringValuesInCells(row, new String[]{" "," "+firstCollectionName+" "," \u00AC"+firstCollectionName+" "," Total "}, tablestartcol, table_GRAY);  
        row=sheet.createRow(tablestartrow+1); 
        outputStringValueInCell(row, tablestartcol, " "+secondCollectionName+" ", table_GRAY);  
        outputNumericValueInCell(row, tablestartcol+1, A_B, table_VIOLET);
        outputNumericValueInCell(row, tablestartcol+2, A_notB, table_RED);
        outputNumericValueInCell(row, tablestartcol+3, totalA, table_SUMMARY);
        row=sheet.createRow(tablestartrow+2); 
        outputStringValueInCell(row, tablestartcol, " \u00AC"+secondCollectionName+" ", table_GRAY);
        outputNumericValueInCell(row, tablestartcol+1, notA_B, table_BLUE);
        outputNumericValueInCell(row, tablestartcol+2, notA_notB, table_YELLOW);
        outputNumericValueInCell(row, tablestartcol+3, (total-totalA), table_SUMMARY);        
        row=sheet.createRow(tablestartrow+3); 
        outputStringValueInCell(row, tablestartcol, " Total ", table_GRAY); 
        outputNumericValueInCell(row, tablestartcol+1, totalB, table_SUMMARY);
        outputNumericValueInCell(row, tablestartcol+2, (total-totalB), table_SUMMARY);
        outputNumericValueInCell(row, tablestartcol+3, total, table_SUMMARY);         
        
        sheet.autoSizeColumn((short)0);
        sheet.autoSizeColumn((short)1);
        sheet.autoSizeColumn((short)2);
        sheet.autoSizeColumn((short)3);
        sheet.autoSizeColumn((short)4);        
        
        row=sheet.createRow(tablestartrow+5);
        outputNumericValueInCell(row, tablestartcol, (((double)A_B/(double)totalA)*100.0), null);
        outputStringValueInCell(row, tablestartcol+1, "% of \""+firstCollectionName+"\" overlaps with \""+secondCollectionName+"\"", null);
        row=sheet.createRow(tablestartrow+6);
        outputNumericValueInCell(row, tablestartcol, (((double)A_B/(double)totalB)*100.0), null);        
        outputStringValueInCell(row, tablestartcol+1, "% of \""+secondCollectionName+"\" overlaps with \""+firstCollectionName+"\"", null);
        
        row=sheet.createRow(tablestartrow+8);
        outputStringValueInCell(row, tablestartcol, "p-value (overlap>=observed) = ", null);
        outputNumericValueInCell(row, tablestartcol+3, pvalueAtLeastObservedOverlap, null);
        row=sheet.createRow(tablestartrow+9);
        outputStringValueInCell(row, tablestartcol, "p-value (overlap<=observed) = ", null);
        outputNumericValueInCell(row, tablestartcol+3, pvalueAtMostObservedOverlap, null);
         
        outputStringValueInCell(sheet.createRow(0), 0, "Compare collections analysis", title);
        StringBuilder firstLine=new StringBuilder();
        firstLine.append("Comparison between ");
        firstLine.append(collectionType.toLowerCase());
        firstLine.append(" collections \"");
        firstLine.append(firstCollectionName);
        firstLine.append("\" and \"");
        firstLine.append(secondCollectionName);
        firstLine.append("\" with respect to a total of ");
        firstLine.append(total);
        firstLine.append(" entries");
        if (backgroundCollectionName!=null) {            
            firstLine.append(" from collection \"");
            firstLine.append(backgroundCollectionName);
            firstLine.append("\"");
        }
        outputStringValueInCell(sheet.createRow(2), 0, firstLine.toString(), null);
        
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,"xls");        
        try {
            BufferedOutputStream stream=new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(stream);
            stream.close();
        } catch (Exception e) {
            throw new ExecutionError("An error occurred when creating the Excel file: "+e.toString(),0);
        }
        if (format!=null) format.setProgress(100);        
        outputobject.setBinary(true);        
        outputobject.setDirty(true); // this is not set automatically since I don't append to the document
        outputobject.setDataFormat(EXCEL); // this is not set automatically since I don't append to the document       
        return outputobject;
    }    

    @Override
    public OutputData formatRaw(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();

        int totalA=contingencyTable[FIRST_SIZE];
        int totalB=contingencyTable[SECOND_SIZE];
        int total=contingencyTable[BACKGROUND_SIZE];
        int A_B=contingencyTable[FIRST_INTERSECTION_SECOND];
        int A_notB=totalA-A_B;
        int notA_B=totalB-A_B;
        int notA_notB=total-contingencyTable[FIRST_UNION_SECOND];
        DecimalFormat decimalformatter=new DecimalFormat("0.0");
        task.setStatusMessage("Executing operation: output");
        outputobject.append("#Comparison between "+collectionType.toLowerCase()+" collections '"+firstCollectionName+"' (size="+totalA+") and '"+secondCollectionName+"' (size="+totalB+")",RAWDATA);
        outputobject.append(" with respect to a total of "+total+" entries",RAWDATA);
        if (backgroundCollectionName!=null) outputobject.append(" from collection '"+backgroundCollectionName+"'",RAWDATA);
        outputobject.append("\n\n",RAWDATA);
        outputobject.append("\t"+secondCollectionName+"\tNot "+secondCollectionName+"\tTotal\n",RAWDATA);
        outputobject.append(firstCollectionName+"\t"+A_B+"\t"+A_notB+"\t"+totalA+"\n",RAWDATA);
        outputobject.append("Not "+firstCollectionName+"\t"+notA_B+"\t"+notA_notB+"\t"+(total-totalA)+"\n",RAWDATA);
        outputobject.append("Total\t"+totalB+"\t"+(total-totalB)+"\t"+total+"\n\n",RAWDATA);
        outputobject.append(decimalformatter.format(((double)A_B/(double)totalA)*100.0)+"% of '"+firstCollectionName+"' overlaps with '"+secondCollectionName+"'\n",RAWDATA);
        outputobject.append(decimalformatter.format(((double)A_B/(double)totalB)*100.0)+"% of '"+secondCollectionName+"' overlaps with '"+firstCollectionName+"'\n",RAWDATA);         
        outputobject.append("\np-value (overlap>=observed)\t"+pvalueAtLeastObservedOverlap+"\n",RAWDATA);
        outputobject.append("p-value (overlap<=observed)\t"+pvalueAtMostObservedOverlap+"\n",RAWDATA);
        format.setProgress(100);
        return outputobject;
    }
    
    @Override
    public OutputData formatExcel(OutputData outputobject, MotifLabEngine engine, ParameterSettings settings, ExecutableTask task, DataFormat format) throws ExecutionError, InterruptedException {
        int totalA=contingencyTable[FIRST_SIZE];
        int totalB=contingencyTable[SECOND_SIZE];
        int total=contingencyTable[BACKGROUND_SIZE];
        int A_B=contingencyTable[FIRST_INTERSECTION_SECOND];
        int A_notB=totalA-A_B;
        int notA_B=totalB-A_B;
        int notA_notB=total-contingencyTable[FIRST_UNION_SECOND];
        Workbook workbook=null;
        try {
            InputStream stream = CompareRegionDatasetsAnalysis.class.getResourceAsStream("resources/AnalysisTemplate_CompareCollections.xlt");
            workbook = WorkbookFactory.create(stream);
            stream.close();
            Sheet sheet = workbook.getSheetAt(0);   // just use the first sheet  
            
            sheet.setForceFormulaRecalculation(true);
            String descriptionString="Comparison between "+collectionType.toLowerCase()+" collections \""+firstCollectionName+"\" and \""+secondCollectionName+"\" with respect to a total of "+total+" entries";
            if (backgroundCollectionName!=null) descriptionString+=" from collection \""+backgroundCollectionName+"\"";
            sheet.getRow(2).getCell(0).setCellValue(descriptionString);
            
            // the matrix
            sheet.getRow(18).getCell(1).setCellValue(firstCollectionName);
            sheet.getRow(19).getCell(1).setCellValue("\u00AC"+firstCollectionName);
            sheet.getRow(20).getCell(1).setCellValue("Total");
            sheet.getRow(17).getCell(2).setCellValue(secondCollectionName);
            sheet.getRow(17).getCell(3).setCellValue("\u00AC"+secondCollectionName);
            sheet.getRow(17).getCell(4).setCellValue("Total"); 
            
            sheet.getRow(18).getCell(2).setCellValue(A_B);
            sheet.getRow(18).getCell(3).setCellValue(A_notB);
            sheet.getRow(18).getCell(4).setCellValue(totalA);
            
            sheet.getRow(19).getCell(2).setCellValue(notA_B);
            sheet.getRow(19).getCell(3).setCellValue(notA_notB);
            sheet.getRow(19).getCell(4).setCellValue(total-totalA);
            
            sheet.getRow(20).getCell(2).setCellValue(totalB);
            sheet.getRow(20).getCell(3).setCellValue(total-totalB);
            sheet.getRow(20).getCell(4).setCellValue(total);            
            
            // Overlap and p-values
            sheet.getRow(23).getCell(1).setCellValue((double)A_B/(double)totalA);
            sheet.getRow(23).getCell(2).setCellValue(" of \""+firstCollectionName+"\" overlaps with \""+secondCollectionName+"\"");
            sheet.getRow(24).getCell(1).setCellValue((double)A_B/(double)totalB);           
            sheet.getRow(24).getCell(2).setCellValue(" of \""+secondCollectionName+"\" overlaps with \""+firstCollectionName+"\"");           
            
            sheet.getRow(26).getCell(3).setCellValue(pvalueAtLeastObservedOverlap);
            sheet.getRow(27).getCell(3).setCellValue(pvalueAtMostObservedOverlap);
            
            // update numbers in the Venn diagram
            HSSFPatriarch pat = ((HSSFSheet)sheet).getDrawingPatriarch();
            List children = pat.getChildren();
            Iterator it = children.iterator(); 
            while(it.hasNext()) {           
                Object shape = it.next();
                if (shape instanceof HSSFTextbox){
                  HSSFTextbox textbox = (HSSFTextbox)shape;
                  HSSFRichTextString richString = textbox.getString();
                  String str = richString.getString();
                  String newText=null;
                       if (str.equals("S1")) newText=(""+A_notB);
                  else if (str.equals("S2")) newText=(""+notA_B);
                  else if (str.equals("U12")) newText=(""+A_B);
                  else if (str.equals("Total")) newText=("Total = "+total);
                  else if (str.equals("Union")) newText=("Union = "+contingencyTable[FIRST_UNION_SECOND]);
                  else if (str.equals("Total_FirstSet")) newText=(firstCollectionName+" ("+totalA+")");
                  else if (str.equals("Total_SecondSet")) newText=(secondCollectionName+" ("+totalB+")");
                  else if (str.equals("NotInSet")) newText=(""+notA_notB);
                  if (newText!=null) textbox.setString(replaceText(richString,newText,(HSSFWorkbook)workbook));
                }
             }              
                             
            // HSSFFormulaEvaluator.evaluateAllFormulaCells(workbook);
            
        } catch (Exception e) {
            throw new ExecutionError(e.getMessage());
        }
       
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,"xls");        
        try {
            BufferedOutputStream stream=new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(stream);
            stream.close();
        } catch (Exception e) {
            throw new ExecutionError("An error occurred when creating the Excel file: "+e.toString(),0);
        }
        outputobject.setBinary(true);        
        outputobject.setDirty(true); // this is not set automatically since I don't append to the document
        outputobject.setDataFormat(EXCEL); // this is not set automatically since I don't append to the document
        return outputobject;        
    }      
    
        
    private HSSFRichTextString replaceText(HSSFRichTextString oldString, String newString, HSSFWorkbook workbook) {
       short fontIndex=oldString.getFontAtIndex(0);
       HSSFFont font=(HSSFFont)workbook.getFontAt(fontIndex);
       short colorIndex = font.getColor();
       HSSFColor color = font.getHSSFColor(workbook);  
       HSSFRichTextString newrichText=new HSSFRichTextString(newString);
       //font.setColor(colorIndex);
       newrichText.applyFont(font);
       return newrichText;                  
    }

    @Override
    protected Dimension getDefaultDisplayPanelDimensions() {
        return new Dimension(600,538);
    }

    @Override
    public void runAnalysis(OperationTask task) throws Exception {
        DataCollection firstCollection=(DataCollection)task.getParameter("First");
        DataCollection secondCollection=(DataCollection)task.getParameter("Second");
        DataCollection backgroundCollection=(DataCollection)task.getParameter("Total");
        if (!firstCollection.getClass().equals(secondCollection.getClass())) throw new ExecutionError("The collections must be of the same type");
        collectionType=task.getEngine().getTypeNameForDataClass(firstCollection.getMembersClass());
        if (backgroundCollection!=null) {
            if (!backgroundCollection.getClass().equals(firstCollection.getClass())) throw new ExecutionError("The collections must be of the same type");
            backgroundCollectionName=backgroundCollection.getName();
        } else {
                 if (firstCollection instanceof SequenceCollection) backgroundCollection=task.getEngine().getDefaultSequenceCollection();
            else if (firstCollection instanceof MotifCollection) {backgroundCollection=new MotifCollection("temp"); ((MotifCollection)backgroundCollection).addMotifNames(task.getEngine().getNamesForAllDataItemsOfType(Motif.class));}
            else if (firstCollection instanceof ModuleCollection) {backgroundCollection=new MotifCollection("temp"); ((ModuleCollection)backgroundCollection).addModuleNames(task.getEngine().getNamesForAllDataItemsOfType(Module.class));}
        }
        firstCollectionName=firstCollection.getName();
        secondCollectionName=secondCollection.getName();
        task.checkExecutionLock(); // checks to see if this task should suspend execution
        if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
        task.setStatusMessage("Executing analysis: "+getAnalysisName());
        task.setProgress(20); //
        Thread.yield();
        contingencyTable=new int[6];
        int firstCount=0;
        int secondCount=0;
        int intersection=0;
        int union=0;
        for (String entry:firstCollection.getValues()) {
            if (!backgroundCollection.contains(entry)) continue; // do not count those that are not in the background
            firstCount++;
            if (secondCollection.contains(entry)) intersection++;
            union++;
            
        }
        for (String entry:secondCollection.getValues()) {
            if (!backgroundCollection.contains(entry)) continue;
            secondCount++;
            if (!firstCollection.contains(entry)) union++;
        }
        contingencyTable[BACKGROUND_SIZE]=backgroundCollection.size();
        contingencyTable[FIRST_SIZE]=firstCount;
        contingencyTable[SECOND_SIZE]=secondCount;
        contingencyTable[FIRST_INTERSECTION_SECOND]=intersection;
        contingencyTable[FIRST_UNION_SECOND]=union;
        contingencyTable[NEITHER_FIRST_NOR_SECOND]=backgroundCollection.size()-union;
        // calculate statistics
        HypergeometricDistribution hypergeometric = new HypergeometricDistribution(backgroundCollection.size(), firstCount, secondCount);
        pvalueAtLeastObservedOverlap=hypergeometric.upperCumulativeProbability(intersection);
        if (pvalueAtLeastObservedOverlap>1.0) pvalueAtLeastObservedOverlap=1.0; // this could happen because of rounding errors
        pvalueAtMostObservedOverlap=hypergeometric.cumulativeProbability(intersection);
        if (pvalueAtMostObservedOverlap>1.0) pvalueAtMostObservedOverlap=1.0; // this could happen because of rounding errors
    }


    // ------------ Serialization ---------
    private static final long serialVersionUID = 1L;

    private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
         short currentinternalversion=1; // this is an internal version number for serialization of objects of this type
         out.writeShort(currentinternalversion);
         out.defaultWriteObject();
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
         short currentinternalversion=in.readShort(); // the internalversion number is used to determine correct format of data
         if (currentinternalversion==1) {
             in.defaultReadObject();
         } else if (currentinternalversion>1) throw new ClassNotFoundException("Newer version");
    }
}
