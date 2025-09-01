/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.motiflab.gui.prompt;

import java.util.Comparator;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.NumericMap;
import org.motiflab.engine.data.TextMap;

/**
 * @author kjetikl
 */
public class MapValueComparator implements Comparator<Object> {
    
    private NumericMap numericmap=null;
    private TextMap textmap=null;    
    
    public MapValueComparator(NumericMap map) {
        this.numericmap=map;
    }
    public MapValueComparator(TextMap map) {
        this.textmap=map;
    }    
    
    @SuppressWarnings("unchecked")
    @Override
    public int compare(Object o1, Object o2) {
        if (numericmap!=null) {
            if (o1==null) o1=new Double(numericmap.getValue());
            if (o2==null) o2=new Double(numericmap.getValue());
            return ((Comparable)o1).compareTo(o2);
        }
        if (textmap!=null) {
            if (o1==null) o1=(String)textmap.getValue();
            if (o2==null) o2=(String)textmap.getValue();
            return MotifLabEngine.compareNaturalOrder(o1.toString(), o2.toString());
        }
        return 0;
    }
}    
    
