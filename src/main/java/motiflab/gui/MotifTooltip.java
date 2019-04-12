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
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;
import motiflab.engine.data.Motif;
import motiflab.engine.data.Region;
import motiflab.engine.data.RegionSequenceData;
import motiflab.engine.data.Sequence;

/**
 * This class is used to draw tooltips that appear when hoovering over binding sites in motif tracks (and module tracks) in normal contracted view. 
 * The class 'ExpandedViewMotifTooltip' is used as a utility class to draw tooltips when a single module region is targetet.
 * @author kjetikl
 */
public class MotifTooltip extends JToolTip implements MouseMotionListener {
    protected DataTrackVisualizer_Region track=null;
    protected Font logofont;
    protected Font font;
    protected char[] bases=new char[]{'A','C','G','T'};
    protected double[] letterXoffset=new double[4];        
    protected int fontheight=26; // if you change this size you must also change the ascentCorrection below accordingly (but I am not sure about the relationship...)
    protected double ascentCorrection=-2.6;// this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
    protected MotifLabEngine engine;
    protected VisualizationSettings settings;
    protected int modulelogoheight=32;
    protected int positionTextHeight=23;
    protected int lineheight=20;
    protected int xoffset=21;
    protected int lastposition=Integer.MAX_VALUE; // keeps track of the last genomic position. MAX_VALUE is a flag used for illegal values
    protected Dimension lastsize=null; // keeps track of the last tooltip dimension
    protected ArrayList<Region> regions=null;
    protected Color[] basecolors=new Color[4];
    protected ExpandedViewMotifTooltip expandedViewtooltip; // this is used to draw Module tooltips when there is only one module under the mouse pointer
    private int maxLines=20; // maximum number of overlapping regions to display tooltip for
    
    public MotifTooltip(DataTrackVisualizer_Region trackvisualizer, VisualizationSettings settings) {
        this(settings);
        track=trackvisualizer;
        expandedViewtooltip=new ExpandedViewMotifTooltip(trackvisualizer, settings);
        addMouseMotionListener(this); 
    }
    
    protected MotifTooltip(VisualizationSettings settings) {
        logofont=MotifLogo.getLogoFont(fontheight);
        Font currentfont=getFont();
        font=(currentfont!=null)?currentfont:new Font(Font.SERIF,Font.PLAIN,13);
        this.settings=settings;
        engine=settings.getEngine();        
    }
    
    
    @Override
    @SuppressWarnings("unchecked")
    protected void paintComponent(Graphics g) {       
        super.paintComponent(g);
        if (track==null) return;
        if (track.isMotifTrack()) {          
            paintGraphicalTooltip(g);
        } else if (track.isModuleTrack()) {
            if (regions!=null && regions.size()==1) { // if only one module to paint: use expanded view tooltip to show more details
                expandedViewtooltip.paintComponent(g);
            } else {
                paintGraphicalTooltipModules(g);
            }
        } // else: the tooptip text will have been set in DataTrackVisualizer's mouseMoved() method and painted as a regular JToolTip by the super.paintComponent(g) call
    }
  
    
    @SuppressWarnings("unchecked")    
    private void paintGraphicalTooltip(Graphics g) {
           int pos=track.getCurrentGenomicMousePosition();
           RegionSequenceData regiontrack=(RegionSequenceData)track.getTrack();
           if (regions==null || regions.isEmpty()) return;
           int yoffset=positionTextHeight;
           int totalsize=0;
           int leftmost=Integer.MAX_VALUE; // position of leftmost
           Object[] list = new Object[regions.size()];
           int shownorientation=settings.getSequenceOrientation(regiontrack.getSequenceName());
           int motifspainted=0;
           for (int i=0;i<regions.size();i++) { // locate motifs and sequences. Set 'leftmost' variable
               Region region=regions.get(i);
               String sequence=(String)region.getProperty("sequence");
               String motifname=region.getType();
               Data data=engine.getDataItem(motifname);
               if (data!=null && data instanceof Motif && sequence!=null && region.getLength()==((Motif)data).getLength() && region.getLength()==sequence.length()) {
                   if (region.getOrientation()!=shownorientation && sequence!=null) sequence=MotifLabEngine.reverseSequence(sequence);               
                   int start=region.getGenomicStart();
                   if (start<leftmost) leftmost=start;
                   list[i]=new Object[]{region,data,sequence};
               } else {
                   list[i]=region;
               }              
           }
           for (int i=0;i<regions.size();i++) { // set 'longest' variable
               if (list[i] instanceof Region) continue; // the region has no motif association
               Region region=(Region)((Object[])list[i])[0];
               int offset=region.getGenomicStart()-leftmost;
               int total=region.getLength()+offset;
               if (total>totalsize) totalsize=total;
           }  
           // sort the array so that all regions with motifs are first and second according to position (which is orientation dependent!)
           RegionComparator comparator=new RegionComparator(shownorientation==Sequence.DIRECT);
           Arrays.sort(list, comparator);             
           for (int i=0;i<list.length;i++) { // paint info for each region
               Object object=list[i];
               Region region=null;
               Motif motif=null;
               String sequence=null;
               if (object instanceof Region) {
                   region=(Region)object;
               } else {
                   region=(Region)((Object[])object)[0];
                   motif=(Motif)((Object[])object)[1];
                   sequence=(String)((Object[])object)[2];
               }
               int offset=region.getGenomicStart()-leftmost;
               if (shownorientation==Sequence.REVERSE) offset=totalsize-(offset+region.getLength());
               g.setColor(Color.WHITE);
               g.fillRect(1, yoffset, lineheight, lineheight);
               Color featurecolor=settings.getFeatureColor(region.getType());
               g.setColor(featurecolor);
               g.fillRect(6, yoffset+5, 10, 10);
              
               // bevel effect
               g.setColor(featurecolor.darker());
               g.drawLine(7, yoffset+14, 15, yoffset+14);
               g.drawLine(15, yoffset+6, 15, yoffset+14);         
               g.setColor(featurecolor.equals(Color.BLACK)?new Color(106,106,106):motiflab.engine.Graph.brighter(featurecolor));
               g.drawLine(7, yoffset+6, 16, yoffset+6);
               g.drawLine(7, yoffset+6, 7,  yoffset+14);
                        
               g.setColor(Color.BLACK);
               g.drawRect(6, yoffset+5, 10, 10);
               g.drawRect(1, yoffset, lineheight, lineheight);
               int textoffset=xoffset;
               String motiftext=null;
               if (object instanceof Object[]) {
                   motifspainted++;
                   motiftext=getRegionString(region,motif);
                   textoffset=paintMatchLogo(g, motif, sequence,xoffset, yoffset, totalsize, offset, shownorientation, region.getOrientation());
               } else {
                   motiftext=getRegionString(region,null);
               }
               if (motiftext!=null) {
                    g.setColor(Color.BLACK);
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawString(motiftext, textoffset+5, yoffset+lineheight-5);
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                }
               yoffset+=lineheight;
           }
           // draw base cursor 
           if (motifspainted>0) {
               FontMetrics metrics=g.getFontMetrics(logofont); 
               int widthG=metrics.charWidth('G');             
               g.setColor(settings.getBaseCursorColor());
               int currentoffset=pos-leftmost;  
               if (shownorientation==Sequence.REVERSE) currentoffset=totalsize-(currentoffset+1);
               g.drawRect(xoffset+currentoffset*widthG, positionTextHeight+1, widthG, lineheight*motifspainted-2);     
           }  
    }
    
    @SuppressWarnings("unchecked")
    private void paintGraphicalTooltipModules(Graphics g) {
           int pos=track.getCurrentGenomicMousePosition();
           RegionSequenceData regiontrack=(RegionSequenceData)track.getTrack();
           if (regions==null || regions.isEmpty()) return;
           int yoffset=positionTextHeight;
           Object[] list = new Object[regions.size()];
           int shownorientation=settings.getSequenceOrientation(regiontrack.getSequenceName());
           Object targetModule=null; // if all module regions are of the same type, this targetModule will be assigned to that type
           for (int i=0;i<regions.size();i++) { //
               Region region=regions.get(i);
               Data data=engine.getDataItem(region.getType());
               if (data instanceof Module) {
                   if (targetModule==null) targetModule=data;
                   else if (targetModule instanceof Module && !((Module)targetModule).equals((Module)data)) targetModule=""; // this string is just a flag to indicate non-homogenous modules in list
                   list[i]=new Object[]{region,data};
               }
               else list[i]=region;
           }
           if (targetModule instanceof Module) { // make room to draw module logo
               ModuleLogo.paintModuleLogo((Graphics2D)g, (Module)targetModule, 5, yoffset+8, settings, null,0);
               yoffset+=modulelogoheight;
           }
           // sort the array so that all regions with modules are first and second according to position (which is orientation dependent!)
           RegionComparator comparator=new RegionComparator(shownorientation==Sequence.DIRECT);
           Arrays.sort(list, comparator);
           for (int i=0;i<list.length;i++) { // paint info for each region
               Object object=list[i];
               Region region=null;
               Module module=null;
               if (object instanceof Region) {
                   region=(Region)object;
               } else {
                   region=(Region)((Object[])object)[0];
                   module=(Module)((Object[])object)[1];
               }
               g.setColor(settings.getFeatureColor(region.getType()));
               g.fillRect(1, yoffset, lineheight, lineheight);
               String modulemotif=(module!=null)?getMotifAtPosition(region, module, regiontrack.getRelativePositionFromGenomic(pos)):null;
               Color modulemotifcolor=null;
               if (modulemotif!=null) {
                   modulemotifcolor=settings.getFeatureColor(module.getName()+"."+modulemotif);
                   g.setColor(modulemotifcolor);
                   g.fillRect(6, yoffset+5, 10, 10);
                   // bevel effect
                   g.setColor(modulemotifcolor.darker());
                   g.drawLine(7, yoffset+14, 15, yoffset+14);
                   g.drawLine(15, yoffset+6, 15, yoffset+14);         
                   g.setColor(modulemotifcolor.equals(Color.BLACK)?new Color(106,106,106):motiflab.engine.Graph.brighter(modulemotifcolor));
                   g.drawLine(7, yoffset+6, 16, yoffset+6);
                   g.drawLine(7, yoffset+6, 7,  yoffset+14);                   
                   g.setColor(Color.BLACK);
                   if (modulemotif!=null) g.drawRect(6, yoffset+5, 10, 10);
               } else g.setColor(Color.BLACK);
               g.drawRect(1, yoffset, lineheight, lineheight);
               String moduletext=null;
               if (object instanceof Object[]) {
                   moduletext=getRegionStringModule(region,module);
                } else {
                   moduletext=getRegionString(region,null);
               }
               if (moduletext!=null) {
                    g.setColor(Color.BLACK);
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.drawString(moduletext, xoffset+5, yoffset+lineheight-5);
                    ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                }
               yoffset+=lineheight;
           }
    }
        
    
    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        if (track==null || !(track.isMotifTrack() || track.isModuleTrack())) {
           return dim;
        } 
        int pos=track.getCurrentGenomicMousePosition();
        if (pos==lastposition) return lastsize;        
        int width=dim.width;
        int modulelogospace=0;
        regions=track.getVisibleRegions(pos);
        if (regions==null || regions.isEmpty()) {lastsize=dim;return dim;}
        if (track.isMotifTrack()) {
            FontMetrics metrics=getFontMetrics(font);
            int charwidth=getFontMetrics(logofont).charWidth('G');
            int[] stringlengths=new int[regions.size()];
            boolean[] hasmotif=new boolean[regions.size()];
            int leftmost=Integer.MAX_VALUE;
            for (int i=0;i<regions.size();i++) { // determine width of description string (excluding logo)
                Region region=regions.get(i);
                String motifname=region.getType();
                Data data=engine.getDataItem(motifname);
                if (data!=null && data instanceof Motif) {
                   if (region.getLength()==((Motif)data).getLength()) { // is motif-region link valid?
                       hasmotif[i]=true;
                       int start=region.getGenomicStart();
                       if (start<leftmost) leftmost=start;
                   }
                   String string=getRegionString(region,(Motif)data);
                   stringlengths[i]=metrics.stringWidth(string);
                } else { // region is not a motif
                  String string=getRegionString(region,null);
                  stringlengths[i]=metrics.stringWidth(string);
                }
            }
            int totalsize=0;
            for (int i=0;i<regions.size();i++) { // set 'longest' variable
                   if (!hasmotif[i]) continue; // the region has no motif association
                   Region region=regions.get(i);
                   int offset=region.getGenomicStart()-leftmost;
                   int total=region.getLength()+offset;
                   if (total>totalsize) totalsize=total;
            }
            int drawingsize=xoffset+totalsize*charwidth+10; //
            for (int i=0;i<regions.size();i++) {
                 int linewidth=stringlengths[i]+drawingsize;
                 if (linewidth>width) width=linewidth;
            }
        } else { // ModuleTrack!
            if (regions.size()==1) return expandedViewtooltip.getPreferredSize(); // this tooltip will be painted instead for single modules
            FontMetrics metrics=getFontMetrics(font);
            Object targetModule=null;
            for (int i=0;i<regions.size();i++) { // determine width of description string (excluding logo)
                Region region=regions.get(i);
                Data data=engine.getDataItem(region.getType());
                if (data!=null && data instanceof Module) {
                   if (targetModule==null) targetModule=data;
                   else if (targetModule instanceof Module && !((Module)targetModule).equals((Module)data)) targetModule=""; // this string is just a flag to indicate non-homogenous modules in list
                   String string=getRegionStringModule(region,(Module)data);
                   int linewidth=metrics.stringWidth(string)+xoffset+10;
                   if (linewidth>width) width=linewidth;
                } else { // region is not a motif
                   String string=getRegionString(region,null);
                   int linewidth=metrics.stringWidth(string)+xoffset+10;
                   if (linewidth>width) width=linewidth;
                }
            }
            if (targetModule instanceof Module) { // make room to draw module logo
               Font modulemotiffont=ModuleLogo.getModuleMotifFont();
               FontMetrics modulelogometrics=this.getFontMetrics(modulemotiffont);
               int logowidth=ModuleLogo.getLogoWidth(modulelogometrics,(Module)targetModule)+10;
               if (logowidth>width) width=logowidth;
               modulelogospace=modulelogoheight;
            }
        }
        Dimension newsize=new Dimension(width,modulelogospace+lineheight*regions.size()+positionTextHeight+2);
        lastsize=newsize;
        lastposition=pos;
        return newsize;
    }

    /**
     * Returns a string that describes this region
     * If a corresponding Motif object is supplied, the the returned name of the Region 
     * will be be presentation name of the motif. If motif==null then the returned name
     * will just be the type of the region 
     */
    private String getRegionString(Region region, Motif motif) {
        String orientation;
        if (region.getOrientation()==Region.DIRECT) orientation="+";
        else if (region.getOrientation()==Region.REVERSE) orientation="\u2013"; // &ndash;
        else orientation=".";
        String type=(motif!=null)?motif.getPresentationName():region.getType();
        if (type==null) type="UNKNOWN";
        StringBuilder builder=new StringBuilder();
        builder.append(type);
        builder.append(" [");
        builder.append(orientation);
        builder.append("], ");
        builder.append(region.getLength());
        builder.append(" bp, score=");
        builder.append(MotifLabGUI.formatNumber(region.getScore()));
        return builder.toString();
    }

    /**
     * Returns a string that describes this region
     * If a corresponding Motif object is supplied, the the returned name of the Region
     * will be be presentation name of the motif. If motif==null then the returned name
     * will just be the type of the region
     */
    private String getRegionStringModule(Region region, Module module) {
        String orientation;
        if (region.getOrientation()==Region.DIRECT) orientation="+";
        else if (region.getOrientation()==Region.REVERSE) orientation="\u2013"; // &ndash;
        else orientation=".";
        String type=region.getType();
        if (type==null) type="UNKNOWN";
        int[] stats=getModuleMotifRegionFullCount(region,module);
        int cardinality=module.getCardinality();
        int present=stats[1];
        int multiple=stats[3];
        StringBuilder builder=new StringBuilder();
        builder.append(type);
        builder.append(", Motifs=");
        builder.append(present);
        builder.append((multiple>0)?"*":"");
        builder.append("/");
        builder.append(cardinality);
        builder.append(", [");
        builder.append(orientation);
        builder.append("], ");
        builder.append(region.getLength());
        builder.append(" bp, score=");
        builder.append(MotifLabGUI.formatNumber(region.getScore()));
        return builder.toString();
    }

    private String getMotifAtPosition(Region region, Module module, int pos) {
        for (String modulemotifname:module.getSingleMotifNames()) {
           Object mm=region.getProperty(modulemotifname);
           if (mm instanceof Region) {
               int start=((Region)mm).getRelativeStart();
               int end=((Region)mm).getRelativeEnd();
               if (pos>=start && pos<=end) return modulemotifname; // this module motif is at the position
           } else if (mm instanceof ArrayList) {
               for (Object o:((ArrayList)mm)) {
                    if (o instanceof Region) {
                       int start=((Region)o).getRelativeStart();
                       int end=((Region)o).getRelativeEnd();
                       if (pos>=start && pos<=end) return modulemotifname; // this module motif is at the position
                   }
               }
           }
        }
        return null;
    }

    protected int paintMatchLogo(Graphics graphics, Motif motif, CharSequence region, int xoffset, int yoffset, int size, int offset, int shownorientation, int motiforientation) {
        double[][] matrix=motif.getMatrix();
        if (matrix==null) return 0;
        basecolors[0]=settings.getBaseColor('A');
        basecolors[1]=settings.getBaseColor('C');
        basecolors[2]=settings.getBaseColor('G');
        basecolors[3]=settings.getBaseColor('T');
        if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();        
        Graphics2D g = (Graphics2D)graphics;
        FontMetrics metrics=g.getFontMetrics(logofont);
        int ascent=(int)(metrics.getAscent()-ascentCorrection+0.5);
        int widthA=metrics.charWidth('A');     
        int widthC=metrics.charWidth('C');     
        int widthG=metrics.charWidth('G');     
        int widthT=metrics.charWidth('T');
        letterXoffset[0]=(widthG-widthA)/2;
        letterXoffset[1]=(widthG-widthC)/2;
        letterXoffset[3]=(widthG-widthT)/2;
        // note that letterXoffset[2] is deliberately left at 0
        int logowidth=matrix.length*widthG;
        int fullwidth=size*widthG;
        Font oldfont=g.getFont();
        g.setFont(logofont);
        g.setColor(java.awt.Color.RED);
        AffineTransform restore=g.getTransform();
        AffineTransform save=g.getTransform();           
        if (logowidth>getWidth()) save.scale((double)getWidth()/(double)logowidth, 1); // scale X-direction so that logo fits irrespective of size
        AffineTransform scaleY = new AffineTransform();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(xoffset, yoffset, fullwidth, ascent);
        g.setColor(Color.WHITE);
        g.fillRect(xoffset+offset*widthG, yoffset, logowidth, ascent);
        double xpos=xoffset+offset*widthG;
        boolean showDirectMotif=(shownorientation==motiforientation);
        if (settings.useMotifAntialiasing()) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i=0;i<matrix.length;i++) {
            int pos=(showDirectMotif)?i:(matrix.length-(i+1));
            char baseAtPosition=(region==null)?'\n':Character.toUpperCase(region.charAt(i)); // note that the matching sequence has already been reversed when needed. '\n' is used as a flag to indicate that sequence match should not be used           
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
                if (baseAtPosition=='\n' || baseAtPosition==bases[base]) {
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
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.drawRect(xoffset,yoffset,fullwidth,ascent);
        g.setTransform(restore);
        g.setFont(oldfont);
        return xoffset+fullwidth+1;
    }     

    /** Clears any cached data for this tooltip */
    public void clearData() {
       lastposition=Integer.MAX_VALUE;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
       lastposition=Integer.MAX_VALUE;
    }

    int trap=-1; // this is used to end recursive loops
    @Override
    public void mouseMoved(MouseEvent e) { // this method is included to allow the tooltip to be updated when the mouse moves over it (in which case the underlying component is not normally notified)
       int pos=track.getCurrentGenomicMousePosition();
       track.mouseMoved(SwingUtilities.convertMouseEvent(this, e, track));
       if (pos!=trap) {
           javax.swing.ToolTipManager tpm=javax.swing.ToolTipManager.sharedInstance();
           tpm.setEnabled(false);
           tpm.setEnabled(true);
       }
       trap=pos;
       lastposition=Integer.MAX_VALUE;
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
    
    public class RegionComparator implements Comparator {
        boolean direct=true;
        public RegionComparator(boolean direct) {
            this.direct=direct;
        }
        @Override
        public int compare(Object o1, Object o2) {
                 if (o1 instanceof Object[] && o2 instanceof Region) return -1;
            else if (o1 instanceof Region && o2 instanceof Object[]) return 1;
            else {
                  Region region1=null;
                  Region region2=null;
                  if (o1 instanceof Region && o2 instanceof Region) { 
                      region1=(Region)o1;
                      region2=(Region)o2;
                  } else if (o1 instanceof Object[] && o2 instanceof Object[]) {
                      region1=(Region)((Object[])o1)[0];
                      region2=(Region)((Object[])o2)[0];                 
                  } else return 0; // this should not happen    
                  if (direct) {
                       int start1=region1.getRelativeStart();
                       int start2=region2.getRelativeStart();
                       if (start1<start2) return -1;
                       else if (start2>start1) return 1;
                       else return 0;
                  } else { // reverse
                       int end1=region1.getRelativeEnd();
                       int end2=region2.getRelativeEnd();
                       if (end1>end2) return -1;
                       else if (end1<end2) return 1;
                       else return 0;                       
                  }
             } // end same type object                
        } // end compare              
    } // end RegionComparator
    
}
