/*
 
 
 */

package org.motiflab.engine.data;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.task.OperationTask;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.Parameter;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.SystemError;
import org.motiflab.engine.data.classifier.Classifier;
import org.motiflab.engine.data.classifier.ClassifierDataSet;
import org.motiflab.engine.data.classifier.Example;
import org.motiflab.engine.data.classifier.adaboost.AdaBoost;
import org.motiflab.engine.operations.Operation_new;
import org.motiflab.engine.protocol.ParseError;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Kjetil
 */
public class PriorsGenerator extends Data {
    private static String typedescription="Priors Generator";
    private String name=null;
    private Classifier classifier=null;
    private String filename=null; // Priors Generators must be loaded from file since they can not be fully specified in a parameter-string
    private String configfilename=null; // ...alternatively, the setup and training procedure for the PG can be specified in a config-file
    private String[] featureNames=null; // the names of the features used to train this classifier (in correct order)
    private double convertOutsideRegionsToValue=0; // the value to use to convert non-regions in Region datasets to numeric values. (this could for example be 0 or -1)
    private ClassifierDataSet trainingDatasetProperties=null; //
    private String targetFeatureName="";

    /** Creates a new (empty) Priors Generator with the given name */
    public PriorsGenerator(String name) {
        this.name=name;
    }


    /**
     * Specifies a new name for this PriorsGenerator
     * @param name the name for this PriorsGenerator
     */
    public void setName(String name) {
        this.name=name;
    }

    @Override
    public void rename(String name) {
        setName(name);
    }

   /**
    * Returns the name of this PriorsGenerator
    *
    * @return name
    */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {return this;} // should maybe change later


    @Override
    public Data clone() {
        return this; // PriorsGenerator are immutable!
    }

    /** Returns the name of the feature that this Priors Generator was build to predict */
    public String getTargetFeatureName() {
        return targetFeatureName;
    }

   /** Sets the name of the feature that this Priors Generator is build to predict */
    public void setTargetFeatureName(String targetFeatureName) {
        this.targetFeatureName=targetFeatureName;
    }


    @Override
    public String getValueAsParameterString() {
        if (configfilename!=null) return Operation_new.CONFIGURATION_PREFIX+"\""+configfilename+"\"";
        return Operation_new.FILE_PREFIX+filename; // I don't think this is used...
    }

    @Override
    public void importData(Data source) throws ClassCastException {
        PriorsGenerator other=(PriorsGenerator)source;
        this.classifier=other.classifier;
        this.filename=other.filename;
        this.configfilename=other.configfilename;
        this.featureNames=other.featureNames;
        this.convertOutsideRegionsToValue=other.convertOutsideRegionsToValue;
        this.trainingDatasetProperties=other.trainingDatasetProperties;
        this.targetFeatureName=other.targetFeatureName;
    }

    /**
     * Returns the classifier with the given name (if exists)
     * @return
     */
    public Classifier getClassifier(String classifierName) {
        if (classifier.getName().equals(classifierName)) return classifier;
        else if (classifier instanceof AdaBoost) {
            for (Classifier c:((AdaBoost)classifier).getClassifiers()) {
                if (c.getName().equals(classifierName)) return c;
            }
        }
        return null;
    }


    /** Returns the classifier used by this PriorsGenerator (if any)
     *  The classifier can be a single classifier or an AdaBoost classifier (with many component classifiers)
     */
    public Classifier getClassifier() {
        return classifier;
    }

    /** Sets the classifier to use for this PriorsGenerator
     *  This classifier can be a single classifier or an AdaBoost classifier
     */
    public void setClassifier(Classifier classifier) {
        this.classifier=classifier;
    }

    /**
     * Returns the names of the features used by this PriorsGenerator
     */
    public String[] getFeatures() {
        return featureNames.clone();
    }

    /**
     * Sets the names of the features used by this PriorsGenerator
     */
    public void setFeatures(String[] features) {
        this.featureNames=features;
    }

    /**
     * This method should be called after the PriorsGenerator classifier has been trained
     * to provide information about the trainingdataset used. Note the the dataset will be
     * changed (cleared) as a result of this call and should thus not be used again.
     */
    public void setTrainingDataSetUsed(ClassifierDataSet dataset) {
       dataset.clearExamples();
       trainingDatasetProperties=dataset;
    }

    /** Returns the name of the file that stores this PriorsGenerator
     *  (This might not be properly set if the PriorsGenerator was created from a protocol command)
     */
    public String getFilename() {
        return filename;
    }

   /** Sets the name of the file that should be used to store this PriorsGenerator */
    public void setFilename(String filename) {
        this.filename=filename;
    }
    
    /** Returns the name of a configuration file that contains instructions on how to setup and train this PriorsGenerator
     */
    public String getConfigurationFilename() {
        return configfilename;
    }

   /** Sets the name of a configuration file that contains instructions on how to setup and train this PriorsGenerator */
    public void setConfigurationFilename(String filename) {
        this.configfilename=filename;
    }    

    @Override
    public boolean containsSameData(Data otherdataobject) {
        //throw new UnsupportedOperationException("PriorsGenerator.containsSameData(): Not supported yet.");
        // return otherdataobject==this;
        return true;
    }

    public static String getType() {return typedescription;}

    @Override
    public String getDynamicType() {
        return typedescription;
    }    
    
    @Override
    public String getTypeDescription() {return typedescription+" : predicting '"+targetFeatureName+"'";}

    @Override
    public void setAdditionalOperationNewTaskParameters(OperationTask task) {
        if (configfilename!=null) task.setParameter(Operation_new.FILENAME, configfilename);
        task.setParameter(Operation_new.FILENAME, (configfilename!=null)?configfilename:filename);   
        task.setParameter(OperationTask.TARGET,this); // this will force Operation_new to reuse this already existing PG rather than create a new object
        // DATA_FORMAT and DATA_FORMAT_SETTINGS will default, but that is OK since only one dataformat exists for PriorsGenerators
    }

    
    public static PriorsGenerator createPriorsGeneratorFromParameterString(String name, String parameterString, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
        if (parameterString==null) throw new ExecutionError("Missing necessary parameters required to create Priors Generator");
        if (parameterString.startsWith(Operation_new.FILE_PREFIX)) {
            // this should probably not happen since the FILE_PREFIX is handled by Operation_new in a different way
            throw new ExecutionError("Unable to initialize Priors Generator directly from file");
        } else if (parameterString.startsWith(Operation_new.CONFIGURATION_PREFIX)) {
            try {
                ArrayList<String> parameters=MotifLabEngine.splitOnComma(parameterString.substring(Operation_new.CONFIGURATION_PREFIX.length()));
                HashMap<String,String> settings=new HashMap<String, String>();
                for (String parameter:parameters) {
                    String[] parts=parameter.split("\\s*=\\s*",2);
                    String key=(parts.length==2)?parts[0]:"filename"; // to keep backwards compatibility a "single" value (not in key=value pair) is considered to be the filename of the config-file
                    String value=(parts.length==2)?parts[1]:parts[0]; 
                    if (value.startsWith("\"")) value=value.substring(1);
                    if (value.endsWith("\"")) value=value.substring(0,value.length()-1);   
                    settings.put(key, value);
                }   
                return initializePriorsGeneratorFromConfigurationFile(name,settings,engine,task);            
            } catch (ParseError p) {
                throw new ExecutionError("Unable to parse parameter for Priors Generator: "+p.getMessage(),p);
            }
        } else throw new ExecutionError("Unable to parse parameter for Priors Generator: "+parameterString);
    }

    /**
     *
     * @param sequence The name of the target sequence
     * @param position The relative position in the sequence
     * @param engine
     * @return
     */
    public double estimatePriorForPosition(String sequenceName, int position, MotifLabEngine engine) throws ExecutionError {
        if (classifier==null) throw new ExecutionError(getName()+" has no classifiers");
        if (featureNames==null || !classifier.isTrained()) throw new ExecutionError(getName()+" has not been properly trained");
        Object[] featureVector=new Object[featureNames.length];
        for (int i=0;i<featureNames.length;i++) {
            Data dataset=engine.getDataItem(featureNames[i]);
            if (dataset==null) throw new ExecutionError(getName()+" requires a feature track with the name '"+featureNames[i]+"'");
            if (!(dataset instanceof FeatureDataset)) throw new ExecutionError("The feature '"+featureNames[i]+"' should be a Numeric or Region Dataset in order to be used by "+getName());
            FeatureSequenceData sequencedata=((FeatureDataset)dataset).getSequenceByName(sequenceName);
            if (sequencedata instanceof NumericSequenceData) {
                featureVector[i]=new Double(((NumericSequenceData)sequencedata).getValueAtRelativePosition(position).doubleValue());
            } else if (sequencedata instanceof RegionSequenceData) {
                int overlapping=((RegionSequenceData)sequencedata).getNumberOfRegionsAtRelativePosition(position);
                featureVector[i]=new Double((overlapping>0)?1:convertOutsideRegionsToValue);
            }
        }
        Example example=new Example(featureVector);
        example.setDataSet(trainingDatasetProperties);
        return classifier.scoreExample(example);
    }

   /**
     * Estimates the priors for an entire Numeric Dataset (all sequences)
     * @param targetdataset The target numeric dataset. Its contents will be replaced!
     * @param engine
     * @return
     */
    public NumericDataset estimatePriorForDataset(NumericDataset targetdataset, MotifLabEngine engine, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (classifier==null) throw new ExecutionError(getName()+" has no classifiers");
        if (featureNames==null || !classifier.isTrained()) throw new ExecutionError(getName()+" has not been properly trained");
        FeatureSequenceData[] features=new FeatureSequenceData[featureNames.length];
        Object[] featureVector=new Object[featureNames.length];
        ArrayList<FeatureSequenceData> targetSequences=targetdataset.getAllSequences();
        for (FeatureSequenceData targetSequence:targetSequences) { // for each sequence in the target dataset
            if (Thread.interrupted() || task.getStatus().equals(ExecutableTask.ABORTED)) throw new InterruptedException();
            String sequenceName=targetSequence.getSequenceName();
            for (int i=0;i<featureNames.length;i++) { // collect the corresponding sequences for the features in an "easy-to-look-up" array
                Data dataset=engine.getDataItem(featureNames[i]);
                if (dataset==null) throw new ExecutionError(getName()+" requires a feature track with the name '"+featureNames[i]+"'");
                if (!(dataset instanceof FeatureDataset)) throw new ExecutionError("The feature '"+featureNames[i]+"' should be a Numeric or Region Dataset in order to be used by "+getName());
                features[i]=((FeatureDataset)dataset).getSequenceByName(sequenceName);
            }
            for (int pos=0;pos<targetSequence.getSize();pos++) { // go through every position in the target sequence
                for (int i=0;i<featureNames.length;i++) { // create the featurevector
                     if (features[i] instanceof NumericSequenceData) {
                        featureVector[i]=new Double(((NumericSequenceData)features[i]).getValueAtRelativePosition(pos).doubleValue());
                     } else if (features[i] instanceof RegionSequenceData) {
                        int overlapping=((RegionSequenceData)features[i]).getNumberOfRegionsAtRelativePosition(pos);
                        featureVector[i]=new Double((overlapping>0)?1:convertOutsideRegionsToValue);
                     }
                 }
                 Example example=new Example(featureVector);
                 example.setDataSet(trainingDatasetProperties);
                 double prior=classifier.scoreExample(example);
                 ((NumericSequenceData)targetSequence).setValueAtRelativePosition(pos, prior);
            }
        }
        return targetdataset;
    }


    /**
     * Estimates the priors for a single sequence (NumericSequenceData)
     * @param targetSequence The target numeric data sequence. Its contents will be replaced!
     * @param engine
     * @return
     */
    public NumericSequenceData estimatePriorForDataset(NumericSequenceData targetSequence, MotifLabEngine engine, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (classifier==null) throw new ExecutionError(getName()+" has no classifiers");
        if (featureNames==null || !classifier.isTrained()) throw new ExecutionError(getName()+" has not been properly trained");
        String sequenceName=targetSequence.getSequenceName();
        FeatureSequenceData[] features=new FeatureSequenceData[featureNames.length];
        Object[] featureVector=new Object[featureNames.length];
        for (int i=0;i<featureNames.length;i++) { // collect the corresponding sequences for the features in an "easy-to-look-up" array
            Data dataset=engine.getDataItem(featureNames[i]);
            if (dataset==null) throw new ExecutionError(getName()+" requires a feature track with the name '"+featureNames[i]+"'");
            if (!(dataset instanceof FeatureDataset)) throw new ExecutionError("The feature '"+featureNames[i]+"' should be a Numeric or Region Dataset in order to be used by "+getName());
            features[i]=((FeatureDataset)dataset).getSequenceByName(sequenceName);
        }
        for (int pos=0;pos<targetSequence.getSize();pos++) { // go through every position in the target sequence
            for (int i=0;i<featureNames.length;i++) { // create the featurevector
                 if (features[i] instanceof NumericSequenceData) {
                    featureVector[i]=new Double(((NumericSequenceData)features[i]).getValueAtRelativePosition(pos).doubleValue());
                 } else if (features[i] instanceof RegionSequenceData) {
                    int overlapping=((RegionSequenceData)features[i]).getNumberOfRegionsAtRelativePosition(pos);
                    featureVector[i]=new Double((overlapping>0)?1:convertOutsideRegionsToValue);
                 }
             }
             Example example=new Example(featureVector);
             example.setDataSet(trainingDatasetProperties);
             double prior=classifier.scoreExample(example);
             ((NumericSequenceData)targetSequence).setValueAtRelativePosition(pos, prior);
        }
        return targetSequence;
    }

    public void debug() {
        System.err.println("---- PriorsGenerator ----");
        System.err.println("Name: "+name);
        System.err.println("Features: "+featureNames);
        System.err.println("Classifier: "+classifier);
        System.err.println("filename: "+filename);
    }


   /** Reads setup and training instructions for the PG from a file*/
   private static PriorsGenerator initializePriorsGeneratorFromConfigurationFile(String dataname, HashMap<String,String> overrides, MotifLabEngine engine, OperationTask task) throws ExecutionError, InterruptedException {
      if (task!=null) task.setProgress(0);
      if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
      String configTargetFeatureName=null;
      ArrayList<String> inputFeatures=new ArrayList<String>();
      Classifier classifier=null;
      int trainingSetSize=0;
      int validationSetSize=0;
      boolean filterDuplicatesTraining=false;
      boolean filterDuplicatesValidation=false;
      String trainingSamplingStrategy=null;
      String trainingSubset=null;
      String trainingFilename=null;
      String validationSamplingStrategy=null;
      String validationSubset=null;   
      String validationFilename=null;   
      String filename=overrides.get("filename");
      if (filename==null) throw new ExecutionError("Missing filename");
      try {
         InputStream inputstream=null;
         if (filename.startsWith("http://") || filename.startsWith("https://") || filename.startsWith("ftp://")|| filename.startsWith("ftps://")) {
            URL url=new URL(filename);         
            inputstream = url.openStream();
         } else {
            File file=engine.getFile(filename);
            inputstream=MotifLabEngine.getInputStreamForFile(file);
         }
         BufferedInputStream buffer=new BufferedInputStream(inputstream);
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document doc = builder.parse(buffer);
         buffer.close();
         inputstream.close();
         if (task!=null) task.setProgress(10);
         if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();          
         NodeList pgnodes = doc.getElementsByTagName("PriorsGenerator");
         if (pgnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'PriorsGenerator' node found");
         Element pgNode = (Element) pgnodes.item(0);
         NodeList datasetnodes = pgNode.getElementsByTagName("Datasets");
         if (datasetnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Datasets' node found");
         Element datasetsNode = (Element) datasetnodes.item(0);
         NodeList featuresnodes = pgNode.getElementsByTagName("Features");
         if (featuresnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Features' node found");
         Element featuresNode = (Element) featuresnodes.item(0);
         NodeList classifiernodes = pgNode.getElementsByTagName("Classifier");
         if (classifiernodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Classifier' node found");
         Element classifierNode = (Element) classifiernodes.item(0);
         
         NodeList targetfeaturesnodes = featuresNode.getElementsByTagName("Target");
         if (targetfeaturesnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Target' feature node found");
         Element targetFeatureNode = (Element) targetfeaturesnodes.item(0);  
         configTargetFeatureName=targetFeatureNode.getTextContent();
         if (configTargetFeatureName!=null) configTargetFeatureName=configTargetFeatureName.trim();
         
         NodeList inputfeaturesnodes = featuresNode.getElementsByTagName("Input");
         if (inputfeaturesnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Input' feature nodes found");
         for (int i=0;i<inputfeaturesnodes.getLength();i++) {
             Element inputFeatureNode = (Element) inputfeaturesnodes.item(i);  
             String inputFeature=inputFeatureNode.getTextContent();
             if (inputFeature!=null && !inputFeature.trim().isEmpty()) inputFeatures.add(inputFeature.trim());                    
         } 
         NodeList trainingsetnodes = datasetsNode.getElementsByTagName("Trainingset");
         if (trainingsetnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Trainingset' node found");
         Element trainingsetNode = (Element) trainingsetnodes.item(0);
         trainingSamplingStrategy=(overrides.containsKey("trainingset.sampling"))?overrides.get("trainingset.sampling"):trainingsetNode.getAttribute("sampling");
         String trainingsetSize=(overrides.containsKey("trainingset.samples"))?overrides.get("trainingset.samples"):trainingsetNode.getAttribute("samples");
         try {
            trainingSetSize=Integer.parseInt(trainingsetSize);           
         } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse 'samples' parameter for trainingset (should be integer). Found: "+trainingsetSize);}
         trainingSubset=(overrides.containsKey("trainingset.subset"))?overrides.get("trainingset.subset"):trainingsetNode.getAttribute("subset");
         String trainingsetRemoveDuplicates=(overrides.containsKey("trainingset.remove_duplicates"))?overrides.get("trainingset.remove_duplicates"):trainingsetNode.getAttribute("remove_duplicates");
         filterDuplicatesTraining=(trainingsetRemoveDuplicates.equalsIgnoreCase("true") || trainingsetRemoveDuplicates.equalsIgnoreCase("yes"));
         trainingFilename=(overrides.containsKey("trainingset.filename"))?overrides.get("trainingset.filename"):trainingsetNode.getAttribute("filename");
         if (trainingFilename!=null && trainingFilename.trim().isEmpty()) trainingFilename=null;

         NodeList validationsetnodes = datasetsNode.getElementsByTagName("Validationset");
         //if (validationsetnodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo 'Validationset' node found");
         if (validationsetnodes.getLength()>0) {
             Element validationsetNode = (Element) validationsetnodes.item(0);
             validationSamplingStrategy=(overrides.containsKey("validationset.sampling"))?overrides.get("validationset.sampling"):validationsetNode.getAttribute("sampling");
             String validationsetSize=(overrides.containsKey("validationset.samples"))?overrides.get("validationset.samples"):validationsetNode.getAttribute("samples");
             try {
                validationSetSize=Integer.parseInt(validationsetSize);
             } catch (NumberFormatException e) {throw new ExecutionError("Unable to parse 'samples' parameter for validationset (should be integer). Found: "+validationsetSize);}
             validationSubset=(overrides.containsKey("validationset.subset"))?overrides.get("validationset.subset"):validationsetNode.getAttribute("subset");
             String validationsetRemoveDuplicates=(overrides.containsKey("validationset.remove_duplicates"))?overrides.get("validationset.remove_duplicates"):validationsetNode.getAttribute("remove_duplicates");
             filterDuplicatesValidation=(validationsetRemoveDuplicates.equalsIgnoreCase("true") || validationsetRemoveDuplicates.equalsIgnoreCase("yes"));
             validationFilename=(overrides.containsKey("validationset.filename"))?overrides.get("validationset.filename"):validationsetNode.getAttribute("filename");
             if (validationFilename!=null && validationFilename.trim().isEmpty()) validationFilename=null;             
         }
         
         if (task!=null) task.setProgress(30);
         if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException(); 
         String classifierType=classifierNode.getAttribute("type");         
         int samplingStrategy=Classifier.getSamplingStrategyForName(classifierNode.getAttribute("sampling"));
         if (classifierType.equals("AdaBoost")) {
             classifier=new AdaBoost();            
             classifier.setName(classifierNode.getAttribute("name"));
             NodeList nestedclassifiernodes = classifierNode.getElementsByTagName("Classifier");
             if (nestedclassifiernodes.getLength()==0) throw new SystemError("An error occurred while loading PriorsGenerator config-file ["+filename+"]:\n\nNo component Classifiers found for AdaBoost");
             for (int i=0;i<nestedclassifiernodes.getLength();i++) {
                 if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();
                 Element nesteClassifierNode = (Element) nestedclassifiernodes.item(i);
                 Classifier componentClassifier=initializeClassifierFromConfig(nesteClassifierNode,engine,inputFeatures.size());
                 componentClassifier.setSamplingStrategy(samplingStrategy);
                 componentClassifier.setUseWeightedExamples(true);
                 ((AdaBoost)classifier).addClassifier(componentClassifier);
             }            
         
         } else { // single classifier
              classifier=initializeClassifierFromConfig(classifierNode,engine,inputFeatures.size());
              classifier.setSamplingStrategy(Classifier.NONE);
              classifier.setUseWeightedExamples(false);
         }
         if (task!=null) task.setProgress(40);
         if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();          
      } // -- end try
      catch (Exception e) {
           String message=(e instanceof SystemError || e instanceof ExecutionError)?e.getMessage():(e.getClass().getSimpleName()+":"+e.getMessage());
           throw new ExecutionError(message);
      }
      
      if (configTargetFeatureName==null || configTargetFeatureName.isEmpty()) throw new ExecutionError("Missing name of target feature in PriorsGenerator configuration file");
      if (inputFeatures.isEmpty()) throw new ExecutionError("Missing names of input features in PriorsGenerator configuration file");
      File trainingDatasetFile=(trainingFilename!=null)?(engine.getFile(trainingFilename)):null;
      File validationDatasetFile=(validationFilename!=null)?(engine.getFile(validationFilename)):null;
      RegionDataset targetFeature=null;
      Data targetItem=engine.getDataItem(configTargetFeatureName);
      if (targetItem==null) throw new ExecutionError("Unknown target feature: "+configTargetFeatureName);
      if (!(targetItem instanceof RegionDataset)) throw new ExecutionError("The selected target '"+configTargetFeatureName+"' is not a Region Dataset but "+(targetItem.getDynamicType()));
      targetFeature=(RegionDataset)targetItem;
      ArrayList<FeatureDataset> features=new ArrayList<FeatureDataset>();
      for (String dataName:inputFeatures) {
          Data dataItem=engine.getDataItem(dataName);
          if (dataItem==null) throw new ExecutionError("Unknown input feature: "+dataName);
          if (!(dataItem instanceof FeatureDataset)) throw new ExecutionError("The selected input '"+dataName+"' is not a Feature Dataset but "+(targetItem.getDynamicType()));       
          features.add((FeatureDataset)dataItem);
      }
      ClassifierDataSet[] datasets=null;
      try {
          datasets=ClassifierDataSet.setupDatasets(trainingSamplingStrategy, trainingSubset,trainingSetSize, filterDuplicatesTraining, filterDuplicatesValidation, validationSamplingStrategy, validationSubset, validationSetSize, features, targetFeature, trainingDatasetFile, validationDatasetFile, new boolean[]{false}, (javax.swing.JProgressBar)null, engine, task);
      } catch (Exception e) {
          //e.printStackTrace(System.err);
          throw new ExecutionError("Unable to setup training (and validation) dataset for PriorsGenerator: "+e.getMessage());       
      }     
      if (datasets==null) throw new ExecutionError("Unable to setup training (and validation) dataset for PriorsGenerator");
      
      if (task!=null) task.setProgress(50);
      if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();       
      // setup classifier
      if (classifier==null) throw new ExecutionError("Missing valid description of classifier in PriorsGenerator configuration file");
      try {
          classifier.train(datasets[0],datasets[1],task);
          HashMap<String,Object> results=classifier.validate(datasets[0],Classifier.TRAINING);
          results.putAll(classifier.validate(datasets[1],Classifier.VALIDATION));
          int trainingCorrect=((Integer)results.get(Classifier.TRAINING_CORRECT));
          int trainingMistakes=((Integer)results.get(Classifier.TRAINING_MISTAKES));
          int validationCorrect=((Integer)results.get(Classifier.VALIDATION_CORRECT));
          int validationMistakes=((Integer)results.get(Classifier.VALIDATION_MISTAKES));
          int trainingTP=((Integer)results.get(Classifier.TRAINING_TP));
          int trainingFP=((Integer)results.get(Classifier.TRAINING_FP));
          int trainingTN=((Integer)results.get(Classifier.TRAINING_TN));
          int trainingFN=((Integer)results.get(Classifier.TRAINING_FN));
          int validationTP=((Integer)results.get(Classifier.VALIDATION_TP));
          int validationFP=((Integer)results.get(Classifier.VALIDATION_FP));
          int validationTN=((Integer)results.get(Classifier.VALIDATION_TN));
          int validationFN=((Integer)results.get(Classifier.VALIDATION_FN));  
          Double trainingRankCorrelation=((Double)results.get(Classifier.TRAINING_RANK_CORRELATION));
          Double validationRankCorrelation=((Double)results.get(Classifier.VALIDATION_RANK_CORRELATION));
          int trainingPosEx=datasets[0].getNumberOfExamplesInClass(Classifier.CLASS_POSITIVE);
          int trainingNegEx=datasets[0].getNumberOfExamplesInClass(Classifier.CLASS_NEGATIVE);
          int validationPosEx=datasets[1].getNumberOfExamplesInClass(Classifier.CLASS_POSITIVE);
          int validationNegEx=datasets[1].getNumberOfExamplesInClass(Classifier.CLASS_NEGATIVE);
          java.text.DecimalFormat formatter=new java.text.DecimalFormat("0.###");
          double trainingCorrectFraction=(datasets[0].size()==0)?0:trainingCorrect*100.0/datasets[0].size();
          double validationCorrectFraction=(datasets[1].size()==0)?0:validationCorrect*100.0/datasets[1].size();
          engine.logMessage("PriorsGenerator training completed!   Trainingset size = "+datasets[0].size()+" ("+trainingPosEx+" positive / "+trainingNegEx+" negative).  Validationset size = "+datasets[1].size()+" ("+validationPosEx+" positive / "+validationNegEx+" negative).",10);   
          engine.logMessage("   Trainingset  :  Correctly classified "+trainingCorrect+" of "+datasets[0].size()+" => "+formatter.format(trainingCorrectFraction)+"%.  (TP="+trainingTP+", FP="+trainingFP+", TN="+trainingTN+", FN="+trainingFN+"). Rank correlation="+trainingRankCorrelation,10);   
          engine.logMessage("   Validationset:  Correctly classified "+validationCorrect+" of "+datasets[1].size()+" => "+formatter.format(validationCorrectFraction)+"%.  (TP="+validationTP+", FP="+validationFP+", TN="+validationTN+", FN="+validationFN+"). Rank correlation="+validationRankCorrelation,10);   
      } 
      catch (InterruptedException ie) {throw ie;}      
      catch (Exception e) {throw new ExecutionError(e.getClass().getSimpleName()+":"+e.getMessage(),e);}

      if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();      
      // output classifier evaluation results... 
      PriorsGenerator priorsgenerator=new PriorsGenerator(dataname);
      priorsgenerator.setTargetFeatureName(configTargetFeatureName);
      priorsgenerator.setFeatures(inputFeatures.toArray(new String[inputFeatures.size()]));
      priorsgenerator.setClassifier(classifier);
      priorsgenerator.setTrainingDataSetUsed(datasets[0]); // Note that this call will clear all examples in the trainingSet
      priorsgenerator.setFilename(null);
      priorsgenerator.setConfigurationFilename(filename);   
      if (task!=null) task.setProgress(100);
      if (Thread.interrupted() || (task!=null && task.isAborted())) throw new InterruptedException();           
      return priorsgenerator;
   }
    
   private static Classifier initializeClassifierFromConfig(Element classifierNode, MotifLabEngine engine, int inputfeatures) throws ExecutionError, InterruptedException {     
       String classifierType=classifierNode.getAttribute("type"); 
       Classifier newclassifier=Classifier.getNewClassifier(classifierType);
       if (newclassifier==null) throw new ExecutionError("Unknown classifier type: "+classifierType);
       newclassifier.setName(classifierNode.getAttribute("name"));  
       Parameter[] classifierParameters=newclassifier.getParameters();
       ParameterSettings settings=new ParameterSettings();
       NodeList parameternodes = classifierNode.getElementsByTagName("Parameter");
       for (int i=0;i<parameternodes.getLength();i++) {
           Element parameterNode = (Element) parameternodes.item(i);  
           String parametername=parameterNode.getAttribute("name");
           if (parametername==null || parametername.isEmpty()) throw new ExecutionError("Missing 'name' attribute for parameter");
           String value=parameterNode.getAttribute("value");
           if ((value==null || value.isEmpty()) && !settings.isHidden(parametername, classifierParameters)) throw new ExecutionError("Missing 'value' attribute for parameter '"+parametername+"'");
           settings.setParameter(parametername, value);
       }     
       if (newclassifier instanceof org.motiflab.engine.data.classifier.neuralnet.FeedForwardNeuralNet) {
            String topology=(String)settings.getResolvedParameter("Topology", classifierParameters, engine);
            topology=inputfeatures+","+topology+",1";
            settings.setParameter("UseTopology", topology);           
       } // this is a "hack" to add the number of input and output nodes to the "Topology" setting
       newclassifier.initializeFromParameters(settings, engine);          
       return newclassifier;
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
