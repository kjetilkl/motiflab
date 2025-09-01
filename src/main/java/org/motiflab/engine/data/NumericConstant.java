/*
 
 
 */

package org.motiflab.engine.data;

/**
 *
 * @author kjetikl
 */
public class NumericConstant extends ConstantData {
    private String name; 
    private Double value;
    private static String typedescription="Numeric constant";
    
    public NumericConstant(String name, double value) {
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
    
    @Override
    public void importData(Data source) throws ClassCastException {
        NumericConstant datasource=(NumericConstant)source;
        this.name=datasource.name;
        this.value=datasource.value;       
        //notifyListenersOfDataUpdate(); 
    }
    
    @Override
    public String getTypeDescription() {return typedescription;}
    

    public static String getType() {return typedescription;}
  
    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public boolean containsSameData(Data other) {
        if (other==null || !(other instanceof NumericConstant)) return false;
        NumericConstant othernumber=(NumericConstant)other;
        return value.equals(othernumber.value);
    }

    @Override
    public NumericConstant clone() {      
        NumericConstant newdata=new NumericConstant(name, value);
        return newdata;
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
