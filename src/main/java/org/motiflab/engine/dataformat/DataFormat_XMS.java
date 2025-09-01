package org.motiflab.engine.dataformat;

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
import org.motiflab.engine.task.ExecutableTask;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.data.OutputData;
import org.motiflab.engine.ExecutionError;
import org.motiflab.engine.ParameterSettings;
import org.motiflab.engine.protocol.ParseError;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifCollection;

/**
 *
 * @author kjetikl
 */
public class DataFormat_XMS extends DataFormat {
    private String name="XMS";
    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};


    public DataFormat_XMS() {

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canFormatOutput(Data data) {
        return (data instanceof MotifCollection || data instanceof Motif);
    }

    @Override
    public boolean canFormatOutput(Class dataclass) {
        return (dataclass.equals(MotifCollection.class) || dataclass.equals(Motif.class));
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
        return "xms";
    }

    @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(5);

        StringBuilder outputString=new StringBuilder();
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            outputString.append("<motifset>\n");
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif,outputString, true);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%20==0) Thread.yield();
            }
            outputString.append("</motifset>\n");
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject, outputString, false);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append(outputString.toString(),getName());
        return outputobject;
    }


    /** outputformats a single motif */
    private void outputMotif(Motif motif, StringBuilder outputString, boolean doindent) {
            double[][] matrix=motif.getMatrixAsFrequencyMatrix();
            if (matrix==null) return;
            String indent=(doindent)?"\t":"";
            outputString.append(indent);
            outputString.append("<motif>\n");
            outputString.append(indent);
            outputString.append("\t<name>");
            outputString.append(motif.getName());
            outputString.append("</name>\n");
            outputString.append(indent);
            outputString.append("\t<weightmatrix alphabet=\"DNA\" columns=\"");
            outputString.append(matrix.length);
            outputString.append("\">\n");
            String[] symbols=new String[]{"adenine","cytosine","guanine","thymine"};
            for (int i=0;i<matrix.length;i++) {
                outputString.append(indent);
                outputString.append("\t\t<column pos=\"");
                outputString.append(i);
                outputString.append("\">\n");
                for (int j=0;j<4;j++) {
                    outputString.append(indent);
                    outputString.append("\t\t\t<weight symbol=\"");
                    outputString.append(symbols[j]);
                    outputString.append("\">");
                    outputString.append(matrix[i][j]);
                    outputString.append("</weight>\n");
                }
                outputString.append(indent);
                outputString.append("\t\t</column>\n");
            }
            outputString.append(indent);
            outputString.append("\t</weightmatrix>\n");
            outputString.append(indent);
            outputString.append("</motif>\n");
    }



    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
        if (target==null) throw new ParseError("Unable to parse unknown (null) target Data in DataFormat_XMS.parseInput(ArrayList<String> input, Data target)");
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
            NodeList motifnodes = doc.getElementsByTagName("motif");
            for (int i=0;i<motifnodes.getLength();i++) {
                 if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                 if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();                 
                 Element motifnode = (Element) motifnodes.item(i);
                 NodeList namenodes = motifnode.getElementsByTagName("name");
                 if (namenodes.getLength()==0) throw new ParseError("Missing <name> for motif");
                 String motifname=((Element)namenodes.item(0)).getTextContent();
                 if (motifname==null || motifname.isEmpty()) throw new ParseError("Missing <name> for motif");
                 motifname=motifname.replaceAll("\\W", "_");
                 NodeList matrixnodes = motifnode.getElementsByTagName("weightmatrix");
                 if (matrixnodes.getLength()==0) throw new ParseError("Missing <weightmatrix> for motif "+motifname);
                 Element matrixnode=(Element)matrixnodes.item(0);
                 String colstring=matrixnode.getAttribute("columns");
                 if (colstring==null || colstring.isEmpty()) throw new ParseError("Missing 'columns' attribute in <weightmatrix> for motif "+motifname);
                 int columns=0;
                 try {
                     columns=Integer.parseInt(colstring);
                 } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value for 'columns' attribute in <weightmatrix> for motif "+motifname);}
                 NodeList columnnodes = matrixnode.getElementsByTagName("column");
                 if (columnnodes.getLength()!=columns) throw new ParseError("Expected "+columns+" <column> elements for motif "+motifname+" but found "+columnnodes.getLength());
                 double[][] matrix=new double[columns][4];
                 for (int c=0; c<columnnodes.getLength();c++) {
                     Element columnnode = (Element) columnnodes.item(c);
                     String positionString=columnnode.getAttribute("pos");
                     if (positionString==null || positionString.isEmpty()) throw new ParseError("Missing position attribute for column #"+c+" in motif "+motifname);
                     int position=0;
                     try {
                         position=Integer.parseInt(positionString);
                     } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric value for 'pos' attribute for column #"+c+" in motif "+motifname);}
                     if (position<0 || position>=columns) throw new ParseError("Value for 'pos' attribute for column #"+c+" in motif "+motifname+" is outside range 0-"+(columns-1));
                     NodeList weightnodes = columnnode.getElementsByTagName("weight");
                     if (weightnodes.getLength()!=4) throw new ParseError("Expected 4 <weight> elements for column in position "+position+" in motif "+motifname+", but found "+weightnodes.getLength());
                     for (int w=0;w<weightnodes.getLength();w++) {
                         int base=0;
                         Element weightnode = (Element) weightnodes.item(w);
                         String symbol=weightnode.getAttribute("symbol");
                         if (symbol==null || symbol.isEmpty()) throw new ParseError("Missing 'symbol' attribute in <weight> for column in position "+position+" in motif "+motifname);
                              if (symbol.equalsIgnoreCase("adenine") || symbol.equalsIgnoreCase("A")) base=0;
                         else if (symbol.equalsIgnoreCase("cytosine") || symbol.equalsIgnoreCase("C")) base=1;
                         else if (symbol.equalsIgnoreCase("guanine") || symbol.equalsIgnoreCase("G")) base=2;
                         else if (symbol.equalsIgnoreCase("thymine") || symbol.equalsIgnoreCase("T")) base=3;
                         else throw new ParseError("Unrecognized value for 'symbol' attribute in <weight> for column in position "+position+" in motif "+motifname+": "+symbol);
                         String weightvalueString=weightnode.getTextContent();
                         if (weightvalueString==null || weightvalueString.isEmpty()) throw new ParseError("Missing weight-value specification for base "+symbol+" for column in position "+position+" in motif "+motifname);
                         double weight=0;
                         try {
                             weight=Double.parseDouble(weightvalueString);
                         } catch(NumberFormatException nfe) {throw new ParseError("Unable to parse expected numeric weight-value for base "+symbol+" for column in position "+position+" in motif "+motifname);}

                         matrix[position][base]=weight;
                     }
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



}





