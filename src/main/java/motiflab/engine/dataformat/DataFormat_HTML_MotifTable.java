/*
 
 
 */

package motiflab.engine.dataformat;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.data.DataMap;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.MotifNumericMap;
import motiflab.engine.data.MotifTextMap;
import motiflab.engine.data.OutputDataDependency;
import motiflab.gui.MotifLogo;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class DataFormat_HTML_MotifTable extends DataFormat {
    private String name="HTML_MotifTable";
    private static final String HTML=DataFormat_HTML.HTML;

    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};
    
    public DataFormat_HTML_MotifTable() {      
        String[] standard=Motif.getAllStandardProperties(true);
        String[] standardExt=new String[standard.length+4];
        System.arraycopy(standard, 0, standardExt, 0, standard.length);
        standardExt[standardExt.length-1]="ID";
        standardExt[standardExt.length-2]="Matrix";
        standardExt[standardExt.length-3]="Logo";
        standardExt[standardExt.length-4]="Classpath";        
        Arrays.sort(standardExt);
        StringBuilder builder=new StringBuilder();
        builder.append("<html>An ordered, comma-separated list of properties to output for each motif.<br>Note that standard properties are case-insensitive,<br>but user-defined properties are case-sensitive!<br>");
        builder.append("<br>Standard motif properties include:<br><br>");
        for (String prop:standardExt) {builder.append(prop);builder.append("<br>");}
        builder.append("<br>The name of a property can be followed by the name of another property within parenthesis.");
        builder.append("<br>This second property will be displayed as a tooltip when the mouse is over the table-cell with the first property:");
        builder.append("<br>E.g. the format:<pre>  ID, Short Name(Long Name)  </pre>will create a table with 2 columns.");
        builder.append("<br>The first column will be the Motif ID and the second the motif's short name.");
        builder.append("<br>If the user points the mouse at the short name of a motif, its long name will be displayed in a tooltip.");
        builder.append("</html>");
        addParameter("Format", "ID,Short Name(Long Name),Classification(Class Name),Logo", null,builder.toString(),true,false);
        addOptionalParameter("Sequence logo height",24, new Integer[]{0,200},"<html>Height of sequence logos (if included in the table)</html>");
        addOptionalParameter("Sequence logo max width",0, new Integer[]{0,10000},"<html>Maximum width of sequence logos<br>If this is set, logos will be scaled to fit within the limit.<br>A value of 0 means no limit</html>");
        addOptionalParameter("Sequence logos","Shared images", new String[]{"Shared images","New images"},"Should the image files create for logos have standard names (motifID.gif) or be unique to each output");           
        //addOptionalParameter("Image format","png", new String[]{"png","svg"},"Format ");           
        addOptionalParameter("Multiline",Boolean.TRUE, new Boolean[]{true,false},"If selected, list-properties will be split over several lines");        
        addOptionalParameter("Headline", "", null,"Add an optional headline which will be displayed at the top of the page");
        //addOptionalParameter("Sort by","Motif ID", new String[]{"Motif ID","Short name","Long name","Size","Information content","Classification"},null);        
        //addOptionalParameter("Sort direction","Ascending", new String[]{"Ascending","Descending"},null);        
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
        return false;
    }
    
    @Override
    public boolean canParseInput(Class dataclass) {
        return false;
    }
    
      
    @Override
    public Class[] getSupportedDataTypes() {
        return supportedTypes;
    }
    
    @Override
    public String getSuffix() {
        return "html";
    }

    @Override
    public boolean isAppendable() {
        return false;
    }
    
   @Override
    public OutputData format(Data dataobject, OutputData outputobject, ParameterSettings settings, ExecutableTask task) throws ExecutionError, InterruptedException {
        if (!canFormatOutput(dataobject)) {
            if (task!=null) throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format",task.getLineNumber());
            else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        }
        setProgress(1);
        String format="";
        int sequenceLogoHeight=0; 
        int sequenceLogoMaxWidth=0; 
        boolean multiline=true;        
        String headline="";
        String showSequenceLogosString="Shared images";
        //String imageformat="png";
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             format=(String)settings.getResolvedParameter("Format",defaults,engine);
             sequenceLogoHeight=(Integer)settings.getResolvedParameter("Sequence logo height",defaults,engine); 
             sequenceLogoMaxWidth=(Integer)settings.getResolvedParameter("Sequence logo max width",defaults,engine); 
             //imageformat=(String)settings.getResolvedParameter("Image format",defaults,engine);
             multiline=(Boolean)settings.getResolvedParameter("Multiline",defaults,engine); 
             headline=(String)settings.getResolvedParameter("Headline",defaults,engine);
             showSequenceLogosString=(String)settings.getResolvedParameter("Sequence logos",defaults,engine);             
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             format=(String)getDefaultValueForParameter("Format");
             sequenceLogoHeight=(Integer)getDefaultValueForParameter("Sequence logo height");
             sequenceLogoMaxWidth=(Integer)getDefaultValueForParameter("Sequence logo max width");
             // imageformat=(String)getDefaultValueForParameter("Image format");
             multiline=(Boolean)getDefaultValueForParameter("multiline");         
             headline=(String)getDefaultValueForParameter("Header");
        }
        headline=escapeHTML(headline.trim());
        engine.createHTMLheader((!headline.isEmpty())?headline:"Motifs", null, null, true, true, true, outputobject);
        if (!headline.isEmpty()) {
            outputobject.append("<h1 class=\"headline\">"+headline+"</h1>\n",HTML);
        }        
        outputobject.append("<table class=\"sortable\">\n<tr>",HTML);        
        // process format
        boolean includeLogo=false;
        boolean sharedLogos=true;
        if (showSequenceLogosString.equalsIgnoreCase("New images")) sharedLogos=false;
        Pattern pattern=Pattern.compile("(.+)?\\((.+)?\\)");
       
        String[] parts=format.trim().split("\\s*,\\s*");
        String[][] properties=new String[parts.length][2];
                
        for (int i=0;i<parts.length;i++) {
            Matcher matcher=pattern.matcher(parts[i]);
            if (matcher.find()) {
               properties[i][0]=matcher.group(1).trim();
               properties[i][1]=matcher.group(2);
            } else properties[i][0]=parts[i];  
            if (properties[i][0].equalsIgnoreCase("Logo")) includeLogo=true;
            if (properties[i][1]!=null && properties[i][1].equalsIgnoreCase("Logo")) properties[i][1]="Consensus"; // I don't think graphical tooltips are allowed in standard HTML            
            if (properties[i][0].equalsIgnoreCase("Class")||properties[i][0].equalsIgnoreCase("Classification")) outputobject.append("<th class=\"sorttable_ip\">\n",HTML);
            else outputobject.append("<th>\n",HTML);
            outputobject.append(escapeHTML(properties[i][0]),HTML);
            outputobject.append("</th>\n",HTML);
        }        
        outputobject.append("</tr>\n",HTML);        
        MotifLogo sequencelogo=null;   
        if (includeLogo) {
            if (sequenceLogoHeight<=0 || sequenceLogoHeight>200) throw new ExecutionError("Sequence logo height should be within [0,100]");
            VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
            Color [] basecolors=vizSettings.getBaseColors();//new Color[]{Color.GREEN,Color.BLUE,new Color(220,220,0),Color.RED};   
            sequencelogo=new MotifLogo(basecolors,(int)(sequenceLogoHeight*1.25));
            sequencelogo.setMaxWidth(sequenceLogoMaxWidth);
            sequencelogo.setDrawBorder(false); // The border does not always fit the the image size, so I will draw it myself later on
        }  


        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputobject, properties, multiline, sequencelogo, sequenceLogoHeight,sharedLogos);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject, outputobject, properties, multiline, sequencelogo, sequenceLogoHeight,sharedLogos);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append("</table>\n",HTML);
        outputobject.append("</body>\n",HTML);
        outputobject.append("</html>\n",HTML);
        setProgress(100);        
        return outputobject;
    }


    /** output-formats a single motif */
    protected void outputMotif(Motif motif, OutputData outputobject, String[][] properties, boolean multiline, MotifLogo sequencelogo, int logoheight, boolean sharedLogos) throws ExecutionError {
        outputobject.append("<tr>", HTML);
        for (String[] propertyPair:properties) {
            String property=propertyPair[0];
            String tooltip=propertyPair[1];           
            outputobject.append("<td", HTML);
            if (tooltip!=null) {
               Object tooltipvalue=null;
               try { 
                   tooltipvalue=motif.getPropertyValue(tooltip,engine);
               } catch (Exception e) { // check if the property could be a map instead
                   Data item=engine.getDataItem(tooltip);
                   if (item instanceof MotifTextMap || item instanceof MotifNumericMap) tooltipvalue=((DataMap)item).getValue(motif.getName());
                   else throw new ExecutionError("'"+tooltip+"' is not a recognized motif property or applicable Map");
               }               
               if (tooltipvalue!=null) {
                   outputobject.append(" title=\"", HTML);
                   outputProperty(tooltipvalue, outputobject, false, true);
                   outputobject.append("\"", HTML);  
               }
            }
            // append td-class here if necessary
            outputobject.append(">", HTML);
            if (property.equalsIgnoreCase("Logo")) {
                String link=getSequenceLogoTag(motif, outputobject, sequencelogo, logoheight, sharedLogos);
                outputobject.append(link, HTML);    
            } else {
                Object value=null;
                try {
                   value=motif.getPropertyValue(property,engine);
                } catch (Exception e) { // check if the property could be a map instead
                   Data item=engine.getDataItem(property);
                   if (item instanceof MotifTextMap || item instanceof MotifNumericMap) value=((DataMap)item).getValue(motif.getName());
                   else throw new ExecutionError("'"+property+"' is not a recognized motif property or applicable Map");
                }
                if (value==null && (property.equalsIgnoreCase("class") || property.equalsIgnoreCase("classification"))) value="unknown";
                outputProperty(value, outputobject, multiline, false);
            }                        
            outputobject.append("</td>", HTML);     
        }
        outputobject.append("</tr>\n", HTML);
    }

    private void outputProperty(Object value, OutputData output, boolean multiline, boolean tooltip) {
        if (value==null) {}
        else if (value instanceof ArrayList) outputArrayList((ArrayList)value, output, multiline, tooltip);
        else {
	    String string=escapeHTML((value!=null)?value.toString():"");
            if (string.indexOf(' ')>0 && !tooltip) output.append("<nobr>"+string+"</nobr>",HTML); // try to  avoid linebreaks in normal table cells	     
            else output.append(string,HTML);            
        }
    }
    
    /** Outputs an ArrayList either as a list of comma-separated values or one entry on each line */
    private void outputArrayList(ArrayList list, OutputData output, boolean multiline, boolean tooltip) {
        Iterator i = list.iterator();
	boolean first=true;
        if (!multiline && !tooltip) output.append("<nobr>",HTML);
	while (i.hasNext()) {
	    if (first) first=false; else output.append((multiline)?"<br>":", ",HTML);
            Object e = i.next();
	    String string=escapeHTML((e!=null)?e.toString():"");
            if (string.indexOf(' ')>0 && !tooltip) output.append("<nobr>"+string+"</nobr>",HTML);	     
            else output.append(string,HTML);	    
	}
        if (!multiline && !tooltip) output.append("</nobr>",HTML);
    }    
    
    
    /** Creates a sequence logo image for the given motif, saves it to a temp-file and return an IMG-tag that can 
     *  be inserted in HTML-documents to display the image
     */
    protected String getSequenceLogoTag(Motif motif, OutputData outputobject, MotifLogo sequencelogo, int sequenceLogoHeight, boolean useSharedLogos) {
        File imagefile=null;
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();
        String imageFormat=(String)settings.getSettingAsType("motif.imageFormat","gif");
        if (imageFormat==null) imageFormat="gif";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="gif";     
        if (imageFormat.equals("svg")) imageFormat="gif"; // I have disabled this for now because SVG does not work properly
        boolean border=(Boolean)settings.getSettingAsType("motif.border",Boolean.TRUE);          
        if (useSharedLogos) {
            String logofileID=motif.getName();
            boolean sharedDependencyExists=(engine.getSharedOutputDependency(logofileID)!=null);
            OutputDataDependency dependency=outputobject.createSharedDependency(engine,logofileID, imageFormat,true); // returns new or existing shared dependency
            if (!sharedDependencyExists) { // the dependency has not been created before so we must save the image to file
                imagefile=dependency.getFile();
                sequencelogo.setMotif(motif);
                try {
                    saveSequenceLogoImage(imagefile,sequencelogo, sequenceLogoHeight, imageFormat, border); // an image height of 19 corresponds with a logo height of 22 which is "hardcoded" above (but probably should not be) 
                } catch (IOException e) {
                    engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
                }
            } else {
                imagefile=new File(dependency.getInternalPathName());
            }            
        } else { // always save any logo to a new file
            imagefile=outputobject.createDependentFile(engine,imageFormat);
            sequencelogo.setMotif(motif);
            try {              
                saveSequenceLogoImage(imagefile,sequencelogo, sequenceLogoHeight, imageFormat, border); // an image height of 19 corresponds with a logo height of 22 which is "hardcoded" above (but probably should not be) 
            } catch (IOException e) {
                engine.errorMessage("An error occurred when creating image file: "+e.toString(),0);
            }           
        }
        return "<img src=\"file:///"+imagefile.getAbsolutePath()+"\" />";
    }

    private void saveSequenceLogoImage(File file, MotifLogo sequencelogo, int motifheight, String imageFormat, boolean border) throws IOException {
        int width=sequencelogo.getDefaultMotifWidth();
        if (imageFormat==null) imageFormat="gif";
        imageFormat=imageFormat.toLowerCase();
        if (imageFormat.equals("gif") || imageFormat.equals("png")) {
            BufferedImage image=new BufferedImage(width, motifheight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g=image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, motifheight);
            sequencelogo.paintLogo(g);
            if (border) {
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, width-1, motifheight-1);     
            }
            OutputStream output=MotifLabEngine.getOutputStreamForFile(file);
            ImageIO.write(image, imageFormat, output);
            output.close(); 
            g.dispose();            
        } else {                     
            VectorGraphics2D g=null;
                 if (imageFormat.equals("svg")) g = new SVGGraphics2D(0, 0, width, motifheight);
            else if (imageFormat.equals("pdf")) g = new PDFGraphics2D(0, 0, width, motifheight);
            else if (imageFormat.equals("eps")) g = new EPSGraphics2D(0, 0, width, motifheight);
            else throw new IOException("Unknown image format: "+imageFormat);
            g.setClip(0, 0, width,motifheight);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, motifheight);
            sequencelogo.paintLogo(g);
            if (border) {
                g.setColor(Color.BLACK);
                g.drawRect(0, 0, width-1, motifheight-1);     
            }                                 
            FileOutputStream fileStream = new FileOutputStream(file);
            try {
                fileStream.write(g.getBytes());
            } finally {
                fileStream.close();
            } 
            g.dispose();            
        }        
        
        
        
        
        
        
        
    } 
    
    @Override
    public Data parseInput(ArrayList<String> input, Data target, ParameterSettings settings, ExecutableTask task) throws ParseError, InterruptedException  {
       throw new ParseError("Unable to parse Motif input with data format HTML_MotifTable");
    }
    
    protected String escapeHTML(String string) {
        if (string==null) return "";
        if (string.contains("&")) string=string.replace("&", "&amp;"); // this must be first
        if (string.contains("<")) string=string.replace("<", "&lt;");
        if (string.contains(">")) string=string.replace(">", "&gt;");
        if (string.contains("\"")) string=string.replace("\"", "&#34;");    
        return string;
    }     
    
}

        
       
        
        
