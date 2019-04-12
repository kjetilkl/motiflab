/*
 
 
 */

package motiflab.engine.data;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import motiflab.engine.SystemError;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.protocol.StandardDisplaySettingsParser;

/**
 * This class contains a library of utility methods to work with motif classes 
 * based on the hierarchical classification model introduced by TRANSFAC. 
 * This hierarchy consists of 6 levels, and at each level the various alternatives are 
 * assigned different numbers. A particular motif class can thus be written as
 * a string of (up to) six numbers separated by dots: XXX.XXX.XXX.XXX.XXX.XXX
 * 
 * The levels are:
 * 
 *   1 = Superclass (top level)
 *   2 = Class
 *   3 = Family
 *   4 = Subfamily
 *   5 = Genus (TF)
 *   6 = Factor species
 * 
 * For example, the class "4.3.1.1" denotes the subfamily of "P53" factors.
 * 
 * This library has methods to parse class strings, compare classes, 
 * and convert between class strings and their definitions.
 * 
 * Class definitions can be read from file with the method initializeFromFile().
 * The file "motiflab/engine/resources/TFclass.config" contains class definitions up to four levels.
 * 
 * 
 * @author kjetikl
 */
public class MotifClassification {
    
private static String[][] classes = new String[0][0]; // contains class strings and names    
private static String[] classStrings=null; // this is a list of all the class strings [X.X.X+]
private static HashMap<String,String> classToNameMap=null;
private static HashMap<String,Color> classToColorMap=null;
private static DefaultComboBoxModel comboboxModel=null;
private static DefaultTreeModel treeModel=null;

public static String UNKNOWN_CLASS_LABEL="Unknown class";

    
/** Returns the class name for a given class string
 * E.g. the argument "1.2" will return "Helix-loop-helix factors (bHLH)"
 */    
public static String getNameForClass(String classString) {
    return classToNameMap.get(classString);
}
    
/** Returns the level (as integer) for the class (as string)
 *  1 = Superclass (top level)
 *  2 = Class
 *  3 = Family
 *  4 = Subfamily
 *  5 = Genus (TF)
 *  6 = Factor species
 */
public static int getClassLevel(String classString) {
    return classString.split("\\.").length;
}
    
/** Returns a list of all available classStrings*/
public static String[] getClassStrings() {
    return classStrings;
}

/** Returns a class string representing the parent level of the given string */
public static String getParentLevel(String classString) {
    int index=classString.lastIndexOf(".");
    if (index>=0) return classString.substring(0,index);
    else return null;
    
}

/** If the classString is a properly formated classString (e.g.: "2.3.0.1")
 *  This method will remove subclasses at the end so that the resulting
 *  classString contains at most the specified number of levels
 *  e.g. trimToLevel("2.4.1.5.2.0",3) will return "2.4.1"
 */
public static String trimToLevel(String classString, int level) {
    if (classString==null || classString.isEmpty()) return classString;
    String[] split=classString.split("\\.");
    String[] result=new String[split.length];
    if (result.length<=level) return classString;
    String trimmed=split[0];
    for (int i=1;i<level;i++) {
        trimmed=trimmed+"."+split[i];
    }
    return trimmed;
}

/**
 * Returns a list containing the incremental path for the given class string
 * starting from the top level. E.g. if the method is called with argument "2.1.1.12"
 * the return list will be ["2","2.1","2.1.1","2.1.1.12"]
 * @param classString
 * @return
 */
public static String[] getClassPath(String classString) {
    String[] split=classString.split("\\.");
    String[] result=new String[split.length];
    result[0]=split[0];
    for (int i=1;i<result.length;i++) {
        result[i]=result[i-1]+"."+split[i];
    }
    return result;
}

/**
 * Returns the levels for a given classString as a list of integers
 * (or null if the string could not be parsed properly)
 * E.g. if the method is called with argument "2.1.1.12"
 * the return list will be [2,1,1,12]
 * @param classString
 * @return
 */
public static int[] getClassPathAsNumbers(String classString) {
    String[] split=classString.split("\\.");
    int[] result=new int[split.length];
    try {
        for (int i=0;i<result.length;i++) {
            result[i]=Integer.parseInt(split[i]);
        }
    } catch (NumberFormatException e) {return null;}
    return result;
}

/** Returns a ComboboxModel containing the classStrings
 * The first item in the model is NULL which signifies "unknown" class
 */
public static DefaultComboBoxModel getClassComboBoxModel() {
    if (comboboxModel==null) {
           String[] combostring=new String[classes.length+1];
           for (int i=0;i<classes.length;i++) {
               combostring[i+1]=classes[i][0];
           } 
           combostring[0]=null; // this signifies the unknown class
           comboboxModel=new DefaultComboBoxModel(combostring);        
    }
    return comboboxModel;
}

/** Returns a TreeModel containing the classStrings
 * The first item in the model is NULL which signifies "unknown" class
 */
public static DefaultTreeModel getClassTreeModel() {
    if (treeModel==null) {
           DefaultMutableTreeNode root=new DefaultMutableTreeNode("root");
           treeModel=new DefaultTreeModel(root);
           root.add(new DefaultMutableTreeNode(null));
           int currentlevel=1;
           DefaultMutableTreeNode currentparent=root;
           for (String classString:classStrings) {
               DefaultMutableTreeNode newnode=new DefaultMutableTreeNode(classString);
               DefaultMutableTreeNode parent=findParentNode(root, classString);
               parent.add(newnode);
           }           
    }
    return treeModel;
}


private static DefaultMutableTreeNode findParentNode(DefaultMutableTreeNode rootNode, String classString) {
   DefaultMutableTreeNode newnode=new DefaultMutableTreeNode(classString);
   int nodelevel=getClassLevel(classString); 
   if (nodelevel==1) return rootNode;
   else {
       DefaultMutableTreeNode current=rootNode;
       String[] path=getClassPath(classString);
       for (int i=0;i<path.length-1;i++) {
           current=getChild(current,path[i]);
       }
       return current;
   }
}

private static DefaultMutableTreeNode getChild(DefaultMutableTreeNode parent, String value) {
    if (parent==null || parent.isLeaf()) return null;
    for (int i=0;i<parent.getChildCount();i++) {
       DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);  
       String childval=(String)child.getUserObject();
       if (childval!=null && childval.equals(value)) return child;
    }
    return null;
}

public static TreeNode[] getPathToClass(DefaultMutableTreeNode root, String classString) {
    String[] classpath = getClassPath(classString);
    DefaultMutableTreeNode current=root;
    for (int i=0;i<classpath.length;i++) {
        current=getChild(current, classpath[i]);
    }
    return current.getPath();
}

/** Returns true if the argument string represents a known class
 *  or false if the clasString is null or unknown
 */
public static boolean isKnownClassString(String classString) {
    if (classString==null || classString.isEmpty()) return false;
    for (String s:classStrings) {
        if (classString.equals(s)) return true;
    }
    return false;
}


/**
 * Return a full description of all levels (also parent levels)
 * for a given class string
 */
public static String getFullLevelsString(String classString) {
    if (classString==null || !isKnownClassString(classString)) return "Unknown class";
    String result="";
    String[] split=classString.split("\\.");
    if (split.length>=6) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3]+"."+split[4]+"."+split[5];
        result=levelString+"\t"+getNameForClass(levelString)+"\n"+result;
    }
    if (split.length>=5) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3]+"."+split[4];
        result=levelString+"\t"+getNameForClass(levelString)+"\n"+result;
    }    
    if (split.length>=4) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3];
        result=levelString+"\t"+getNameForClass(levelString)+"\n"+result;
    }
    if (split.length>=3) {
        String levelString=split[0]+"."+split[1]+"."+split[2];
        result=levelString+"\t"+getNameForClass(levelString)+"\n"+result;
    }
    if (split.length>=2) {
        String levelString=split[0]+"."+split[1];
        result=levelString+"\t"+getNameForClass(levelString)+"\n"+result;
    }
    result=split[0]+"\t"+getNameForClass(split[0])+"\n"+result;
    return result.trim();
}
/**
 * Return a full description of all levels (also parent levels)
 * for a  given class string
 */
public static String getFullLevelsStringAsHTML(String classString) {
    if (classString==null || !isKnownClassString(classString)) return "Unknown class";
    String result="";
    String[] split=classString.split("\\.");
    if (split.length>=6) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3]+"."+split[3]+"."+split[5];
        result=levelString+" &rarr; "+getNameForClass(levelString)+"<br>"+result;
    }    
    if (split.length>=5) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3]+"."+split[4];
        result=levelString+" &rarr; "+getNameForClass(levelString)+"<br>"+result;
    }    
    if (split.length>=4) {
        String levelString=split[0]+"."+split[1]+"."+split[2]+"."+split[3];
        result=levelString+" &rarr; "+getNameForClass(levelString)+"<br>"+result;
    }
    if (split.length>=3) {
        String levelString=split[0]+"."+split[1]+"."+split[2];
        result=levelString+" &rarr; "+getNameForClass(levelString)+"<br>"+result;
    }
    if (split.length>=2) {
        String levelString=split[0]+"."+split[1];
        result=levelString+" &rarr; "+getNameForClass(levelString)+"<br>"+result;
    }
    result=split[0]+" &rarr; "+getNameForClass(split[0])+"<br>"+result;
    return "<html>"+result+"</html>";
}


/**
 * Returns a list containing level strings for all sublevels (at all levels) of the given classString (including the string itself)
 * In other words, it will return all those levels whose level string start with the given classString
 */
public static ArrayList<String> getAllSubLevels(String classString) {
    ArrayList<String> result=new ArrayList<String>();
    for (int i=0;i<classes.length;i++) {
        if (classes[i][0].startsWith(classString)) result.add(classes[i][0]);
    }
    return result;
}

/**
 * Returns a list containing level strings for all sublevels (at all levels) of the given classString (including the string itself)
 * In other words, it will return all those levels whose level string start with the given classString
 */
public static ArrayList<String> getAllSuperLevels(String classString) {
    ArrayList<String> result=new ArrayList<String>();
    for (int i=0;i<classes.length;i++) {
        if (classString.startsWith(classes[i][0])) result.add(classes[i][0]);
    }
    return result;
}

/**
 * Compares two classStrings and returns 0 if they are equal, -1 if classString1 should be sorted
 * before classString2 or +1 if classString2 should be sorted before classString1
 * @param classString1
 * @param classString2
 */
public static int compareClassStrings(String classString1, String classString2) {
    if (classString1==null && classString2==null) return 0;
    else if (classString1==null && classString2!=null) return 1;
    else if (classString1!=null && classString2==null) return -1;
    if (classString1.equals(classString2)) return 0;
    int[] path1=getClassPathAsNumbers(classString1);
    int[] path2=getClassPathAsNumbers(classString2);
    if (path1==null && path2==null) return 0;
    else if (path1==null && path2!=null) return 1;
    else if (path1!=null && path2==null) return -1;
    int levels=(path1.length<path2.length)?path1.length:path2.length;
    // they should have at least 1 level each at this point
    if (path1[0]==0 && path2[0]!=0) return 1;  // sort top-level 0 at the bottom
    if (path1[0]!=0 && path2[0]==0) return -1; // sort top-level 0 at the bottom
    for (int i=0;i<levels;i++) {
        if (path1[i]==path2[i]) continue;
        if (path1[i]<path2[i]) return -1; else return 1;
    }
    // similar levels up to length of shortest
         if (path1.length==path2.length) return 0;
    else if (path1.length<path2.length) return -1;
    else return 1;
    
}

    /** This method can be called to initialize the MotifClassification
     *  to override the static defaults 
     */
    public static void initializeFromFile(File file) throws IOException, SystemError {       
        initializeFromStream(new FileInputStream(file));
    }    
    
    public static void initializeFromStream(InputStream input) throws IOException, SystemError {
        ArrayList<Object[]> entries=new ArrayList<Object[]>();
        BufferedReader inputStream=null;
        try {
            inputStream=new BufferedReader(new InputStreamReader(input));
            String line;
            while((line=inputStream.readLine())!=null) {
                if (line.startsWith("#") || line.trim().isEmpty()) continue; // comments and empty lines
                String[] fields=line.split("\\t");
                if (fields.length<2 || fields.length>3) throw new SystemError("Expected 2 or 3 fields per line in MotifClassification definition file, but got "+fields.length+":\n"+line);
                String classString=fields[0];
                String className=fields[1];
                String colorString=(fields.length==3)?fields[2]:null;
                Color color=null;
                if (colorString!=null && !colorString.isEmpty()) {
                    try {
                      color=StandardDisplaySettingsParser.parseColor(colorString);       
                    } catch (ParseError e) {throw new SystemError(e.getMessage());}
                }
                if (!classString.matches("\\d+(\\.\\d+(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?(\\.\\d+)?)?")) throw new SystemError("Motif class string in wrong format: "+fields[0]);
                Object[] mapping=new Object[]{classString,className, color};
                entries.add(mapping);
            }
            inputStream.close();
        } catch (IOException e) {
            throw e;
        } finally {
            try {if (inputStream!=null) inputStream.close();} catch (IOException ioe) {}
        }
        ArrayList<String[]> classesArray=new ArrayList<String[]>(entries.size());
        Color[] classColors=new Color[entries.size()];        
        for (int i=0;i<entries.size();i++) {
            Object[] entry=entries.get(i);
            classesArray.add(new String[]{(String)entry[0],(String)entry[1]});
            classColors[i]=(Color)entry[2];
        }         
        
        classToNameMap=new HashMap<String,String>(classesArray.size());
        classToColorMap=new HashMap<String,Color>(classesArray.size());
        for (int i=0;i<classesArray.size();i++) {
           String[] cl=classesArray.get(i);
           if (classToNameMap.containsKey(cl[0])) throw new SystemError("SYSTEM ERROR: Duplicate definition for motif class "+cl[0]);
           classToNameMap.put(cl[0], cl[1]);           
           if (classColors[i]!=null) classToColorMap.put(cl[0], classColors[i]);           
        }  
        // check if any nodes are missing parents and add them if necessary
        HashSet<String> keys=new HashSet<String>(classToNameMap.keySet());
        for (String entry:keys) {
            String parent=getParentLevel(entry);
            if (parent!=null && !classToNameMap.containsKey(parent)) {
                // first find the closest parent present
                String closestParent=parent;
                while (!classToNameMap.containsKey(closestParent) && getClassLevel(closestParent)>1) {
                    closestParent=getParentLevel(closestParent);
                }
                String parentClassName=classToNameMap.get(closestParent);               
                if (parentClassName==null) {parentClassName="Missing name";classToNameMap.put(closestParent,parentClassName);}
                // fill inn all levels inbetween
                while (parent!=null && !classToNameMap.containsKey(parent)) {
                    classToNameMap.put(parent,parentClassName);
                    classesArray.add(new String[]{parent,parentClassName});
                    parent=getParentLevel(parent);
                }
            }
        }       
        Collections.sort(classesArray,new sortOrderComparator());
        
        classStrings=new String[classesArray.size()];
        classes=new String[classesArray.size()][2];
        for (int i=0;i<classesArray.size();i++) {
            String[] entry=classesArray.get(i);
            classStrings[i]=entry[0];
            classes[i][0]=entry[0];
            classes[i][1]=entry[1];
        }          
    }
    

    
    
    public static Color getColorForMotifClass(String classname) {
         if (classname==null || classToColorMap==null || classname.equals(MotifClassification.UNKNOWN_CLASS_LABEL)) return Color.BLACK;
         Color color=classToColorMap.get(classname);
         return (color!=null)?color:Color.BLACK;    
    }   
    
    private static class sortOrderComparator implements Comparator<String[]> {
        @Override
        public int compare(String[] o1, String o2[]) {
            if (o1==null && o2==null) return 0;
            if (o1!=null && o2==null) return -1;
            if (o1==null && o2!=null) return 1;
            int[] path1=getClassPathAsNumbers(o1[0]);
            int[] path2=getClassPathAsNumbers(o2[0]);
            int length=(path1.length<path2.length)?path1.length:path2.length;
            for (int i=0;i<length;i++) {
                     if (path1[i]<path2[i]) return -1;
                else if (path1[i]>path2[i]) return 1;
            }
            // all common entries are the same. Does one have additional sublevels?
            return path1.length-path2.length;
        }
        
        
    }

}
