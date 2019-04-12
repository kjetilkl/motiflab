/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JToolTip;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleMotif;
import motiflab.engine.data.Region;
import motiflab.engine.data.Sequence;
import motiflab.engine.data.Data;
import sun.swing.SwingUtilities2;

/**
 *
 * @author kjetikl
 */
public class ExpandedViewMotifTooltip extends JToolTip {
    private static final Color darkGreen=new Color(0,200,0);
    private static final Font modulemotiffont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    private static Font logofont;
    private char[] bases=new char[]{'A','C','G','T'};
    private int fontheight=26; // if you change this size you must also change the ascentCorrection below accordingly (but I am not sure about the relationship...)
    private double ascentCorrection=-2.6; // this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
    private static final int xmargin=4;
    private static final int lineheight=20;
    private static final int xoffset=xmargin+lineheight; // offset for motif logo
    private Color[] basecolors;
    private MotifLabEngine engine;
    private DataTrackVisualizer_Region trackVisualizer;
    private VisualizationSettings settings;
    private static final int modulelogoheight=32;


    public ExpandedViewMotifTooltip(DataTrackVisualizer_Region trackVisualizer, VisualizationSettings settings) {
        logofont=MotifLogo.getLogoFont(fontheight);
        engine=settings.getEngine();
        this.trackVisualizer=trackVisualizer;
        this.settings=settings;
        basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};
    }

    private String getToolTipText(Data data, Region region, int position) {
        if (data instanceof Motif) {          
            Motif motif=(Motif)data;
            String longname=motif.getLongName();
            if (longname==null || longname.isEmpty()) longname=motif.getShortName();
            if (longname==null) longname="";
            if (!longname.equals("")) longname=" - "+longname;
            String orientationString="";
            if (region.getOrientation()==Region.DIRECT) orientationString=", direct strand"; // orientationString=", [+]"; //
            else if (region.getOrientation()==Region.REVERSE) orientationString=", reverse strand"; // orientationString=", [\u2013]"; // &ndash;
            else orientationString=", undetermined orientation"; // orientationString=", [.]"; //              
            StringBuilder builder=new StringBuilder();
            builder.append("<html><nobr>position = ");
            builder.append(trackVisualizer.getPositionString(position));
            builder.append("&nbsp;&nbsp<b>");
            builder.append(trackVisualizer.featureName);             
            builder.append("</b></nobr><br><nobr>");
            Color color=settings.getFeatureColor(region.getType());
            builder.append("<font color=\"");
            builder.append(VisualizationSettings.convertColorToHTMLrepresentation(color));
            builder.append("\">&#9632;</font> <b>");  
            builder.append(motif.getName());
            builder.append("</b>");
            builder.append(longname);          
            builder.append("</nobr><br><nobr>");
            builder.append(region.getLength());
            builder.append(" bp");
            builder.append(orientationString);
            builder.append(", score=");
            builder.append(MotifLabGUI.formatNumber(region.getScore()));
            builder.append("</nobr></html>");
            return builder.toString();
        } else if (data instanceof Module) {
            int[] stats=getModuleMotifRegionFullCount(region, (Module)data);
            int cardinality=((Module)data).getCardinality();
            int present=stats[1];
            int multiple=stats[3];
            String orientationString="";
            if (region.getOrientation()==Region.DIRECT) orientationString=", direct strand"; // orientationString=", [+]"; //
            else if (region.getOrientation()==Region.REVERSE) orientationString=", reverse strand"; // orientationString=", [\u2013]"; // &ndash;
            else orientationString=", undetermined orientation"; // orientationString=", [.]"; //             
            StringBuilder builder=new StringBuilder();
            builder.append("<html><nobr>position = ");
            builder.append(trackVisualizer.getPositionString(position)); 
            builder.append("&nbsp;&nbsp<b>");
            builder.append(trackVisualizer.featureName);            
            builder.append("</b></nobr><br><nobr>");
            Color color=settings.getFeatureColor(region.getType());
            builder.append("<font color=\"");
            builder.append(VisualizationSettings.convertColorToHTMLrepresentation(color));
            builder.append("\">&#9632;</font> <b>");            
            builder.append(data.getName());             
            builder.append("</b></nobr><br><nobr>Motifs=");  
            builder.append(present); 
            builder.append(((multiple>0)?"*":"")); 
            builder.append("/");             
            builder.append(cardinality);  
            builder.append(", "); 
            builder.append(region.getLength()); 
            builder.append(" bp");             
            builder.append(orientationString);  
            builder.append(", score="); 
            builder.append(MotifLabGUI.formatNumber(region.getScore())); 
            builder.append("</nobr></html>");             
            return builder.toString();
        } else if (region!=null) { // the region is not a motif or module (but still a region)
            String orientationString="";
            if (region.getOrientation()==Region.DIRECT) orientationString="&nbsp;&nbsp;[+]&nbsp;&nbsp;";  // ", direct strand";
            else if (region.getOrientation()==Region.REVERSE) orientationString="&nbsp;&nbsp;[\u2013]&nbsp;&nbsp;"; // &ndash; // orientationString=", reverse strand"; // 
            else orientationString="&nbsp;&nbsp;[.]&nbsp;&nbsp;"; // orientationString=", undetermined orientation";           
            StringBuilder builder=new StringBuilder();
            builder.append("<html><nobr>position = ");
            builder.append(trackVisualizer.getPositionString(position));   
            builder.append("&nbsp;&nbsp<b>");
            builder.append(trackVisualizer.featureName);               
            builder.append("</b></nobr><br>");
            if (settings.useMultiColoredRegions(trackVisualizer.featureName)) {
                Color color=settings.getFeatureColor(region.getType());
                builder.append("<font color=\"");
                builder.append(VisualizationSettings.convertColorToHTMLrepresentation(color));
                builder.append("\">&#9632;</font> ");
            }       
            builder.append("<b>");
            builder.append(region.getType());           
            builder.append("</b><nobr>");    
            builder.append(orientationString);              
            builder.append(region.getLength());           
            builder.append(" bp");                    
            builder.append(", score=");           
            builder.append(MotifLabGUI.formatNumber(region.getScore()));
            String otherProperties=region.getPropertiesAsString("<br>",0,false);
            if (otherProperties!=null && !otherProperties.isEmpty()) {
              builder.append("<br>");
              builder.append(otherProperties);
            }            
            builder.append("</nobr>");
            builder.append("</html>");           
            return builder.toString();
        } else { // outside region
            StringBuilder builder=new StringBuilder();
            builder.append("<html><nobr>position = ");           
            builder.append(trackVisualizer.getPositionString(position));   
            builder.append("&nbsp;&nbsp<b>");
            builder.append(trackVisualizer.featureName);               
            builder.append("</b>");              
            builder.append("</nobr></html>");           
            return builder.toString();
        }
    }



    @Override
    @SuppressWarnings("unchecked")
    protected void paintComponent(Graphics g) {
       Region region=trackVisualizer.getCurrentRegion();
       Object data=(region==null)?null:engine.getDataItem(region.getType());
       String sequence=(region==null)?null:(String)region.getProperty("sequence");
       if (data instanceof Motif) {
             setTipText(getToolTipText((Motif)data, region, trackVisualizer.getCurrentGenomicMousePosition()));
             super.paintComponent(g);
             Dimension dim=getPreferredSize();
             int yoffset=dim.height-(lineheight+6);
             if (sequence!=null && region!=null && sequence.length()==region.getLength() && region.getLength()==((Motif)data).getLength()) paintMotifLineWithCursor(g, yoffset, region, sequence, (Motif)data, Color.WHITE, false);
       } else if (data instanceof Module) {
             int currentGenomicPos=trackVisualizer.getCurrentGenomicMousePosition();
             setTipText(getToolTipText((Module)data, region, currentGenomicPos));
             super.paintComponent(g);
             Dimension dim=getPreferredSize();
             int subregcount=getModuleMotifRegionCount(region, (Module)data);
             int yoffset=dim.height-(subregcount*lineheight+6+modulelogoheight);
             paintModuleLogo((Graphics2D)g, (Module)data, xmargin, yoffset);
             yoffset+=modulelogoheight;
             for (String mmname:((Module)data).getSingleMotifNames()) {
                 Color outerboxcolor=settings.getFeatureColor(((Module)data).getName()+"."+mmname);
                 Object property=region.getProperty(mmname);
                 if (property instanceof Region) {
                     Object subdata=engine.getDataItem(((Region)property).getType());
                     String subregionsequence=(String)((Region)property).getProperty("sequence");
                     if (subdata instanceof Motif) paintMotifLineWithCursor(g, yoffset, ((Region)property), subregionsequence, (Motif)subdata, outerboxcolor, true);
                     yoffset+=lineheight;
                 }
                 else if (property instanceof ArrayList) {
                   for (Object obj:(ArrayList)property) {
                       if (obj instanceof Region) {
                           Object subdata=engine.getDataItem(((Region)obj).getType());
                           String subregionsequence=(String)((Region)obj).getProperty("sequence");
                           if (subdata instanceof Motif) paintMotifLineWithCursor(g, yoffset, ((Region)obj), subregionsequence, (Motif)subdata, outerboxcolor, true);
                           yoffset+=lineheight;
                       }
                   }
                 }
             }
       }
       else  {
             String tiptext=getToolTipText(null, region, trackVisualizer.getCurrentGenomicMousePosition());
             setTipText(tiptext);
             super.paintComponent(g);
       }
    }


    private void paintMotifLineWithCursor(Graphics g, int yoffset, Region region, String sequence, Motif motif, Color outerboxcolor, boolean paintMotifName) {
         g.setColor(outerboxcolor);
         g.fillRect(xmargin, yoffset, lineheight, lineheight);
         Color featureColor=settings.getFeatureColor(region.getType());
         g.setColor(featureColor);
         g.fillRect(xmargin+5, yoffset+5, 10, 10);
         // bevel effect
         g.setColor(featureColor.darker());
         g.drawLine(xmargin+6, yoffset+14, xmargin+15, yoffset+14);
         g.drawLine(xmargin+14, yoffset+6, xmargin+14, yoffset+14);         
         g.setColor(featureColor.equals(Color.BLACK)?new Color(106,106,106):motiflab.engine.Graph.brighter(featureColor));
         g.drawLine(xmargin+6, yoffset+6, xmargin+15, yoffset+6);
         g.drawLine(xmargin+6, yoffset+6, xmargin+6,  yoffset+14);   
         
         g.setColor(Color.BLACK);
         g.drawRect(xmargin+5, yoffset+5, 10, 10);
         g.drawRect(xmargin, yoffset, lineheight, lineheight);
         // draw logo
         boolean showDirectMotif=(trackVisualizer.getStrandOrientation()==region.getOrientation());
         if (!showDirectMotif && sequence!=null) sequence=MotifLabEngine.reverseSequence(sequence);
         if (sequence==null || region.getLength()!=motif.getLength() || region.getLength()!=sequence.length()) sequence=null;
         paintMatchLogo(g, motif, sequence, xoffset, yoffset, showDirectMotif, paintMotifName);
         // draw base cursor
         if (sequence!=null) {
             FontMetrics metrics=g.getFontMetrics(logofont);
             int widthG=metrics.charWidth('G');
             g.setColor(settings.getBaseCursorColor());
             int currentoffset=trackVisualizer.getCurrentGenomicMousePosition()-region.getGenomicStart();
             if (trackVisualizer.getStrandOrientation()==Sequence.REVERSE) currentoffset=sequence.length()-(currentoffset+1);
             if (currentoffset>=0 && currentoffset<sequence.length()) g.drawRect(xoffset+currentoffset*widthG, yoffset+1, widthG, lineheight-2);
         }
    }

    /** Returns a list of the module motif regions that might be nested within a module region */
    private ArrayList<Region> getModuleMotifRegions(Region region, Module module) {
        ArrayList<Region> list=new ArrayList<Region>();
        for (String mmname:module.getSingleMotifNames()) {
            Object property=region.getProperty(mmname);
            if (property instanceof Region) list.add((Region)property);
            else if (property instanceof ArrayList) {
               for (Object obj:(ArrayList)property) {
                   if (obj instanceof Region) list.add((Region)obj);
               }
            }
        }
        return list;
    }
     /** Returns the number of module motif regions that might be nested within a module region
      *  Some single motifs could be missing and others could have multiple occurrences in a list
      */
    private int getModuleMotifRegionCount(Region region, Module module) {
        int count=0;
        for (String mmname:(module).getSingleMotifNames()) {
            Object property=region.getProperty(mmname);
            if (property instanceof Region) {
                count++;
            } else if (property instanceof ArrayList) {
               for (Object obj:(ArrayList)property) {
                   if (obj instanceof Region) count++;
               }
            }
        }
        return count;
    }
     /** Returns an array of counts specifying the following properties
      *  [0] => number of total subregions for this module instance (counting multiple instances of same modulemotifs)
      *  [1] => number of modulemotifs which are present
      *  [2] => number of modulemotifs that are absent ([1]+[2] should sum to module cardinality)
      *  [3] => number of modulemotifs which multiple instances
      */
    private int[] getModuleMotifRegionFullCount(Region region, Module module) {
        int[] count=new int[]{0,0,0,0};
        for (String mmname:(module).getSingleMotifNames()) {
            Object property=region.getProperty(mmname);
            if (property==null) {count[2]++;}
            else if(property instanceof Region) {
                count[0]++;
                count[1]++;
            } else if (property instanceof ArrayList) {
               if (!((ArrayList)property).isEmpty()) count[1]++;
               if (((ArrayList)property).size()>1) count[3]++;
               for (Object obj:(ArrayList)property) {
                   if (obj instanceof Region) {
                       count[0]++;
                   }
               }
            }
        }
        return count;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        Region region=trackVisualizer.getCurrentRegion();
        if (region==null) {
            this.setTipText(getToolTipText(null, null, trackVisualizer.getCurrentGenomicMousePosition()));
            return super.getPreferredSize();
        }
        Object data=engine.getDataItem(region.getType());
        String description;
        int logowidth=lineheight;
        int height=0;
        String sequence=(String)region.getProperty("sequence");
        if (data instanceof Motif) {
            description=getToolTipText((Motif)data, region, trackVisualizer.getCurrentGenomicMousePosition());
            if (sequence!=null && region.getLength()==((Motif)data).getLength() && region.getLength()==sequence.length()) {
               int charwidth=getFontMetrics(logofont).charWidth('G');
               logowidth+=charwidth*((Motif)data).getLength()+10; // 10 is just a margin
               height+=lineheight+6;
            }
        } else if (data instanceof Module) {
            description=getToolTipText((Module)data, region, trackVisualizer.getCurrentGenomicMousePosition());
            int charwidth=getFontMetrics(logofont).charWidth('G');
            FontMetrics namemetrics=getFontMetrics(getFont());
            int motifcount=0;
            int maxmotiflength=0;
            for (String mmname:((Module)data).getSingleMotifNames()) {
                Object property=region.getProperty(mmname);
                if (property instanceof Region) {
                    motifcount++;
                    String motifname=mmname;
                    Data motifdata=engine.getDataItem(((Region)property).getType());
                    if (motifdata instanceof Motif) motifname=((Motif)motifdata).getPresentationName();
                    int motiflogolength=0;
                    String regsequence=(String)((Region)property).getProperty("sequence");
                    if (regsequence!=null && ((Region)property).getLength()==((Motif)motifdata).getLength() && ((Region)property).getLength()==regsequence.length()) motiflogolength=regsequence.length();
                    int motiflength=motiflogolength*charwidth+namemetrics.stringWidth(motifname+"   ("+((Region)property).getLength()+" bp)");
                    if (motiflength>maxmotiflength) maxmotiflength=motiflength;
                } else if (property instanceof ArrayList) {
                   for (Object obj:(ArrayList)property) {
                       if (obj instanceof Region) {
                          motifcount++;
                          String motifname=mmname;
                          Data motifdata=engine.getDataItem(((Region)obj).getType());
                          if (motifdata instanceof Motif) motifname=((Motif)motifdata).getPresentationName();
                          int motiflogolength=0;
                          String regsequence=(String)((Region)obj).getProperty("sequence");
                          if (regsequence!=null && ((Region)obj).getLength()==((Motif)motifdata).getLength() && ((Region)obj).getLength()==regsequence.length()) motiflogolength=regsequence.length();
                          int motiflength=motiflogolength*charwidth+namemetrics.stringWidth(motifname+"   ("+((Region)obj).getLength()+" bp)");
                          if (motiflength>maxmotiflength) maxmotiflength=motiflength;
                       }
                   }
                }
            }           
            logowidth+=maxmotiflength+lineheight; // 10 is just a margin
            height+=(lineheight*motifcount)+6;
            int modulelogowidth=getModuleLogoWidth((Module)data, this)+xmargin+9;
            if (modulelogowidth>logowidth) logowidth=modulelogowidth;
            height+=modulelogoheight; // modulelogoheight
        }
        else description=getToolTipText(null, region, trackVisualizer.getCurrentGenomicMousePosition());
        
        this.setTipText(description);
        this.revalidate();
        dim=super.getPreferredSize();
        int width=dim.width+8;
        height+=dim.height;
        return new Dimension((width>logowidth)?width:logowidth,height);
    }


   public static int getModuleLogoWidth(Module module,JComponent c) {
       FontMetrics metrics=SwingUtilities2.getFontMetrics(c,modulemotiffont);
       int boxspacing=19;
       int width=0;
       int size=module.getCardinality();
       for (int i=0;i<size;i++) {
           if (i>0) {width+=boxspacing;}
           ModuleMotif mm=module.getSingleMotif(i);
           String mmName=mm.getRepresentativeName();
           int namewidth=metrics.stringWidth(mmName);
           width+=(namewidth+6); // 3px border on each side
       }
       return width;
   }

   public static int getModuleLogoHeight(Module module,JComponent c) {
       return modulelogoheight;
   }

    private void paintMatchLogo(Graphics graphics, Motif motif, CharSequence sequence, int xoffset, int yoffset, boolean showDirectMotif, boolean paintMotifName) {
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics=g.getFontMetrics(logofont);
        int ascent=(int)(metrics.getAscent()-ascentCorrection+0.5);
        double xpos=xoffset;
        if (sequence!=null) {
            Font oldfont=graphics.getFont();
            double[][] matrix=motif.getMatrix();
            if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();
            double[] letterXoffset=new double[4];
            if (matrix==null) return;
            int widthA=metrics.charWidth('A');
            int widthC=metrics.charWidth('C');
            int widthG=metrics.charWidth('G');
            int widthT=metrics.charWidth('T');
            letterXoffset[0]=(widthG-widthA)/2;
            letterXoffset[1]=(widthG-widthC)/2;
            letterXoffset[3]=(widthG-widthT)/2;
            int logowidth=matrix.length*widthG;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(logofont);
            g.setColor(java.awt.Color.RED);
            AffineTransform restore=g.getTransform();
            AffineTransform save=g.getTransform();
            AffineTransform scaleY = new AffineTransform();
            g.setColor(Color.WHITE);
            g.fillRect(xoffset, yoffset, logowidth, ascent);
            for (int i=0;i<matrix.length;i++) {
                int pos=(showDirectMotif)?i:(matrix.length-(i+1));
                char baseAtPosition=Character.toUpperCase(sequence.charAt(i)); // note that the matching sequence har already been reversed when needed
                double[] counts;
                if (showDirectMotif) counts=new double[]{matrix[pos][0],matrix[pos][1],matrix[pos][2],matrix[pos][3]};
                else counts=new double[]{matrix[pos][3],matrix[pos][2],matrix[pos][1],matrix[pos][0]};
                double total=counts[0]+counts[1]+counts[2]+counts[3];
                double ic=Motif.calculateColumnIC(counts[0], counts[1], counts[2], counts[3], false);
                int[] sorted=new int[4]; // sorted in ascending order. Values are base-indices (i.e. 0=>A, 1=>C, 2=>G, 3=>T)
                int indexA=0, indexC=0, indexG=0, indexT=0;
                if (counts[0]>=counts[1]) indexA++;
                if (counts[0]>=counts[2]) indexA++;
                if (counts[0]>=counts[3]) indexA++;
                if (counts[1]>counts[0]) indexC++;
                if (counts[1]>=counts[2]) indexC++;
                if (counts[1]>=counts[3]) indexC++;
                if (counts[2]>counts[0]) indexG++;
                if (counts[2]>counts[1]) indexG++;
                if (counts[2]>=counts[3]) indexG++;
                if (counts[3]>counts[0]) indexT++;
                if (counts[3]>counts[1]) indexT++;
                if (counts[3]>counts[2]) indexT++;
                sorted[indexA]=0;
                sorted[indexC]=1;
                sorted[indexG]=2;
                sorted[indexT]=3;
                double currentYoffset=yoffset+ascent-(ascent*ic/2f);
                for (int j=3;j>=0;j--) { // draws letters from top to bottom (most frequent first)
                    int base=sorted[j];
                    double fraction=counts[base]/total;
                    scaleY.setTransform(save);
                    scaleY.translate(letterXoffset[base],currentYoffset); // translated in 1-to-1 scale
                    scaleY.scale(1,ic/2f); // scale by IC-content in position
                    scaleY.scale(1, fraction);
                    if (baseAtPosition==bases[base]) {
                        g.setColor(basecolors[base]);
                    }
                    else g.setColor(Color.LIGHT_GRAY);
                    g.setTransform(scaleY);
                    g.drawString(""+bases[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                    currentYoffset+=ascent*fraction*(ic/2f);
                } // end for each base
                xpos+=widthG; //
            } // end for each position
            g.setTransform(save);
            g.setColor(Color.BLACK);
            g.drawRect(xoffset,yoffset,logowidth,ascent);
            g.setTransform(restore);
            g.setFont(oldfont);
        }
        if (paintMotifName) {
            g.setColor(Color.BLACK);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String motiftext=motif.getPresentationName()+"   ("+motif.getLength()+" bp)";
            g.drawString(motiftext, (int)(xpos+5), yoffset+ascent-4);
        }
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }


   /** Paints a small graphical representation of the module (with motifs rendered as boxes) */
   public void paintModuleLogo(Graphics2D graphics, Module module, int xoffset, int yoffset) {
       Object alias=graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
       int size=module.getCardinality();
       int x=xoffset;
       int y=yoffset+6;
       int boxspacing=19; // Note duplicate in getModuleLogoWidth(). This should be 18 but must be 19 because of drawRect drawing "outside"
       FontMetrics metrics=graphics.getFontMetrics(modulemotiffont);
       int boxheight=metrics.getHeight()+2;
       Font oldfont=graphics.getFont();
       graphics.setFont(modulemotiffont);
       for (int i=0;i<size;i++) {
           if (i>0) {
               if (module.isOrdered()) { // draw distance constraint
                   int[] distance=module.getDistance(i-1, i);
                   //graphics.setColor((distance!=null)?Color.RED:Color.BLACK);
                   graphics.setColor(Color.BLACK);
                   // draw lines between boxes;
                   int starty=(int)(y+(boxheight/2))+1;
                   graphics.drawLine(x+1, starty, (int)(x+boxspacing/2)+1, starty-4);
                   graphics.drawLine((int)(x+boxspacing/2), starty-4, x+boxspacing-1, starty);
                   if (distance!=null) { // distance constrained symbol
                       int xd=x+6;
                       int yd=y-2;
                       graphics.drawLine(xd,yd,xd,yd+4); // vertical left
                       graphics.drawLine(xd+7,yd,xd+7,yd+4); // vertical right
                       graphics.drawLine(xd,yd,xd+1,yd); // serif top left
                       graphics.drawLine(xd,yd+4,xd+1,yd+4); // serif bottom left
                       graphics.drawLine(xd+6,yd,xd+7,yd); // serif top right
                       graphics.drawLine(xd+6,yd+4,xd+7,yd+4); // serif bottom right

                   }
               }
               x+=boxspacing;
           }
           ModuleMotif mm=module.getSingleMotif(i);
           String mmName=mm.getRepresentativeName();
           int namewidth=metrics.stringWidth(mmName);
           int boxwidth=namewidth+6; // 3px border on each side
           Color mmColor=settings.getFeatureColor(module.getName()+"."+mmName);
           boolean isDark=VisualizationSettings.isDark(mmColor);
           graphics.setColor(mmColor);
           graphics.fillRect(x, y, boxwidth, boxheight);
           graphics.setColor(Color.BLACK);
           graphics.drawRect(x, y, boxwidth, boxheight);
           if (isDark) graphics.setColor(Color.WHITE); // else black (current)
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           graphics.drawString(mmName, x+4, y+boxheight-4);
           graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, alias);
           int orientation=mm.getOrientation();
           if (orientation!=Module.INDETERMINED) {
              graphics.setColor((orientation==Module.DIRECT)?darkGreen:Color.RED);
              graphics.drawLine(x, y-3, x+boxwidth, y-3);
              graphics.drawLine(x, y-4, x+boxwidth, y-4);
              int l1=((orientation==Module.DIRECT))?x+boxwidth-2:x+2; // larger line
              int l2=((orientation==Module.DIRECT))?x+boxwidth-1:x+1; // smaller line
              graphics.drawLine(l1, y-6, l1, y-1);
              graphics.drawLine(l2, y-5, l2, y-2);
           }
           x+=boxwidth;
       }
       graphics.setFont(oldfont);
   }
}
