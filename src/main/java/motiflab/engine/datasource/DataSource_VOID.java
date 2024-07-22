/*
 
 
 */

package motiflab.engine.datasource;

import java.util.ArrayList;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.DNASequenceDataset;
import motiflab.engine.data.DataSegment;
import motiflab.engine.data.NumericDataset;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionDataset;

/**
 * The VOID protocol will always return "empty" tracks, 
 * i.e. a Region Dataset with no regions, a Numeric Dataset where all values are zero
 * or a DNA Sequence Dataset where all bases are 'N'
 * 
 * @author Kjetil
 */
public class DataSource_VOID extends DataSource {

    public static final String PROTOCOL_NAME="VOID";
    
    public DataSource_VOID(DataTrack datatrack, int organism, String genomebuild) {
        super(datatrack,organism,genomebuild,null);
        this.name="VOID";
    }

    private DataSource_VOID() {}
    
    public static DataSource_VOID getTemplateInstance() {
        return new DataSource_VOID();
    }  
    
    
    @Override
    public DataSource clone() {
        DataSource_VOID copy=new DataSource_VOID(dataTrack,organism,genomebuild);
        return copy;
    }

    @Override
    public String getProtocol() {
        return PROTOCOL_NAME;
    }

    @Override
    public Class[] getSupportedData() {
        return new Class[]{DNASequenceDataset.class,RegionDataset.class,NumericDataset.class};
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
        protocol.setAttribute("type", PROTOCOL_NAME);
        element.appendChild(protocol);
        return element;
    }

     @Override
    public boolean useCache() {
        return false;
    }       
        
    @Override
    public boolean usesStandardDataFormat() {
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
