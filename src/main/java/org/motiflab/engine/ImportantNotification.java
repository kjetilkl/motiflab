/*
 * 
 */
package org.motiflab.engine;

import java.util.Date;

/**
 * This class is used to encapsulate important notifications that are published on the MotifLab web site
 * and should also be reported to the user through MotifLab's GUI, such as information about critical
 * bugs that have been detected or important updates and releases.
 * @author kjetikl
 */
public class ImportantNotification {
    public static final int MINOR=1;
    public static final int LOW=2;
    public static final int REGULAR=3;
    public static final int IMPORTANT=4;
    public static final int CRITICAL=5;
    
    private static final String[] levelStrings=new String[]{"Minor","Low","Normal","Important","Critical"};
    
    private String message;
    private Date datestamp;
    private int level=MINOR; // importance level. See above for definitions
    private boolean HTML=false;
    private int messageIndex=0;
    
    
    public ImportantNotification(int index, String message, Date date, int level) {
        this.messageIndex=index;
        this.message=message;
        this.datestamp=date;
        if (level<MINOR) level=MINOR;
        if (level>CRITICAL) level=CRITICAL;        
        this.level=level;
    }

     public ImportantNotification(int index) {
        this.messageIndex=index;
        this.message="";
        this.datestamp=new Date();
    }   
    
    public Date getDate() {
        return datestamp;
    }

    public void setDate(Date datestamp) {
        this.datestamp = datestamp;
    }

    public int getLevel() {
        return level;
    }
    
    public int getMessageIndex() {
        return messageIndex;
    }    

    public void setLevel(int level) {
        if (level<MINOR) level=MINOR;
        if (level>CRITICAL) level=CRITICAL;
        this.level = level;
    }

    /** Returns the raw message text. Note that this can contain HTML or be encoded */
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    /** Returns TRUE if this message is at a level at least as high as the target level */
    public boolean isAtLevel(int target) {
        return (level>=target);
    }
    
    public static int getLevelFromString(String stringlevel) {
        if (stringlevel==null || stringlevel.isEmpty()) return MINOR;
        if (stringlevel.equalsIgnoreCase("MINOR")) return MINOR;
        else if (stringlevel.equalsIgnoreCase("LOW")) return LOW;
        else if (stringlevel.equalsIgnoreCase("REGULAR") || stringlevel.equalsIgnoreCase("NORMAL")) return REGULAR;
        else if (stringlevel.equalsIgnoreCase("IMPORTANT")) return IMPORTANT;
        else if (stringlevel.equalsIgnoreCase("CRITICAL")) return CRITICAL;
        else {
            try {
                int levelvalue=Integer.parseInt(stringlevel);
                if (levelvalue<MINOR) levelvalue=MINOR;
                if (levelvalue>CRITICAL) levelvalue=CRITICAL;   
                return levelvalue;
            } catch (NumberFormatException e) {}
        }
        return REGULAR; //
    }
    
    public String getLevelAsString() {
        return levelStrings[level-1]; // level starts at 1 but levelStrings at 0
    }
    
    public static String[] getMessageLevels() {
        return (String[])levelStrings.clone();
    }
    
    public java.awt.Color getLevelAsColor() {
        switch (level) {
            case MINOR: return java.awt.Color.gray;
            case LOW: return java.awt.Color.gray;
            case REGULAR: return java.awt.Color.BLACK;
            case IMPORTANT: return new java.awt.Color(255,150,130);
            case CRITICAL: return java.awt.Color.RED;
            default: return java.awt.Color.BLACK;
        }        
    }
}
