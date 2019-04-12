package motiflab.engine.dataformat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.ParameterSettings;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_MatrixREDUCE extends DataFormat {
    private String name="MatrixREDUCE";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_MatrixREDUCE() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return false;
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return false;
    }

    @Override
    public boolean canParseInput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }

    @Override
    public boolean canParseInput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
    }


    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }

    @Override
    public String getSuffix() {
        return "xml";
    }

     @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        throw new ExecutionError("DATAFORMAT ERROR: Unable to output data in MatrixREDUCE format (functionality not implemented)");
    } 


    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_MatrixREDUCE.parseInput(ArrayList<String> input, Data target)");
        StringBuilder builder=new StringBuilder();
        for (String s:input) builder.append(s);
        byte[] bytes=null;
        try {
            bytes=builder.toString().getBytes("UTF-8");
        } catch (Exception e) {throw new ParseError("Unsupported encoding:"+e.getMessage());}
        InputStream stream = new ByteArrayInputStream(bytes);
        if (target instanceof MotifCollection) return parseMotifCollection(stream, (MotifCollection)target, task);
        else if (target instanceof Motif) {
            MotifCollection col=parseMotifCollection(stream, (MotifCollection)target, task);
            List<Motif> payload=col.getPayload();
            if (payload.isEmpty()) throw new ParseError("No motifs found");
            else return payload.get(0);
        }
        else throw new ParseError("Unable to parse Motif input to target data of type "+target.getTypeDescription());
    }



    private MotifCollection parseMotifCollection(InputStream inputstream, MotifCollection target, ExecutableTask task) throws ParseError, InterruptedException {
        if (target==null) target=new MotifCollection("MotifCollection");
        DocumentBuilder builder;
         try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(inputstream);
            NodeList motifnodes = doc.getElementsByTagName("psam");
            for (int i=0;i<motifnodes.getLength();i++) {
                 if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                 if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                 
                 Element motifnode = (Element) motifnodes.item(i);
                 String psam_id=motifnode.getAttribute("psam_id");
                 if (psam_id==null || psam_id.isEmpty()) throw new ParseError("Missing 'psam_id' parameter for motif");
                 String motifname="psam_"+psam_id;
                 NodeList matrixnodes = motifnode.getElementsByTagName("affinities");
                 if (matrixnodes.getLength()==0) throw new ParseError("Missing <affinities> for motif "+motifname);
                 Element matrixnode=(Element)matrixnodes.item(0);
                 // use the <optimal_sequence> tag to determine the size of the motif
                 String optimalsequence=getNodeContents("optimal_sequence",motifnode);
                 if (optimalsequence==null || optimalsequence.isEmpty()) throw new ParseError("Unable to determine motif size based on <optimal_sequence> for motif "+motifname);
                 int columns=optimalsequence.length();
                 
                 NodeList columnnodes = matrixnode.getElementsByTagName("base_pair");
                 if (columnnodes.getLength()!=columns) throw new ParseError("Expected "+columns+" <base_pair> elements for motif "+motifname+" but found "+columnnodes.getLength());
                 double[][] matrix=new double[columns][4];
                 for (int c=0; c<columnnodes.getLength();c++) {
                     Element columnnode = (Element) columnnodes.item(c);
                     String positionString=columnnode.getAttribute("pos");
                     if (positionString==null || positionString.isEmpty()) throw new ParseError("Missing position attribute for base_pair "+positionString+" in motif "+motifname);
                     int position=0;
                     try {
                         position=Integer.parseInt(positionString);
                     } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value for 'pos' attribute for base_pair "+positionString+" in motif "+motifname);}
                     if (position<=0 || position>columns) throw new ParseError("Value for 'pos' attribute for base_pair "+positionString+" in motif "+motifname+" is outside range 1-"+(columns));
                     double Aweight=getNodeNumericValue("A",columnnode);
                     double Cweight=getNodeNumericValue("C",columnnode);
                     double Gweight=getNodeNumericValue("G",columnnode);
                     double Tweight=getNodeNumericValue("T",columnnode);
                     matrix[position-1][0]=Aweight;
                     matrix[position-1][1]=Cweight;
                     matrix[position-1][2]=Gweight;
                     matrix[position-1][3]=Tweight;                   
                 }
                 Motif newmotif=new Motif(motifname);
                 newmotif.setMatrix(matrix);
                 target.addMotifToPayload(newmotif);
            }
         }
         catch (ParseError e) {throw e;}
         catch (ParserConfigurationException e) {throw new ParseError("Unable to instantiate XML Document Builder");}
         catch (SAXException e) {throw new ParseError("SAXException: "+e.getMessage());}
         catch (IOException e) {throw new ParseError("I/O-error:"+e.getMessage());}
         catch (Exception e) {throw new ParseError(e.getClass().getSimpleName()+":"+e.getMessage());}
        
        return target;
    }

    /** Given the name of an XML tag and a parent node, this method will return the text content of the first such child element */
    private String getNodeContents(String tagname, Element parent) throws ParseError {
         NodeList optimalsequencenodes = parent.getElementsByTagName(tagname);
         if (optimalsequencenodes.getLength()==0) throw new ParseError("Missing expected <"+tagname+"> tag");
         return ((Element)optimalsequencenodes.item(0)).getTextContent();         
    }
    
    private double getNodeNumericValue(String tagname, Element parent) throws ParseError {
         String text=getNodeContents(tagname,parent);
         if (text==null || text.isEmpty()) throw new ParseError("Unable to find expected number within <"+tagname+">");
         if (text.startsWith("+")) text=text.substring(1);
         try {
             double value=Double.parseDouble(text);
             return value;
         } catch (NumberFormatException e) {
             throw new ParseError("Unable to parse expected number within <"+tagname+">. Found '"+text+"'");
         }
    }    

}





