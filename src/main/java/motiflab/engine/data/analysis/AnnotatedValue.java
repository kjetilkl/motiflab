
package motiflab.engine.data.analysis;

import java.io.Serializable;

/**
 * This class represents values that might have an optional
 * annotation or 'markup' associated with them
 * @author Kjetil
 */
public class AnnotatedValue implements Comparable, Serializable {
    private Object value;
    private String markup=null;

    public AnnotatedValue(Object value, String markup) {
       this.value=value;
       this.markup=markup;
    }

    public Object getValue() {
        return value;
    }
    
    public void setValue(Object newvalue) {
        value=newvalue;
    }    
    
    public String getMarkup() {
        return markup;
    }
    
    public void setMarkup(String newmarkup) {
        this.markup=newmarkup;
    } 
    
    @Override
    public String toString() {
        if (value==null) return "null";
        else return value.toString();
    }

    @Override
    public int compareTo(Object o) {
        Object other=(o instanceof AnnotatedValue)?((AnnotatedValue)o).getValue():o;
        if (value==null && other==null) return 0;
        else if (value!=null && other==null) return -1;
        else if (value==null && other!=null) return 1;
        if (value.getClass()==other.getClass() && value instanceof Comparable) return ((Comparable)value).compareTo((Comparable)other);
        else return value.toString().compareTo(other.toString());
    }

    private static final long serialVersionUID = 1L;
}
