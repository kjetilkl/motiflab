/*
 
 
 */

package motiflab.engine.dataformat;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import motiflab.engine.task.ExecutableTask;
import motiflab.engine.data.Data;
import motiflab.engine.data.OutputData;
import motiflab.engine.ExecutionError;

import motiflab.engine.MotifLabEngine;
import motiflab.engine.ParameterSettings;
import motiflab.engine.Parameter;
import motiflab.engine.protocol.ParseError;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;
import motiflab.engine.data.OutputDataDependency;
import motiflab.engine.data.analysis.Analysis;
import motiflab.gui.MotifLogo;
import motiflab.gui.VisualizationSettings;

/**
 *
 * @author kjetikl
 */
public class DataFormat_HTML_Matrix extends DataFormat {
    private String name="HTML_Matrix";
    private static final String HTML=DataFormat_HTML.HTML;
    public static final String VERTICAL="Vertical";
    public static final String HORIZONTAL="Horizontal";    

    private Class[] supportedTypes=new Class[]{MotifCollection.class, Motif.class};
    
    public DataFormat_HTML_Matrix() {      
        addOptionalParameter("Orientation",VERTICAL, new String[]{VERTICAL,HORIZONTAL},"<html>Vertical: matrix has N rows and 4 columns<br>Horizontal: matrix has 4 rows and N columns</html>");
        addOptionalParameter("Header","ID", new String[]{"ID","ID-Name","ID Name"},"Which properties of a motif to include in the header");
        addOptionalParameter("Logos",Analysis.MOTIF_LOGO_NO, new String[]{Analysis.MOTIF_LOGO_NO,Analysis.MOTIF_LOGO_NEW,Analysis.MOTIF_LOGO_SHARED,Analysis.MOTIF_LOGO_TEXT},"Include sequence logos");            
        addOptionalParameter("Headline", "", null,"Add an optional headline which will be displayed at the top of the page");          
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
        String orientation;
        String header="ID";
        String headline="";
        String showSequenceLogosString=Analysis.MOTIF_LOGO_NO;
        if (settings!=null) {
          try{
             Parameter[] defaults=getParameters();
             showSequenceLogosString=(String)settings.getResolvedParameter("Logos",defaults,engine);
             orientation=(String)settings.getResolvedParameter("Orientation",defaults,engine); 
             header=(String)settings.getResolvedParameter("Header",defaults,engine);   
             headline=(String)settings.getResolvedParameter("Headline",defaults,engine);
          } catch (ExecutionError e) {
             throw e;
          } catch (Exception ex) {
              throw new ExecutionError("An error occurred during output formatting", ex);
          }
        } else {
             showSequenceLogosString=(String)getDefaultValueForParameter("Logos");
             orientation=(String)getDefaultValueForParameter("Orientation"); 
             header=(String)getDefaultValueForParameter("Header"); 
             headline=(String)getDefaultValueForParameter("Headline");               
        }
        headline=escapeHTML(headline.trim());
        engine.createHTMLheader((!headline.isEmpty())?headline:"Motifs", null, null, true, true, true, outputobject);
        if (!headline.isEmpty()) {
            outputobject.append("<h1 class=\"headline\">"+headline+"</h1>\n",HTML);
        }        
        outputobject.append("<br>",HTML);        
        // process format
        MotifLogo sequenceLogo=null;
        int logoheight=24;
        VisualizationSettings vizSettings=engine.getClient().getVisualizationSettings();
        Color [] basecolors=vizSettings.getBaseColors();//new Color[]{Color.GREEN,Color.BLUE,new Color(220,220,0),Color.RED};           
        if (showSequenceLogosString.equals(Analysis.MOTIF_LOGO_NEW) || showSequenceLogosString.equals(Analysis.MOTIF_LOGO_SHARED)) {
            sequenceLogo=new MotifLogo(basecolors,(int)(logoheight*1.25));
            sequenceLogo.setDrawBorder(false); // The border does not always fit the the image size, so I will draw it myself later on
        }  
              
        if (dataobject instanceof MotifCollection) {
            ArrayList<Motif> motiflist=((MotifCollection)dataobject).getAllMotifs(engine);
            int size=motiflist.size();
            int i=0;
            for (Motif motif:motiflist) {
                if (task!=null) task.checkExecutionLock(); // checks to see if this task should suspend execution
                if (Thread.interrupted() || (task!=null && task.getStatus().equals(ExecutableTask.ABORTED))) throw new InterruptedException();
                outputMotif(motif, outputobject, header, orientation, showSequenceLogosString, sequenceLogo, logoheight, basecolors);
                // task.setStatusMessage("Motif "+(i+1)+" of "+size);
                setProgress(i+1, size);
                i++;
                if (i%100==0) Thread.yield();
            }
        } else if (dataobject instanceof Motif){
                outputMotif((Motif)dataobject, outputobject, header, orientation, showSequenceLogosString, sequenceLogo, logoheight, basecolors);
        } else throw new ExecutionError("Unable to format object '"+dataobject+"' in "+name+" format");
        outputobject.append("</body>\n",HTML);
        outputobject.append("</html>\n",HTML);
        setProgress(100);        
        return outputobject;
    }


    /** output-formats a single motif */
    private void outputMotif(Motif motif, OutputData outputobject, String header, String orientation, String logoFormat, MotifLogo sequencelogo, int logoheight, Color[] basecolors) throws ExecutionError {
        double[][] matrix=motif.getMatrix(); // (format.equals("Frequencies"))?motif.getMatrixAsFrequencyMatrix():motif.getMatrix();
        if (matrix==null) return;
        int tableColumns=(orientation.equals(VERTICAL))?5:matrix.length+1;
        String headerstring=null;
        if (header.equals("ID")) headerstring=motif.getName();
        else {
            String shortname=motif.getShortName();
            String presentationName=motif.getName();
            if (shortname!=null && !shortname.isEmpty()) {
                if (header.equalsIgnoreCase("ID-Name")) presentationName+="-"+shortname;
                else if (header.equalsIgnoreCase("ID Name")) presentationName+=" "+shortname;
            }
            headerstring=presentationName;
        }        
        String[] colors=new String[]{"#000000",null,null,null,null};
        for (int i=0;i<4;i++) {
            colors[i+1]=VisualizationSettings.convertColorToHTMLrepresentation(basecolors[i]);
        }
        
        outputobject.append("<table bgcolor=\"#FFF0D1\" style=\"table-layout: fixed;\">", HTML);
        outputobject.append("<tr><td bgcolor=\"#F0FFD1\" colspan="+tableColumns+"><b>"+headerstring+"</b></td></tr>", HTML);
        if (!(logoFormat.equals(Analysis.MOTIF_LOGO_NO))) {
             String logotag=getSequenceLogoTag(motif, outputobject, sequencelogo, logoheight, logoFormat);
             outputobject.append("<tr><td bgcolor=\"#F0FFD1\" colspan="+tableColumns+">"+logotag+"</td></tr>", HTML);
        }
        if (orientation.equals(VERTICAL)) { // Vertical orientation
            outputobject.append("<tr><th>&nbsp;</th>",HTML);
            outputobject.append("<th align=center><font color=\""+colors[1]+"\"><b>A</b></font></th>",HTML);
            outputobject.append("<th align=center><font color=\""+colors[2]+"\"><b>C</b></font></th>",HTML);
            outputobject.append("<th align=center><font color=\""+colors[3]+"\"><b>G</b></font></th>",HTML);
            outputobject.append("<th align=center><font color=\""+colors[4]+"\"><b>T</b></font></th>",HTML);
            outputobject.append("</tr>", HTML);
            int pos=0;
            for (double[] row:matrix) {
                outputobject.append("<tr>",HTML);
                pos++;
                outputobject.append("<th align=center width=30><b>"+pos+"</b></th>",HTML);
                outputobject.append("<td align=center>"+getTextValueFor(row[0])+"</td>",HTML);
                outputobject.append("<td align=center>"+getTextValueFor(row[1])+"</td>",HTML);
                outputobject.append("<td align=center>"+getTextValueFor(row[2])+"</td>",HTML);
                outputobject.append("<td align=center>"+getTextValueFor(row[3])+"</td>",HTML);                      
                outputobject.append("</tr>",HTML);
            }
        } else { // Horizontal orientation
            int length=matrix.length;
            String[] first=new String[]{"&nbsp;","A","C","G","T"};
            for (int base=0;base<=4;base++) { // one base on each row preceded by a position row 
                
                outputobject.append("<tr><th align=center width=30><font color=\""+colors[base]+"\"><b>"+first[base]+"</b></font></th>",HTML);
                for (int i=0;i<length;i++) { // 
                   if (base==0) {
                       String posString=(i+1<10)?("&nbsp;"+(i+1)):(""+(i+1));
                       outputobject.append("<th align=center>"+posString+"</th>",HTML);
                   } else {
                       outputobject.append("<td align=center>"+getTextValueFor(matrix[i][base-1])+"</td>",HTML);

                   } 
                }    
                outputobject.append("</tr>",HTML);
            }
        }
        outputobject.append("</table><br><br>", HTML);
    }
    
    private String getTextValueFor(double value) {
        if (value==(int)value) return ""+((int)value);
        else return ""+value;
    }

    
    /** Creates a sequence logo image for the given motif, saves it to a temp-file and return an IMG-tag that can 
     *  be inserted in HTML-documents to display the image
     */
    protected String getSequenceLogoTag(Motif motif, OutputData outputobject, MotifLogo sequencelogo, int sequenceLogoHeight, String logoFormat) {
        if (logoFormat.equals(Analysis.MOTIF_LOGO_NO)) return "";
        else if (logoFormat.equals(Analysis.MOTIF_LOGO_TEXT)) return motif.getConsensusMotif();
        File imagefile=null;
        VisualizationSettings settings=engine.getClient().getVisualizationSettings();
        String imageFormat=(String)settings.getSettingAsType("motif.imageFormat","gif");
        if (imageFormat==null) imageFormat="gif";
        imageFormat=imageFormat.toLowerCase();
        if (!(imageFormat.equals("gif") || imageFormat.equals("png") || imageFormat.equals("svg"))) imageFormat="gif";     
        if (imageFormat.equals("svg")) imageFormat="gif"; // because SVG does not work properly
        boolean border=(Boolean)settings.getSettingAsType("motif.border",Boolean.TRUE);          
        if (logoFormat.equals(Analysis.MOTIF_LOGO_SHARED)) {
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

        
       
        
        
