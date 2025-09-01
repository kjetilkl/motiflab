/*
 
 
 */

package org.motiflab.engine.data.classifier;

import java.util.HashMap;

/**
 *
 * @author kjetikl
 */
public class ClassifierOutput extends HashMap<String, Object> {

public static String STDOUT = "_TEXT";
public static String STATUSBAR = "_STATUS";
public static String PROGRESS = "_PROGRESS";
  /** Creates new ClassifierOutput */

  public ClassifierOutput() {
    super();
  }

  public ClassifierOutput(String text) {
    super();
    this.put(STDOUT,text);
  }

  public ClassifierOutput(HashMap<String, Object> map) {
    super();
    this.putAll(map);
  }

  public void setStatusBarInformation(String text) {
      this.put(STATUSBAR,text);
  }
  public String getStatusBarInformation() {
      return (String)this.get(STATUSBAR);
  }

  public void setOutputText(String text) {
      this.put(STDOUT,text);
  }
  public String getOutputText() {
      return (String)this.get(STDOUT);
  }

  public void set(String key, Object value) {
      this.put(key,value);
  }

  public void setProgress(int i) {
      this.put(PROGRESS,new Integer(i));
  }
  public Integer getProgress() {
      return (Integer)this.get(PROGRESS);
  }

  @Override
  public String toString() {return (String)this.get(STDOUT);}

  public void debug() {
      System.err.println("OutputInformation debug dump\n----------------------");
      for (Object o : this.keySet()) {
          String key=(String) o;
          System.err.println("  "+key+" => "+this.get(key).toString());
      }

  }
}