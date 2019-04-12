/*
 
 
 */

package motiflab.engine.data.classifier;

/**
 *
 * @author kjetikl
 */
public interface ClassifierOutputListener {

  public void round(ClassifierOutput info);

  public void finished(ClassifierOutput info);

  public void start(ClassifierOutput info);

}
