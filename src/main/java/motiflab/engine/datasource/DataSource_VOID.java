/*
 
 
 */

package motiflab.engine.datasource;

import java.util.ArrayList;
import javax.swing.plaf.synth.Region;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.RegionDataset;

/**
 *
 * @author Kjetil
 */
public class DataSource_VOID extends DataSource {

    public DataSource_VOID(DataTrack datatrack, int organism, String genomebuild) {
        super(datatrack,organism,genomebuild,null);
        this.name="VOID";
    }

    @Override
    public DataSource clone() {
        DataSource_VOID copy=new DataSource_VOID(dataTrack,organism,genomebuild);
        return copy;
    }

    @Override
    public String getProtocol() {
        return VOID;
    }

    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class,NumericDataset.class};
    }     
    
    public static boolean supportsFeatureDataType(Class type) {
        return (type==DNASequenceDataset.class || type==RegionDataset.class || type==NumericDataset.class);
    }    
    
    @Override
    public String getServerAddress() {
        return "VOID";
    }
    
    @Override
    public boolean setServerAddress(String address) {
        return true;
    }    

        @Override
    public org.w3c.dom.Element getXMLrepresentation(org.w3c.dom.Document document) {
        org.w3c.dom.Element element = super.getXMLrepresentation(document);
        org.w3c.dom.Element protocol=document.createElement("Protocol");
        protocol.setAttribute("type", VOID);
        element.appendChild(protocol);
        return element;
    }

     @Override
    public boolean useCache() {
        return false;
    }       
        
    @Override
    public DataSegment loadDataSegment(DataSegment segment, ExecutableTask task) throws Exception {
        Object data=null;
        int segmentsize=segment.getSegmentEnd()-segment.getSegmentStart()+1;
        if (dataTrack.getDataType()==NumericDataset.class) {
            data=new double[segmentsize];
        } else if (dataTrack.getDataType()==RegionDataset.class) {
            data=new ArrayList<Region>(); // create empty region list
        } else if (dataTrack.getDataType()==DNASequenceDataset.class) {
            char[] buffer=new char[segmentsize];
            for (int i=0;i<buffer.length;i++) buffer[i]='N';
            data=buffer;
        }
        segment.setSegmentData(data);
        return segment;
    }


}
