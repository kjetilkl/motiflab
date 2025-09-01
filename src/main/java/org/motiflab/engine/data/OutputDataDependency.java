/*
 * The OutputDataDependency class represents resources that are linked to OutputData, 
 * such as images within HTML-documents
 */
package org.motiflab.engine.data;

import java.io.File;
import java.io.Serializable;

/**
 *
 * @author kjetikl
 */
public class OutputDataDependency implements Serializable {
    private String internalPathname=null; // the name of a temp-file for this dependency
    private String sharedID=null; // an indentifier used if this dependcy should be shared
    private boolean isProcessed=false; // used by shared instances to indicated that the dependency has already been processed during (de)serialization
    private boolean isBinary=false; // should be set to TRUE if this dependency is in some unknown binary format instead of regular text
    
    public OutputDataDependency(String pathname) {
        this.internalPathname=pathname;
    }
    public OutputDataDependency(String pathname, String sharedID) {
        this.internalPathname=pathname;
        this.sharedID=sharedID;
    }
    public OutputDataDependency(String pathname, String sharedID, boolean binary) {
        this.internalPathname=pathname;
        this.sharedID=sharedID;
        this.isBinary=binary;
    }    
    
    
    public String getInternalPathName() {
        return internalPathname;
    }
    
    public File getFile() {
        return new File(internalPathname);
    }    
    
    public String getSharedID() {
        return sharedID;
    }   
    
    public boolean isShared() {
        return sharedID!=null;
    }
    
    /** Sets a shared identifier for this object if it does not already have one 
     *  @return TRUE if the object did not have a sharedID before (but has one now) or FALSE if the object already had a sharedID
     */
    public boolean setSharedID(String identifier) {
        if (sharedID!=null) return false;
        sharedID=identifier;
        return true;
    }  
    
    
    public boolean hasBeenProcessed() {
        return isProcessed;
    }
    
    public void setHasBeenProcessed(boolean status) {
        isProcessed=status;
    }
    
    public boolean isBinary() {
        return isBinary;
    }
    
    public void setBinary(boolean binary) {
        isBinary=binary;
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final OutputDataDependency other = (OutputDataDependency) obj;
        if ((this.internalPathname == null) ? (other.internalPathname != null) : !this.internalPathname.equals(other.internalPathname)) {
            return false;
        }
        if ((this.sharedID == null) ? (other.sharedID != null) : !this.sharedID.equals(other.sharedID)) {
            return false;
        } 
        if (this.isBinary!=other.isBinary) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.internalPathname != null ? this.internalPathname.hashCode() : 0);
        return hash;
    }
    
    @Override
    public String toString() {
        return "path=\""+internalPathname+"\", sharedID=\""+sharedID+"\", binary="+isBinary;
    }

    private static final long serialVersionUID  = 6696000714195839782L; // for backwards compatibility
     
}
