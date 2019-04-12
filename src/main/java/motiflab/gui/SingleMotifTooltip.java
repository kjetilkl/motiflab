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
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JToolTip;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.Data;
import motiflab.engine.data.Module;
import motiflab.engine.data.ModuleMotif;
import motiflab.engine.data.Motif;
import motiflab.engine.data.MotifCollection;

/**
 * This tooltip is used when the mouse is over Motifs (or Modules or ModuleMotifs) in the MotifsPanel
 * (it is also used by Prompt_Module to show tooltips for ModuleMotifs)
 * @author kjetikl
 */
public class SingleMotifTooltip extends JToolTip {
    private char[] bases=new char[]{'A','C','G','T'};
    private static final int fontheight=26; // if you change this size you must also change the ascentCorrection below accordingly (but I am not sure about the relationship...)
    private static final double ascentCorrection=-2.6; // this is a needed hack because the ascent returned by the FontMetrics is not really the same height as that of a capital letter (even though that was stated in the documentation)
    private static final int lineheight=20;
    private static final int xoffset=4;
    private static final Color darkGreen=new Color(0,200,0);
    private Color[] basecolors=new Color[4];
    private MotifLabEngine engine;
    private DecimalFormat formatter=new DecimalFormat("#.###");
    private VisualizationSettings settings;
    private static Font logofont=MotifLogo.getLogoFont(fontheight);
    private static Font modulemotiffont = new Font(Font.SANS_SERIF, Font.BOLD, 11);
    
    
    public SingleMotifTooltip(VisualizationSettings settings) {
        this.settings=settings;
        engine=settings.getEngine();
        //basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};
    }

    private String getMotifDescription(Motif motif) {
        String longname=motif.getLongName();
        if (longname==null || longname.isEmpty()) longname=motif.getShortName(); 
        StringBuilder builder=new StringBuilder();
        builder.append("<html><nobr><b>");
        builder.append(motif.getName());
        builder.append("</b>");
        if (longname!=null && !longname.isEmpty()) {
            builder.append(" &ndash; ");
            builder.append(longname);
        }
        builder.append("</nobr><br>");
        builder.append(motif.getLength());
        builder.append("&nbsp;bp&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;IC:&nbsp;");
        builder.append(formatter.format(motif.getICcontent()));
        builder.append("</html>");
        return builder.toString();
    }

    private String getModuleDescription(Module module) {
        StringBuilder builder=new StringBuilder();
        builder.append("<html><nobr><b>");
        builder.append(module.getName());
        builder.append("</b>&nbsp;&nbsp;&nbsp;(");
        builder.append(module.getCardinality());
        builder.append(" component motif");
        if (module.getCardinality()!=1) builder.append("s");
        builder.append(")</nobr></html>");
        return builder.toString();
    }

   private String getModuleMotifDescription(ModuleMotif modulemotif, int size, String modulename) {
        StringBuilder builder=new StringBuilder();
        builder.append("<html><nobr>");
        builder.append(modulename);
        builder.append(" : <b>");
        builder.append(modulemotif.getRepresentativeName());
        builder.append("</b> (");
        builder.append(size);
        builder.append(" motif model");
        if (size!=1) builder.append("s");
        builder.append(")</nobr></html>");
        return builder.toString();
    }

   private String getModuleMotifDescription(ArrayList<Motif> motiflist) {
        StringBuilder builder=new StringBuilder();
        builder.append("<html>");
        if (motiflist!=null) {
            int i=0;
            for (Motif motif:motiflist) {
                if (i>0) builder.append("<br>");
                builder.append(motif.getPresentationName());
                builder.append("&nbsp;&nbsp;&nbsp;(");
                builder.append(motif.getLength());
                builder.append(" bp)");
                i++;
            }
        }
        builder.append("</html>");
        return builder.toString();
    }
            
    @Override
    @SuppressWarnings("unchecked")
    protected void paintComponent(Graphics g) {
       String oldtiptext=getTipText();
       Object data=engine.getDataItem(oldtiptext);
       if (data!=null && data instanceof Motif) {
             setTipText(getMotifDescription((Motif)data));
             super.paintComponent(g);    
             Dimension dim=getPreferredSize();
             setTipText(oldtiptext);
             paintSequenceLogo(g, (Motif)data, xoffset, dim.height, 0, false);
       } else if (data instanceof Module) {
            setTipText(getModuleDescription((Module)data));
            super.paintComponent(g);
            Dimension dim=getPreferredSize();
            setTipText(oldtiptext);
            int logoYoffset=dim.height+6; // "approx"
            paintModuleLogo((Graphics2D)g, (Module)data, xoffset, logoYoffset);
            int size=((Module)data).getCardinality();
            FontMetrics mmMetrics=getFontMetrics(modulemotiffont);
            int boxheight=mmMetrics.getHeight()+2;            
            int currentY=logoYoffset+boxheight+24; // 24 is for margins and padding
            for (int i=0;i<size;i++) {
                ModuleMotif mm=((Module)data).getSingleMotif(i);
                Motif motif=mm.getFirstMotifInCollection(engine);
                if (motif==null) continue; // do not draw if no motifs are present (this has been accounted for in preferred size)
                   Color mmColor=settings.getFeatureColor(((Module)data).getName()+"."+mm.getRepresentativeName());
                   g.setColor(mmColor);
                   g.fillRect(xoffset, currentY, lineheight, lineheight);
                   g.setColor(settings.getFeatureColor(motif.getName()));
                   g.fillRect(xoffset+5, currentY+5, 10, 10);
                   g.setColor(Color.BLACK);
                   g.drawRect(xoffset+5, currentY+5, 10, 10);
                   g.drawRect(xoffset, currentY, lineheight, lineheight);
                   paintSequenceLogo(g, motif, xoffset+lineheight, currentY, 0, true);
                   currentY+=(lineheight+6);
            }
       } else if (oldtiptext!=null && oldtiptext.startsWith("#MM#")) { // flag for modulemotifs: #MM#<modulename>:<modulemotifname>
           int colonpos=oldtiptext.indexOf(':');
           String modulename=oldtiptext.substring(4,colonpos);
           String modulemotifname=oldtiptext.substring(colonpos+1,oldtiptext.length());
           Data module=engine.getDataItem(modulename);
           if (module instanceof Module) {
               ModuleMotif modulemotif=((Module)module).getModuleMotif(modulemotifname);
               MotifCollection motifs=modulemotif.getMotifAsCollection();
               setTipText(getModuleMotifDescription(modulemotif, motifs.size(),modulename));
               super.paintComponent(g);
               Dimension dim=getPreferredSize();
               setTipText(oldtiptext);
               int currentY=dim.height;
               ArrayList<Motif> motiflist=motifs.getAllMotifs(engine);
               int maxmotifsize=0;
               for (Motif motif:motiflist) {
                   if (motif.getLength()>maxmotifsize) maxmotifsize=motif.getLength();
               }
               Object alias=((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
               ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                   for (Motif motif:motiflist) {
                   g.setColor(Color.WHITE);
                   g.fillRect(xoffset, currentY, lineheight, lineheight);
                   g.setColor(settings.getFeatureColor(motif.getName()));
                   g.fillRect(xoffset+5, currentY+5, 10, 10);
                   g.setColor(Color.BLACK);
                   g.drawRect(xoffset+5, currentY+5, 10, 10);
                   g.drawRect(xoffset, currentY, lineheight, lineheight);
                   paintSequenceLogo(g, motif, xoffset+lineheight, currentY, maxmotifsize, true);
                   currentY+=lineheight;
               }
               ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, alias);
           } else super.paintComponent(g);
       } else if (oldtiptext!=null && oldtiptext.startsWith("#M#") && !oldtiptext.equals("#M#")) { // flag for list of motif names: #M#<modulemotifname>:motif1,motif2,...,motifN
           String modulemotifname=null;
           int colonpos=oldtiptext.indexOf(':');
           if (colonpos>=0) {
                modulemotifname=oldtiptext.substring(3,colonpos);
           } else colonpos=3;
           String[] motifnames=(oldtiptext.length()>colonpos+1)?oldtiptext.substring(colonpos+1).split(","):new String[0];
           ArrayList<Motif> motiflist=new ArrayList<Motif>();
           for (String motifname:motifnames) {
                Data motif=engine.getDataItem(motifname);
                if (motif instanceof Motif) motiflist.add((Motif)motif);
           }
           int motiflistsize=motiflist.size();
           if (modulemotifname!=null) setTipText(modulemotifname+" ("+motiflistsize+ " motif model"+((motiflistsize!=1)?"s":"")+")");
           else setTipText(motiflistsize + " motif model"+((motiflistsize!=1)?"s":""));
           super.paintComponent(g);
           Dimension dim=getPreferredSize();
           setTipText(oldtiptext); // this must be here!
           int currentY=dim.height;
           int maxmotifsize=0;
           for (Motif motif:motiflist) {
               if (motif.getLength()>maxmotifsize) maxmotifsize=motif.getLength();
           }
           Object alias=((Graphics2D)g).getRenderingHint(RenderingHints.KEY_ANTIALIASING);
           ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
           for (Motif motif:motiflist) {
               g.setColor(Color.WHITE);
               g.fillRect(xoffset, currentY, lineheight, lineheight);
               g.setColor(settings.getFeatureColor(motif.getName()));
               g.fillRect(xoffset+5, currentY+5, 10, 10);
               g.setColor(Color.BLACK);
               g.drawRect(xoffset+5, currentY+5, 10, 10);
               g.drawRect(xoffset, currentY, lineheight, lineheight);
               paintSequenceLogo(g, motif, xoffset+lineheight, currentY, maxmotifsize, true);
               currentY+=lineheight;
           }
           ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,alias);

       } else super.paintComponent(g);
    }
    
       
    @Override
    public Dimension getPreferredSize() {
        Dimension dim=super.getPreferredSize();
        String oldtiptext=getTipText();
        Data data=engine.getDataItem(oldtiptext);
        if (data instanceof Motif) {
            JLabel testlabel=new JLabel(getMotifDescription((Motif)data));
            testlabel.setFont(getFont());
            Dimension labeldim=testlabel.getPreferredSize();        
            int width=labeldim.width+9;
            int charwidth=getFontMetrics(logofont).charWidth('G');
            int logowidth=charwidth*((Motif)data).getLength()+10; // 10 is margin
            return new Dimension((width>logowidth)?width:logowidth,labeldim.height+lineheight+14); // 12 is just a margin
        } if (data instanceof Module) {
            int size=((Module)data).getCardinality();
            JLabel testlabel=new JLabel(getModuleDescription((Module)data));
            testlabel.setFont(getFont());
            Dimension labeldim=testlabel.getPreferredSize();
            int width=labeldim.width+9;
            int modulewidth=getModuleLogoWidth((Module)data)+xoffset+9;
            if (modulewidth>width) width=modulewidth;
            FontMetrics metrics=getFontMetrics(getFont());
            FontMetrics mmMetrics=getFontMetrics(modulemotiffont);
            int boxheight=mmMetrics.getHeight()+2;
            int height=labeldim.height+boxheight+40; // 40 is margin/padding. this number will be added to later
            int maxmotiflinewidth=0;
            for (int i=0;i<size;i++) {
                ModuleMotif mm=((Module)data).getSingleMotif(i);
                Motif motif=mm.getFirstMotifInCollection(engine);
                if (motif==null) continue;
                int motiflength=motif.getLength();
                int logowidth=getFontMetrics(logofont).charWidth('G')*motiflength; // 10 is margin
                int namewidth=metrics.stringWidth(motif.getPresentationName()+"   ("+motif.getLength()+" bp)");
                int motifwidth=lineheight+logowidth+5+namewidth+10;
                if (motifwidth>maxmotiflinewidth) maxmotiflinewidth=motifwidth;
                height+=(lineheight+6);
            }
            return new Dimension((width>maxmotiflinewidth)?width:maxmotiflinewidth,height);
            //return dim;
        } if (oldtiptext!=null && oldtiptext.startsWith("#MM#")) { // flag for modulemotif
            int colonpos=oldtiptext.indexOf(':');
            String modulename=oldtiptext.substring(4,colonpos);
            String modulemotifname=oldtiptext.substring(colonpos+1,oldtiptext.length());
            Data module=engine.getDataItem(modulename);
            if (!(module instanceof Module)) return dim;
            ModuleMotif modulemotif=((Module)module).getModuleMotif(modulemotifname);
            MotifCollection motifs=modulemotif.getMotifAsCollection();
            ArrayList<Motif> motiflist=motifs.getAllMotifs(engine);
            int maxmotifsize=0;
            for (Motif motif:motiflist) {
                if (motif.getLength()>maxmotifsize) maxmotifsize=motif.getLength();
            }
            JLabel testlabel=new JLabel(getModuleMotifDescription(modulemotif, motifs.size(),modulename));
            testlabel.setFont(getFont());
            Dimension headerlabeldim=testlabel.getPreferredSize();
            testlabel.setText(getModuleMotifDescription(motiflist));
            Dimension motifslabeldim=testlabel.getPreferredSize();
            int width=headerlabeldim.width+9;
            int charwidth=getFontMetrics(logofont).charWidth('G');
            int logowidth=charwidth*maxmotifsize+lineheight+15+motifslabeldim.width; // lineheight is size of motif-color rectangle, 10 is margin
            return new Dimension((width>logowidth)?width:logowidth,headerlabeldim.height+lineheight*motifs.size()+14); // 12 is just a margin
        } else if (oldtiptext!=null && oldtiptext.startsWith("#M#") && !oldtiptext.equals("#M#")) { // flag for list of single motifs
            String modulemotifname=null;
            int colonpos=oldtiptext.indexOf(':');
            if (colonpos>=0) {
                modulemotifname=oldtiptext.substring(3,colonpos);
            } else colonpos=3;
            String[] motifnames=(oldtiptext.length()>colonpos+1)?oldtiptext.substring(colonpos+1).split(","):new String[0];
            ArrayList<Motif> motiflist=new ArrayList<Motif>();
            for (String motifname:motifnames) {
                Data motif=engine.getDataItem(motifname);
                if (motif instanceof Motif) motiflist.add((Motif)motif);
            }
            int maxmotifsize=0;
            for (Motif motif:motiflist) {
                if (motif.getLength()>maxmotifsize) maxmotifsize=motif.getLength();
            }
            String header;
            int motiflistsize=motiflist.size();
            if (modulemotifname!=null) header=(modulemotifname+" ("+motiflistsize+ " motif model"+((motiflistsize!=1)?"s":"")+")");
            else header=(motiflistsize + " motif model"+((motiflistsize!=1)?"s":"")+")");
            JLabel testlabel=new JLabel(header);
            testlabel.setFont(getFont());
            Dimension headerlabeldim=testlabel.getPreferredSize();
            testlabel.setText(getModuleMotifDescription(motiflist));
            Dimension motifslabeldim=testlabel.getPreferredSize();
            int width=headerlabeldim.width+9;
            int charwidth=getFontMetrics(logofont).charWidth('G');
            int logowidth=charwidth*maxmotifsize+lineheight+15+motifslabeldim.width; // lineheight is size of motif-color rectangle, 10 is margin
            return new Dimension((width>logowidth)?width:logowidth,headerlabeldim.height+lineheight*motiflist.size()+14); // 12 is just a margin
        } else return dim;
    }

    
    private void paintSequenceLogo(Graphics graphics, Motif motif, int xoffset, int yoffset, int maxsize, boolean paintMotifName) {
        basecolors[0]=settings.getBaseColor('A');
        basecolors[1]=settings.getBaseColor('C');
        basecolors[2]=settings.getBaseColor('G');
        basecolors[3]=settings.getBaseColor('T');
        Font oldfont=graphics.getFont();
        double[][] matrix=motif.getMatrix();
        if (motif.getMatrixType()==Motif.LOG_ODDS_MATRIX) matrix=motif.getMatrixAsFrequencyMatrix();        
        double[] letterXoffset=new double[4];                
        if (matrix==null) return;
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
        int logowidth=matrix.length*widthG;
        g.setFont(logofont);
        g.setColor(java.awt.Color.RED);
        AffineTransform restore=g.getTransform();
        AffineTransform save=g.getTransform();           
        AffineTransform scaleY = new AffineTransform();
        if (maxsize>0 && maxsize>matrix.length) { // draw gray background
            g.setColor(Color.DARK_GRAY);
            g.fillRect(xoffset, yoffset, maxsize*widthG, ascent);
            g.setColor(Color.BLACK);
            g.drawRect(xoffset, yoffset, maxsize*widthG, ascent);
        }
        g.setColor(Color.WHITE);
        g.fillRect(xoffset, yoffset, logowidth, ascent);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double xpos=xoffset;
        for (int i=0;i<matrix.length;i++) {
            double[] counts=new double[]{matrix[i][0],matrix[i][1],matrix[i][2],matrix[i][3]};
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
                g.setColor(basecolors[base]);               
                g.setTransform(scaleY);
                g.drawString(""+bases[base], (float)xpos, (float)ascent); // draw all letters at same position and use transform to place them correctly
                currentYoffset+=ascent*fraction*(ic/2f);
            } // end for each base  
            xpos+=widthG; // 
        } // end for each position
        g.setTransform(save);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setColor(Color.BLACK);
        g.drawRect(xoffset,yoffset,logowidth,ascent);
        g.setTransform(restore);
        g.setFont(oldfont);
        if (paintMotifName) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int textXpos=xoffset+((maxsize>0 && maxsize>matrix.length)?(maxsize*widthG):logowidth);
            String motiftext=motif.getPresentationName()+"   ("+motif.getLength()+" bp)";
            g.drawString(motiftext, textXpos+5, yoffset+ascent-4);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

    }

   private int getModuleLogoWidth(Module module) {
       FontMetrics metrics=getFontMetrics(modulemotiffont);
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

   /** Paints a small graphical representation of the module (with motifs rendered as boxes) */
   private void paintModuleLogo(Graphics2D graphics, Module module, int xoffset, int yoffset) {
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
