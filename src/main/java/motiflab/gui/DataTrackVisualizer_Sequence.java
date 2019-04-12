/*
 
 
 */

package motiflab.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.SwingUtilities;
import motiflab.engine.MotifLabEngine;
import motiflab.engine.data.DNASequenceData;
import motiflab.engine.data.Sequence;


/**
 * This is the abstract superclass for DataTrackVisualizers used to render DNA Sequence Dataset tracks
 * @author kjetikl
 */
public abstract class DataTrackVisualizer_Sequence extends DataTrackVisualizer {
          
    /** Constructs a new DefaultDataTrackVisualizer */
    public DataTrackVisualizer_Sequence() {
        super();
    }

    @Override    
    public void initialize(String sequenceName, String featureName, VisualizationSettings settings)  {
       super.initialize(sequenceName, featureName, settings);
       if (sequencedata!=null && !(sequencedata instanceof DNASequenceData)) {System.err.println("WARNING: DataTrackVisualizer_Sequence is used to render data of type:"+sequencedata.getClass().toString());}      
    } 
    
    @Override
    public void describeBaseForTooltip(int genomicCoordinate, int shownOrientation, StringBuilder buffer) {
        if (sequencedata==null) return;
        Object value=sequencedata.getValueAtGenomicPosition(genomicCoordinate);    
        if (value instanceof Character) { 
              char base=((Character)value).charValue();
              buffer.append("DNA-base = ");
              if (shownOrientation==VisualizationSettings.REVERSE) {
                  buffer.append(reverseBase(base));
                  buffer.append(" (reverse)");
              } else {
                  buffer.append(base);
                  buffer.append(" (direct)");
              }
        } else if (value==null) {
              if (genomicCoordinate>=sequenceStart && genomicCoordinate<=sequenceEnd) buffer.append("NULL!");
              else buffer.append("Outside sequence:  "+sequenceStart+"-"+sequenceEnd+"  ("+(sequenceEnd-sequenceStart+1)+" bp)");            
        }        
    }    
    
    
    @Override
    public boolean optimizeForSpeed(double scale) {
        return scale<optimizationThreshold;
    }    
    
    /*
     * This default implementation draws a solid color background corresponding to the visible sequence segment
     */
    @Override    
    public void drawVisibleSegmentBackground(Graphics2D graphics, int start, int height,int width, int bases, int graphicsXoffset, int graphicsYoffset, int orientation, boolean optimize) {      
        Color background=settings.getBackGroundColor(featureName);
        if (background==null) return;
        graphics.setColor(background);
        AffineTransform saveTransform=graphics.getTransform();
        graphics.setTransform(saveAt); // restores 'original state' (saveAt is set in superclass)
        double scale=settings.getScale(sequenceName); 
        int pixels=(optimize)?width:bases;  
        int leftOffset=graphicsXoffset;
        int vizStart=settings.getSequenceViewPortStart(sequenceName);
        int vizEnd=settings.getSequenceViewPortEnd(sequenceName);
        if (orientation==Sequence.REVERSE) {
           if (vizEnd>sequenceEnd && !optimize) leftOffset+=(int)(scale*(vizEnd-sequenceEnd));
        } else {
           if (vizStart<sequenceStart && !optimize) leftOffset+=(int)(scale*(sequenceStart-vizStart));
        }         
        if (vizStart<sequenceStart) vizStart=sequenceStart;
        if (vizEnd>sequenceEnd) vizEnd=sequenceEnd;          
        if (optimize) {          
            int first[] = getScreenCoordinateFromGenomic(vizStart);
            int last[]  = getScreenCoordinateFromGenomic(vizEnd);
            int startX  = (first[0]<=last[0])?first[0]:last[0];
            int endX    = (first[0]<=last[0])?last[1]:first[1];
            graphics.fillRect(leftOffset+startX, graphicsYoffset, endX-startX+1, height);                                
        } else {
            graphics.fillRect(leftOffset, graphicsYoffset, (int)(pixels*scale), height);           
        }
        graphics.setTransform(saveTransform);     
    }      
   
//    ---------------   Functionality for the "Draw tool"   ------------------------------  
    
    private char currentValue=Character.MIN_VALUE;;
    private int currentX=0;
    private DNASequenceData buffer;
    private DNASequenceData original;
    private int startY=0; // the y-coordinate that drawing started at the current base
    private char bases[]=new char[]{'A','C','G','T'};
    private boolean editmode=false; // edit by typing at keybard
    private boolean paintmode=false; // edit by moving mouse up and down
    private boolean buttondown=false; // mousebutton has been pressed but not released
    private KeyAdapter keyadapter=new EditKeyAdapter();
    private FocusHandler focushandler=new FocusHandler();

    
   @Override
   public void drawEditOverlay(Graphics2D g, int start, int height, int width, int bases, int xoffset, int yoffset, int orientation) {
       if (editmode || buttondown) {
            int[] pos=getScreenCoordinateFromGenomic(currentX);
            int startPosition=pos[0]+bordersize; // position relative to component
            int endPosition=pos[1]+bordersize; // position relative to component.
            int basewidth=endPosition-startPosition+1;
            g.setColor(Color.WHITE);
            if (basewidth>=7) {
                g.drawRect(startPosition+1, 1, basewidth-2, height-1); // white
                g.setColor(Color.BLACK);
                g.drawRect(startPosition+2, 2, basewidth-4, height-3); // inner black
                g.drawRect(startPosition, 0, basewidth, height+1); // outer black
            } else g.drawRect(startPosition, 1, basewidth, height-1); // bounding box
       }
    } // end drawEditOverlay    

    /** Create a buffer containing a copy of the original sequence and display it */
    private void startEdit() {
         original=(DNASequenceData)sequencedata;
         buffer=(DNASequenceData)sequencedata.clone();
         sequencedata=buffer; // 'buffer' is not referenced in the engine. We switch the current 'reference' (sequencedata) to point to this buffer
    }

    /** Replace the original sequence with the buffer that has been updated by the user */
    private void endEdit() {
         if (!buffer.containsSameData(original)) {
            gui.updatePartialDataItem(featureName, sequenceName, null, buffer); // update the registered dataset with the new data from the buffer (this will update the whole dataset not just the single sequence).
          }
         sequencedata=original; // and point back to the (new) data now registered with the engine instead of the temporary buffer which is not registered
         buffer=null;
         repaintVisibleSequenceSegment();
         currentValue=Character.MIN_VALUE;
         currentX=0;
    }

    @Override
    public void mousePressed(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return;     
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
           buttondown=true;
           int[] range=getGenomicCoordinateFromScreenOffset(e.getX()-bordersize); // remember to subtract the border when calculating relative X-coordinate
           currentX=range[0];      
           startY=e.getY();
           Character cur=(buffer==null)?null:buffer.getValueAtGenomicPosition(currentX);
           currentValue=(cur!=null)?cur:Character.MIN_VALUE;
           repaintVisibleSequenceSegment();
       }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
       if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return; 
       if (gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) {
           buttondown=false;
           if (editmode) return;
           if (paintmode) {
              endEdit();
              paintmode=false;
           }     
       }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return; 
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        if (editmode) return;
        int y=e.getY();
        int[] range=getGenomicCoordinateFromScreenOffset(e.getX()-bordersize); // remember to subtract the border when calculating relative X-coordinate
        if (!paintmode) {
            startEdit();
            currentX=range[0];
            Character value=buffer.getValueAtGenomicPosition(currentX);
            if (value==null) return;
            currentValue=value;
        }
        paintmode=true;
        int newX=range[0];
        if (newX!=currentX) {
            currentX=newX;
            Character value=buffer.getValueAtGenomicPosition(currentX);
            if (value==null) return;
            currentValue=value;
            startY=y;
        }
        if (e.isControlDown()) { // always replace with N if CTRL is down
            currentValue=(e.isShiftDown())?'n':'N';
            buffer.setValueAtGenomicPosition(currentX,currentValue);
            repaintVisibleSequenceSegment(); // this is used instead of regular repaint() to avoid artifacts that appeared on the flanks around the sequence
        } else {
            int diff=(settings.getSequenceOrientation(sequenceName)==Sequence.DIRECT)?(y-startY):(startY-y);
            char newValue=currentValue;
            int baseindex=MotifLabEngine.getBaseIndex(currentValue);
            if (diff<-4) { // moving up
                baseindex--;
                if (baseindex<0) baseindex=3;
                startY=y;
                newValue=(e.isShiftDown())?Character.toLowerCase(bases[baseindex]):bases[baseindex];
            } else if (diff>4) { // moving down
                baseindex++;
                if (baseindex>=4) baseindex=0;
                startY=y; // update reference point
                newValue=(e.isShiftDown())?Character.toLowerCase(bases[baseindex]):bases[baseindex];
            }
            buffer.setValueAtGenomicPosition(currentX,newValue);
            repaintVisibleSequenceSegment(); // this is used instead of regular repaint() to avoid artifacts that appeared on the flanks around the sequence
            currentValue=newValue;
        }
    }


    /** removes keylistener, reinstalls old actions and exits editmode */
    private void cleanUpAfterEdit() {
        SequenceVisualizer seqviz=gui.getVisualizationPanel().getSequenceVisualizer(sequenceName);
        if (seqviz==null) return;
        this.removeKeyListener(keyadapter);
        this.removeFocusListener(focushandler);
        seqviz.setEnabledOnAllActions(true);  // reenable keyboard-responsive actions in SeqViz
        gui.getVisualizationPanel().setEnabledOnAllActions(true); // and VizPanel
        this.setFocusable(false);
        seqviz.requestFocusInWindow();
        editmode=false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (gui.getVisualizationPanel().isMouseEventsBlocked() || e.isPopupTrigger() || SwingUtilities.isRightMouseButton(e)) return;     
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        if (editmode) {
            this.requestFocusInWindow();
            return;
        }
        SequenceVisualizer seqviz=gui.getVisualizationPanel().getSequenceVisualizer(sequenceName);
        if (seqviz==null) return;
        int orientation=settings.getSequenceOrientation(sequenceName);
        int[] range=getGenomicCoordinateFromScreenOffset(e.getX()-bordersize); // remember to subtract the border when calculating relative X-coordinate
        currentX=(orientation==Sequence.DIRECT)?range[0]:range[1];
        editmode=true;
        startEdit();
        seqviz.setEnabledOnAllActions(false); // block keyboard-responsive actions in SeqViz
        gui.getVisualizationPanel().setEnabledOnAllActions(false); // and VizPanel
        this.setFocusable(true);
        this.requestFocusInWindow();
        this.addKeyListener(keyadapter);
        this.addFocusListener(focushandler);
        repaintVisibleSequenceSegment();
    }

    private class EditKeyAdapter extends KeyAdapter {
        @Override
        public void keyTyped(KeyEvent e) {
            boolean repaintAll=false;
            char base=e.getKeyChar();
            if (base==KeyEvent.CHAR_UNDEFINED) return;
            if (e.isShiftDown()) base=Character.toLowerCase(base); else base=Character.toUpperCase(base);
            int orientation=settings.getSequenceOrientation(sequenceName);
            if (orientation==Sequence.REVERSE) base=MotifLabEngine.reverseBase(base);
            buffer.setValueAtGenomicPosition(currentX,base);
            if (orientation == Sequence.DIRECT) currentX++; else currentX--;
            SequenceVisualizer seqviz=gui.getVisualizationPanel().getSequenceVisualizer(sequenceName);
            if (currentX>settings.getSequenceViewPortEnd(sequenceName)) {
                if (orientation==Sequence.DIRECT) seqviz.moveSequenceRight(0, false); else seqviz.moveSequenceLeft(0, false);
                repaintAll=true;
            } else if (currentX<settings.getSequenceViewPortStart(sequenceName)) {
                if (orientation==Sequence.DIRECT) seqviz.moveSequenceLeft(0, false); else seqviz.moveSequenceRight(0, false);
                repaintAll=true;
            }
            if (repaintAll) seqviz.repaint();
            else DataTrackVisualizer_Sequence.this.repaint();
        }

        @Override
        public void keyPressed(KeyEvent e) { // this is to handle LEFT and RIGHT keys (which do not report to keyTyped)
            boolean repaintAll=false;
            int orientation=settings.getSequenceOrientation(sequenceName);
            int keycode=e.getKeyCode();
            if (keycode==KeyEvent.VK_RIGHT) {
                if (orientation == Sequence.DIRECT) currentX++; else currentX--;
            } else if (keycode==KeyEvent.VK_LEFT) {
                if (orientation == Sequence.DIRECT) currentX--; else currentX++;   
            } else if (keycode==KeyEvent.VK_ENTER) {
                endEdit();
                cleanUpAfterEdit();
                DataTrackVisualizer_Sequence.this.repaint();
                return;
            }
            SequenceVisualizer seqviz=gui.getVisualizationPanel().getSequenceVisualizer(sequenceName);
            if (currentX>settings.getSequenceViewPortEnd(sequenceName)) {
                if (orientation==Sequence.DIRECT) seqviz.moveSequenceRight(0, false); else seqviz.moveSequenceLeft(0, false);
                repaintAll=true;
            } else if (currentX<settings.getSequenceViewPortStart(sequenceName)) {
                if (orientation==Sequence.DIRECT) seqviz.moveSequenceLeft(0, false); else seqviz.moveSequenceRight(0, false);
                repaintAll=true;
            }            
            if (repaintAll) seqviz.repaint();
            else DataTrackVisualizer_Sequence.this.repaint();            
        }

    }

    private class FocusHandler extends FocusAdapter {
        @Override
        public void focusLost(FocusEvent e) {
            if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
            if (!editmode) return;
            endEdit();
            cleanUpAfterEdit();
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (!gui.getSelectedTool().equals(MotifLabGUI.DRAW_TOOL)) return;
        if (!editmode) return;
        endEdit();
        cleanUpAfterEdit();
    }   
}
