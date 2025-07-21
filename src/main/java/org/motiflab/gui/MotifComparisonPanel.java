/*
 
 
 */

package org.motiflab.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.motiflab.engine.data.Data;
import org.motiflab.engine.MotifLabEngine;
import org.motiflab.engine.data.Region;
import org.motiflab.engine.data.Motif;
import org.motiflab.engine.data.MotifClassification;
import org.motiflab.engine.util.MotifComparator;

/**
 * This panel displays a table wherein all currently existing motifs are 
 * compared against a single target motif.
 * MotifComparisonPanel is used by the MotifComparisonDialog (stand-alone tool)
 * or in the "compare" tab of motif prompts.
 * @author kjetikl
 */
public class MotifComparisonPanel extends JPanel {

    protected MotifLabEngine engine;
    protected MotifLabGUI gui;
    private Motif targetMotif=null;
    private DefaultTableModel model;
    private JTable motifsTable;
    private MotifLogo logorenderer;
    private boolean isprocessing=false;
    private boolean isprocessingInterrupted=false;
    private MotifComparator motifComparator=null;
    private CompareAndAddMotifsSwingWorker worker=null;

    private GenericMotifBrowserPanel motifsPanel;
    private javax.swing.JPanel comparisonSettingsPanel;
    private javax.swing.JCheckBox alignMotifsCheckbox;
    private javax.swing.JComboBox motifComparatorCombobox;
    private JSearchTextField searchBox;
    private javax.swing.JProgressBar progressbar;
    private JDialog parentDialog; // this need not be set
    private boolean isModal=false;

    private DecimalFormat formatter=new DecimalFormat("#.###");    

    private static final int MOTIF_COLOR_COLUMN=0;
    private static final int MOTIF_NAME_COLUMN=1;
    private static final int MOTIF_SCORE_COLUMN=2;
    private static final int MOTIF_ORIENTATION_COLUMN=3;
    private static final int MOTIF_CLASS_COLUMN=4;
    private static final int MOTIF_LOGO_COLUMN=5;
    private static final int MOTIF_OFFSET_COLUMN=6;
      
    /**
     * Creates a new MotifComparisonPanel
     * @param gui
     * @param parentDialog An optional reference to a JDialog if the panel is to be displayed within one
     * @param target The target motif which all other known motifs should be compared to
      */
    public MotifComparisonPanel(MotifLabGUI gui, JDialog parentDialog, Motif target, boolean modal) {
        this.engine=gui.getEngine();
        this.gui=gui;
        this.isModal=modal;
        this.parentDialog=parentDialog;
        targetMotif=target;
        setupTableModel();      
        initComponents();
        motifComparatorCombobox.setModel(setupMotifComparators());
        progressbar.setVisible(false);
        alignMotifsCheckbox.setSelected(false); //
        logorenderer.setShowAligned(alignMotifsCheckbox.isSelected());
        alignMotifsCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean showAligned=alignMotifsCheckbox.isSelected();
                logorenderer.setShowAligned(showAligned);
                if (showAligned && targetMotif!=null) {
                    logorenderer.setHighlightSegment(targetMotif.getLength());
                } else logorenderer.setHighlightSegment(0);
                motifsTable.repaint();
            }
        });
        // -- now start comparison with 'default' comparator
        motifComparatorCombobox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                motifComparator=(MotifComparator)motifComparatorCombobox.getSelectedItem();
                motifComparatorCombobox.setToolTipText(motifComparator.getName()+"  ["+((motifComparator.isDistanceMetric()?"Distance metric":"Similarity metric"))+"]");             
                sortByScore(motifComparator.isDistanceMetric());
                compareToKnownMotifs(targetMotif);
            }
        });
        motifComparatorCombobox.setSelectedIndex(0); // select first comparator to start things off
    }

    /** resorts the table by score value either ascending or descending*/
    private void sortByScore(boolean ascending) {
       ArrayList<SortKey> list=new ArrayList<SortKey>(1);
       list.add(new SortKey(MOTIF_SCORE_COLUMN, (ascending)?SortOrder.ASCENDING:SortOrder.DESCENDING));
       motifsTable.getRowSorter().setSortKeys(list);
             
    }

    private void initComponents() {
        this.setLayout(new BorderLayout());
        searchBox=motifsPanel.getSearchField();
        searchBox.doNotFilterItem(targetMotif);
        progressbar=new JProgressBar();
        progressbar.setPreferredSize(new Dimension(40,20));
        comparisonSettingsPanel.add(new JLabel("      Compare using  "));
        motifComparatorCombobox=new JComboBox();
        comparisonSettingsPanel.add(motifComparatorCombobox);
        comparisonSettingsPanel.add(new JLabel("     "));
        alignMotifsCheckbox=new JCheckBox("Show alignment", false);
        comparisonSettingsPanel.add(alignMotifsCheckbox);
        comparisonSettingsPanel.add(new JLabel("     "));
        comparisonSettingsPanel.add(progressbar);        
        this.add(motifsPanel,BorderLayout.CENTER);
    }


   /** This sets up the combobox for selecting motif comparators
     */
    private DefaultComboBoxModel setupMotifComparators() {
        ArrayList<MotifComparator> comparators=engine.getAllMotifComparators();
        return new DefaultComboBoxModel(comparators.toArray());
    }
 
    @SuppressWarnings("unchecked")
    private void setupTableModel() {
        model=new DefaultTableModel(null,new String[]{" ","Motif","Score","\u00B1","Class","Logo","Offset"}) {
            @Override
            public Class getColumnClass(int col) {
                switch (col) {
                    case MOTIF_COLOR_COLUMN: return Boolean.class;
                    case MOTIF_NAME_COLUMN: return Motif.class;
                    case MOTIF_SCORE_COLUMN: return Double.class;
                    case MOTIF_ORIENTATION_COLUMN: return Integer.class;
                    case MOTIF_CLASS_COLUMN: return String.class;
                    case MOTIF_LOGO_COLUMN: return Motif.class;
                    case MOTIF_OFFSET_COLUMN: return Integer.class;
                    default: return Object.class;
                }
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
            
        };
        motifsPanel=new GenericMotifBrowserPanel(gui, model, true, isModal);
        motifsPanel.setCustomTooltipForModelColumn(new CompareTooltip(gui.getVisualizationSettings()), MOTIF_LOGO_COLUMN); // 
        motifsTable=motifsPanel.getTable();        
        VisualizationSettings settings=gui.getVisualizationSettings();
        Color [] basecolors=new Color[]{settings.getBaseColor('A'),settings.getBaseColor('C'),settings.getBaseColor('G'),settings.getBaseColor('T')};   
        int rowheight=motifsTable.getRowHeight();
        logorenderer=new LogoRenderer(basecolors,(int)(rowheight*1.25));
        logorenderer.setStrandColumn(MOTIF_ORIENTATION_COLUMN);
        logorenderer.setOffsetColumn(MOTIF_OFFSET_COLUMN);
        logorenderer.setUseAntialias(gui.getVisualizationSettings().useMotifAntialiasing());
        CellRenderer_misc miscRenderer = new CellRenderer_misc();        
        motifsTable.getColumn(" ").setMinWidth(20);  
        motifsTable.getColumn(" ").setMaxWidth(20);  
        motifsTable.getColumn("\u00B1").setMinWidth(20); // orientation
        motifsTable.getColumn("\u00B1").setMaxWidth(20);  // orientation
        motifsTable.getColumn("Score").setMaxWidth(100); 
        motifsTable.getColumn("Class").setMaxWidth(70); 
        motifsTable.getColumn("Logo").setPreferredWidth(200); // Logo
        motifsTable.getColumn("Motif").setCellRenderer(new CellRenderer_Motif());
        motifsTable.getColumn("Score").setCellRenderer(new CellRenderer_Score());
        motifsTable.getColumn("\u00B1").setCellRenderer(miscRenderer);
        motifsTable.getColumn("Class").setCellRenderer(miscRenderer);
        motifsTable.getColumn("Logo").setCellRenderer(logorenderer);
        motifsTable.getColumn(" ").setCellRenderer(new FeatureColorCellRenderer(gui));   
        motifsTable.getColumnModel().removeColumn(motifsTable.getColumn("Offset"));
        ((TableRowSorter)motifsTable.getRowSorter()).setComparator(MOTIF_NAME_COLUMN, null); // use default comparator for this column in sorting (to avoid sorting by motif logo length which is the default in Generic Motif Browser Panel)        
        comparisonSettingsPanel=motifsPanel.getControlsPanel();          
        motifsTable.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_PLUS || e.getKeyCode()==KeyEvent.VK_ADD) {
                    int newheight=motifsTable.getRowHeight()+1;
                    if (newheight>80) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    motifsTable.setRowHeight(newheight);
                } else if (e.getKeyCode()==KeyEvent.VK_MINUS || e.getKeyCode()==KeyEvent.VK_SUBTRACT) {
                    int newheight=motifsTable.getRowHeight()-1;
                    if (newheight<8) return;
                    logorenderer.setFontHeight((int)(newheight*1.25));
                    motifsTable.setRowHeight(newheight);
                }
            }
        });
        ActionListener createMapListener=new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isProcessing()) return; // do not proceed if it has not finished
                HashMap<String,Double> values=new HashMap<String,Double>(motifsTable.getRowCount());
                for (int i=0;i<motifsTable.getRowCount();i++) {
                    Motif motif=(Motif)motifsTable.getValueAt(i, MOTIF_NAME_COLUMN);
                    Double score=(Double)motifsTable.getValueAt(i, MOTIF_SCORE_COLUMN);
                    if (!score.isInfinite() && !score.isNaN()) values.put(motif.getName(),score);
                }
                gui.promptAndCreateMotifNumericMap(values);
            }
        };
        JPopupMenu contextMenu=motifsPanel.getContextMenu();
        if (contextMenu instanceof GenericMotifBrowserPanelContextMenu) ((GenericMotifBrowserPanelContextMenu)contextMenu).addExtraMenuItem("Create Numeric Map", false, createMapListener);
    }

  /** Cancels the current comparison process.
   *  A call to this method is synchronous and will not return until
   *  the process is finished or properly canceled.
   */
  public void cancelProcessingAndWait() {
      isprocessingInterrupted=true;
      if (worker!=null) {
          while (!worker.isDone()) {
               Thread.yield();
          }
      }
      isprocessing=false;
      isprocessingInterrupted=false;
      searchBox.setEnabled(true);
  }

  /** Returns TRUE if the panel is currently involved in calculating comparisons */
  public boolean isProcessing() {
      return isprocessing;
  }

  /** Does the comparison in an off-EDT thread */
  private void compareToKnownMotifs(final Motif target) {
        if (worker!=null && !worker.isDone()) cancelProcessingAndWait();
        searchBox.setEnabled(false);
        while (model.getRowCount()>0) model.removeRow(0); // remove all current rows
        logorenderer.setLogoSize(0);
        logorenderer.setDefaultOffset(0);
        isprocessingInterrupted=false;
        if (parentDialog!=null && parentDialog instanceof MotifComparisonDialog) {
            ((MotifComparisonDialog)parentDialog).notifyComparisonStarted();
        }
        progressbar.setVisible(true);
        worker=new CompareAndAddMotifsSwingWorker(target);
        isprocessing=true;
        progressbar.setValue(0);
        progressbar.setVisible(true);
        worker.execute();
    }


 private void addMotifToList(final Motif motif, final double score,final int orientation, final int offset, final int progress, final CompareAndAddMotifsSwingWorker worker) {
     Runnable runner=new Runnable() {
            @Override
            public void run() {
                if (!worker.isStillActive) return;
                if (motif!=null) {
                    String motifclass=motif.getClassification();
                    if (motifclass==null) motifclass="Unknown";
                    model.addRow(new Object[]{Boolean.TRUE,motif,new Double(score),orientation, motifclass, motif, offset});
                    int maxsize=offset+motif.getLength();
                    int defaultoffset=-offset;
                    if (defaultoffset>logorenderer.getDefaultOffset()) logorenderer.setDefaultOffset(defaultoffset);
                    if (maxsize>logorenderer.getLogoSize()) logorenderer.setLogoSize(maxsize);
                }
                progressbar.setValue(progress);
            }
        };
     SwingUtilities.invokeLater(runner);
 }


 private class CompareAndAddMotifsSwingWorker extends SwingWorker<Boolean, Void> {
            boolean isStillActive=true; // still active means it was not cancelled
            Motif target;

            public CompareAndAddMotifsSwingWorker(Motif targetmotif) {
                target=targetmotif;
            }

            @Override
            public Boolean doInBackground() {
                addMotifToList(targetMotif, (motifComparator.isDistanceMetric())?Double.NEGATIVE_INFINITY:Double.POSITIVE_INFINITY, Region.DIRECT,0,0,this); // ensures that the target is always listed on top always
                ArrayList<Data>motifs=engine.getAllDataItemsOfType(Motif.class);
                int size=motifs.size();
                int counter=0;
                for (Data motif:motifs) {
                    counter++;
                    if (isprocessingInterrupted) {
                        isprocessing=false;
                        isStillActive=false;
                        return Boolean.TRUE;
                    }
                    if (counter%100==0) Thread.yield();
                    if (motif==target || motif.getName().equals(target.getName())) continue;

                    double[] comparison=motifComparator.compareMotifs(target, (Motif)motif);
                    // System.err.println("Comparison:  "+comparison[0]+","+comparison[1]+","+comparison[2]);
                    double bestscore=comparison[0];
                    int orientation=Region.DIRECT;
                    int motifoffset=(int)comparison[2];
                    if (comparison[1]==-1) orientation=Region.REVERSE;
                    if (bestscore!=Double.MAX_VALUE && bestscore!=-Double.MAX_VALUE) { 
                        addMotifToList((Motif)motif, bestscore, orientation, motifoffset, (int)((float)counter/(float)size*100f),this);
                    } else addMotifToList(null, bestscore, orientation, motifoffset, (int)((float)counter/(float)size*100f),this); // this is just to update progress. No motif is really added
                }
                return Boolean.TRUE;
            } // end doInBackground
            @Override
            public void done() { // this method is invoked on the EDT!
                isprocessing=false;
                searchBox.setEnabled(true);
                if (isStillActive) { // if still active then it was allowed to complete
                    progressbar.setValue(0);
                    progressbar.setVisible(false);
                    if (parentDialog!=null && parentDialog instanceof MotifComparisonDialog) {
                        ((MotifComparisonDialog)parentDialog).notifyComparisonCompleted();
                    }
                }
            }
        }; // end of SwingWorker class


private class CellRenderer_Motif extends DefaultTableCellRenderer {
    public CellRenderer_Motif() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEFT);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof Motif) {
           setText(((Motif)value).getPresentationName());
           setToolTipText(((Motif)value).getLongName());
       }
       else super.setValue(value);
    }
}// end class CellRenderer_Motif
private class CellRenderer_Score extends DefaultTableCellRenderer {
    public CellRenderer_Score() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
    }
    @Override
    public void setValue(Object value) {
       if (value instanceof Double && ((Double)value).isInfinite()) {
           setText("TARGET");
       }
       else super.setValue(value);
       }
}// end class CellRenderer_Motif
private class CellRenderer_misc extends DefaultTableCellRenderer {
    public CellRenderer_misc() {
       super();
       this.setHorizontalAlignment(DefaultTableCellRenderer.LEADING);
    }
    @Override
    public void setValue(Object value) {
       if (value!=null && value instanceof Integer) {
           if (((Integer)value)==Region.DIRECT) {
               setText("+");
               setForeground(java.awt.Color.GREEN);
               setToolTipText("Direct orientation");
           } else {
              setText("-");
              setForeground(java.awt.Color.RED);
              setToolTipText("Reverse orientation");
           }
       } else if (value!=null && value instanceof String) {
           setText((String)value);
           setForeground(java.awt.Color.BLACK);
           setToolTipText(MotifClassification.getFullLevelsStringAsHTML((String)value));
       }
       }
}// end class CellRenderer_Orientation


private class FeatureColorCellRenderer extends DefaultTableCellRenderer {
    
    SimpleDataPanelIcon selectedicon=new SimpleDataPanelIcon(12,12,SimpleDataPanelIcon.COLOR_ICON,SimpleDataPanelIcon.SIMPLE_BORDER, null);
    VisualizationSettings settings;

    public FeatureColorCellRenderer(MotifLabGUI gui) {
         super();
         settings=gui.getVisualizationSettings();
         setIcon(selectedicon);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column); // this is necessary in order to get correct alternating row rendering 
        Motif motif=(Motif)table.getValueAt(row, MOTIF_NAME_COLUMN);
        String motifname=motif.getName();
        selectedicon.setForegroundColor(settings.getFeatureColor(motifname));
        selectedicon.setBorderColor((settings.isRegionTypeVisible(motifname))?Color.BLACK:Color.LIGHT_GRAY);
        setIcon(selectedicon);
        return this;
    }           
} 


 private class LogoRenderer extends MotifLogo {
    public LogoRenderer(Color[] basecolors, int fontheight) {
        super(basecolors,fontheight);
    }  
    @Override
    public String getMotifInfoTooltip() {
        Point p=motifsTable.getMousePosition();
        if (p!=null) {      
            int tablerow=motifsTable.rowAtPoint(p);
            int modelrow=motifsTable.convertRowIndexToModel(tablerow);   
            Object object=model.getValueAt(modelrow, MOTIF_LOGO_COLUMN);
            if (object instanceof Motif) {
                return "Compare "+targetMotif.getName()+" to "+((Motif)object).getName();
            }
        }
        return "";
    }    
 }

private class CompareTooltip extends MotifTooltip {
    
    private Motif[] motifs; // first should be target, the second should be the other motif
    private int[] orientations;
    private int[] offsets;
    
    public CompareTooltip(VisualizationSettings settings) {
        super(settings);
    }

    @Override
    public void setComponent(JComponent c) {    
        super.setComponent(c);
        Point p=motifsTable.getMousePosition();
        if (p!=null) {      
            int tablerow=motifsTable.rowAtPoint(p);
            int modelrow=motifsTable.convertRowIndexToModel(tablerow);   
            Object object=model.getValueAt(modelrow, MOTIF_LOGO_COLUMN);
            if (object instanceof Motif) {
               Motif otherMotif=(Motif)object;
               int orientation=(Integer)model.getValueAt(modelrow, MOTIF_ORIENTATION_COLUMN);
               int offset=(Integer)model.getValueAt(modelrow, MOTIF_OFFSET_COLUMN); // this can be negative
               int targetOffset=0;
               if (offset<0) {
                  targetOffset=-offset;
                  offset=0;
               }
               setMotifs(new Motif[]{targetMotif,otherMotif}, new int[]{1,orientation}, new int[]{targetOffset,offset});                               
             } else {
                setMotifs(new Motif[]{targetMotif}, new int[]{1}, new int[]{0});                
            }
        } else {
            setMotifs(new Motif[]{targetMotif}, new int[]{1}, new int[]{0});
        }
     }
    
        
    public void setMotifs(Motif[] motifs,int[] orientations, int[] offsets) {
        this.motifs=motifs;
        this.orientations=orientations;
        this.offsets=offsets;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected void paintComponent(Graphics g) {       
       super.paintComponent(g);
       int yoffset=positionTextHeight;
       int totalsize=0;
       for (int i=0;i<motifs.length;i++) { // set 'longest' variable
           int total=motifs[i].getLength()+offsets[i];
           if (total>totalsize) totalsize=total;
       }  
       // paint info for each motif
       for (int i=0;i<motifs.length;i++) { 
           Motif motif=motifs[i];
           g.setColor(Color.WHITE);
           g.fillRect(1, yoffset, lineheight, lineheight);
           g.setColor(settings.getFeatureColor(motif.getName()));
           g.fillRect(6, yoffset+5, 10, 10);
           g.setColor(Color.BLACK);
           g.drawRect(6, yoffset+5, 10, 10);
           g.drawRect(1, yoffset, lineheight, lineheight);

           int textoffset=paintMatchLogo(g, motif, null, xoffset, yoffset, totalsize, offsets[i], Region.DIRECT, orientations[i]);
           String motiftext=getMotifString(motif);
           if (motiftext!=null) {
                g.setColor(Color.BLACK);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawString(motiftext, textoffset+5, yoffset+lineheight-5);
                ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            }
           yoffset+=lineheight;
       } 
    }
    
    @Override
    public Dimension getPreferredSize() {         
        Dimension dim=super.getPreferredSize();     
        int width=dim.width;
        FontMetrics metrics=getFontMetrics(font);
        int charwidth=getFontMetrics(logofont).charWidth('G');
        int[] stringlengths=new int[motifs.length];
        int totalsize=0;
        for (int i=0;i<motifs.length;i++) { // determine width of description string (excluding logo)
            String string=getMotifString(motifs[i]);
            stringlengths[i]=metrics.stringWidth(string);
            int total=motifs[i].getLength()+offsets[i];
            if (total>totalsize) totalsize=total;            
        }
        int drawingsize=xoffset+totalsize*charwidth+10; //
        for (int i=0;i<motifs.length;i++) {
             int linewidth=stringlengths[i]+drawingsize;
             if (linewidth>width) width=linewidth;
        }
         
        Dimension newsize=new Dimension(width,lineheight*motifs.length+positionTextHeight+2);
        return newsize;
    }    
    
    private String getMotifString(Motif motif) {
        StringBuilder string=new StringBuilder();
        string.append(motif.getName());
        string.append(", ");       
        string.append(motif.getLength());
        formatter.setMinimumFractionDigits(1);
        formatter.setMaximumFractionDigits(1);
        string.append(" bp, GC=");
        string.append(formatter.format(motif.getGCcontent()*100.0));
        string.append("%, IC=");
        formatter.setMinimumFractionDigits(3);
        formatter.setMaximumFractionDigits(3);
        string.append(formatter.format(motif.getICcontent()));
        return string.toString();        
    }
       
}

}
