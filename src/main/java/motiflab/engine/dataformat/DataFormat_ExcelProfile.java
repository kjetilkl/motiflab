/* 
   Some of the code is copied from a tutorial at:  http://mrbool.com/reading-excel-file-with-java/24562
 */

package motiflab.engine.dataformat;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.ExpressionProfile;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.SequenceCollection;
import motiflab.engine.protocol.ParseError;
import motiflab.gui.VisualizationSettings;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ConditionalFormattingThreshold;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFColorScaleFormatting;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingThreshold;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author kjetikl
 */
public class DataFormat_ExcelProfile extends DataFormat {
    private String name="ExcelProfile";
    private Class[] supportedTypes=new Class[]{ExpressionProfile.class};


    public DataFormat_ExcelProfile() {
        addOptionalParameter("Key column", new Integer(1), new Integer[]{1,Integer.MAX_VALUE},"The number of the column which contains the sequence names");
        addOptionalParameter("Value columns", "", null, "<html>This parameter can be used to explicitly specify which columns to retrieve from the Excel sheet.<br>The columns can be stated as a list of comma-separated numbers or by using dashes to specify a range.<br>E.g. a parameter value of \"3,4,6-8,9-11,14\" will retrieve the columns 3,4,6,7,8,9,10,11 and 14.<br>If left unspecified, all columns to the right of the key column will be retrieved.</html>");
        addOptionalParameter("Include entries",null, new Class[]{SequenceCollection.class},"Specifies which entries to include in the output. The default is to include data for all sequences.");
        addOptionalParameter("Header",Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"<html>For output: include the column headers in the first row.<br>For input: Use the values in the first row as column headers.</html>");
        addOptionalParameter("Color",Boolean.FALSE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"<html>Add a background color to each cell depending on the value</html>");
        setParameterFilter("Include entries","output");   
        setParameterFilter("Key column","input");   
        setParameterFilter("Value columns","input");  
        setParameterFilter("Color","output");
    }

    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean isAppendable() {
        return false;
    }    

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof ExpressionProfile); 
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (ExpressionProfile.class.isAssignableFrom(dataclass));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof ExpressionProfile);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (ExpressionProfile.class.isAssignableFrom(dataclass));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "xlsx";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"xls","xlsx"};}
    
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        ExpressionProfile profile=(ExpressionProfile)dataobject;
        SequenceCollection includeEntries;
        boolean header=false;
        boolean useColors=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             includeEntries=(SequenceCollection)settings.getResolvedParameter("Include entries",defaults,engine);
             header=(Boolean)settings.getResolvedParameter("Header",defaults,engine);    
             useColors=(Boolean)settings.getResolvedParameter("Color",defaults,engine); 
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           includeEntries=(SequenceCollection)getDefaultValueForParameter("Include entries");
           header=(Boolean)getDefaultValueForParameter("Header");           
        }
        
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(outputobject.getName()); 

        int rownum = 0;
        if (header) {
            XSSFCellStyle style = workbook.createCellStyle();
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setFillForegroundColor(new XSSFColor(new Color(255,255,200),null));
            style.setAlignment(HorizontalAlignment.CENTER);
            XSSFFont font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            Row row = sheet.createRow(rownum++);  
            int cellnumber=0;
            Cell headercell = row.createCell(cellnumber++);
            headercell.setCellValue("Sequence");
            headercell.setCellStyle(style);
            for (int j=0;j<profile.getNumberOfConditions();j++) {
               headercell = row.createCell(cellnumber++);
               headercell.setCellValue(profile.getHeader(j));  
               headercell.setCellStyle(style);
            }
        }
        if (includeEntries==null) includeEntries=engine.getDefaultSequenceCollection();
        ArrayList<Sequence> sequences=includeEntries.getAllSequencesInDefaultOrder(engine); 
        int size=sequences.size();            
        int i=0; 
        for (Sequence sequence:sequences) { // for each entry
              String sequencename=sequence.getName();
              Row row = sheet.createRow(rownum++);
              Cell keycell = row.createCell(0);
              keycell.setCellValue(sequencename);
              int cellnumber=1;
              for (int j=0;j<profile.getNumberOfConditions();j++) {
                   Cell valuecell = row.createCell(cellnumber++);
                   Double value=profile.getValue(sequencename,j);
                   if (value!=null) valuecell.setCellValue(value); 
              }
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%100==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        if (useColors) {
           // apply conditional formatting with colorscale
            XSSFSheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
            XSSFConditionalFormattingRule rule = sheetCF.createConditionalFormattingColorScaleRule();
            XSSFColorScaleFormatting colorScale = rule.getColorScaleFormatting();
            colorScale.setNumControlPoints(3);
            
            VisualizationSettings visualizationSettings=task.getMotifLabClient().getVisualizationSettings();
            XSSFColor minColor = new XSSFColor(visualizationSettings.getExpressionColorDownregulated(),new DefaultIndexedColorMap());
            XSSFColor midColor = new XSSFColor(visualizationSettings.getExpressionColorBackground(),new DefaultIndexedColorMap());
            XSSFColor maxColor = new XSSFColor(visualizationSettings.getExpressionColorUpregulated(),new DefaultIndexedColorMap());
            colorScale.setColors(new XSSFColor[]{minColor,midColor,maxColor});
            double[] valuesRange=profile.getValuesRange();
            double minvalue=(valuesRange!=null)?valuesRange[0]:-3.0;
            double maxvalue=(valuesRange!=null)?valuesRange[1]:3.0;
            ConditionalFormattingThreshold[] thresholds=colorScale.getThresholds();
            thresholds[0].setRangeType(ConditionalFormattingThreshold.RangeType.NUMBER);
            thresholds[0].setValue(minvalue);           
            thresholds[1].setRangeType(ConditionalFormattingThreshold.RangeType.NUMBER);
            thresholds[1].setValue(0.0);
            thresholds[2].setRangeType(ConditionalFormattingThreshold.RangeType.NUMBER);
            thresholds[2].setValue(maxvalue); 
            colorScale.setThresholds(thresholds);
            CellRangeAddress[] regions = { new CellRangeAddress((header)?1:0,rownum-1,1,profile.getNumberOfConditions()) }; 
            sheetCF.addConditionalFormatting(regions, rule);                       
        }
        sheet.autoSizeColumn((short)0);
        // now write to the outputobject. The binary Excel file is included as a dependency in the otherwise empty OutputData object.
        File excelFile=outputobject.createDependentBinaryFile(engine,getSuffix());        
        try {
            BufferedOutputStream stream=new BufferedOutputStream(new FileOutputStream(excelFile));
            workbook.write(stream);
            stream.close();
        } catch (Exception e) {
            throw new ExecutionError("An error occurred when creating the Excel file: "+e.toString(),0);
        }
        outputobject.setBinary(true);        
        outputobject.setDirty(true); // this is not set automatically since I don't append to the document
        outputobject.setDataFormat(name); // this is not set automatically since I don't append to the document
        return outputobject;
    }

    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Inappropriate use of parseInput(ArrayList<String>...) method in ExcelProfile format");
    }

    @Override
    public Data parseInput(InputStream input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_ExcelProfile.parseInput(ArrayList<String> input, Data target)");
        else if (!(target instanceof ExpressionProfile)) throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using DataFormat_ExcelProfile");
        ((ExpressionProfile)target).clear();
        ExpressionProfile profile=((ExpressionProfile)target);
        profile.clear();
        Integer keycolumn;
        String valuecolumnString;
        boolean header=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             keycolumn=(Integer)settings.getResolvedParameter("Key column",defaults,engine);
             valuecolumnString=(String)settings.getResolvedParameter("Value columns",defaults,engine);
             header=(Boolean)settings.getResolvedParameter("Header",defaults,engine);               
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           keycolumn=(Integer)getDefaultValueForParameter("Key column");
           valuecolumnString=(String)getDefaultValueForParameter("Value columns");
           header=(Boolean)getDefaultValueForParameter("header");
        }
        keycolumn=keycolumn-1; // offset to 0-indexing
        ArrayList<Integer> valuecolumns=parseValueColumns(valuecolumnString);
        ArrayList<String> eligible=engine.getNamesForAllDataItemsOfType(Sequence.class); 
        int found=0; // count number of recognized entries. If we find none there could be an error in the settings
        
        // now read and parse the input. The following can parse both old (Office 97-2003: xls) and new (Office 2007: xlsx) formats        
        try {                    
            Workbook workbook = WorkbookFactory.create(input);
            Sheet sheet = workbook.getSheetAt(0);   // just use the first sheet         
            Iterator rows = sheet.rowIterator();  
            if (header && rows.hasNext()) {
               int condition=0;
               Row headerRow = (Row) rows.next(); 
               if (valuecolumns!=null) {
                   for (Integer column:valuecolumns) {
                       Cell headercell=headerRow.getCell(column);
                       if (headercell!=null) {
                           String headervalue=headercell.getStringCellValue();
                           profile.addCondition();
                           //System.err.println("Explicit column["+(column+1)+"]. New condition["+condition+"] => "+headervalue);                           
                           profile.setHeader(condition++, headervalue);                        
                       }                       
                   }
               } else { // use all columns after key column as values
                   int last=headerRow.getLastCellNum();
                   for (int j=keycolumn+1;j<=last;j++) {
                       Cell headercell=headerRow.getCell(j);
                       if (headercell!=null) {
                           String headervalue=headercell.getStringCellValue();
                           profile.addCondition();
                           //System.err.println("Implicit column["+(j+1)+"]. New condition["+condition+"] => "+headervalue);                                                      
                           profile.setHeader(condition++, headervalue);
                       }                       
                   }
               }
            } // end of parse header
            while(rows.hasNext()) {  
               Row row = (Row) rows.next();
               Cell keycell = (Cell)row.getCell(keycolumn);
               if (keycell==null) continue;
               String key=keycell.getStringCellValue();
               key=convertIllegalSequenceNamesIfNecessary(key, false);
               String error=engine.checkSequenceNameValidity(key, false);
               if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+key+"' : "+error);                                             
               if (!eligible.contains(key)) continue;  
               profile.addSequence(key);
               int condition=0;
               if (valuecolumns!=null) {
                   for (Integer column:valuecolumns) {
                       Cell valuecell=row.getCell(column);                       
                       if (valuecell!=null) {
                            if (profile.getNumberOfConditions()<=condition) {
                                //System.err.println("Adding new condition (explicit column)");                                            
                                profile.addCondition();
                            }
                            try {
                                double value=valuecell.getNumericCellValue();
                                //System.err.println("Explicit column["+(column+1)+"]. Condition["+condition+"] => "+value);
                                profile.setValue(key,condition++,value);      
                                found++;
                            } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric value for \""+key+"\". Found \""+valuecell.getStringCellValue()+"\"");}                            
                       }                       
                   }
               } else { // use all columns after key column as values
                   int last=row.getLastCellNum();
                   for (int j=keycolumn+1;j<=last;j++) {
                       Cell valuecell=row.getCell(j);
                       if (valuecell!=null) {
                            if (profile.getNumberOfConditions()<=condition) {   
                                profile.addCondition();
                                //System.err.println("Adding new condition (implicit column). Now got "+profile.getNumberOfConditions());                                
                            }
                            try {
                                double value=valuecell.getNumericCellValue();
                                //System.err.println("Implicit column["+(j+1)+"]. Condition["+condition+"] => "+value);                                
                                profile.setValue(key,condition++,value);      
                                found++;
                            } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric value for \""+key+"\". Found \""+valuecell.getStringCellValue()+"\"");}                            
                       }                     
                   }
               }                
            }
        } catch (Exception e) {
             //e.printStackTrace(System.err);
             String message=((e instanceof ExecutionError)?"":(e.getClass().getSimpleName()+":"))+e.getMessage();             
             throw new ParseError(message);
        } finally {try {if (input!=null) input.close();} catch (Exception x){}}  
        
        if (found==0) throw new ParseError("No sequences were recognized in this file. Perhaps the settings are wrong.");
        //profile.debug();
        return profile;      
    }
    
   
    /** Parses the "value columns" parameter. The indices returned are zero-indexes (original index minus one) */
    private ArrayList<Integer> parseValueColumns(String parameter) throws ParseError {
        if (parameter==null || parameter.trim().isEmpty()) return null;
        ArrayList<Integer> list=new ArrayList<Integer>();
        String[] parts=parameter.trim().split("\\s*,\\s*");
        for (String part:parts) {
            if (part.indexOf('-')>=0) { // range
               String[] range=part.split("-");
               if (range.length==1) throw new ParseError("Column indices must be greater or equal to 1: "+part);
               int start=0;
               int end=0;
               try {start=Integer.parseInt(range[0]);} catch (NumberFormatException e1) {throw new ParseError("Unable to parse expected integer number: "+range[0]);}
               try {end=Integer.parseInt(range[1]);} catch (NumberFormatException e1) {throw new ParseError("Unable to parse expected integer number: "+range[1]);}
               if (start<=0 || end<=0) throw new ParseError("Column indices must be greater or equal to 1: "+part);
               if (start>end) throw new ParseError("End index must be greater than start index: "+part);
               for (int i=start;i<=end;i++) {
                   list.add(new Integer(i-1));     
               }
            } else {
                try {
                  int column=Integer.parseInt(part);
                  if (column<=0) throw new ParseError("Column indices must be greater or equal to 1");
                  list.add(new Integer(column-1));                  
                } catch (NumberFormatException e) {throw new ParseError("Unable to parse expected integer number: "+part);}
            }
        }
        return list;
    }
    

}
