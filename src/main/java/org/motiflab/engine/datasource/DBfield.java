
package org.motiflab.engine.datasource;

import org.motiflab.engine.data.Region;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.protocol.ParseError;
import org.w3c.dom.Element;

/**
 * This class represents information about a single Region property (e.g. chromosome, start, end)
 * and how it should be obtained from a database (the corresponding field name)
 * and perhaps transformed?
 * @author kjetikl
 */
public class DBfield implements Cloneable {

    String regionPropertyName=null; // the name of the region property (e.g. "chromosome", "start", "end", "type", "score" etc.
    Class fieldClass=String.class; // this should be either String, Integer, Double or Boolean (these are the supported types)
    String dbFieldName=null; // the name of the corresponding field in the SQL database    
    Object explicitValue=null; // some properties (e.g. "type", "score", "strand" etc.) can be assigned explicit values
                               // that will be used instead of reading these from the database (note: this will only be used if dbFieldName==null)
    Object parameter=null; // this parameter is used to store extra information which can affect how the value should be transformed/processed
                           // the function of this parameter depends on the corresponding property (name or type)
                           // e.g. if the property is "start" or "end" the parameter should be an Integer offset that will be added to the value obtained from the DB (e.g. if the coordinates are in BED-format in the DB, the "start" coordinate should have an offset of +1 to convert it to GFF)
                           // if the property is a text property it can be a String[i][j] map where each element i corresponds to a String[] key-value pair where the key[0] is the "property value" used for the region in MotifLab and the value[1] is the corresponding value in the database
                           // if the property is "chr" the parameter could be a boolean telling whether or not a "chr" prefix should be added to the chromosome value
    
    public DBfield(String property,String dbfield, Class type) {
        this.regionPropertyName=property;
        this.dbFieldName=dbfield;
        this.fieldClass=type;
    }
    
    public DBfield(String property, String dbfield, Class type, Object explicitValue, Object transformparameter) {
        this.regionPropertyName=property;
        this.dbFieldName=dbfield;
        this.fieldClass=type;
        this.explicitValue=explicitValue;
        this.parameter=transformparameter;
    }    
       
    @Override
    protected DBfield clone() {
        Object parameterClone=null;
        if (parameter instanceof String[][]) parameterClone=cloneStringArray((String[][])parameter);
        else parameterClone=parameter;
        return new DBfield(regionPropertyName, dbFieldName, fieldClass, explicitValue, parameterClone);
    }

    private String[][] cloneStringArray(String[][] map) {
        if (map==null || map.length==0) return new String[0][2];
        String[][] mapcopy=new String[map.length][2];
        for (int i=0;i<map.length;i++) {
            System.arraycopy(map[i], 0, mapcopy[i], 0, map[i].length);
        }
        return mapcopy;
    }    
    
    
    public String getPropertyName() {
        return regionPropertyName;
    }
    
    public String getDBfieldName() {
        return dbFieldName;
    }    
    
    public Class getFieldType() {
        return fieldClass;
    }
    
    public Object getTransformParameter() {
        return parameter;
    }       
    
    public void setExplicitValue(Object value) throws ParseError {
        if (value==null) throw new ParseError("Explicit value can not be NULL");
        dbFieldName=null; // clear this
        if (fieldClass==Integer.class) {
            if (value instanceof Integer) explicitValue=value;
            else if (value instanceof Double) explicitValue=new Integer(((Double)value).intValue());
            else {
                try {
                    explicitValue=new Integer(Integer.parseInt(value.toString()));
                } catch (NumberFormatException ne) {
                    throw new ParseError("Unable to parse expected integer: "+value.toString());
                }
            }
        }
        else if (fieldClass==Double.class) {
            if (value instanceof Double) explicitValue=value;
            else if (value instanceof Integer) explicitValue=new Double(((Integer)value).doubleValue());
            else {
                try {
                    explicitValue=new Double(Double.parseDouble(value.toString()));
                } catch (NumberFormatException ne) {
                    throw new ParseError("Unable to parse expected number: "+value.toString());
                }
            }
        }
        else if (fieldClass==Boolean.class) {
            if (value instanceof Boolean) explicitValue=value;
            else {
                String valueString=value.toString();
                if (valueString.equalsIgnoreCase("yes") || valueString.equalsIgnoreCase("on")) explicitValue=Boolean.TRUE;
                else if (valueString.equalsIgnoreCase("no") || valueString.equalsIgnoreCase("off")) explicitValue=Boolean.FALSE;
                else explicitValue=Boolean.parseBoolean(value.toString());
            }
        }
        else explicitValue=value.toString();
    }
    
    
    public Object getExplicitValue() {
        return explicitValue;
    }  
    
    public Object getDefaultValue() {
        if (fieldClass==Integer.class) return new Integer(0);
        else if (fieldClass==Double.class) return new Double(0);
        else if (fieldClass==Boolean.class) return Boolean.FALSE;
        else return "";        
    }
    
    public boolean hasExplicitValue() {
        return (dbFieldName==null || dbFieldName.isEmpty());
    }
       
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element field=document.createElement("Field");
        field.setAttribute("property", regionPropertyName);
        if (hasExplicitValue()) {
            Object value=getExplicitValue().toString();
            if (value==null) value=getDefaultValue().toString();
            field.setAttribute("value", value.toString());
        }
        else field.setAttribute("DBfield", dbFieldName);
        String classString="text";
             if (fieldClass==Double.class) classString="double";
        else if (fieldClass==Integer.class) classString="integer";
        else if (fieldClass==Boolean.class) classString="boolean";
        field.setAttribute("type", classString); 
        if (parameter!=null) {
            if (regionPropertyName.equals("chromosome") && parameter instanceof Boolean) { 
                field.setAttribute("addChrPrefix", ((Boolean)parameter).toString());          
            }
            if (regionPropertyName.equals("start") || regionPropertyName.equals("end")) {
                if (parameter instanceof Integer) {
                    field.setAttribute("offset", ((Integer)parameter).toString());
                }
            } 
//            if (regionPropertyName.equals("strand") || regionPropertyName.equals("orientation")) {
//                if (parameter instanceof String[]) {
//                    if (((String[])parameter)[0]!=null) field.setAttribute("direct", ((String[])parameter)[0]);
//                    if (((String[])parameter)[1]!=null) field.setAttribute("reverse",((String[])parameter)[1]);                
//                }
//            }
            if (parameter instanceof String[][]) {
                String transformMap=getMapAsKeyValuePairs((String[][])parameter);
                field.setAttribute("transformMap",transformMap);
            }
        }
        return field;
    }     
    
    public static String getMapAsKeyValuePairs(String[][] map) {
        StringBuilder builder=new StringBuilder();
        boolean first=true;
        for (String[] pair: map) {
            if (!first) builder.append(",");             
            builder.append(pair[0]);
            builder.append("=");
            builder.append(pair[1]);  
            first=false;
        }
        return builder.toString();
    }
    
    public static String[][] parseTransformMap(String keyValuePairs) {
        String[] parts=keyValuePairs.split("\\s*,\\s*");
        String[][] transformMap=new String[parts.length][2];
        for (int i=0;i<parts.length;i++) {
           String part=parts[i];
           String[] keyvalue=part.split("\\s*=\\s*");
           if (keyvalue!=null && keyvalue.length==2) {
               transformMap[i]=keyvalue;
           } else transformMap[i]=new String[]{part,part}; // invalid syntax. Do not transform, just set key=value
        }
        return transformMap;
    }    
    
    public static DBfield initializeFieldFromXML(Element element) throws ParseError {         
         String regionPropertyName=element.getAttribute("property");
         String dbFieldName=element.getAttribute("DBfield");
         String explicitValue=element.getAttribute("value");
         String type=element.getAttribute("type");
         Class typeclass=String.class;
         Object parameter=null;
         if (regionPropertyName==null || regionPropertyName.isEmpty()) throw new ParseError("Missing 'property' attribute for Field element");
         if (dbFieldName!=null && dbFieldName.isEmpty()) dbFieldName=null; // allow value to be set explicitly
         if (dbFieldName==null && (regionPropertyName.equals("start") || regionPropertyName.equals("end") || regionPropertyName.equals("chromosome") || regionPropertyName.equals("chr"))) throw new ParseError("Missing required 'DBfield' attribute for property: "+regionPropertyName);
         if (type==null || type.isEmpty()) throw new ParseError("Missing 'type' attribute for Field element");
              if (type.equalsIgnoreCase("integer") || type.equalsIgnoreCase("int")) typeclass=Integer.class;
         else if (type.equalsIgnoreCase("double") ) typeclass=Double.class;
         else if (type.equalsIgnoreCase("boolean")) typeclass=Boolean.class;
         else if (type.equalsIgnoreCase("text") || type.equalsIgnoreCase("string")) typeclass=String.class;
         else throw new ParseError("Unrecognized value '"+type+"' for 'type' attribute for Field element");
         DBfield newfield=new DBfield(regionPropertyName, dbFieldName, typeclass);
         if (regionPropertyName.equals("chromosome") || regionPropertyName.equals("chr")) {
            regionPropertyName="chromosome"; // always use long forme! Convert to be sure!
            String chrPrefix=element.getAttribute("addChrPrefix");
            if (chrPrefix!=null && (chrPrefix.equalsIgnoreCase("true") || chrPrefix.equalsIgnoreCase("yes"))) parameter=Boolean.TRUE;           
         }
         if (regionPropertyName.equals("start") || regionPropertyName.equals("end")) {
            String offsetString=element.getAttribute("offset");
            if (offsetString!=null && !offsetString.isEmpty()) {
                try {
                    parameter=new Integer(Integer.parseInt(offsetString));
                } catch (NumberFormatException ex) {
                    throw new ParseError("Unable to parse expected integer value for 'offset' parameter for attribute '"+regionPropertyName+"' for Field element. Got '"+offsetString+"'");
                }
            }           
         } 
         if (regionPropertyName.equals("strand") || regionPropertyName.equals("orientation")) { // check for legacy strand orientation attributes
            String directString=element.getAttribute("direct"); 
            String reverseString=element.getAttribute("reverse");         
            if (directString!=null && !directString.isEmpty() && reverseString!=null && !reverseString.isEmpty()) {
                parameter=new String[][]{
                    {"direct",directString},
                    {"reverse",reverseString}
                }; // convert to common "transformMap" format
            }
         } 
         if (element.hasAttribute("transformMap")) {
             String transformMap=element.getAttribute("transformMap");
             if (transformMap!=null && !transformMap.trim().isEmpty()) parameter=parseTransformMap(transformMap);
         }
         if (dbFieldName==null) newfield.setExplicitValue(explicitValue);
         newfield.parameter=parameter;
         return newfield;
    } 
    
    @Override
    public String toString() {
        StringBuilder builder=new StringBuilder();
        builder.append("property=");
        builder.append(regionPropertyName);
        builder.append(", type=");
        builder.append(fieldClass.toString());
        if (dbFieldName!=null) {builder.append(", DBfield=");builder.append(dbFieldName);}
        else if (explicitValue!=null) {builder.append(", explicit value=");builder.append(explicitValue);}
        if (parameter!=null) {
            builder.append(", transform-parameter=");
            if (parameter instanceof String[][]) {
                String[][] list=(String[][])parameter;
                builder.append("{");
                for (int i=0;i<list.length;i++) {
                    if (i>0) builder.append(",");
                    builder.append(list[i][0]);
                    builder.append("=");
                    builder.append(list[i][1]);                    
                }
                builder.append("}");
            }
            else builder.append(parameter.toString());
        }
        return builder.toString();
    }
    
    public Object getTransformedValue(Object original) throws ExecutionError {
        if (regionPropertyName.equalsIgnoreCase("start")) {
            if (!(original instanceof Integer)) throw new ExecutionError("'start' does not have an integer value: "+((original==null)?"null":original.toString())); 
            if (parameter instanceof Integer) return ((Integer)original)+((Integer)parameter); // use parameter as an offset
            else return original;
        } else if (regionPropertyName.equalsIgnoreCase("end")) {
            if (!(original instanceof Integer)) throw new ExecutionError("'end' does not have an integer value: "+((original==null)?"null":original.toString())); 
            if (parameter instanceof Integer) return ((Integer)original)+((Integer)parameter); // use parameter as an offset
            else return original;
        } else if (regionPropertyName.equalsIgnoreCase("strand")) { 
            String strand=(original==null)?"":original.toString();
            if (parameter instanceof String[][]) { // a mapping exists for strand
                String newstrand=getKeyForValue((String[][])parameter, strand);
                if (newstrand!=null) strand=newstrand;
            }            
            if (strand.equalsIgnoreCase("DIRECT") || strand.startsWith("+") || strand.equals("1")) return Region.DIRECT;
            else if (strand.equalsIgnoreCase("REVERSE") || strand.startsWith("-")) return Region.REVERSE;
            return Region.INDETERMINED;
        } else if (regionPropertyName.equalsIgnoreCase("chromosome") && (parameter instanceof Boolean) && ((Boolean)parameter)) { // add chr prefix?
            if (((String)original).startsWith("chr")) return original;
            else return "chr"+original.toString();
        } else if (parameter instanceof String[][] && original!=null) { //
            String transformedValue=null;
            if (regionPropertyName.equalsIgnoreCase("chromosome")) transformedValue=getValueForKey((String[][])parameter, original.toString()); // chromosomes are only transformed from "property" to "database" since it is used to generate the SQL query
            else transformedValue=getKeyForValue((String[][])parameter, original.toString()); // all other values are transformed from "database" to "property" since these transforms are used to process data returned by the SQL server
            return (transformedValue!=null)?transformedValue:original;
        } 
        else return original;
    }
    
    private String getValueForKey(String[][] map, String key) {
        if (map==null || map.length==0) return null;
        for (int i=0;i<map.length;i++) {
            if (map[i][0].equals(key)) return map[i][1];
        }
        return null;
    }
    
    private String getKeyForValue(String[][] map, String value) {
        if (map==null || map.length==0) return null;
        for (int i=0;i<map.length;i++) {
            if (map[i][1].equals(value)) return map[i][0];
        }
        return null;
    }    
}
