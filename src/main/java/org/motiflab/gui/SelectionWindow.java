/*
 
 
 */

package org.motiflab.gui;

import java.io.Serializable;

/**
 *
 * @author kjetikl
 */
public class SelectionWindow implements Serializable {
    public String sequenceName=null;
    public int start=0;
    public int end=0;
    
    public SelectionWindow(String sequenceName,int start,int end) {
        this.sequenceName=sequenceName;
        this.start=start;
        this.end=end;
    }  
    
    @Override
    public String toString() {
        return sequenceName+":"+start+"-"+end;
    }
    
    private static final long serialVersionUID = -6303117703532124440L;    
}
