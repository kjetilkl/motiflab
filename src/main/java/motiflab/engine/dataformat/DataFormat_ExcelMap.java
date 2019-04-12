/*
 
   Some of the code is copied from a tutorial at:  http://mrbool.com/reading-excel-file-with-java/24562
 */

package motiflab.engine.dataformat;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.ExecutionError;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.Parameter;
import motiflab.engine.ParameterSettings;
import motiflab.engine.data.Data;
import motiflab.engine.data.DataCollection;
import motiflab.engine.data.DataMap;
import motiflab.engine.data.NumericMap;
import motiflab.engine.data.OutputData;
import motiflab.engine.data.SequenceNumericMap;
import motiflab.engine.data.TextMap;
import motiflab.engine.protocol.ParseError;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 *
 * @author kjetikl
 */
public class DataFormat_ExcelMap extends DataFormat {
    private String name="ExcelMap";
    private Class[] supportedTypes=new Class[]{DataMap.class};


    public DataFormat_ExcelMap() {
        addOptionalParameter("Key column", new Integer(1), new Integer[]{1,Integer.MAX_VALUE},"The number of the column which contains the names/identifiers of the motifs, modules or sequences");
        addOptionalParameter("Value column", new Integer(2), new Integer[]{1,Integer.MAX_VALUE},"The number of the column which contains the corresponding values");
        addOptionalParameter("Include entries",null, new Class[]{DataCollection.class},"Specifies which entries to include in the output. The default is to include all entries from the map.");
        addOptionalParameter("Include default",Boolean.TRUE, new Boolean[]{Boolean.FALSE,Boolean.TRUE},"Include an entry in the output for the default value ");
        setParameterFilter("Include entries","output");   
        //setParameterFilter("Include default","output");         
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
        return (data instanceof DataMap); 
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (DataMap.class.isAssignableFrom(dataclass));
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof DataMap);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (DataMap.class.isAssignableFrom(dataclass));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "xls";
    }

    @Override
    public String[] getSuffixAlternatives() {return new String[]{"xls","xlsx"};}
    
    
    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        Integer keycolumn;
        Integer valuecolumn;
        DataCollection includeEntries;
        boolean includeDefault=false;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             keycolumn=(Integer)settings.getResolvedParameter("Key column",defaults,engine);
             valuecolumn=(Integer)settings.getResolvedParameter("Value column",defaults,engine);
             includeEntries=(DataCollection)settings.getResolvedParameter("Include entries",defaults,engine);
             includeDefault=(Boolean)settings.getResolvedParameter("Include default",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
           keycolumn=(Integer)getDefaultValueForParameter("Key column");
           valuecolumn=(Integer)getDefaultValueForParameter("Value column");
           includeEntries=(DataCollection)getDefaultValueForParameter("Include entries");
           includeDefault=(Boolean)getDefaultValueForParameter("Include default");           
        }
        keycolumn=keycolumn-1;
        valuecolumn=valuecolumn-1;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(outputobject.getName()); 
        DataMap map=(DataMap)dataobject;        
        if (includeEntries!=null && !includeEntries.getMembersClass().equals(map.getMembersClass())) includeEntries=null;
        ArrayList<String> keys=(includeEntries==null)?map.getAllKeys(engine):includeEntries.getValues();
        Collections.sort(keys);
        int size=keys.size();
        int i=0;
        int rownum = 0;
        for (String key:keys) { // for each entry
              Row row = sheet.createRow(rownum++);
              Cell keycell = row.createCell(keycolumn);
              Cell valuecell = row.createCell(valuecolumn);
              keycell.setCellValue(key);
              if (map instanceof NumericMap)  valuecell.setCellValue((Double)map.getValue(key));
              else valuecell.setCellValue((String)map.getValue(key));
              if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
              if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
              if (i%100==0) Thread.yield();
              setProgress(i+1,size);
              i++;
        }
        if (includeDefault) {
              Row row = sheet.createRow(rownum++);
              Cell keycell = row.createCell(keycolumn);
              Cell valuecell = row.createCell(valuecolumn);
              keycell.setCellValue(DataMap.DEFAULT_KEY);
              if (map instanceof NumericMap) valuecell.setCellValue((Double)map.getValue());
              else valuecell.setCellValue((String)map.getValue());    
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
        outputobject.setDataFormat(name); // this is not set automatically since I don't append to the document
        return outputobject;
    }


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        throw new ParseError("Inappropriate use of parseInput(ArrayList<String>...) method in ExcelMap format");
    }

    @Override
    public Data parseInput(InputStream input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_ExcelMap.parseInput(ArrayList<String> input, Data target)");
        else if (!(target instanceof DataMap)) throw new ParseError("Unable to parse input to target data of type '"+target.getTypeDescription()+"' using data format "+getName());
        ((DataMap)target).clear();
        ((DataMap)target).clearDefault();
        Integer keycolumn;
        Integer valuecolumn;
        setProgress(5);
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             keycolumn=(Integer)settings.getResolvedParameter("Key column",defaults,engine);
             valuecolumn=(Integer)settings.getResolvedParameter("Value column",defaults,engine);
           } catch (Exception ex) {
              throw new ParseError("An error occurred during parsing:"+ex.getMessage());
           }
        } else {
           keycolumn=(Integer)getDefaultValueForParameter("Key column");
           valuecolumn=(Integer)getDefaultValueForParameter("Value column");
        }
        keycolumn=keycolumn-1; // offset to 0-indexing
        valuecolumn=valuecolumn-1; // offset to 0-indexing
        
        Class type=((DataMap)target).getMembersClass();
        ArrayList<String> eligible=engine.getNamesForAllDataItemsOfType(type); 
        String typename=engine.getTypeNameForDataClass(type);
        int found=0; // count number of recognized entries. If we find none there could be an error in the settings
        
        // now read and parse the input. The following can parse both old (Office 97-2003: xls) and new (Office 2007: xlsx) formats        
        try {                    
            Workbook workbook = WorkbookFactory.create(input);
            Sheet sheet = workbook.getSheetAt(0);   // just use the first sheet         
            Iterator rows = sheet.rowIterator();            
            while(rows.hasNext()) {  
                Row row = (Row) rows.next();
                Cell keycell = (Cell)row.getCell(keycolumn);
                Cell valuecell = (Cell)row.getCell(valuecolumn);
                if (keycell==null || valuecell==null) continue;
                String key=keycell.getStringCellValue();
                if (key.equals(DataMap.DEFAULT_KEY)) {
                    if (target instanceof NumericMap) {
                        try {
                            double value=valuecell.getNumericCellValue();
                            ((NumericMap)target).setDefaultValue(value);  
                        } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric value for \""+key+"\". Found \""+valuecell.getStringCellValue()+"\"");}                  
                    } else {
                        valuecell.setCellType(Cell.CELL_TYPE_STRING); // this is a hack to convert the cell contents to string and avoid exceptions                       
                        ((TextMap)target).setDefaultValue(valuecell.getStringCellValue());
                    }
                    found++;                    
                } else { // regular key
                    if (target instanceof SequenceNumericMap) {
                        key=convertIllegalSequenceNamesIfNecessary(key, false);
                        String error=engine.checkSequenceNameValidity(key, false);
                        if (error!=null) throw new ParseError("Encountered invalid name for sequence '"+key+"' : "+error);                              
                    }
                    if (eligible.contains(key)) {
                        if (target instanceof NumericMap) {
                            try {
                                double value=valuecell.getNumericCellValue();
                                ((NumericMap)target).setValue(key,value);      
                            } catch (Exception e) {throw new ExecutionError("Unable to parse expected numeric value for \""+key+"\". Found \""+valuecell.getStringCellValue()+"\"");}                                  
                        } else {
                            valuecell.setCellType(Cell.CELL_TYPE_STRING); // this is a hack to convert the cell contents to string and avoid exceptions
                            ((TextMap)target).setValue(key,valuecell.getStringCellValue());
                        }
                        found++;
                    }
                }
            }
        } catch (Exception e) {
             //e.printStackTrace(System.err);
             String message=((e instanceof ExecutionError)?"":(e.getClass().getSimpleName()+":"))+e.getMessage();             
             throw new ParseError(message);
        } finally {try {if (input!=null) input.close();} catch (Exception x){}}  
        
        if (found==0) throw new ParseError("No "+typename.toLowerCase()+"s were recognized in this file. Perhaps the settings are wrong.");
        
        return target;      
    }
    
   
    

}
