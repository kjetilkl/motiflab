/*
 
 
 */

package motiflab.engine.data;

import java.util.ArrayList;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.protocol.ParseError;

/**
 *
 * @author kjetikl
 */
public class NumericVariable extends Data {
    private String name; 
    private Double value;
    private static String typedescription="Numeric Variable";
    
    public NumericVariable(String name, double value) {
        this.name=name;
        this.value=new Double(value);
    }
    
    @Override
    public String getName() {return name;}

    @Override
    public void rename(String newname) {
        this.name=newname;
    }     
    
    @Override
    public Double getValue() {return value;}    
    @Override
    public String getValueAsParameterString() {return ""+value.toString();}

    public void setValue(double newvalue) {
        value=new Double(newvalue);
        notifyListenersOfDataUpdate();
    }
    
    @Override
    public void importData(Data source) throws ClassCastException {
        NumericVariable datasource=(NumericVariable)source;
        this.name=datasource.name;
        this.value=datasource.value;      
        //notifyListenersOfDataUpdate(); 
    }
    
    @Override
    public NumericVariable clone() {      
        NumericVariable newdata=new NumericVariable(name, value);
        return newdata;
    }      

    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof NumericVariable)) return false;
        NumericVariable othernumber=(NumericVariable)other;
        return value.equals(othernumber.value);
    }

    @Override
    public String getTypeDescription() {return typedescription+" = "+value.toString();}
    

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String output() {
        return value.toString();
    }   
    
    @Override
    public void inputFromPlain(ArrayList<String> input, MotifLabEngine engine) throws ParseError {
        if (input==null || input.isEmpty()) throw new ParseError("PLAIN format input for '"+getName()+"' is empty");
        try {
            Double newvalue=Double.parseDouble(input.get(0));
            setValue(newvalue);
        } catch (NumberFormatException nfe) {throw new ParseError("Unable to parse expected numerical input in PLAIN format for "+getName()+": "+nfe.getMessage());}       
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
